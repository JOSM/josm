// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.imagery.vectortile.mapbox;

/**
 * Command integers for Mapbox Vector Tiles
 * @author Taylor Smock
 * @since 17862
 */
public enum Command {
    /**
     * For {@link GeometryTypes#POINT}, each {@link #MoveTo} is a new point.
     * For {@link GeometryTypes#LINESTRING} and {@link GeometryTypes#POLYGON}, each {@link #MoveTo} is a new geometry of the same type.
     */
    MoveTo((byte) 1, (byte) 2),
    /**
     * While not explicitly prohibited for {@link GeometryTypes#POINT}, it should be ignored.
     * For {@link GeometryTypes#LINESTRING} and {@link GeometryTypes#POLYGON}, each {@link #LineTo} extends that geometry.
     */
    LineTo((byte) 2, (byte) 2),
    /**
     * This is only explicitly valid for {@link GeometryTypes#POLYGON}. It closes the {@link GeometryTypes#POLYGON}.
     */
    ClosePath((byte) 7, (byte) 0);

    private static final Command[] CACHED_VALUES = Command.values();
    private final byte id;
    private final byte parameters;

    Command(byte id, byte parameters) {
        this.id = id;
        this.parameters = parameters;
    }

    /**
     * Get the command id
     * @return The id
     */
    public byte getId() {
        return this.id;
    }

    /**
     * Get the number of parameters
     * @return The number of parameters
     */
    public byte getParameterNumber() {
        return this.parameters;
    }

    /**
     * Get a pre-calculated array of all {@link Command} values.
     * @return An array of values, meant as a drop-in replacement for {@link Command#values()}
     * <i>where the array is not modified</i>! This can significantly reduce allocations, as there is no defensive
     * array copy.
     */
    static Command[] getAllValues() {
        return CACHED_VALUES;
    }
}
