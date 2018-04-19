// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.protocols.data;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.Optional;

import org.openstreetmap.josm.tools.Utils;

/**
 * Protocol handler for {@code data:} URLs.
 * This class must be named "Handler" and in a package "data" (fixed named convention)!
 * <p>
 * See <a href="http://stackoverflow.com/a/9388757/2257172">StackOverflow</a>.
 * @since 10931
 */
public class Handler extends URLStreamHandler {

    @Override
    protected URLConnection openConnection(URL u) throws IOException {
        return new DataConnection(u);
    }

    /**
     * Installs protocol handler.
     */
    public static void install() {
        String pkgName = Handler.class.getPackage().getName();
        String pkg = pkgName.substring(0, pkgName.lastIndexOf('.'));

        String protocolHandlers = Utils.getSystemProperty("java.protocol.handler.pkgs");
        if (protocolHandlers == null || !protocolHandlers.contains(pkg)) {
            StringBuilder sb = new StringBuilder(Optional.ofNullable(protocolHandlers).orElse(""));
            if (sb.length() > 0) {
                sb.append('|');
            }
            sb.append(pkg);
            Utils.updateSystemProperty("java.protocol.handler.pkgs", sb.toString());
        }
    }
}
