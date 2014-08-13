// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import org.openstreetmap.josm.io.CachedFile;

public class FontsManager {

    public static Map<String, Font> fonts;
    public static Collection<String> includedFonts = Arrays.asList(
            "DroidSans.ttf",
            "DroidSans-Bold.ttf"
    );
    
    public static void initialize() {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        for (String fontFile : includedFonts) {
            String url = "resource://data/fonts/"+fontFile;
            try (InputStream i = new CachedFile(url).getInputStream())
            {
                Font f = Font.createFont(Font.TRUETYPE_FONT, i);
                if (f == null) {
                    throw new RuntimeException("unable to load font: "+fontFile);
                }
                ge.registerFont(f);
            } catch (IOException | FontFormatException ex) {
                throw new RuntimeException(ex);
            }
        }
    }
}
