// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

/**
 * Abstract class for <i>key=value</i> parameters, used in {@link ParameterizedAction}.
 * <p>
 * The key ({@link #name}) is a string and the value of class {@link T}. The value can be
 * converted to and from a string.
 * @param <T> the value type
 */
public abstract class ActionParameter<T> {

    private final String name;

    /**
     * Constructs a new ActionParameter.
     * @param name parameter name (the key)
     */
    public ActionParameter(String name) {
        this.name = name;
    }

    /**
     * Get the name of this action parameter.
     * The name is used as a key, to look up values for the parameter.
     * @return the name of this action parameter
     */
    public String getName() {
        return name;
    }

    /**
     * Get the value type of this action parameter.
     * @return the value type of this action parameter
     */
    public abstract Class<T> getType();

    /**
     * Convert a given value into a string (serialization).
     * @param value the value
     * @return a string representation of the value
     */
    public abstract String writeToString(T value);

    /**
     * Create a value from the given string representation (deserialization).
     * @param s the string representation of the value
     * @return the corresponding value object
     */
    public abstract T readFromString(String s);

    /**
     * Simple ActionParameter implementation for string values.
     */
    public static class StringActionParameter extends ActionParameter<String> {

        public StringActionParameter(String name) {
            super(name);
        }

        @Override
        public Class<String> getType() {
            return String.class;
        }

        @Override
        public String readFromString(String s) {
            return s;
        }

        @Override
        public String writeToString(String value) {
            return value;
        }
    }
}
