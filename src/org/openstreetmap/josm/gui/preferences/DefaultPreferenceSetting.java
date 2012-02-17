// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences;

public abstract class DefaultPreferenceSetting implements PreferenceSetting {

    private final boolean isExpert;
    
    public DefaultPreferenceSetting() {
        this(false);
    }

    public DefaultPreferenceSetting(boolean isExpert) {
        this.isExpert = isExpert;
    }

    @Override
    public boolean isExpert() {
        return isExpert;
    }
}
