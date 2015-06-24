// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.imagery;

import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Callable;

import org.openstreetmap.gui.jmapviewer.tilesources.BingAerialTileSource;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.io.CacheCustomContent;
import org.openstreetmap.josm.io.UTFInputStreamReader;
import org.openstreetmap.josm.tools.Utils;
import org.xml.sax.InputSource;

/**
 * Bing TileSource with cached attribution
 *
 * @author Wiktor Niesiobędzki
 * @since TODO
 *
 */
public class CachedAttributionBingAerialTileSource extends BingAerialTileSource {
    /**
     * Creates tile source
     * @param info ImageryInfo description of this tile source
     */
    public CachedAttributionBingAerialTileSource(ImageryInfo info) {
        super(info);
    }

    class BingAttributionData extends CacheCustomContent<IOException> {

        public BingAttributionData() {
            super("bing.attribution.xml", CacheCustomContent.INTERVAL_HOURLY);
        }

        @Override
        protected byte[] updateData() throws IOException {
            URL u = getAttributionUrl();
            try (Scanner scanner = new Scanner(UTFInputStreamReader.create(Utils.openURL(u)))) {
                String r = scanner.useDelimiter("\\A").next();
                Main.info("Successfully loaded Bing attribution data.");
                return r.getBytes("UTF-8");
            }
        }
    }

    @Override
    protected Callable<List<Attribution>> getAttributionLoaderCallable() {
        return new Callable<List<Attribution>>() {

            @Override
            public List<Attribution> call() throws Exception {
                BingAttributionData attributionLoader = new BingAttributionData();
                int waitTimeSec = 1;
                while (true) {
                    try {
                        String xml = attributionLoader.updateIfRequiredString();
                        return parseAttributionText(new InputSource(new StringReader((xml))));
                    } catch (IOException ex) {
                        Main.warn("Could not connect to Bing API. Will retry in " + waitTimeSec + " seconds.");
                        Thread.sleep(waitTimeSec * 1000L);
                        waitTimeSec *= 2;
                    }
                }
            }
        };
    }
}

