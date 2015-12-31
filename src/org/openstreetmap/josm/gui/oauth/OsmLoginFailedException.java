// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.oauth;

/**
 * OSM login failure exception.
 * @since 2746
 */
public class OsmLoginFailedException extends OsmOAuthAuthorizationException {

    /**
     * Constructs a new {@code OsmLoginFailedException} with the specified cause.
     * @param cause the cause (which is saved for later retrieval by the {@link #getCause()} method).
     */
    public OsmLoginFailedException(Throwable cause) {
        super(cause);
    }
}
