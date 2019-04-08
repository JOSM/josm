// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.spi.preferences;

/**
 * Event triggered when a preference entry value changes.
 * @since 12881
 */
public interface PreferenceChangeEvent {

    /**
     * Returns the class source of this event.
     * @return The class source of this event
     * @since 14977
     */
    Class<?> getSource();

    /**
     * Returns the preference key.
     * @return the preference key
     */
    String getKey();

    /**
     * Returns the old preference value.
     * @return the old preference value
     */
    Setting<?> getOldValue();

    /**
     * Returns the new preference value.
     * @return the new preference value
     */
    Setting<?> getNewValue();
}
