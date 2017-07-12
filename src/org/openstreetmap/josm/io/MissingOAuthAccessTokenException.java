// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

/**
 * Exception thrown when a valid OAuth access token was expected, but not found.
 */
public class MissingOAuthAccessTokenException extends OsmTransferException {

    /**
     * Constructs a new {@code MissingOAuthAccessTokenException}.
     */
    public MissingOAuthAccessTokenException() {
        super();
    }

    /**
     * Constructs a new {@code MissingOAuthAccessTokenException} with the specified detail message and cause.
     * @param message The detail message (which is saved for later retrieval by the {@link #getMessage} method)
     * @param cause   The cause (which is saved for later retrieval by the {@link #getCause} method).
     *                A null value is permitted, and indicates that the cause is nonexistent or unknown.
     */
    public MissingOAuthAccessTokenException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new {@code MissingOAuthAccessTokenException} with the specified detail message.
     * @param message The detail message (which is saved for later retrieval by the {@link #getMessage} method)
     */
    public MissingOAuthAccessTokenException(String message) {
        super(message);
    }

    /**
     * Constructs a new {@code MissingOAuthAccessTokenException} with the specified cause.
     * @param cause   The cause (which is saved for later retrieval by the {@link #getCause} method).
     *                A null value is permitted, and indicates that the cause is nonexistent or unknown.
     */
    public MissingOAuthAccessTokenException(Throwable cause) {
        super(cause);
    }
}
