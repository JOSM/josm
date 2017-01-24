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
     * + xxhash32 checksum creation for headers, content, blocks
     * + block dependence
     */

    private static final int DEFAULT_BLOCK_SIZE = 4096 * 1024;
    private static final List<Integer> BLOCK_SIZES = Arrays.asList(64 * 1024, 256 * 1024, 1024 * 1024,
        DEFAULT_BLOCK_SIZE);
    private static final byte[] END_MARK = new byte[4];

    // used in one-arg write method
    private final byte[] oneByte = new byte[1];

    private final byte[] blockData;
    private final OutputStream out;
    private boolean finished = false;
    private int currentIndex = 0;

    /**
     * Constructs a new output stream that compresses data using the
     * LZ4 frame format using the default block size of 4MB.
     * @param out the OutputStream to which to write the compressed data
     * @throws IOException if writing the signature fails
     */
    public FramedLZ4CompressorOutputStream(OutputStream out) throws IOException {
        this(out, DEFAULT_BLOCK_SIZE);
    }

    /**
     * Constructs a new output stream that compresses data using the
     * LZ4 frame format using the given block size.
     * @param out the OutputStream to which to write the compressed data
     * @param blockSize block size, one of 64 KB, 256 KB, 1 MB or 4 MB.
     * @throws IOException if writing the signature fails
     * @throws IllegalArgumentException if the block size is not supported
     */
    public FramedLZ4CompressorOutputStream(OutputStream out, int blockSize) throws IOException {
        if (!BLOCK_SIZES.contains(blockSize)) {
            throw new IllegalArgumentException("Unsupported block size");
        }
        blockData = new byte[blockSize];
        this.out = out;
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
        out.write(FramedLZ4CompressorInputStream.SUPPORTED_VERSION
            | FramedLZ4CompressorInputStream.BLOCK_INDEPENDENCE_MASK);
        out.write(BLOCK_SIZES.indexOf(blockData.length) << 4);
        out.write(0); // TODO header checksum
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
        } else {
            ByteUtils.toLittleEndian(out, b.length, 4);
            out.write(b);
        }
        // TODO block checksum
        currentIndex = 0;
    }

    private void writeTrailer() throws IOException {
        out.write(END_MARK);
        // TODO content checksum
    }

}

