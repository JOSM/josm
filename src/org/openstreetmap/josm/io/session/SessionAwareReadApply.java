// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.session;

import java.util.Map;

/**
 * Interface to support export to session file (and import back) for a class that
 * stores customizable user settings.
 *
 * @since 12594
 */
public interface SessionAwareReadApply {

    /**
     * Export settings to a map of properties.
     * @return map of properties
     */
    Map<String, String> toPropertiesMap();

    /**
     * Import settings from a map of properties.
     * @param properties properties map
     */
    void applyFromPropertiesMap(Map<String, String> properties);
}
