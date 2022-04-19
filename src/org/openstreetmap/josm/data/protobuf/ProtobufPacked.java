// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.protobuf;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Parse packed values (only numerical values)
 *
 * @author Taylor Smock
 * @since 17862
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
        // By reusing a ByteArrayOutputStream, we can reduce allocations in nextVarInt from 230 MB to 74 MB.
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(4);
        while (this.location < bytes.length) {
            numbersT.add(ProtobufParser.convertByteArray(this.nextVarInt(byteArrayOutputStream), ProtobufParser.VAR_INT_BYTE_SIZE));
            byteArrayOutputStream.reset();
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

    private byte[] nextVarInt(final ByteArrayOutputStream byteArrayOutputStream) {
        // In a real world test, the largest List<Byte> seen had 3 elements. Use 4 to avoid most new array allocations.
        // Memory allocations went from 368 MB to 280 MB by using an initial array allocation. When using a
        // ByteArrayOutputStream, it went down to 230 MB.
        while ((this.bytes[this.location] & ProtobufParser.MOST_SIGNIFICANT_BYTE)
          == ProtobufParser.MOST_SIGNIFICANT_BYTE) {
            // Get rid of the leading bit (shift left 1, then shift right 1 unsigned)
            byteArrayOutputStream.write(this.bytes[this.location++] ^ ProtobufParser.MOST_SIGNIFICANT_BYTE);
        }
        // The last byte doesn't drop the most significant bit
        byteArrayOutputStream.write(this.bytes[this.location++]);
        return byteArrayOutputStream.toByteArray();
    }
}
