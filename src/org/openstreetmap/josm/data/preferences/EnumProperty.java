// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.preferences;

import org.openstreetmap.josm.spi.preferences.Config;

/**
 * A property containing an {@code Enum} value.
 *
 * @author András Kolesár
 * @param <T> the {@code Enum} class
 */
public class EnumProperty<T extends Enum<T>> extends ParametrizedEnumProperty<T> {

    protected final String key;

    /**
     * Constructs a new {@code EnumProperty}.
     * @param key The property key
     * @param enumClass The {@code Enum} class
     * @param defaultValue The default value
     */
    public EnumProperty(String key, Class<T> enumClass, T defaultValue) {
        super(enumClass, defaultValue);
        this.key = key;
        if (Config.getPref() != null) {
            get();
        }
    }

    @Override
    protected String getKey(String... params) {
        return key;
    }
}
