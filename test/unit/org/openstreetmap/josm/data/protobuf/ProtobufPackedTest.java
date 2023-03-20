// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.protobuf;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.io.ByteArrayOutputStream;

import org.junit.jupiter.api.Test;

/**
 * Test class for {@link ProtobufPacked}
 */
class ProtobufPackedTest {
    @Test
    void testSingleByteNumbers() {
        long[] numbers = new ProtobufPacked(new ByteArrayOutputStream(), ProtobufTest.toByteArray(new int[]{0, 0, 1, 1, 2, 2, 3, 3, 4, 4}))
                .getArray();
        assertArrayEquals(new long[] {0, 0, 1, 1, 2, 2, 3, 3, 4, 4}, numbers);
    }

    @Test
    void testMultipleByteNumbers() {
        byte[] bytes = ProtobufTest.toByteArray(new int[] {-128, 64, -18, 49, -70, 3});
        long[] numbers = new ProtobufPacked(new ByteArrayOutputStream(), bytes).getArray();
        assertArrayEquals(new long[] {8192, 6382, 442}, numbers);
    }
}
