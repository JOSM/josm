// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import java.text.MessageFormat;
import java.util.function.Predicate;
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
     * Ensures that a parameter is not null and that a certain condition holds.
     * @param <T> parameter type
     * @param obj parameter value
     * @param parameterName parameter name
     * @param conditionMsg string, stating the condition
     * @param condition the condition to check
     * @throws IllegalArgumentException in case the object is null or the condition
     * is violated
     * @since 12713
     */
    public static <T> void ensure(T obj, String parameterName, String conditionMsg, Predicate<T> condition) {
        ensureParameterNotNull(obj, parameterName);
        if (!condition.test(obj))
            throw new IllegalArgumentException(
                    MessageFormat.format("Parameter value ''{0}'' of type {1} is invalid, violated condition: ''{2}'', got ''{3}''",
                            parameterName,
                            obj.getClass().getCanonicalName(),
                            conditionMsg,
                            obj));
    }

    /**
     * Ensures that a parameter is not null and that a certain condition holds.
     * @param <T> parameter type
     * @param obj parameter value
     * @param parameterName parameter name
     * @param condition the condition to check
     * @throws IllegalArgumentException in case the object is null or the condition
     * is violated
     * @since 12713
     */
    public static <T> void ensure(T obj, String parameterName, Predicate<T> condition) {
        ensureParameterNotNull(obj, parameterName);
        if (!condition.test(obj))
            throw new IllegalArgumentException(
                    MessageFormat.format("Parameter value ''{0}'' of type {1} is invalid, got ''{2}''",
                            parameterName,
                            obj.getClass().getCanonicalName(),
                            obj));
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
