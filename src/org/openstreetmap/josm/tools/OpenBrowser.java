// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Desktop;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;

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

    /**
     * Displays an external URI using platform associated software.
     * A web resource will launch platform's browser, an audio file URI will launch audio player, etc.
     * @param uri The URI to display
     * @return <code>null</code> for success or a string in case of an error.
     * @throws IllegalStateException if no platform is set to which opening the URL can be dispatched,
     * {@link PlatformManager#getPlatform}
     */
    public static String displayUrl(URI uri) {
        CheckParameterUtil.ensureParameterNotNull(uri, "uri");

        Logging.info(tr("Opening URL: {0}", uri));

        try {
            if (PlatformManager.getPlatform() != null) {
                // see #5629 #5108 #9568
                PlatformManager.getPlatform().openUrl(uri.toString());
            } else if (Desktop.isDesktopSupported()) {
                // This is not the case with some Linux environments (see below),
                // and not sure about Mac OS X, so we need to handle API failure
                Desktop.getDesktop().browse(uri);
            } else {
                Logging.warn("Neither Platform nor Desktop class is not supported. Cannot open " + uri);
            }
        } catch (IOException e) {
            Logging.warn(e);
            return e.getMessage();
        }
        return null;
    }

    /**
     * Displays an external URL using platform associated software.
     * A web resource will launch platform's browser, an audio file URL will launch audio player, etc.
     * @param url The URL to display
     * @return <code>null</code> for success or a string in case of an error.
     * @throws IllegalStateException if no platform is set to which opening the URL can be dispatched,
     * {@link PlatformManager#getPlatform}
     */
    public static String displayUrl(String url) {
        try {
            return displayUrl(Utils.urlToURI(url));
        } catch (URISyntaxException | MalformedURLException e) {
            Logging.debug(e);
            return e.getMessage();
        }
    }
}
