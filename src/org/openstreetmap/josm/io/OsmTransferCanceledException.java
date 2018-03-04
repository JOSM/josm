// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

/**
 * Exception thrown when a communication with the OSM server has been cancelled by the user.
 */
public class OsmTransferCanceledException extends OsmTransferException {

    /**
     * Constructs a new {@code OsmTransferCanceledException}, without message.
     * @param message The detail message (which is saved for later retrieval by the {@link #getMessage} method)
     * @since 8415
     */
    public OsmTransferCanceledException(String message) {
        super(message);
    }

    /**
     * Constructs a new {@code OsmTransferCanceledException}, with given root cause.
     * @param cause the cause (which is saved for later retrieval by the {@link #getCause} method).
     *              A <code>null</code> value is permitted, and indicates that the cause is nonexistent or unknown.
     */
    public OsmTransferCanceledException(Throwable cause) {
        super(cause);
    }
}
