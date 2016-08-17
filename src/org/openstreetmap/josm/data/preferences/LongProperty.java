// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.preferences;

/**
 * A property containing an {@code Long} value.
 * @since 10087
 *
 */
public class LongProperty extends AbstractToStringProperty<Long> {

    /**
     * Constructs a new {@code LongProperty}
     * @param key property key
     * @param defaultValue default value
     */
    public LongProperty(String key, long defaultValue) {
        super(key, defaultValue);
    }

    @Override
    public Long get() {
        // Removing this implementation breaks binary compatibility
        return super.get();
    }

    @Override
    public boolean put(Long value) {
        // Removing this implementation breaks binary compatibility
        return super.put(value);
    }

    @Override
    protected Long fromString(String string) {
        try {
            return Long.valueOf(string);
        } catch (NumberFormatException e) {
            throw new InvalidPreferenceValueException(e);
        }
    }

    @Override
    protected String toString(Long t) {
        return t.toString();
    }
}
