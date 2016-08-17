// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.preferences;

/**
 * A property containing an {@code String} value.
 */
public class StringProperty extends AbstractToStringProperty<String> {

    /**
     * Constructs a new {@code StringProperty}.
     * @param key The property key
     * @param defaultValue The default value
     */
    public StringProperty(String key, String defaultValue) {
        super(key, defaultValue);
    }

    @Override
    public String get() {
        // Removing this implementation breaks binary compatibility
        return super.get();
    }

    @Override
    public boolean put(String value) {
        // Removing this implementation breaks binary compatibility
        return super.put(value);
    }

    @Override
    protected String fromString(String string) {
        return string;
    }

    @Override
    protected String toString(String string) {
        return string;
    }
}
