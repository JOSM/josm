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

import static java.lang.Integer.rotateLeft;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.zip.Checksum;

/**
 * Implementation of the xxhash32 hash algorithm.
 *
 * @see <a href="http://cyan4973.github.io/xxHash/">xxHash</a>
 * @NotThreadSafe
 * @since 1.14
 */
public class XXHash32 implements Checksum {

    private static final int BUF_SIZE = 16;
    private static final int ROTATE_BITS = 13;

    private static final int PRIME1 = (int) 2654435761l;
    private static final int PRIME2 = (int) 2246822519l;
    private static final int PRIME3 = (int) 3266489917l;
    private static final int PRIME4 =  668265263;
    private static final int PRIME5 =  374761393;

    private final byte[] oneByte = new byte[1];
    private final int[] state = new int[4];
    private final ByteBuffer buffer = ByteBuffer.allocate(BUF_SIZE).order(ByteOrder.LITTLE_ENDIAN);
    private final int seed;

    private int totalLen;
    private int pos;

    /**
     * Creates an XXHash32 instance with a seed of 0.
     */
    public XXHash32() {
        this(0);
    }

    /**
     * Creates an XXHash32 instance.
     */
    public XXHash32(int seed) {
        this.seed = seed;
        initializeState();
    }

    @Override
    public void reset() {
        initializeState();
        buffer.clear();
        totalLen = 0;
        pos = 0;
    }

    @Override
    public void update(int b) {
        oneByte[0] = (byte) (b & 0xff);
        update(oneByte, 0, 1);
    }

    @Override
    public void update(byte[] b, int off, final int len) {
        if (len <= 0) {
            return;
        }
        totalLen += len;

        final int end = off + len;

        if (pos + len < BUF_SIZE) {
            buffer.put(b, off, len);
            pos += len;
            return;
        }

        if (pos > 0) {
            final int size = BUF_SIZE - pos;
            buffer.put(b, off, size);
            process();
            off += size;
        }

        final int limit = end - BUF_SIZE;
        while (off <= limit) {
            buffer.put(b, off, BUF_SIZE);
            process();
            off += BUF_SIZE;
        }

        if (off < end) {
            pos = end - off;
            buffer.put(b, off, pos);
        }
    }

    @Override
    public long getValue() {
        int hash;
        if (totalLen > BUF_SIZE) {
            hash =
                rotateLeft(state[0],  1) +
                rotateLeft(state[1],  7) +
                rotateLeft(state[2], 12) +
                rotateLeft(state[3], 18);
        } else {
            hash = state[2] + PRIME5;
        }
        hash += totalLen;

        buffer.flip();

        int idx = 0;
        final int limit = pos - 4;
        for (; idx <= limit; idx += 4) {
            hash = rotateLeft(hash + buffer.getInt() * PRIME3, 17) * PRIME4;
        }
        while (idx < pos) {
            hash = rotateLeft(hash + (buffer.get() & 0xff) * PRIME5, 11) * PRIME1;
            idx++;
        }

        hash ^= hash >>> 15;
        hash *= PRIME2;
        hash ^= hash >>> 13;
        hash *= PRIME3;
        hash ^= hash >>> 16;
        return hash & 0xffffffffl;
    }

    private void initializeState() {
        state[0] = seed + PRIME1 + PRIME2;
        state[1] = seed + PRIME2;
        state[2] = seed;
        state[3] = seed - PRIME1;
    }

    private void process() {
        buffer.flip();

        // local shadows for performance
        int s0 = state[0];
        int s1 = state[1];
        int s2 = state[2];
        int s3 = state[3];

        s0 = rotateLeft(s0 + buffer.getInt() * PRIME2, ROTATE_BITS) * PRIME1;
        s1 = rotateLeft(s1 + buffer.getInt() * PRIME2, ROTATE_BITS) * PRIME1;
        s2 = rotateLeft(s2 + buffer.getInt() * PRIME2, ROTATE_BITS) * PRIME1;
        s3 = rotateLeft(s3 + buffer.getInt() * PRIME2, ROTATE_BITS) * PRIME1;

        state[0] = s0;
        state[1] = s1;
        state[2] = s2;
        state[3] = s3;

        buffer.clear();
        pos = 0;
    }
}
