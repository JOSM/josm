// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.preferences;

/**
 * A property containing an {@code Integer} value.
 * @since 3246
 */
public class IntegerProperty extends AbstractToStringProperty<Integer> {

    /**
     * Constructs a new {@code IntegerProperty}.
     * @param key The property key
     * @param defaultValue The default value
     */
    public IntegerProperty(String key, int defaultValue) {
        super(key, defaultValue);
        if (getPreferences() != null) {
            get();
        }
    }

    @Override
    public Integer get() {
        // Removing this implementation breaks binary compatibility
        return super.get();
    }

    @Override
    public boolean put(Integer value) {
        // Removing this implementation breaks binary compatibility
        return super.put(value);
    }

    @Override
    protected Integer fromString(String string) {
        try {
            return Integer.valueOf(string);
        } catch (NumberFormatException e) {
            throw new InvalidPreferenceValueException(e);
        }
    }

    @Override
    protected String toString(Integer t) {
        return t.toString();
    }

    /**
     * parses and saves an integer value
     * @param value the value to be parsed
     * @return true - preference value has changed
     *         false - parsing failed or preference value has not changed
     */
    public boolean parseAndPut(String value) {
        try {
            return put(Integer.valueOf(value));
        } catch (NumberFormatException ex) {
            return false;
        }
    }
}
