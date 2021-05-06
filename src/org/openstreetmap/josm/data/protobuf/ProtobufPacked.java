// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.protobuf;

import java.util.ArrayList;
import java.util.List;

/**
 * Parse packed values (only numerical values)
 *
 * @author Taylor Smock
 * @since xxx
 */
public class ProtobufPacked {
    private final byte[] bytes;
    private final Number[] numbers;
    private int location;

    /**
     * Create a new ProtobufPacked object
     *
     * @param bytes The packed bytes
     */
    public ProtobufPacked(byte[] bytes) {
        this.location = 0;
        this.bytes = bytes;
        List<Number> numbersT = new ArrayList<>();
        while (this.location < bytes.length) {
            numbersT.add(ProtobufParser.convertByteArray(this.nextVarInt(), ProtobufParser.VAR_INT_BYTE_SIZE));
        }

        this.numbers = new Number[numbersT.size()];
        for (int i = 0; i < numbersT.size(); i++) {
            this.numbers[i] = numbersT.get(i);
        }
    }

    /**
     * Get the parsed number array
     *
     * @return The number array
     */
    public Number[] getArray() {
        return this.numbers;
    }

    private byte[] nextVarInt() {
        List<Byte> byteList = new ArrayList<>();
        while ((this.bytes[this.location] & ProtobufParser.MOST_SIGNIFICANT_BYTE)
          == ProtobufParser.MOST_SIGNIFICANT_BYTE) {
            // Get rid of the leading bit (shift left 1, then shift right 1 unsigned)
            byteList.add((byte) (this.bytes[this.location++] ^ ProtobufParser.MOST_SIGNIFICANT_BYTE));
        }
        // The last byte doesn't drop the most significant bit
        byteList.add(this.bytes[this.location++]);
        byte[] byteArray = new byte[byteList.size()];
        for (int i = 0; i < byteList.size(); i++) {
            byteArray[i] = byteList.get(i);
        }

        return byteArray;
    }
}
