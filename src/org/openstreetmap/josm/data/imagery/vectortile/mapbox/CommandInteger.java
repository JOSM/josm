// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.imagery.vectortile.mapbox;

import java.util.Arrays;

/**
 * An indicator for a command to be executed
 * @author Taylor Smock
 * @since 17862
 */
public class CommandInteger {
    private final Command type;
    private final short[] parameters;
    private int added;

    /**
     * Create a new command
     * @param command the command (treated as an unsigned int)
     */
    public CommandInteger(final int command) {
        // Technically, the int is unsigned, but it is easier to work with the long
        final long unsigned = Integer.toUnsignedLong(command);
        // By avoiding using a stream for getting the Command type, we go from 72 MB to 13 MB.
        // By using a cached value for Command.values(), we further reduce the allocations to 5 MB (new short[] call
        // at end of initializer)
        Command rType = null;
        for (Command tType : Command.getAllValues()) {
            if (tType.getId() == (unsigned & 0x7)) {
                rType = tType;
                break;
            }
        }
        this.type = rType;
        if (this.type == null) {
            throw new InvalidMapboxVectorTileException();
        }

        // This is safe, since we are shifting right 3 when we converted an int to a long (for unsigned).
        // So we <i>cannot</i> lose anything.
        final int operationsInt = (int) (unsigned >> 3);
        this.parameters = new short[operationsInt * this.type.getParameterNumber()];
    }

    /**
     * Add a parameter
     * @param parameterInteger The parameter to add (converted to {@code short}).
     */
    public void addParameter(Number parameterInteger) {
        this.parameters[added++] = parameterInteger.shortValue();
    }

    /**
     * Add a parameter
     * @param parameterInteger The parameter to add (converted to {@code short}).
     * @since xxx
     */
    public void addParameter(long parameterInteger) {
        this.parameters[added++] = (short) parameterInteger;
    }

    /**
     * Get the operations for the command
     * @return The operations
     */
    public short[] getOperations() {
        return this.parameters;
    }

    /**
     * Get the command type
     * @return the command type
     */
    public Command getType() {
        return this.type;
    }

    /**
     * Get the expected parameter length
     * @return The expected parameter size
     */
    public boolean hasAllExpectedParameters() {
        return this.added >= this.parameters.length;
    }

    @Override
    public String toString() {
        return "CommandInteger [type=" + type + ", parameters=" + Arrays.toString(parameters) + ']';
    }
}
