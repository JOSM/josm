// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

/**
 * Generic exception raised when illegal data is read.
 * @since 2070
 */
public class IllegalDataException extends Exception {

    /**
     * Constructs a new {@code IllegalDataException}.
     * @param message the detail message (which is saved for later retrieval
     *         by the {@link #getMessage()} method).
     * @param cause the cause (which is saved for later retrieval by the
     *         {@link #getCause()} method).
     */
    public IllegalDataException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new {@code IllegalDataException}.
     * @param message the detail message (which is saved for later retrieval
     *         by the {@link #getMessage()} method).
     */
    public IllegalDataException(String message) {
        super(message);
    }

    /**
     * Constructs a new {@code IllegalDataException}.
     * @param cause the cause (which is saved for later retrieval by the
     *         {@link #getCause()} method).
     */
    public IllegalDataException(Throwable cause) {
        super(cause);
    }
}
