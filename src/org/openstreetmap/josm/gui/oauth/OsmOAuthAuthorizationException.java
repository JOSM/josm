// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.oauth;

/**
 * OSM OAuth authorization exception.
 * @since 2746
 */
public class OsmOAuthAuthorizationException extends Exception {

    /**
     * Constructs a new {@code OsmLoginFailedException} with the specified detail message.
     * @param message the detail message. The detail message is saved for later retrieval by the {@link #getMessage()} method.
     */
    public OsmOAuthAuthorizationException(String message) {
        super(message);
    }

    /**
     * Constructs a new {@code OsmLoginFailedException} with the specified cause.
     * @param cause the cause (which is saved for later retrieval by the {@link #getCause()} method).
     */
    public OsmOAuthAuthorizationException(Throwable cause) {
        super(cause);
    }
}
