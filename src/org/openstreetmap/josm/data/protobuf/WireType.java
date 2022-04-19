// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.protobuf;

/**
 * The WireTypes
 *
 * @author Taylor Smock
 * @since 17862
 */
public enum WireType {
    /**
     * int32, int64, uint32, uint64, sing32, sint64, bool, enum
     */
    VARINT(0),
    /**
     * fixed64, sfixed64, double
     */
    SIXTY_FOUR_BIT(1),
    /**
     * string, bytes, embedded messages, packed repeated fields
     */
    LENGTH_DELIMITED(2),
    /**
     * start groups
     *
     * @deprecated Unknown reason. Deprecated since at least 2012.
     */
    @Deprecated
    START_GROUP(3),
    /**
     * end groups
     *
     * @deprecated Unknown reason. Deprecated since at least 2012.
     */
    @Deprecated
    END_GROUP(4),
    /**
     * fixed32, sfixed32, float
     */
    THIRTY_TWO_BIT(5),

    /**
     * For unknown WireTypes
     */
    UNKNOWN(Byte.MAX_VALUE);

    private static final WireType[] CACHED_VALUES = values();

    private final byte type;

    WireType(int value) {
        this.type = (byte) value;
    }

    /**
     * Get the type representation (byte form)
     *
     * @return The wire type byte representation
     */
    public byte getTypeRepresentation() {
        return this.type;
    }

    /**
     * Get a pre-calculated array of all {@link WireType} values.
     * @return An array of values, meant as a drop-in replacement for {@link WireType#values()}
     * <i>where the array is not modified</i>! This can significantly reduce allocations, as there is no defensive
     * array copy.
     */
    static WireType[] getAllValues() {
        return CACHED_VALUES;
    }
}
