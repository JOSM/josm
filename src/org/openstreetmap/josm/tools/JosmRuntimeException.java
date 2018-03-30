// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

/**
 * JOSM runtime exception.
 * @since 11374
 */
public class JosmRuntimeException extends RuntimeException {

    /**
     * Constructs a new {@code JosmRuntimeException} with the specified detail message.
     * The cause is not initialized, and may subsequently be initialized by a call to {@link #initCause}.
     *
     * @param message the detail message. The detail message is saved for later retrieval by the {@link #getMessage()} method.
     */
    public JosmRuntimeException(String message) {
        super(message);
    }

    /**
     * Constructs a new {@code JosmRuntimeException} with the specified cause and a detail message of
     * <code>(cause==null ? null : cause.toString())</code> (which typically contains the class and detail message of <code>cause</code>).
     * This constructor is useful for runtime exceptions that are little more than wrappers for other throwables.
     *
     * @param cause the cause (which is saved for later retrieval by the {@link #getCause()} method).
     */
    public JosmRuntimeException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new {@code JosmRuntimeException} with the specified detail message and cause.<p>
     * Note that the detail message associated with {@code cause} is <i>not</i> automatically incorporated in this exception's detail message.
     *
     * @param message the detail message (which is saved for later retrieval by the {@link #getMessage()} method).
     * @param cause the cause (which is saved for later retrieval by the {@link #getCause()} method).
     */
    public JosmRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new runtime exception with the specified detail message, cause,
     * suppression enabled or disabled, and writable stack trace enabled or disabled.
     *
     * @param message the detail message
     * @param cause the cause
     * @param enableSuppression whether or not suppression is enabled or disabled
     * @param writableStackTrace whether or not the stack trace should be writable
     */
    public JosmRuntimeException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
