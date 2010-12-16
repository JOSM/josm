// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.preferences;

import org.openstreetmap.josm.Main;

/**
 * captures the common functionality of preference properties
 */
public class AbstractProperty {
    protected final String key;

    public AbstractProperty(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public boolean isSet() {
        return Main.pref.hasKey(key);
    }
}
