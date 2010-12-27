// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.imagery;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.StringTokenizer;

import javax.imageio.ImageIO;

import org.openstreetmap.josm.data.preferences.StringProperty;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.layer.WMSLayer;

public class HTMLGrabber extends WMSGrabber {
    public static final StringProperty PROP_BROWSER = new StringProperty("imagery.wms.browser", "webkit-image {0}");

    public HTMLGrabber(MapView mv, WMSLayer layer) {
        super(mv, layer);
    }

    @Override
    protected BufferedImage grab(URL url, int attempt) throws IOException {
        String urlstring = url.toExternalForm();

        System.out.println("Grabbing HTML " + (attempt > 1? "(attempt " + attempt + ") ":"") + url);

        ArrayList<String> cmdParams = new ArrayList<String>();
        StringTokenizer st = new StringTokenizer(MessageFormat.format(PROP_BROWSER.get(), urlstring));
        while( st.hasMoreTokens() ) {
            cmdParams.add(st.nextToken());
        }

        ProcessBuilder builder = new ProcessBuilder( cmdParams);

        Process browser;
        try {
            browser = builder.start();
        } catch(IOException ioe) {
            throw new IOException( "Could not start browser. Please check that the executable path is correct.\n" + ioe.getMessage() );
        }

        BufferedImage img = ImageIO.read(browser.getInputStream());
        cache.saveImg(urlstring, img);
        return img;
    }
}
