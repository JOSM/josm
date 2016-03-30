// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.preferences;

import org.openstreetmap.josm.Main;

/**
 * A property containing an {@code Long} value.
 * @since 10087
 *
 */
public class LongProperty extends AbstractProperty<Long> {

    /**
     * Constructs a new {@code LongProperty}
     * @param key property key
     * @param defaultValue default value
     */
    public LongProperty(String key, long defaultValue) {
        super(key, defaultValue);
        if (Main.pref != null) {
            get();
        }
    }

    @Override
    public Long get() {
        return Main.pref.getLong(getKey(), getDefaultValue());
    }

    @Override
    public boolean put(Long value) {
        return Main.pref.putLong(getKey(), value);
    }

}
