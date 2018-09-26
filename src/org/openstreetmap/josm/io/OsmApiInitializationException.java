// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

/**
 * Exception thrown when a communication error occurred with the OSM server during API initialization.
 * @see OsmApi#initialize
 */
public class OsmApiInitializationException extends OsmTransferException {

    /**
     * Constructs an {@code OsmApiInitializationException} with the specified detail message.
     * The cause is not initialized, and may subsequently be initialized by a call to {@link #initCause}.
     *
     * @param message The detail message (which is saved for later retrieval by the {@link #getMessage} method)
     */
    public OsmApiInitializationException(String message) {
        super(message);
    }

    /**
     * Constructs an {@code OsmApiInitializationException} with the specified cause and a detail message of
     * <code>(cause==null ? null : cause.toString())</code>
     * (which typically contains the class and detail message of <code>cause</code>).
     *
     * @param cause the cause (which is saved for later retrieval by the {@link #getCause} method).
     *              A <code>null</code> value is permitted, and indicates that the cause is nonexistent or unknown.
     */
    public OsmApiInitializationException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs an {@code OsmApiInitializationException} with the specified detail message and cause.
     *
     * <p> Note that the detail message associated with {@code cause} is <i>not</i> automatically incorporated
     * into this exception's detail message.
     *
     * @param message The detail message (which is saved for later retrieval by the {@link #getMessage} method)
     * @param cause   The cause (which is saved for later retrieval by the {@link #getCause} method).
     *                A null value is permitted, and indicates that the cause is nonexistent or unknown.
     *
     */
    public OsmApiInitializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
