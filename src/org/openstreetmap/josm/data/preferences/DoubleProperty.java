// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.preferences;

/**
 * A property containing an {@code Double} value.
 * @since 3246
 */
public class DoubleProperty extends AbstractToStringProperty<Double> {

    /**
     * Constructs a new {@code DoubleProperty}.
     * @param key The property key
     * @param defaultValue The default value
     */
    public DoubleProperty(String key, double defaultValue) {
        super(key, defaultValue);
    }

    @Override
    public Double get() {
        // Removing this implementation breaks binary compatibility
        return super.get();
    }

    @Override
    public boolean put(Double value) {
        // Removing this implementation breaks binary compatibility
        return super.put(value);
    }

    @Override
    protected Double fromString(String string) {
        try {
            return Double.valueOf(string);
        } catch (NumberFormatException e) {
            throw new InvalidPreferenceValueException(e);
        }
    }

    @Override
    protected String toString(Double t) {
        return t.toString();
    }

    /**
     * parses and saves a double precision value
     * @param value the value to be parsed
     * @return true - preference value has changed
     *         false - parsing failed or preference value has not changed
     */
    public boolean parseAndPut(String value) {
        try {
            return put(Double.valueOf(value));
        } catch (NumberFormatException ex) {
            return false;
        }
    }
}
