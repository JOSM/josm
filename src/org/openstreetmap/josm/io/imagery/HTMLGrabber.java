// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.imagery;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.imageio.ImageIO;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.preferences.StringProperty;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.layer.WMSLayer;
import org.openstreetmap.josm.tools.Utils;

public class HTMLGrabber extends WMSGrabber {
    public static final StringProperty PROP_BROWSER = new StringProperty("imagery.wms.browser", "webkit-image {0}");

    public HTMLGrabber(MapView mv, WMSLayer layer, boolean localOnly) {
        super(mv, layer, localOnly);
    }

    @Override
    protected BufferedImage grab(WMSRequest request, URL url, int attempt) throws IOException {
        String urlstring = url.toExternalForm();

        Main.info("Grabbing HTML " + (attempt > 1? "(attempt " + attempt + ") ":"") + url);

        List<String> cmdParams = new ArrayList<String>();
        StringTokenizer st = new StringTokenizer(MessageFormat.format(PROP_BROWSER.get(), urlstring));
        while (st.hasMoreTokens()) {
            cmdParams.add(st.nextToken());
        }

        ProcessBuilder builder = new ProcessBuilder( cmdParams);

        Process browser;
        try {
            browser = builder.start();
        } catch (IOException ioe) {
            throw new IOException("Could not start browser. Please check that the executable path is correct.\n" + ioe.getMessage(), ioe);
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Utils.copyStream(browser.getInputStream(), baos);
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        BufferedImage img = layer.normalizeImage(ImageIO.read(bais));
        bais.reset();
        layer.cache.saveToCache(layer.isOverlapEnabled()?img:null, bais, Main.getProjection(), request.getPixelPerDegree(), b.minEast, b.minNorth);

        return img;
    }
}
