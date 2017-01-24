/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.commons.compress.compressors.lz4;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.Arrays;

import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.utils.BoundedInputStream;
import org.apache.commons.compress.utils.ByteUtils;
import org.apache.commons.compress.utils.IOUtils;

/**
 * CompressorInputStream for the LZ4 frame format.
 *
 * <p>Based on the "spec" in the version "1.5.1 (31/03/2015)"</p>
 *
 * @see <a href="http://lz4.github.io/lz4/lz4_Frame_format.html">LZ4 Frame Format Description</a>
 * @since 1.14
 * @NotThreadSafe
 */
public class FramedLZ4CompressorInputStream extends CompressorInputStream {
    /*
     * TODO before releasing 1.14:
     *
     * + xxhash32 checksum validation
     * + skippable frames
     * + decompressConcatenated
     * + block dependence
     */

    // used by FramedLZ4CompressorOutputStream as well
    static final byte[] LZ4_SIGNATURE = new byte[] { //NOSONAR
        4, 0x22, 0x4d, 0x18
    };

    static final int VERSION_MASK = 0xC0;
    static final int SUPPORTED_VERSION = 0x40;
    static final int BLOCK_INDEPENDENCE_MASK = 0x20;
    static final int BLOCK_CHECKSUM_MASK = 0x10;
    static final int CONTENT_SIZE_MASK = 0x08;
    static final int CONTENT_CHECKSUM_MASK = 0x04;
    static final int BLOCK_MAX_SIZE_MASK = 0x70;
    static final int UNCOMPRESSED_FLAG_MASK = 0x80000000;

    // used in no-arg read method
    private final byte[] oneByte = new byte[1];

    private final ByteUtils.ByteSupplier supplier = new ByteUtils.ByteSupplier() {
        @Override
        public int getAsByte() throws IOException {
            return readOneByte();
        }
    };

    private final InputStream in;

    private boolean expectBlockChecksum;
    private boolean expectContentSize;
    private boolean expectContentChecksum;

    private InputStream currentBlock;
    private boolean endReached, inUncompressed;

    /**
     * Creates a new input stream that decompresses streams compressed
     * using the LZ4 frame format.
     * @param in  the InputStream from which to read the compressed data
     * @throws IOException if reading fails
     */
    public FramedLZ4CompressorInputStream(InputStream in) throws IOException {
        this.in = in;
        readSignature();
        readFrameDescriptor();
        nextBlock();
    }

    /** {@inheritDoc} */
    @Override
    public int read() throws IOException {
        return read(oneByte, 0, 1) == -1 ? -1 : oneByte[0] & 0xFF;
    }

    /** {@inheritDoc} */
    @Override
    public void close() throws IOException {
        if (currentBlock != null) {
            currentBlock.close();
            currentBlock = null;
        }
        in.close();
    }

    /** {@inheritDoc} */
    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
        if (endReached) {
            return -1;
        }
        int r = readOnce(b, off, len);
        if (r == -1) {
            nextBlock();
            if (!endReached) {
                r = readOnce(b, off, len);
            }
        }
        return r;
    }

    private void readSignature() throws IOException {
        final byte[] b = new byte[4];
        final int read = IOUtils.readFully(in, b);
        count(read);
        if (4 != read || !matches(b, 4)) {
            throw new IOException("Not a LZ4 frame stream");
        }
    }

    private void readFrameDescriptor() throws IOException {
        int flags = readOneByte();
        if (flags == -1) {
            throw new IOException("Premature end of stream while reading frame flags");
        }
        if ((flags & VERSION_MASK) != SUPPORTED_VERSION) {
            throw new IOException("Unsupported version " + (flags >> 6));
        }
        if ((flags & BLOCK_INDEPENDENCE_MASK) == 0) {
            throw new IOException("Block dependence is not supported");
        }
        expectBlockChecksum = (flags & BLOCK_CHECKSUM_MASK) != 0;
        expectContentSize = (flags & CONTENT_SIZE_MASK) != 0;
        expectContentChecksum = (flags & CONTENT_CHECKSUM_MASK) != 0;
        if (readOneByte() == -1) { // max size is irrelevant for this implementation
            throw new IOException("Premature end of stream while reading frame BD byte");
        }
        if (expectContentSize) { // for now we don't care, contains the uncompressed size
            int skipped = (int) IOUtils.skip(in, 8);
            count(skipped);
            if (8 != skipped) {
                throw new IOException("Premature end of stream while reading content size");
            }
        }
        if (readOneByte() == -1) { // partial hash of header. not supported, yet
            throw new IOException("Premature end of stream while reading frame header checksum");
        }
    }

    private void nextBlock() throws IOException {
        maybeFinishCurrentBlock();
        long len = ByteUtils.fromLittleEndian(supplier, 4);
        boolean uncompressed = (len & UNCOMPRESSED_FLAG_MASK) != 0;
        int realLen = (int) (len & (~UNCOMPRESSED_FLAG_MASK));
        if (realLen == 0) {
            endReached = true;
            verifyContentChecksum();
            return;
        }
        InputStream capped = new BoundedInputStream(in, realLen);
        if (uncompressed) {
            inUncompressed = true;
            currentBlock = capped;
        } else {
            inUncompressed = false;
            currentBlock = new BlockLZ4CompressorInputStream(capped);
        }
    }

    private void maybeFinishCurrentBlock() throws IOException {
        if (currentBlock != null) {
            currentBlock.close();
            currentBlock = null;
            if (expectBlockChecksum) {
                int skipped = (int) IOUtils.skip(in, 4);
                count(skipped);
                if (4 != skipped) {
                    throw new IOException("Premature end of stream while reading block checksum");
                }
            }
        }
    }

    private void verifyContentChecksum() throws IOException {
        if (expectContentChecksum) {
            int skipped = (int) IOUtils.skip(in, 4);
            count(skipped);
            if (4 != skipped) {
                throw new IOException("Premature end of stream while reading content checksum");
            }
        }
    }

    private int readOneByte() throws IOException {
        final int b = in.read();
        if (b != -1) {
            count(1);
            return b & 0xFF;
        }
        return -1;
    }

    private int readOnce(byte[] b, int off, int len) throws IOException {
        if (inUncompressed) {
            int cnt = currentBlock.read(b, off, len);
            count(cnt);
            return cnt;
        } else {
            BlockLZ4CompressorInputStream l = (BlockLZ4CompressorInputStream) currentBlock;
            long before = l.getBytesRead();
            int cnt = currentBlock.read(b, off, len);
            count(l.getBytesRead() - before);
            return cnt;
        }
    }

    /**
     * Checks if the signature matches what is expected for a .lz4 file.
     *
     * <p>.lz4 files start with a four byte signature.</p>
     *
     * @param signature the bytes to check
     * @param length    the number of bytes to check
     * @return          true if this is a .sz stream, false otherwise
     */
    public static boolean matches(final byte[] signature, final int length) {

        if (length < LZ4_SIGNATURE.length) {
            return false;
        }

        byte[] shortenedSig = signature;
        if (signature.length > LZ4_SIGNATURE.length) {
            shortenedSig = new byte[LZ4_SIGNATURE.length];
            System.arraycopy(signature, 0, shortenedSig, 0, LZ4_SIGNATURE.length);
        }

        return Arrays.equals(shortenedSig, LZ4_SIGNATURE);
    }
}
