// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins;

public class PluginDownloadException extends Exception {

    public PluginDownloadException() {
        super();
    }

    public PluginDownloadException(String arg0, Throwable arg1) {
        super(arg0, arg1);
    }

    public PluginDownloadException(String arg0) {
        super(arg0);
    }

    public PluginDownloadException(Throwable arg0) {
        super(arg0);
    }
}
