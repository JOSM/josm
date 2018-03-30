// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

/**
 * Signals that an error has been reached unexpectedly while parsing.
 *
 * @see java.text.ParseException
 * @since 9385
 */
public class UncheckedParseException extends RuntimeException {

    /**
     * Constructs a new {@code UncheckedParseException}.
     */
    public UncheckedParseException() {
    }

    /**
     * Constructs a new {@code UncheckedParseException} with the specified detail message.
     *
     * @param message the detail message (which is saved for later retrieval by the {@link #getMessage()} method).
     */
    public UncheckedParseException(String message) {
        super(message);
    }

    /**
     * Constructs a new {@code UncheckedParseException} with the specified detail message and cause.
     *
     * @param message the detail message (which is saved for later retrieval by the {@link #getMessage()} method).
     * @param cause   the cause (which is saved for later retrieval by the {@link #getCause()} method).
     *                (A <code>null</code> value is permitted, and indicates that the cause is nonexistent or unknown.)
     */
    public UncheckedParseException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new {@code UncheckedParseException} with the specified cause.
     *
     * @param cause the cause (which is saved for later retrieval by the {@link #getCause()} method).
     *              (A <code>null</code> value is permitted, and indicates that the cause is nonexistent or unknown.)
     */
    public UncheckedParseException(Throwable cause) {
        super(cause);
    }

}
