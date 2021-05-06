// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.protobuf;

import static org.junit.jupiter.api.Assertions.assertEquals;


import java.io.IOException;

import org.junit.jupiter.api.Test;

/**
 * Test class for specific {@link ProtobufRecord} functionality
 */
class ProtobufRecordTest {
    @Test
    void testFixed32() throws IOException {
        ProtobufParser parser = new ProtobufParser(ProtobufTest.toByteArray(new int[] {0x0d, 0x00, 0x00, 0x80, 0x3f}));
        ProtobufRecord thirtyTwoBit = new ProtobufRecord(parser);
        assertEquals(WireType.THIRTY_TWO_BIT, thirtyTwoBit.getType());
        assertEquals(1f, thirtyTwoBit.asFloat());
    }

    @Test
    void testUnknown() throws IOException {
        ProtobufParser parser = new ProtobufParser(ProtobufTest.toByteArray(new int[] {0x0f, 0x00, 0x00, 0x80, 0x3f}));
        ProtobufRecord unknown = new ProtobufRecord(parser);
        assertEquals(WireType.UNKNOWN, unknown.getType());
        assertEquals(0, unknown.getBytes().length);
    }
}
