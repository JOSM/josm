// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.imagery;

import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Callable;

import org.openstreetmap.gui.jmapviewer.tilesources.BingAerialTileSource;
import org.openstreetmap.gui.jmapviewer.tilesources.TileSourceInfo;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.io.CacheCustomContent;
import org.openstreetmap.josm.io.UTFInputStreamReader;
import org.openstreetmap.josm.tools.Utils;
import org.xml.sax.InputSource;

/**
 * Bing TileSource with cached attribution
 *
 * @author Wiktor Niesiobędzki
 * @since 8526
 */
public class CachedAttributionBingAerialTileSource extends BingAerialTileSource {
    private Runnable attributionDownloadedTask;

    /**
     * Creates tile source
     * @param info ImageryInfo description of this tile source
     */
    public CachedAttributionBingAerialTileSource(ImageryInfo info) {
        super(info);
    }

    /**
     * Creates tile source
     * @param info ImageryInfo description of this tile source
     * @param attributionDownloadedTask runnable to be executed once attribution is loaded
     */

    public CachedAttributionBingAerialTileSource(TileSourceInfo info, Runnable attributionDownloadedTask) {
        super(info);
        this.attributionDownloadedTask = attributionDownloadedTask;
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
                        List<Attribution> ret = parseAttributionText(new InputSource(new StringReader((xml))));
                        if (attributionDownloadedTask != null) {
                            GuiHelper.runInEDT(attributionDownloadedTask);
                            attributionDownloadedTask = null;
                        }
                        return ret;
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
