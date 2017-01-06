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
    public static final int TRUE_MIN_MATCH_SIZE = 3;
    private final int windowSize, minMatchSize, maxMatchSize, maxOffset, maxLiteralSize;

    /**
     * Initializes the compressor's parameters with a
     * <code>minMatchSize</code> of 3 and <code>max*Size</code>
     * equal to <code>windowSize</code>.
     *
     * @param windowSize the size of the sliding window - this
     * determines the maximum offset a back-reference can take.
     * @throws IllegalArgumentException if <code>windowSize</code>
     * is smaller than <code>minMatchSize</code>.
     */
    public Parameters(int windowSize) {
        this(windowSize, TRUE_MIN_MATCH_SIZE, windowSize, windowSize, windowSize);
    }

    /**
     * Initializes the compressor's parameters.
     *
     * @param windowSize the size of the sliding window, must be a
     * power of two - this determines the maximum offset a
     * back-reference can take.
     * @param minMatchSize the minimal size of a match found. A
     * true minimum of 3 is hard-coded inside of this implemention
     * but bigger sizes can be configured.
     * @param maxMatchSize maximal site of a match found. A value
     * smaller than <code>minMatchSize</code> is interpreted as
     * infinite (actually {@link Integer.MAX_VALUE}).
     * @param maxOffset maximal offset of a back-reference. A
     * non-positive value is interpreted as <code>windowSize</code>.
     * @param maxLiteralSize maximal size of a literal block. Negative
     * numbers and 0 as well as values bigger than <code>2 *
     * windowSize</code> are interpreted as <code>windowSize</code>.
     * @throws IllegalArgumentException if <code>windowSize</code> is
     * smaller than <code>minMatchSize</code> or not a power of two.
     */
    public Parameters(int windowSize, int minMatchSize, int maxMatchSize,
                      int maxOffset, int maxLiteralSize) {
        this.minMatchSize = Math.max(TRUE_MIN_MATCH_SIZE, minMatchSize);
        if (windowSize < this.minMatchSize) {
            throw new IllegalArgumentException("windowSize must be at least as big as minMatchSize");
        }
        if (!isPowerOfTwo(windowSize)) {
            throw new IllegalArgumentException("windowSize must be a power of two");
        }
        this.windowSize = windowSize;
        this.maxOffset = maxOffset < 1 ? this.windowSize
            : Math.min(maxOffset, this.windowSize);
        this.maxMatchSize = maxMatchSize < this.minMatchSize ? Integer.MAX_VALUE
            : maxMatchSize;
        this.maxLiteralSize = maxLiteralSize < 1 || maxLiteralSize > 2 * windowSize
            ? windowSize : maxLiteralSize;
    }

    /**
     * Gets the size of the sliding window - this determines the
     * maximum offset a back-reference can take.
     */
    public int getWindowSize() {
        return windowSize;
    }
    /**
     * Gets the minimal size of a match found.
     */
    public int getMinMatchSize() {
        return minMatchSize;
    }
    /**
     * Gets the maximal size of a match found.
     */
    public int getMaxMatchSize() {
        return maxMatchSize;
    }
    /**
     * Gets the maximal offset of a match found.
     */
    public int getMaxOffset() {
        return maxOffset;
    }
    /**
     * Gets the maximal size of a literal block.
     */
    public int getMaxLiteralSize() {
        return maxLiteralSize;
    }

    private static final boolean isPowerOfTwo(int x) {
        // pre-condition: x > 0
        return (x & (x - 1)) == 0;
    }
}
