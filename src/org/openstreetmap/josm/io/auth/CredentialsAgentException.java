// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.auth;

/**
 * Exception thrown for errors while handling credentials.
 * <p>
 * Missing credentials and discarded password dialog are not considered an error.
 * At time of writing, methods return <code>null</code> in this case.
 * @see CredentialsAgent
 */
public class CredentialsAgentException extends Exception {

    /**
     * Constructs a new {@code CredentialsAgentException}.
     * @param message the detail message (which is saved for later retrieval by the {@link #getMessage()} method).
     * @param cause the cause (which is saved for later retrieval by the {@link #getCause()} method).
     */
    public CredentialsAgentException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new {@code CredentialsAgentException}.
     * @param message the detail message (which is saved for later retrieval by the {@link #getMessage()} method).
     */
    public CredentialsAgentException(String message) {
        super(message);
    }

    /**
     * Constructs a new {@code CredentialsAgentException}.
     * @param cause the cause (which is saved for later retrieval by the {@link #getCause()} method).
     */
    public CredentialsAgentException(Throwable cause) {
        super(cause);
    }
}
