// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.imagery;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.openstreetmap.gui.jmapviewer.tilesources.BingAerialTileSource;
import org.openstreetmap.gui.jmapviewer.tilesources.TileSourceInfo;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.gui.progress.swing.PleaseWaitProgressMonitor;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.io.CacheCustomContent;
import org.openstreetmap.josm.io.NetworkManager;
import org.openstreetmap.josm.tools.HttpClient;
import org.openstreetmap.josm.tools.Logging;
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
        this(info, null);
    }

    /**
     * Creates tile source
     * @param info ImageryInfo description of this tile source
     * @param attributionDownloadedTask runnable to be executed once attribution is loaded
     */

    public CachedAttributionBingAerialTileSource(TileSourceInfo info, Runnable attributionDownloadedTask) {
        super(info);
        this.attributionDownloadedTask = attributionDownloadedTask;
        // See #23227 and https://github.com/openstreetmap/iD/issues/9153#issuecomment-1781569820
        // Of specific note:
        // > Due to increased usage of Bing Maps imagery APIs, we decided to separate the free usage of the API
        // > (for OSM editors) from the paid usage of the API.
        // We aren't paying for access, so we should solely use the AerialOSM layer.
        super.setLayer("AerialOSM");
    }

    class BingAttributionData extends CacheCustomContent<IOException> {

        BingAttributionData() {
            super("bing.attribution.xml", CacheCustomContent.INTERVAL_HOURLY);
        }

        @Override
        protected byte[] updateData() throws IOException {
            URL u = getAttributionUrl();
            final String r = HttpClient.create(u).connect().fetchContent();
            Logging.info("Successfully loaded Bing attribution data.");
            return r.getBytes(StandardCharsets.UTF_8);
        }

        @Override
        protected boolean isOffline() {
            try {
                return NetworkManager.isOffline(getAttributionUrl().toExternalForm());
            } catch (MalformedURLException e) {
                Logging.error(e);
                return false;
            }
        }
    }

    @Override
    protected Callable<List<Attribution>> getAttributionLoaderCallable() {
        final AtomicReference<List<Attribution>> attributions = new AtomicReference<>();
        final AtomicBoolean finished = new AtomicBoolean();
        return () -> {
            final PleaseWaitProgressMonitor monitor = new PleaseWaitProgressMonitor();
            monitor.beginTask(tr("Attempting to fetch Bing attribution information"), ProgressMonitor.ALL_TICKS);
            final Timer timer = new Timer(Thread.currentThread().getName() + "-timer", true);
            timer.schedule(new AttributionTimerTask(monitor, timer, 1, attributions, finished), 0);
            synchronized (finished) {
                while (!finished.get() && !monitor.isCanceled()) {
                    finished.wait(1000);
                }
            }
            monitor.finishTask();
            monitor.close();
            return attributions.get();
        };
    }

    /**
     * A timer task for fetching Bing attribution information
     */
    private class AttributionTimerTask extends TimerTask {
        private final ProgressMonitor monitor;
        private final Timer timer;
        private final int waitTimeSec;
        private final AtomicReference<List<Attribution>> attributions;
        private final AtomicBoolean finished;

        /**
         * Create a new task for fetching Bing attribution data
         * @param monitor The monitor to update and use for cancellations
         * @param timer The timer thread to add the next task to, if this task failed to fetch the attribution data
         * @param waitTimeSec The time this task is waiting in seconds prior to execution
         * @param attributions The reference to put attributions in
         * @param finished Set to {@code true} when we have successfully fetched attributions <i>or</i> fetching has been cancelled.
         */
        AttributionTimerTask(ProgressMonitor monitor, Timer timer, int waitTimeSec,
                             AtomicReference<List<Attribution>> attributions, AtomicBoolean finished) {
            this.monitor = monitor;
            this.timer = timer;
            this.waitTimeSec = waitTimeSec;
            this.attributions = attributions;
            this.finished = finished;
        }

        @Override
        public void run() {
            BingAttributionData attributionLoader = new BingAttributionData();
            try {
                String xml = attributionLoader.updateIfRequiredString();
                List<Attribution> ret;
                try (StringReader sr = new StringReader(xml)) {
                    ret = parseAttributionText(new InputSource(sr));
                }
                if (attributionDownloadedTask != null) {
                    GuiHelper.runInEDT(attributionDownloadedTask);
                    attributionDownloadedTask = null;
                }
                this.attributions.set(ret);
                this.finished.set(true);
            } catch (IOException ex) {
                final String message = tr("Could not connect to Bing API. Will retry in {0} seconds.", waitTimeSec);
                Logging.log(Logging.LEVEL_WARN, message, ex);
                if (this.monitor.isCanceled()) {
                    this.finished.set(true);
                    return;
                }
                this.monitor.setCustomText(message);
                this.monitor.worked(1);
                final int newWaitTimeSec = 2 * this.waitTimeSec;
                this.timer.schedule(new AttributionTimerTask(this.monitor, this.timer, newWaitTimeSec, this.attributions, this.finished),
                        newWaitTimeSec * 1000L);
            } finally {
                synchronized (this.finished) {
                    this.finished.notifyAll();
                }
            }
        }
    }
}
