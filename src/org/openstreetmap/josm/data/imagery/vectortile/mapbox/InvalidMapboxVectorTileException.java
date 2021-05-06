// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.imagery.vectortile.mapbox;

/**
 * Thrown when a mapbox vector tile does not match specifications.
 *
 * @author Taylor Smock
 * @since 17862
 */
public class InvalidMapboxVectorTileException extends RuntimeException {
    /**
     * Create a default {@link InvalidMapboxVectorTileException}.
     */
    public InvalidMapboxVectorTileException() {
        super();
    }

    /**
     * Create a new {@link InvalidMapboxVectorTileException} exception with a message
     * @param message The message
     */
    public InvalidMapboxVectorTileException(final String message) {
        super(message);
    }
}
