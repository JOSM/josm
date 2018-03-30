// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection;

/**
 * Exception thrown when a projection cannot be constructed due to invalid input
 * parameters.
 *
 * Used mainly in {@link CustomProjection}, where a parameter string is parsed
 * and converted to a projection.
 */
public class ProjectionConfigurationException extends Exception {

    /**
     * Constructs a new {@code ProjectionConfigurationException}.
     * @param  message the detail message (which is saved for later retrieval
     *         by the {@link #getMessage()} method).
     * @param  cause the cause (which is saved for later retrieval by the
     *         {@link #getCause()} method).  (A <code>null</code> value is
     *         permitted, and indicates that the cause is nonexistent or unknown.)
     */
    public ProjectionConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new {@code ProjectionConfigurationException}.
     * @param   message   the detail message. The detail message is saved for
     *          later retrieval by the {@link #getMessage()} method.
     */
    public ProjectionConfigurationException(String message) {
        super(message);
    }

    /**
     * Constructs a new {@code ProjectionConfigurationException}.
     * @param  cause the cause (which is saved for later retrieval by the
     *         {@link #getCause()} method).  (A <code>null</code> value is
     *         permitted, and indicates that the cause is nonexistent or unknown.)
     */
    public ProjectionConfigurationException(Throwable cause) {
        super(cause);
    }
}
