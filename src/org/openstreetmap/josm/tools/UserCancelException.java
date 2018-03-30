// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

/**
 * Exception thrown when an operation is canceled by user.
 * @since 1001 (creation)
 * @since 8919 (move into this package)
 */
public class UserCancelException extends Exception {

    /**
     * Constructs a new {@code UserCancelException}.
     */
    public UserCancelException() {
        super();
    }

    /**
     * Constructs a new {@code UserCancelException} with the specified detail message and cause.
     *
     * @param  message the detail message (which is saved for later retrieval by the {@link #getMessage()} method).
     * @param  cause the cause (which is saved for later retrieval by the {@link #getCause()} method).
     *         (A <code>null</code> value is permitted, and indicates that the cause is nonexistent or unknown.)
     */
    public UserCancelException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new {@code UserCancelException} with the specified detail message.
     *
     * @param  message the detail message (which is saved for later retrieval by the {@link #getMessage()} method).
     */
    public UserCancelException(String message) {
        super(message);
    }

    /**
     * Constructs a new {@code UserCancelException} with the specified cause.
     *
     * @param  cause the cause (which is saved for later retrieval by the {@link #getCause()} method).
     *         (A <code>null</code> value is permitted, and indicates that the cause is nonexistent or unknown.)
     */
    public UserCancelException(Throwable cause) {
        super(cause);
    }
}
