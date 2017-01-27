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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.compress.compressors.CompressorOutputStream;
import org.apache.commons.compress.utils.ByteUtils;

/**
 * CompressorOutputStream for the LZ4 frame format.
 *
 * <p>Based on the "spec" in the version "1.5.1 (31/03/2015)"</p>
 *
 * @see <a href="http://lz4.github.io/lz4/lz4_Frame_format.html">LZ4 Frame Format Description</a>
 * @since 1.14
 * @NotThreadSafe
 */
public class FramedLZ4CompressorOutputStream extends CompressorOutputStream {
    /*
     * TODO before releasing 1.14:
     *
     * + block dependence
     */

    private static final byte[] END_MARK = new byte[4];

    // used in one-arg write method
    private final byte[] oneByte = new byte[1];

    private final byte[] blockData;
    private final OutputStream out;
    private final Parameters params;
    private boolean finished = false;
    private int currentIndex = 0;

    // used for frame header checksum and content checksum, if requested
    private final XXHash32 contentHash = new XXHash32();
    // used for block checksum, if requested
    private final XXHash32 blockHash;

    /**
     * The block sizes supported by the format.
     */
    public enum BlockSize {
        /** Block size of 64K */
        K64(64 * 1024, 0),
        /** Block size of 256K */
        K256(256 * 1024, 1),
        /** Block size of 1M */
        M1(1024 * 1024, 2),
        /** Block size of 4M */
        M4(1024 * 1024, 4);

        private final int size, index;
        private BlockSize(int size, int index) {
            this.size = size;
            this.index = index;
        }
        int getSize() {
            return size;
        }
        int getIndex() {
            return index;
        }
    }

    /**
     * Parameters of the LZ4 frame format.
     */
    public static class Parameters {
        private final BlockSize blockSize;
        private final boolean withContentChecksum, withBlockChecksum;

        /**
         * The default parameters of 4M block size, enabled content
         * checksum, disabled block checksums and independent blocks.
         *
         * <p>This matches the defaults of the lz4 command line utility.</p>
         */
        public static Parameters DEFAULT = new Parameters(BlockSize.M4, true, false);

        /**
         * Sets up custom parameters for the LZ4 stream.
         * @param blockSize the size of a single block.
         * @param withContentChecksum whether to write a content checksum
         * @param withBlockChecksum whether to write a block checksum.
         * Note that block checksums are not supported by the lz4
         * command line utility
         */
        public Parameters(BlockSize blockSize, boolean withContentChecksum, boolean withBlockChecksum) {
            this.blockSize = blockSize;
            this.withContentChecksum = withContentChecksum;
            this.withBlockChecksum = withBlockChecksum;
        }
        @Override
        public String toString() {
            return "LZ4 Parameters with BlockSize " + blockSize + ", withContentChecksum " + withContentChecksum
                + ", withBlockChecksum " + withBlockChecksum;
        }
    }

    /**
     * Constructs a new output stream that compresses data using the
     * LZ4 frame format using the default block size of 4MB.
     * @param out the OutputStream to which to write the compressed data
     * @throws IOException if writing the signature fails
     */
    public FramedLZ4CompressorOutputStream(OutputStream out) throws IOException {
        this(out, Parameters.DEFAULT);
    }

    /**
     * Constructs a new output stream that compresses data using the
     * LZ4 frame format using the given block size.
     * @param out the OutputStream to which to write the compressed data
     * @param params the parameters to use
     * @throws IOException if writing the signature fails
     */
    public FramedLZ4CompressorOutputStream(OutputStream out, Parameters params) throws IOException {
        this.params = params;
        blockData = new byte[params.blockSize.getSize()];
        this.out = out;
        blockHash = params.withBlockChecksum ? new XXHash32() : null;
        out.write(FramedLZ4CompressorInputStream.LZ4_SIGNATURE);
        writeFrameDescriptor();
    }

    @Override
    public void write(int b) throws IOException {
        oneByte[0] = (byte) (b & 0xff);
        write(oneByte);
    }

    @Override
    public void write(byte[] data, int off, int len) throws IOException {
        contentHash.update(data, off, len);
        if (currentIndex + len > blockData.length) {
            flushBlock();
            while (len > blockData.length) {
                System.arraycopy(data, off, blockData, 0, blockData.length);
                off += blockData.length;
                len -= blockData.length;
                currentIndex = blockData.length;
                flushBlock();
            }
        }
        System.arraycopy(data, off, blockData, currentIndex, len);
        currentIndex += len;
    }

    @Override
    public void close() throws IOException {
        finish();
        out.close();
    }

    /**
     * Compresses all remaining data and writes it to the stream,
     * doesn't close the underlying stream.
     * @throws IOException if an error occurs
     */
    public void finish() throws IOException {
        if (!finished) {
            if (currentIndex > 0) {
                flushBlock();
            }
            writeTrailer();
            finished = true;
        }
    }

    private void writeFrameDescriptor() throws IOException {
        int flags = FramedLZ4CompressorInputStream.SUPPORTED_VERSION
            | FramedLZ4CompressorInputStream.BLOCK_INDEPENDENCE_MASK;
        if (params.withContentChecksum) {
            flags |= FramedLZ4CompressorInputStream.CONTENT_CHECKSUM_MASK;
        }
        if (params.withBlockChecksum) {
            flags |= FramedLZ4CompressorInputStream.BLOCK_CHECKSUM_MASK;
        }
        out.write(flags);
        contentHash.update(flags);
        int bd = params.blockSize.getIndex() << 4;
        out.write(bd);
        contentHash.update(bd);
        out.write((int) ((contentHash.getValue() >> 8) & 0xff));
        contentHash.reset();
    }

    private void flushBlock() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (OutputStream o = new BlockLZ4CompressorOutputStream(baos)) {
            o.write(blockData, 0, currentIndex);
        }
        byte[] b = baos.toByteArray();
        if (b.length > currentIndex) { // compression increased size, maybe beyond blocksize
            ByteUtils.toLittleEndian(out, currentIndex | FramedLZ4CompressorInputStream.UNCOMPRESSED_FLAG_MASK,
                4);
            out.write(blockData, 0, currentIndex);
            if (params.withBlockChecksum) {
                blockHash.update(blockData, 0, currentIndex);
            }
        } else {
            ByteUtils.toLittleEndian(out, b.length, 4);
            out.write(b);
            if (params.withBlockChecksum) {
                blockHash.update(b, 0, b.length);
            }
        }
        if (params.withBlockChecksum) {
            ByteUtils.toLittleEndian(out, blockHash.getValue(), 4);
            blockHash.reset();
        }
        currentIndex = 0;
    }

    private void writeTrailer() throws IOException {
        out.write(END_MARK);
        ByteUtils.toLittleEndian(out, contentHash.getValue(), 4);
    }

}

