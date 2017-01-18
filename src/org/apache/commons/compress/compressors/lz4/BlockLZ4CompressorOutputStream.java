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
import java.io.OutputStream;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.compress.compressors.CompressorOutputStream;
import org.apache.commons.compress.compressors.lz77support.LZ77Compressor;
import org.apache.commons.compress.compressors.lz77support.Parameters;
import org.apache.commons.compress.utils.ByteUtils;

/**
 * CompressorOutputStream for the LZ4 block format.
 *
 * @see <a href="http://lz4.github.io/lz4/lz4_Block_format.html">LZ4 Block Format Description</a>
 * @since 1.14
 */
public class BlockLZ4CompressorOutputStream extends CompressorOutputStream {

    private static final int MIN_BACK_REFERENCE_LENGTH = 4;
    private static final int MIN_LENGTH_OF_LAST_LITERAL = 5;
    private static final int MIN_OFFSET_OF_LAST_BACK_REFERENCE = 12;

    /*

      The LZ4 block format has a few properties that make it less
      straight-forward than one would hope:

      * literal blocks and back-references must come in pairs (except
        for the very last literal block), so consecutive literal
        blocks created by the compressor must be merged into a single
        block.

      * the start of a literal/back-reference pair contains the length
        of the back-reference (at least some part of it) so we can't
        start writing the literal before we know how long the next
        back-reference is going to be.

      * there is a special rule for the final blocks

        > There are specific parsing rules to respect in order to remain
        > compatible with assumptions made by the decoder :
        >
        >     1. The last 5 bytes are always literals
        >
        >     2. The last match must start at least 12 bytes before end of
        >        block. Consequently, a block with less than 13 bytes cannot be
        >        compressed.

        which means any back-reference may need to get rewritten as a
        literal block unless we know the next block is at least of
        length 5 and the sum of this block's length and offset and the
        next block's length is at least twelve.

    */

    private final LZ77Compressor compressor;
    private final OutputStream os;

    // used in one-arg write method
    private final byte[] oneByte = new byte[1];

    private boolean finished = false;

    /**
     * Creates a new LZ4 output stream.
     *
     * @param os
     *            An OutputStream to read compressed data from
     *
     * @throws IOException if reading fails
     */
    public BlockLZ4CompressorOutputStream(final OutputStream os) throws IOException {
        this.os = os;
        int maxLen = BlockLZ4CompressorInputStream.WINDOW_SIZE - 1;
        compressor = new LZ77Compressor(new Parameters(BlockLZ4CompressorInputStream.WINDOW_SIZE,
            MIN_BACK_REFERENCE_LENGTH, maxLen, maxLen, maxLen),
            new LZ77Compressor.Callback() {
                public void accept(LZ77Compressor.Block block) throws IOException {
                    //System.err.println(block);
                    if (block instanceof LZ77Compressor.LiteralBlock) {
                        addLiteralBlock((LZ77Compressor.LiteralBlock) block);
                    } else if (block instanceof LZ77Compressor.BackReference) {
                        addBackReference((LZ77Compressor.BackReference) block);
                    } else if (block instanceof LZ77Compressor.EOD) {
                        writeFinalLiteralBlock();
                    }
                }
            });
    }

    @Override
    public void write(int b) throws IOException {
        oneByte[0] = (byte) (b & 0xff);
        write(oneByte);
    }

    @Override
    public void write(byte[] data, int off, int len) throws IOException {
        compressor.compress(data, off, len);
    }

    @Override
    public void close() throws IOException {
        finish();
        os.close();
    }

    /**
     * Compresses all remaining data and writes it to the stream,
     * doesn't close the underlying stream.
     * @throws IOException if an error occurs
     */
    public void finish() throws IOException {
        if (!finished) {
            compressor.finish();
            finished = true;
        }
    }

    private void addLiteralBlock(LZ77Compressor.LiteralBlock block) throws IOException {
    }

    private void addBackReference(LZ77Compressor.BackReference block) throws IOException {
    }

    private void writeFinalLiteralBlock() throws IOException {
    }

    final static class Pair {
        private final List<byte[]> literals = new LinkedList<>();
        private int brOffset, brLength;

        void addLiteral(LZ77Compressor.LiteralBlock block) {
            literals.add(Arrays.copyOfRange(block.getData(), block.getOffset(),
                block.getOffset() + block.getLength()));
        }
        void setBackReference(LZ77Compressor.BackReference block) {
            if (hasBackReference()) {
                throw new IllegalStateException();
            }
            brOffset = block.getOffset();
            brLength = block.getLength();
        }
        boolean hasBackReference() {
            return brOffset > 0;
        }
        boolean canBeWritten(int lengthOfBlocksAfterThisPair) {
            return hasBackReference()
                && lengthOfBlocksAfterThisPair >= MIN_LENGTH_OF_LAST_LITERAL
                && lengthOfBlocksAfterThisPair + brOffset + brLength >= MIN_OFFSET_OF_LAST_BACK_REFERENCE;
        }
        int length() {
            return literalLength() + brLength;
        }
        void writeTo(OutputStream out) throws IOException {
            int litLength = literalLength();
            out.write(lengths(litLength, brLength));
            if (litLength >= BlockLZ4CompressorInputStream.BACK_REFERENCE_SIZE_MASK) {
                writeLength(litLength - BlockLZ4CompressorInputStream.BACK_REFERENCE_SIZE_MASK, out);
            }
            for (byte[] b : literals) {
                out.write(b);
            }
            if (hasBackReference()) {
                ByteUtils.toLittleEndian(out, brOffset, 2);
                if (brLength - MIN_BACK_REFERENCE_LENGTH >= BlockLZ4CompressorInputStream.BACK_REFERENCE_SIZE_MASK) {
                    writeLength(brLength - MIN_BACK_REFERENCE_LENGTH
                        - BlockLZ4CompressorInputStream.BACK_REFERENCE_SIZE_MASK, out);
                }
            }
        }
        private int literalLength() {
            int length = 0;
            for (byte[] b : literals) {
                length += b.length;
            }
            return length;
        }
        private static int lengths(int litLength, int brLength) {
            int l = litLength < 15 ? litLength : 15;
            int br = brLength < 4 ? 0 : (brLength < 19 ? brLength - 4 : 15);
            return (l << BlockLZ4CompressorInputStream.SIZE_BITS) | br;
        }
        private static void writeLength(int length, OutputStream out) throws IOException {
            while (length >= 255) {
                out.write(255);
                length -= 255;
            }
            out.write(length);
        }
    }
}
