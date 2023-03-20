// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.protobuf;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

/**
 * Parse packed values (only numerical values)
 *
 * @author Taylor Smock
 * @since 17862
 */
public class ProtobufPacked {
    private final byte[] bytes;
    private final long[] numbers;
    private int location;

    /**
     * Create a new ProtobufPacked object
     *
     * @param ignored A reusable ByteArrayOutputStream (no longer used)
     * @param bytes The packed bytes
     * @deprecated since we aren't using the output stream anymore
     */
    @Deprecated
    public ProtobufPacked(ByteArrayOutputStream ignored, byte[] bytes) {
        this(bytes);
    }

    /**
     * Create a new ProtobufPacked object
     *
     * @param bytes The packed bytes
     * @since 18695
     */
    public ProtobufPacked(byte[] bytes) {
        this.location = 0;
        this.bytes = bytes;

        // By creating a list of size bytes.length, we avoid 36 MB of allocations from list growth. This initialization
        // only adds 3.7 MB to the ArrayList#init calls. Note that the real-world test case (Mapillary vector tiles)
        // primarily created Shorts.
        long[] numbersT = new long[bytes.length];
        int index = 0;
        // By reusing a ByteArrayOutputStream, we can reduce allocations in nextVarInt from 230 MB to 74 MB.
        while (this.location < bytes.length) {
            int start = this.location;
            numbersT[index] = ProtobufParser.convertByteArray(this.bytes, ProtobufParser.VAR_INT_BYTE_SIZE,
                    start, this.nextVarInt());
            index++;
        }

        if (numbersT.length == index) {
            this.numbers = numbersT;
        } else {
            this.numbers = Arrays.copyOf(numbersT, index);
        }
    }

    /**
     * Get the parsed number array
     *
     * @return The number array
     */
    public long[] getArray() {
        return this.numbers;
    }

    /**
     * Gets the location where the next var int begins. Note: changes {@link ProtobufPacked#location}.
     * @return The next varint location
     */
    private int nextVarInt() {
        while ((this.bytes[this.location] & ProtobufParser.MOST_SIGNIFICANT_BYTE)
          == ProtobufParser.MOST_SIGNIFICANT_BYTE) {
            // Get rid of the leading bit (shift left 1, then shift right 1 unsigned)
            this.bytes[this.location] = (byte) (this.bytes[this.location] ^ ProtobufParser.MOST_SIGNIFICANT_BYTE);
            this.location++;
        }
        return ++this.location;
    }
}
