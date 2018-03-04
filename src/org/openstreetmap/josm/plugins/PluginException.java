// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins;

import static org.openstreetmap.josm.tools.I18n.tr;

/**
 * Exception that wraps any exception thrown by plugins. It is used in the JOSM main system
 * and there is no particular reason to use this within the plugin itself (although there
 * is also no reason against this.. ;)
 *
 * @author Immanuel.Scholz
 * @since 149
 */
public class PluginException extends Exception {

    /** Plugin proxy, can be null */
    public final transient PluginProxy plugin;

    /**
     * Constructs a new {@code PluginException} with the specified plugin and cause.
     * @param plugin plugin proxy
     * @param name plugin name
     * @param cause cause the cause (which is saved for later retrieval by the {@link #getCause()} method).
     */
    public PluginException(PluginProxy plugin, String name, Throwable cause) {
        super(tr("An error occurred in plugin {0}", name), cause);
        this.plugin = plugin;
    }

    /**
     * Constructs a new {@code PluginException} with the specified detail message.
     * @param message message the detail message (which is saved for later retrieval by the {@link #getMessage()} method).
     */
    public PluginException(String message) {
        super(message);
        this.plugin = null;
    }

    /**
     * Constructs a new {@code PluginException} with the specified plugin name, cause and a detail message of
     * <code>(cause==null ? null : cause.toString())</code> (which typically contains the class and detail message of <code>cause</code>).
     * @param name plugin name
     * @param cause cause the cause (which is saved for later retrieval by the {@link #getCause()} method).
     */
    public PluginException(String name, Throwable cause) {
        super(tr("An error occurred in plugin {0}", name), cause);
        this.plugin = null;
    }
}
