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
    private static final Number[] NO_NUMBERS = new Number[0];
    private final byte[] bytes;
    private final Number[] numbers;
    private int location;

    /**
     * Create a new ProtobufPacked object
     *
     * @param byteArrayOutputStream A reusable ByteArrayOutputStream (helps to reduce memory allocations)
     * @param bytes The packed bytes
     */
    public ProtobufPacked(ByteArrayOutputStream byteArrayOutputStream, byte[] bytes) {
        this.location = 0;
        this.bytes = bytes;

        // By creating a list of size bytes.length, we avoid 36 MB of allocations from list growth. This initialization
        // only adds 3.7 MB to the ArrayList#init calls. Note that the real-world test case (Mapillary vector tiles)
        // primarily created Shorts.
        List<Number> numbersT = new ArrayList<>(bytes.length);
        // By reusing a ByteArrayOutputStream, we can reduce allocations in nextVarInt from 230 MB to 74 MB.
        while (this.location < bytes.length) {
            numbersT.add(ProtobufParser.convertByteArray(this.nextVarInt(byteArrayOutputStream), ProtobufParser.VAR_INT_BYTE_SIZE));
        }

        this.numbers = numbersT.toArray(NO_NUMBERS);
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
        // ByteArrayOutputStream, it went down to 230 MB. By further reusing the ByteArrayOutputStream between method
        // calls, it went down further to 73 MB.
        while ((this.bytes[this.location] & ProtobufParser.MOST_SIGNIFICANT_BYTE)
          == ProtobufParser.MOST_SIGNIFICANT_BYTE) {
            // Get rid of the leading bit (shift left 1, then shift right 1 unsigned)
            byteArrayOutputStream.write(this.bytes[this.location++] ^ ProtobufParser.MOST_SIGNIFICANT_BYTE);
        }
        // The last byte doesn't drop the most significant bit
        byteArrayOutputStream.write(this.bytes[this.location++]);
        try {
            return byteArrayOutputStream.toByteArray();
        } finally {
            byteArrayOutputStream.reset();
        }
    }
}
