// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.preferences;

/**
 * A property containing a {@code Boolean} value.
 */
public class BooleanProperty extends AbstractToStringProperty<Boolean> {

    /**
     * Constructs a new {@code BooleanProperty}.
     * @param key The property key
     * @param defaultValue The default value
     */
    public BooleanProperty(String key, boolean defaultValue) {
        super(key, defaultValue);
    }

    @Override
    public Boolean get() {
        // Removing this implementation breaks binary compatibility
        return super.get();
    }

    @Override
    public boolean put(Boolean value) {
        // Removing this implementation breaks binary compatibility
        return super.put(value);
    }

    @Override
    protected Boolean fromString(String string) {
        return Boolean.valueOf(string);
    }

    @Override
    protected String toString(Boolean t) {
        return t.toString();
    }
}
