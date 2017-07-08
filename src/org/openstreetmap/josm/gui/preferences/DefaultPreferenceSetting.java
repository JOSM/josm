// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences;

/**
 * Abstract base class for {@link PreferenceSetting} implementations.
 *
 * Handles the flag that indicates if a PreferenceSetting is and expert option
 * or not.
 */
public abstract class DefaultPreferenceSetting implements PreferenceSetting {

    private final boolean isExpert;

    /**
     * Constructs a new DefaultPreferenceSetting.
     *
     * (Not an expert option by default.)
     */
    public DefaultPreferenceSetting() {
        this(false);
    }

    /**
     * Constructs a new DefaultPreferenceSetting.
     *
     * @param isExpert true, if it is an expert option
     */
    public DefaultPreferenceSetting(boolean isExpert) {
        this.isExpert = isExpert;
    }

    @Override
    public boolean isExpert() {
        return isExpert;
    }
}
