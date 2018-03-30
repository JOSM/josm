// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins;

/**
 * Exception thrown during plugin download.
 * @since 2817
 */
public class PluginDownloadException extends Exception {

    /**
     * Constructs a new {@code PluginDownloadException} with the specified detail message and cause.
     * @param message message the detail message (which is saved for later retrieval by the {@link #getMessage()} method).
     * @param cause cause the cause (which is saved for later retrieval by the {@link #getCause()} method).
     */
    public PluginDownloadException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new {@code PluginDownloadException} with the specified detail message.
     * The cause is not initialized, and may subsequently be initialized by a call to {@link #initCause}.
     * @param message message the detail message (which is saved for later retrieval by the {@link #getMessage()} method).
     */
    public PluginDownloadException(String message) {
        super(message);
    }

    /**
     * Constructs a new {@code PluginDownloadException} with the specified cause and a detail message of
     * <code>(cause==null ? null : cause.toString())</code> (which typically contains the class and detail message of <code>cause</code>).
     * @param cause cause the cause (which is saved for later retrieval by the {@link #getCause()} method).
     */
    public PluginDownloadException(Throwable cause) {
        super(cause);
    }
}
