// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins;

import static org.openstreetmap.josm.tools.I18n.tr;

/**
 * Exception that wraps any exception thrown by plugins. It is used in the JOSM main system
 * and there is no particular reason to use this within the plugin itself (although there
 * is also no reason against this.. ;)
 *
 * @author Immanuel.Scholz
 */
public class PluginException extends Exception {
    public final PluginProxy plugin;
    public final String name;

    public PluginException(PluginProxy plugin, String name, Throwable cause) {
        super(tr("An error occurred in plugin {0}", name), cause);
        this.plugin = plugin;
        this.name = name;
    }

    public PluginException(String name, String message) {
        super(message);
        this.plugin = null;
        this.name = name;
    }

    public PluginException(String name, Throwable cause) {
        super(tr("An error occurred in plugin {0}", name), cause);
        this.plugin = null;
        this.name = name;
    }
}
