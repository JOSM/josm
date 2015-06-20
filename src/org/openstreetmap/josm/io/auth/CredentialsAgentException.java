// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.auth;

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
