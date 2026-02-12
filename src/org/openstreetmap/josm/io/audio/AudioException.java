// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.audio;

/**
 * Generic audio exception. Mainly used to wrap backend exceptions varying between implementations.
 * @since 12328
 */
public class AudioException extends Exception {

    /**
     * Constructs a new {@code AudioException}.
     * @param message the detail message (which is saved for later retrieval by the {@link #getMessage()} method).
     * @param cause the cause (which is saved for later retrieval by the {@link #getCause()} method).
     */
    public AudioException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new {@code AudioException}.
     * @param message the detail message (which is saved for later retrieval by the {@link #getMessage()} method).
     */
    public AudioException(String message) {
        super(message);
    }

    /**
     * Constructs a new {@code AudioException}.
     * @param cause the cause (which is saved for later retrieval by the {@link #getCause()} method).
     */
    public AudioException(Throwable cause) {
        super(cause);
    }
}
