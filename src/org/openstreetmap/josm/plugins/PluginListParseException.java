// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins;

public class PluginListParseException extends Exception {
    public PluginListParseException() {
        super();
    }

    public PluginListParseException(String arg0, Throwable arg1) {
        super(arg0, arg1);
    }

    public PluginListParseException(String arg0) {
        super(arg0);
    }

    public PluginListParseException(Throwable arg0) {
        super(arg0);
    }
}
