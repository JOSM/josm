// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import java.text.MessageFormat;
import java.util.function.Supplier;

/**
 * This utility class provides a collection of static helper methods for checking
 * parameters at run-time.
 * @since 2711
 */
public final class CheckParameterUtil {

    private CheckParameterUtil() {
        // Hide default constructor for utils classes
    }

    /**
     * Ensures a parameter is not {@code null}
     * @param value The parameter to check
     * @param parameterName The parameter name
     * @throws IllegalArgumentException if the parameter is {@code null}
     */
    public static void ensureParameterNotNull(Object value, String parameterName) {
        if (value == null)
            throw new IllegalArgumentException(MessageFormat.format("Parameter ''{0}'' must not be null", parameterName));
    }

    /**
     * Ensures a parameter is not {@code null}. Can find line number in the stack trace, so parameter name is optional
     * @param value The parameter to check
     * @throws IllegalArgumentException if the parameter is {@code null}
     * @since 3871
     */
    public static void ensureParameterNotNull(Object value) {
        if (value == null)
            throw new IllegalArgumentException("Parameter must not be null");
    }

    /**
     * Ensures that the condition {@code condition} holds.
     * @param condition The condition to check
     * @param message error message
     * @throws IllegalArgumentException if the condition does not hold
     * @see #ensureThat(boolean, Supplier)
     */
    public static void ensureThat(boolean condition, String message) {
        if (!condition)
            throw new IllegalArgumentException(message);
    }

    /**
     * Ensures that the condition {@code condition} holds.
     *
     * This method can be used when the message is not a plain string literal,
     * but somehow constructed. Using a {@link Supplier} improves the performance,
     * as the string construction is skipped when the condition holds.
     * @param condition The condition to check
     * @param messageSupplier supplier of the error message
     * @throws IllegalArgumentException if the condition does not hold
     * @since 12822
     */
    public static void ensureThat(boolean condition, Supplier<String> messageSupplier) {
        if (!condition)
            throw new IllegalArgumentException(messageSupplier.get());
    }
}
