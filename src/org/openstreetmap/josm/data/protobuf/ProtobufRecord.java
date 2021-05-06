// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.protobuf;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import org.openstreetmap.josm.tools.Utils;

/**
 * A protobuf record, storing the {@link WireType}, the parsed field number, and the bytes for it.
 *
 * @author Taylor Smock
 * @since 17862
 */
public class ProtobufRecord implements AutoCloseable {
    private static final byte[] EMPTY_BYTES = {};
    private final WireType type;
    private final int field;
    private byte[] bytes;

    /**
     * Create a new Protobuf record
     *
     * @param parser The parser to use to create the record
     * @throws IOException - if an IO error occurs
     */
    public ProtobufRecord(ProtobufParser parser) throws IOException {
        Number number = ProtobufParser.convertByteArray(parser.nextVarInt(), ProtobufParser.VAR_INT_BYTE_SIZE);
        // I don't foresee having field numbers > {@code Integer#MAX_VALUE >> 3}
        this.field = (int) number.longValue() >> 3;
        // 7 is 111 (so last three bits)
        byte wireType = (byte) (number.longValue() & 7);
        this.type = Stream.of(WireType.values()).filter(wType -> wType.getTypeRepresentation() == wireType).findFirst()
          .orElse(WireType.UNKNOWN);

        if (this.type == WireType.VARINT) {
            this.bytes = parser.nextVarInt();
        } else if (this.type == WireType.SIXTY_FOUR_BIT) {
            this.bytes = parser.nextFixed64();
        } else if (this.type == WireType.THIRTY_TWO_BIT) {
            this.bytes = parser.nextFixed32();
        } else if (this.type == WireType.LENGTH_DELIMITED) {
            this.bytes = parser.nextLengthDelimited();
        } else {
            this.bytes = EMPTY_BYTES;
        }
    }

    /**
     * Get as a double ({@link WireType#SIXTY_FOUR_BIT})
     *
     * @return the double
     */
    public double asDouble() {
        long doubleNumber = ProtobufParser.convertByteArray(asFixed64(), ProtobufParser.BYTE_SIZE).longValue();
        return Double.longBitsToDouble(doubleNumber);
    }

    /**
     * Get as 32 bits ({@link WireType#THIRTY_TWO_BIT})
     *
     * @return a byte array of the 32 bits (4 bytes)
     */
    public byte[] asFixed32() {
        // TODO verify, or just assume?
        // 4 bytes == 32 bits
        return this.bytes;
    }

    /**
     * Get as 64 bits ({@link WireType#SIXTY_FOUR_BIT})
     *
     * @return a byte array of the 64 bits (8 bytes)
     */
    public byte[] asFixed64() {
        // TODO verify, or just assume?
        // 8 bytes == 64 bits
        return this.bytes;
    }

    /**
     * Get as a float ({@link WireType#THIRTY_TWO_BIT})
     *
     * @return the float
     */
    public float asFloat() {
        int floatNumber = ProtobufParser.convertByteArray(asFixed32(), ProtobufParser.BYTE_SIZE).intValue();
        return Float.intBitsToFloat(floatNumber);
    }

    /**
     * Get the signed var int ({@code WireType#VARINT}).
     * These are specially encoded so that they take up less space.
     *
     * @return The signed var int ({@code sint32} or {@code sint64})
     */
    public Number asSignedVarInt() {
        final Number signed = this.asUnsignedVarInt();
        return ProtobufParser.decodeZigZag(signed);
    }

    /**
     * Get as a string ({@link WireType#LENGTH_DELIMITED})
     *
     * @return The string (encoded as {@link StandardCharsets#UTF_8})
     */
    public String asString() {
        return Utils.intern(new String(this.bytes, StandardCharsets.UTF_8));
    }

    /**
     * Get the var int ({@code WireType#VARINT})
     *
     * @return The var int ({@code int32}, {@code int64}, {@code uint32}, {@code uint64}, {@code bool}, {@code enum})
     */
    public Number asUnsignedVarInt() {
        return ProtobufParser.convertByteArray(this.bytes, ProtobufParser.VAR_INT_BYTE_SIZE);
    }

    @Override
    public void close() {
        this.bytes = null;
    }

    /**
     * Get the raw bytes for this record
     *
     * @return The bytes
     */
    public byte[] getBytes() {
        return this.bytes;
    }

    /**
     * Get the field value
     *
     * @return The field value
     */
    public int getField() {
        return this.field;
    }

    /**
     * Get the WireType of the data
     *
     * @return The {@link WireType} of the data
     */
    public WireType getType() {
        return this.type;
    }
}
