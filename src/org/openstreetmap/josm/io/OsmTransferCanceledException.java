// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

/**
 * Exception thrown when a communication with the OSM server has been cancelled by the user.
 */
public class OsmTransferCanceledException extends OsmTransferException {

    /**
     * Constructs a new {@code OsmTransferCanceledException}, without root cause.
     */
    public OsmTransferCanceledException() {
        
    }

    /**
     * Constructs a new {@code OsmTransferCanceledException}, with given root cause.
     * @param cause the cause (which is saved for later retrieval by the {@link #getCause} method).
     *              A <tt>null</tt> value is permitted, and indicates that the cause is nonexistent or unknown.
     */
    public OsmTransferCanceledException(Throwable cause) {
        super(cause);
    }
}
