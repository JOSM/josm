// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;

import org.openstreetmap.josm.io.CachedFile;

/**
 * Custom fonts manager that provides some embedded fonts to ensure
 * a common rendering on different platforms.
 * @since 7383
 */
public final class FontsManager {

    /**
     * List of fonts embedded into JOSM jar.
     */
    private static final Collection<String> INCLUDED_FONTS = Arrays.asList(
            "DroidSans.ttf",
            "DroidSans-Bold.ttf"
    );

    private FontsManager() {
        // Hide constructor for utility classes
    }

    /**
     * Initializes the fonts manager.
     */
    public static void initialize() {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        for (String fontFile : INCLUDED_FONTS) {
            String url = "resource://data/fonts/"+fontFile;
            try (CachedFile cf = new CachedFile(url); InputStream i = cf.getInputStream()) {
                ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, i));
            } catch (IOException | FontFormatException ex) {
                Logging.error("Unable to register font {0}", fontFile);
                Logging.error(ex);
            }
        }
    }
}
