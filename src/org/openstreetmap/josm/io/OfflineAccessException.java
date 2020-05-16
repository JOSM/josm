// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

/**
 * Exception thrown when an online resource is accessed while in offline mode.
 * @since 7434
 */
public class OfflineAccessException extends IllegalStateException {

    /**
     * Constructs a new {@code OfflineAccessException}.
     * @param s the String that contains a detailed message
     */
    public OfflineAccessException(String s) {
        super(s);
    }

    /**
     * Returns a new OfflineAccessException with a translated message for the given resource
     * @param name the translated name/description of the resource
     * @return a new OfflineAccessException
     */
    public static OfflineAccessException forResource(String name) {
        return new OfflineAccessException(tr("{0} not available (offline mode)", name));
    }
}
