// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.openstreetmap.josm.Main;

/**
 * Helper to open platform web browser on different platforms
 *
 * This now delegates the real work to a platform specific class.
 *
 * @author Imi
 */
public final class OpenBrowser {

    private OpenBrowser() {
        // Hide default constructor for utils classes
    }

    private static void displayUrlFallback(URI uri) throws IOException {
        if (Main.platform == null)
            throw new IllegalStateException(tr("Failed to open URL. There is currently no platform set. Please set a platform first."));
        Main.platform.openUrl(uri.toString());
    }

    /**
     * Displays an external URI using platform associated software.
     * A web resource will launch platform's browser, an audio file URI will launch audio player, etc.
     * @param uri The URI to display
     * @return <code>null</code> for success or a string in case of an error.
     * @throws IllegalStateException if no platform is set to which opening the URL can be dispatched,
     * {@link Main#platform}
     */
    public static String displayUrl(URI uri) {
        CheckParameterUtil.ensureParameterNotNull(uri, "uri");

        Logging.info(tr("Opening URL: {0}", uri));

        if (Desktop.isDesktopSupported()) {
            try {
                if (Main.isPlatformWindows()) {
                    // Desktop API works fine under Windows, so we don't try any fallback in case of I/O exceptions because it's not API's fault
                    Desktop.getDesktop().browse(uri);
                } else if (Main.platform instanceof PlatformHookUnixoid || Main.platform instanceof PlatformHookOsx) {
                    // see #5629 #5108 #9568
                    Main.platform.openUrl(uri.toString());
                } else {
                    // This is not the case with some Linux environments (see below),
                    // and not sure about Mac OS X, so we need to handle API failure
                    try {
                        Desktop.getDesktop().browse(uri);
                    } catch (IOException e) {
                        // Workaround for KDE (Desktop API is severely flawed)
                        // see https://bugs.openjdk.java.net/browse/JDK-6486393
                        Logging.log(Logging.LEVEL_WARN, "Desktop class failed. Platform dependent fall back for open url in browser.", e);
                        displayUrlFallback(uri);
                    }
                }
            } catch (IOException e) {
                Logging.warn(e);
                return e.getMessage();
            }
        } else {
            try {
                Logging.warn("Desktop class is not supported. Platform dependent fall back for open url in browser.");
                displayUrlFallback(uri);
            } catch (IOException e) {
                Logging.debug(e);
                return e.getMessage();
            }
        }
        return null;
    }

    /**
     * Displays an external URL using platform associated software.
     * A web resource will launch platform's browser, an audio file URL will launch audio player, etc.
     * @param url The URL to display
     * @return <code>null</code> for success or a string in case of an error.
     * @throws IllegalStateException if no platform is set to which opening the URL can be dispatched,
     * {@link Main#platform}
     */
    public static String displayUrl(String url) {
        try {
            return displayUrl(new URI(url));
        } catch (URISyntaxException e) {
            Logging.debug(e);
            return e.getMessage();
        }
    }
}
