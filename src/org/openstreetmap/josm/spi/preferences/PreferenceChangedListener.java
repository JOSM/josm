// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.spi.preferences;

/**
 * Listener to preference change events.
 * @since 12881
 */
@FunctionalInterface
public interface PreferenceChangedListener {

    /**
     * Triggered when a preference entry value changes.
     * @param e the preference change event
     */
    void preferenceChanged(PreferenceChangeEvent e);
    
}
