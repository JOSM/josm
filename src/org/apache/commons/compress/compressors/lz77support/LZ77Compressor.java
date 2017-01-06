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
package org.apache.commons.compress.compressors.lz77support;

/**
 * Helper class for compression algorithms that use the ideas of LZ77.
 *
 * <p>Most LZ77 derived algorithms split input data into blocks of
 * uncompressed data (called literal blocks) and back-references
 * (pairs of offsets and lengths) that state "add <code>length</code>
 * bytes that are the same as those already written starting
 * <code>offset</code> bytes before the current position. The details
 * of how those blocks and back-references are encoded are quite
 * different between the algorithms and some algorithms perform
 * additional steps (Huffman encoding in the case of DEFLATE for
 * example).</p>
 *
 * <p>This class attempts to extract the core logic - finding
 * back-references - so it can be re-used. It follows the algorithm
 * explained in section 4 of RFC 1951 (DEFLATE) and currently doesn't
 * implement the "lazy match" optimization. The three-byte hash function
 * used in this class is the same used by zlib and InfoZIP's ZIP
 * implementation of DEFLATE.</p>
 *
 * <p>LZ77 is used vaguely here (as well as many other places that
 * talk about it :-), LZSS would likely be closer to the truth but
 * LZ77 has become the synonym for a whole family of algorithms.</p>
 *
 * <p>The API consists of a compressor that is fed <code>byte</code>s
 * and emits {@link Block}s to a registered callback where the blocks
 * represent either {@link LiteralBlock literal blocks}, {@link
 * BackReference back references} or {@link EOD end of data
 * markers}. In order to ensure the callback receives all information,
 * the {@code #finish} method must be used once all data has been fed
 * into the compressor.</p>
 *
 * <p>Several parameters influence the outcome of the "compression":</p>
 * <dl>
 *
 *  <dt><code>windowSize</code></dt> <dd>the size of the sliding
 *  window, must be a power of two - this determines the maximum
 *  offset a back-reference can take. The compressor maintains a
 *  buffer of twice of <code>windowSize</code> - real world values are
 *  in the area of 32k.</dd>
 *
 *  <dt><code>minMatchSize</code></dt>
 *  <dd>Minimal size of a match found. A true minimum of 3 is
 *  hard-coded inside of this implemention but bigger sizes can be
 *  configured.</dd>
 *
 *  <dt><code>maxMatchSize</code></dt>
 *  <dd>Maximal size of a match found.</dd>
 *
 *  <dt><code>maxOffset</code></dt>
 *  <dd>Maximal offset of a back-reference.</dd>
 *
 *  <dt><code>maxLiteralSize</code></dt>
 *  <dd>Maximal size of a literal block.</dd>
 * </dl>
 *
 * @see "https://tools.ietf.org/html/rfc1951#section-4"
 * @since 1.14
 * @NotThreadSafe
 */
public class LZ77Compressor {

    /**
     * Base class representing things the compressor may emit.
     */
    public static abstract class Block { }
    /**
     * Represents a literal block of data.
     */
    public static final class LiteralBlock extends Block {
        private final byte[] data;
        private LiteralBlock(byte[] data) {
            this.data = data;
        }
        /**
         * The literal data.
         *
         * <p>This returns a life view of the actual data in order to
         * avoid copying, modify the array at your own risk.</p>
         */
        public byte[] getData() {
            return data;
        }
    }
    /**
     * Represents a back-reference to a match.
     */
    public static final class BackReference extends Block {
        private final int offset, length;
        private BackReference(int offset, int length) {
            this.offset = offset;
            this.length = length;
        }
        /**
         * Provides the offset of the match.
         */
        public int getOffset() {
            return offset;
        }
        /**
         * Provides the length of the match.
         */
        public int getLength() {
            return length;
        }
    }
    /**
     * A simple "we are done" marker.
     */
    public static final class EOD extends Block { }

    /**
     * Callback invoked while the compressor processes data.
     *
     * <p>The callback is invoked on the same thread that receives the
     * bytes to compress and may be invoked multiple times during the
     * execution of {@link #compress} or {@link #finish}.</p>
     */
    public interface Callback /* extends Consumer<Block> */ {
        void accept(Block b);
    }

    private final Parameters params;
    private final Callback callback;

    /**
     * Initializes a compressor with parameters and a callback.
     * @param params the parameters
     * @param callback the callback
     * @throws NullPointerException if either parameter is <code>null</code>
     */
    public LZ77Compressor(Parameters params, Callback callback) {
        if (params == null) {
            throw new NullPointerException("params must not be null");
        }
        if (callback == null) {
            throw new NullPointerException("callback must not be null");
        }
        this.params = params;
        this.callback = callback;
    }

    /**
     * Feeds bytes into the compressor which in turn may emit zero or
     * more blocks to the callback during the execution of this
     * method.
     * @param data the data to compress - must not be null
     */
    public void compress(byte[] data) {
        compress(data, 0, data.length);
    }

    /**
     * Feeds bytes into the compressor which in turn may emit zero or
     * more blocks to the callback during the execution of this
     * method.
     * @param data the data to compress - must not be null
     * @param off the start offset of the data
     * @param len the number of bytes to compress
     */
    public void compress(byte[] data, int off, int len) {
    }

    /**
     * Tells the compressor to process all remaining data and signal
     * end of data to the callback.
     *
     * <p>The compressor will in turn emit at least one block ({@link
     * EOD}) but potentially multiple blocks to the callback during
     * the execution of this method.</p>
     */
    public void finish() {
    }

}
