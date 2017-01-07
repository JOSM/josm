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
 * implement the "lazy match" optimization. The three-byte hash
 * function used in this class is the same used by zlib and InfoZIP's
 * ZIP implementation of DEFLATE. Strongly inspired by InfoZIP's
 * implementation.</p>
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
     *
     * <p>For performance reasons this encapsulates the real data, not
     * a copy of it. Don't modify the data and process it inside of
     * {@link Callback#accept} immediately as it will get overwritten
     * sooner or later.</p>
     */
    public static final class LiteralBlock extends Block {
        private final byte[] data;
        private final int offset, length;
        /* package private for tests */ LiteralBlock(byte[] data, int offset, int length) {
            this.data = data;
            this.offset = offset;
            this.length = length;
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
        /**
         * Offset into data where the literal block starts.
         */
        public int getOffset() {
            return offset;
        }
        /**
         * Length of literal block.
         */
        public int getLength() {
            return length;
        }

        @Override
        public String toString() {
            return "LiteralBlock starting at " + offset + " with length " + length;
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

        @Override
        public String toString() {
            return "BackReference with " + offset + " and length " + length;
        }
    }
    /**
     * A simple "we are done" marker.
     */
    public static final class EOD extends Block { }

    private static final EOD THE_EOD = new EOD();

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

    static final int NUMBER_OF_BYTES_IN_HASH = 3;
    private static final int NO_MATCH = -1;

    private final Parameters params;
    private final Callback callback;

    // the sliding window, twice as big as "windowSize" parameter
    private final byte[] window;
    // the head of hash-chain - indexed by hash-code, points to the
    // location inside of window of the latest sequence of bytes with
    // the given hash.
    private final int[] head;
    // for each window-location points to the latest earlier location
    // with the same hash. Only stored values for the latest
    // "windowSize" elements, the index is "window location modulo
    // windowSize".
    private final int[] prev;

    // bit mask used when indexing into prev
    private final int wMask;

    private boolean initialized = false;
    // the position inside of window that shall be encoded right now
    private int currentPosition;
    // the number of bytes available to compress including the one at
    // currentPosition
    private int lookahead = 0;
    // the hash of the three bytes stating at the current position
    private int insertHash = 0;
    // the position inside of the window where the current literal
    // block starts (in case we are inside of a literal block).
    private int blockStart = 0;
    // position of the current match
    private int matchStart = NO_MATCH;

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

        final int wSize = params.getWindowSize();
        window = new byte[wSize * 2];
        wMask = wSize - 1;
        head = new int[HASH_SIZE];
        for (int i = 0; i < HASH_SIZE; i++) {
            head[i] = NO_MATCH;
        }
        prev = new int[wSize];
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
        final int wSize = params.getWindowSize();
        while (len > wSize) {
            doCompress(data, off, wSize);
            off += wSize;
            len -= wSize;
        }
        if (len > 0) {
            doCompress(data, off, len);
        }
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
        if (blockStart != currentPosition || lookahead > 0) {
            currentPosition += lookahead;
            flushLiteralBlock();
        }
        callback.accept(THE_EOD);
    }

    // we use a 15 bit hashcode as calculated in updateHash
    private static final int HASH_SIZE = 1 << 15;
    private static final int HASH_MASK = HASH_SIZE - 1;
    private static final int H_SHIFT = 5;

    /**
     * Assumes we are calculating the hash for three consecutive bytes
     * as a rolling hash, i.e. for bytes ABCD if H is the hash of ABC
     * the new hash for BCD is nextHash(H, D).
     *
     * <p>The hash is shifted by five bits on each update so all
     * effects of A have been swapped after the third update.</p>
     */
    private int nextHash(int oldHash, byte nextByte) {
        final int nextVal = nextByte & 0xFF;
        return ((oldHash << H_SHIFT) ^ nextVal) & HASH_MASK;
    }

    // performs the actual algorithm with the pre-condition len <= windowSize
    private void doCompress(byte[] data, int off, int len) {
        int spaceLeft = window.length - currentPosition - lookahead;
        if (len > spaceLeft) {
            slide();
        }
        System.arraycopy(data, off, window, currentPosition + lookahead, len);
        lookahead += len;
        if (!initialized && lookahead >= params.getMinMatchSize()) {
            initialize();
        }
        if (initialized) {
            compress();
        }
    }

    private void slide() {
        final int wSize = params.getWindowSize();
        System.arraycopy(window, wSize, window, 0, wSize);
        currentPosition -= wSize;
        matchStart -= wSize;
        blockStart -= wSize;
        for (int i = 0; i< HASH_SIZE; i++) {
            int h = head[i];
            head[i] = h >= wSize ? h - wSize : NO_MATCH;
        }
        for (int i = 0; i < wSize; i++) {
            int p = prev[i];
            prev[i] = p >= wSize ? p - wSize : NO_MATCH;
        }
    }

    private void initialize() {
        for (int i = 0; i < NUMBER_OF_BYTES_IN_HASH - 1; i++) {
            insertHash = nextHash(insertHash, window[i]);
        }
        initialized = true;
    }

    private void compress() {
        final int minMatch = params.getMinMatchSize();

        while (lookahead >= minMatch) {
            int matchLength = 0;
            int hashHead = insertString();
            if (hashHead != NO_MATCH && hashHead - currentPosition <= params.getMaxOffset()) {
                // sets matchStart as a side effect
                matchLength = longestMatch(hashHead);
            }
            if (matchLength >= minMatch) {
                if (blockStart != currentPosition) {
                    // emit preceeding literal block
                    flushLiteralBlock();
                    blockStart = NO_MATCH;
                }
                lookahead -= matchLength;
                // inserts strings contained in current match
                for (int i = 0; i < matchLength - 1; i++) {
                    currentPosition++;
                    insertString();
                }
                currentPosition++;
                flushBackReference(matchLength);
                blockStart = currentPosition;
            } else {
                // no match, append to current or start a new literal
                lookahead--;
                currentPosition++;
                if (currentPosition - blockStart >= params.getMaxLiteralSize()) {
                    flushLiteralBlock();
                    blockStart = currentPosition;
                }
            }
        }
    }

    /**
     * Inserts the current three byte sequence into the dictionary and
     * returns the previous previous head of the hash-chain.
     *
     * <p>Updates <code>insertHash</code> and <code>prev</code> as a
     * side effect.</p>
     */
    private int insertString() {
        insertHash = nextHash(insertHash, window[currentPosition -1 + NUMBER_OF_BYTES_IN_HASH]);
        int hashHead = head[insertHash];
        prev[currentPosition & wMask] = hashHead;
        head[insertHash] = currentPosition;
        return hashHead;
    }

    private void flushBackReference(int matchLength) {
        callback.accept(new BackReference(matchStart, matchLength));
    }

    private void flushLiteralBlock() {
        callback.accept(new LiteralBlock(window, blockStart, currentPosition - blockStart));
    }

    private int longestMatch(int matchHead) {
        return 0;
    }
}
