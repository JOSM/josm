// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins;

/**
 * Exception thrown during plugin list parsing.
 * @since 2817
 */
public class PluginListParseException extends Exception {

    /**
     * Constructs a new {@code PluginListParseException} with the specified detail message and cause.
     * @param message message the detail message (which is saved for later retrieval by the {@link #getMessage()} method).
     * @param cause cause the cause (which is saved for later retrieval by the {@link #getCause()} method).
     */
    public PluginListParseException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new {@code PluginListParseException} with the specified cause and a detail message of
     * <code>(cause==null ? null : cause.toString())</code> (which typically contains the class and detail message of <code>cause</code>).
     * @param cause cause the cause (which is saved for later retrieval by the {@link #getCause()} method).
     */
    public PluginListParseException(Throwable cause) {
        super(cause);
    }
}
