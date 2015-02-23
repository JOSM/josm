// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

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
}
