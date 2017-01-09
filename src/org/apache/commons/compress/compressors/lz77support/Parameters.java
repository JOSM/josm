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
 * Parameters of the {@link LZ77Compressor compressor}.
 */
public final class Parameters {
    public static final int TRUE_MIN_MATCH_LENGTH = LZ77Compressor.NUMBER_OF_BYTES_IN_HASH;
    private final int windowSize, minMatchLength, maxMatchLength, maxOffset, maxLiteralLength;

    /**
     * Initializes the compressor's parameters with a
     * <code>minMatchLength</code> of 3 and <code>max*Length</code>
     * equal to <code>windowSize - 1</code>.
     *
     * @param windowSize the size of the sliding window - this
     * determines the maximum offset a back-reference can take.
     * @throws IllegalArgumentException if <code>windowSize</code>
     * is smaller than <code>minMatchLength</code>.
     */
    public Parameters(int windowSize) {
        this(windowSize, TRUE_MIN_MATCH_LENGTH, windowSize - 1, windowSize - 1, windowSize);
    }

    /**
     * Initializes the compressor's parameters.
     *
     * @param windowSize the size of the sliding window, must be a
     * power of two - this determines the maximum offset a
     * back-reference can take.
     * @param minMatchLength the minimal length of a match found. A
     * true minimum of 3 is hard-coded inside of this implemention
     * but bigger lengths can be configured.
     * @param maxMatchLength maximal length of a match found. A value
     * smaller than <code>minMatchLength</code> as well as values
     * bigger than <code>windowSize - 1</code> are interpreted as
     * <code>windowSize - 1</code>.
     * @param maxOffset maximal offset of a back-reference. A
     * non-positive value as well as values bigger than
     * <code>windowSize - 1</code> are interpreted as <code>windowSize
     * - 1</code>.
     * @param maxLiteralLength maximal length of a literal
     * block. Negative numbers and 0 as well as values bigger than
     * <code>windowSize</code> are interpreted as
     * <code>windowSize</code>.
     * @throws IllegalArgumentException if <code>windowSize</code> is
     * smaller than <code>minMatchLength</code> or not a power of two.
     */
    public Parameters(int windowSize, int minMatchLength, int maxMatchLength,
                      int maxOffset, int maxLiteralLength) {
        this.minMatchLength = Math.max(TRUE_MIN_MATCH_LENGTH, minMatchLength);
        if (windowSize < this.minMatchLength) {
            throw new IllegalArgumentException("windowSize must be at least as big as minMatchLength");
        }
        if (!isPowerOfTwo(windowSize)) {
            throw new IllegalArgumentException("windowSize must be a power of two");
        }
        this.windowSize = windowSize;
        int limit = windowSize - 1;
        this.maxOffset = maxOffset < 1 ? limit : Math.min(maxOffset, limit);
        this.maxMatchLength = maxMatchLength < this.minMatchLength ? limit
            : Math.min(maxMatchLength, limit);
        this.maxLiteralLength = maxLiteralLength < 1 ? windowSize
            : Math.min(maxLiteralLength, windowSize);
    }

    /**
     * Gets the size of the sliding window - this determines the
     * maximum offset a back-reference can take.
     * @return the size of the sliding window
     */
    public int getWindowSize() {
        return windowSize;
    }
    /**
     * Gets the minimal length of a match found.
     * @return the minimal length of a match found
     */
    public int getMinMatchLength() {
        return minMatchLength;
    }
    /**
     * Gets the maximal length of a match found.
     * @return the maximal length of a match found
     */
    public int getMaxMatchLength() {
        return maxMatchLength;
    }
    /**
     * Gets the maximal offset of a match found.
     * @return the maximal offset of a match found
     */
    public int getMaxOffset() {
        return maxOffset;
    }
    /**
     * Gets the maximal length of a literal block.
     * @return the maximal length of a literal block
     */
    public int getMaxLiteralLength() {
        return maxLiteralLength;
    }

    private static final boolean isPowerOfTwo(int x) {
        // pre-condition: x > 0
        return (x & (x - 1)) == 0;
    }
}
