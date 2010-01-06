// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.tools;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.JApplet;

import org.openstreetmap.josm.Main;

/**
 * Helper to open platform web browser on different platforms
 *
 * This now delegates the real work to a platform specific class.
 *
 * @author Imi
 */
public class OpenBrowser {

    /**
     * @return <code>null</code> for success or a string in case of an error.
     * @throws IllegalStateException thrown if no platform is set to which opening the URL can be dispatched,
     * {@see Main#platform}
     */
    public static String displayUrl(String url) throws IllegalStateException {
        if (Main.applet) {
            try {
                JApplet applet = (JApplet) Main.parent;
                applet.getAppletContext().showDocument(new URL(url));
                return null;
            } catch (MalformedURLException mue) {
                return mue.getMessage();
            }
        }

        if (Main.platform == null)
            throw new IllegalStateException(tr("Failed to open URL. There is currently no platform set. Please set a platform first."));
        try {
            Main.platform.openUrl(url);
        } catch (IOException e) {
            return e.getMessage();
        }
        return null;
    }

}
