// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.protobuf;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Test class for {@link ProtobufParser}
 * @author Taylor Smock
 * @since 17862
 */
class ProtobufParserTest {
    /**
     * Check that we are appropriately converting values to the "smallest" type
     */
    @Test
    void testConvertLong() {
        // No casting due to auto conversions
        assertEquals(Byte.MAX_VALUE, ProtobufParser.convertLong(Byte.MAX_VALUE));
        assertEquals(Byte.MIN_VALUE, ProtobufParser.convertLong(Byte.MIN_VALUE));
        assertEquals(Short.MIN_VALUE, ProtobufParser.convertLong(Short.MIN_VALUE));
        assertEquals(Short.MAX_VALUE, ProtobufParser.convertLong(Short.MAX_VALUE));
        assertEquals(Integer.MAX_VALUE, ProtobufParser.convertLong(Integer.MAX_VALUE));
        assertEquals(Integer.MIN_VALUE, ProtobufParser.convertLong(Integer.MIN_VALUE));
        assertEquals(Long.MIN_VALUE, ProtobufParser.convertLong(Long.MIN_VALUE));
        assertEquals(Long.MAX_VALUE, ProtobufParser.convertLong(Long.MAX_VALUE));
    }

    /**
     * Check that zig zags are appropriately encoded.
     */
    @Test
    void testEncodeZigZag() {
        assertEquals(0, ProtobufParser.encodeZigZag(0).byteValue());
        assertEquals(1, ProtobufParser.encodeZigZag(-1).byteValue());
        assertEquals(2, ProtobufParser.encodeZigZag(1).byteValue());
        assertEquals(3, ProtobufParser.encodeZigZag(-2).byteValue());
        assertEquals(254, ProtobufParser.encodeZigZag(Byte.MAX_VALUE).shortValue());
        assertEquals(255, ProtobufParser.encodeZigZag(Byte.MIN_VALUE).shortValue());
        assertEquals(65_534, ProtobufParser.encodeZigZag(Short.MAX_VALUE).intValue());
        assertEquals(65_535, ProtobufParser.encodeZigZag(Short.MIN_VALUE).intValue());
        // These integers check a possible boundary condition (the boundary between using the 32/64 bit encoding methods)
        assertEquals(4_294_967_292L, ProtobufParser.encodeZigZag(Integer.MAX_VALUE - 1).longValue());
        assertEquals(4_294_967_293L, ProtobufParser.encodeZigZag(Integer.MIN_VALUE + 1).longValue());
        assertEquals(4_294_967_294L, ProtobufParser.encodeZigZag(Integer.MAX_VALUE).longValue());
        assertEquals(4_294_967_295L, ProtobufParser.encodeZigZag(Integer.MIN_VALUE).longValue());
        assertEquals(4_294_967_296L, ProtobufParser.encodeZigZag(Integer.MAX_VALUE + 1L).longValue());
        assertEquals(4_294_967_297L, ProtobufParser.encodeZigZag(Integer.MIN_VALUE - 1L).longValue());
    }
}
