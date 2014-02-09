// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.downloadtasks;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.DataSource;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.BoundingBoxDownloader;
import org.openstreetmap.josm.io.OsmServerLocationReader;
import org.openstreetmap.josm.io.OsmServerReader;
import org.openstreetmap.josm.io.OsmTransferCanceledException;
import org.openstreetmap.josm.io.OsmTransferException;
import org.openstreetmap.josm.tools.Utils;
import org.xml.sax.SAXException;

/**
 * Open the download dialog and download the data.
 * Run in the worker thread.
 */
public class DownloadOsmTask extends AbstractDownloadTask {

    protected static final String PATTERN_OSM_API_URL           = "http://.*/api/0.6/(map|nodes?|ways?|relations?|\\*).*";
    protected static final String PATTERN_OVERPASS_API_URL      = "http://.*/interpreter\\?data=.*";
    protected static final String PATTERN_OVERPASS_API_XAPI_URL = "http://.*/xapi(\\?.*\\[@meta\\]|_meta\\?).*";
    protected static final String PATTERN_EXTERNAL_OSM_FILE     = "https?://.*/.*\\.osm";

    protected Bounds currentBounds;
    protected DataSet downloadedData;
    protected DownloadTask downloadTask;

    protected OsmDataLayer targetLayer;

    protected String newLayerName = null;

    @Override
    public String[] getPatterns() {
        if (this.getClass() == DownloadOsmTask.class) {
            return new String[]{PATTERN_OSM_API_URL, PATTERN_OVERPASS_API_URL,
                PATTERN_OVERPASS_API_XAPI_URL, PATTERN_EXTERNAL_OSM_FILE};
        } else {
            return super.getPatterns();
        }
    }

    @Override
    public String getTitle() {
        if (this.getClass() == DownloadOsmTask.class) {
            return tr("Download OSM");
        } else {
            return super.getTitle();
        }
    }

    protected void rememberDownloadedData(DataSet ds) {
        this.downloadedData = ds;
    }

    /**
     * Replies the {@link DataSet} containing the downloaded OSM data.
     * @return The {@link DataSet} containing the downloaded OSM data.
     */
    public DataSet getDownloadedData() {
        return downloadedData;
    }

    @Override
    public Future<?> download(boolean newLayer, Bounds downloadArea, ProgressMonitor progressMonitor) {
        return download(new BoundingBoxDownloader(downloadArea), newLayer, downloadArea, progressMonitor);
    }

    /**
     * Asynchronously launches the download task for a given bounding box.
     *
     * Set <code>progressMonitor</code> to null, if the task should create, open, and close a progress monitor.
     * Set progressMonitor to {@link NullProgressMonitor#INSTANCE} if progress information is to
     * be discarded.
     *
     * You can wait for the asynchronous download task to finish by synchronizing on the returned
     * {@link Future}, but make sure not to freeze up JOSM. Example:
     * <pre>
     *    Future&lt;?&gt; future = task.download(...);
     *    // DON'T run this on the Swing EDT or JOSM will freeze
     *    future.get(); // waits for the dowload task to complete
     * </pre>
     *
     * The following example uses a pattern which is better suited if a task is launched from
     * the Swing EDT:
     * <pre>
     *    final Future&lt;?&gt; future = task.download(...);
     *    Runnable runAfterTask = new Runnable() {
     *       public void run() {
     *           // this is not strictly necessary because of the type of executor service
     *           // Main.worker is initialized with, but it doesn't harm either
     *           //
     *           future.get(); // wait for the download task to complete
     *           doSomethingAfterTheTaskCompleted();
     *       }
     *    }
     *    Main.worker.submit(runAfterTask);
     * </pre>
     * @param reader the reader used to parse OSM data (see {@link OsmServerReader#parseOsm})
     * @param newLayer true, if the data is to be downloaded into a new layer. If false, the task
     *                 selects one of the existing layers as download layer, preferably the active layer.
     * @param downloadArea the area to download
     * @param progressMonitor the progressMonitor
     * @return the future representing the asynchronous task
     */
    public Future<?> download(OsmServerReader reader, boolean newLayer, Bounds downloadArea, ProgressMonitor progressMonitor) {
        return download(new DownloadTask(newLayer, reader, progressMonitor), downloadArea);
    }

    protected Future<?> download(DownloadTask downloadTask, Bounds downloadArea) {
        this.downloadTask = downloadTask;
        this.currentBounds = new Bounds(downloadArea);
        // We need submit instead of execute so we can wait for it to finish and get the error
        // message if necessary. If no one calls getErrorMessage() it just behaves like execute.
        return Main.worker.submit(downloadTask);
    }

    /**
     * This allows subclasses to perform operations on the URL before {@link #loadUrl} is performed.
     */
    protected String modifyUrlBeforeLoad(String url) {
        return url;
    }

    /**
     * Loads a given URL from the OSM Server
     * @param new_layer True if the data should be saved to a new layer
     * @param url The URL as String
     */
    @Override
    public Future<?> loadUrl(boolean new_layer, String url, ProgressMonitor progressMonitor) {
        url = modifyUrlBeforeLoad(url);
        downloadTask = new DownloadTask(new_layer,
                new OsmServerLocationReader(url),
                progressMonitor);
        currentBounds = null;
        // Extract .osm filename from URL to set the new layer name
        extractOsmFilename("https?://.*/(.*\\.osm)", url);
        return Main.worker.submit(downloadTask);
    }

    protected final void extractOsmFilename(String pattern, String url) {
        Matcher matcher = Pattern.compile(pattern).matcher(url);
        newLayerName = matcher.matches() ? matcher.group(1) : null;
    }

    @Override
    public void cancel() {
        if (downloadTask != null) {
            downloadTask.cancel();
        }
    }

    protected class DownloadTask extends PleaseWaitRunnable {
        protected OsmServerReader reader;
        protected DataSet dataSet;
        protected boolean newLayer;

        public DownloadTask(boolean newLayer, OsmServerReader reader, ProgressMonitor progressMonitor) {
            super(tr("Downloading data"), progressMonitor, false);
            this.reader = reader;
            this.newLayer = newLayer;
        }

        protected DataSet parseDataSet() throws OsmTransferException {
            return reader.parseOsm(progressMonitor.createSubTaskMonitor(ProgressMonitor.ALL_TICKS, false));
        }

        @Override public void realRun() throws IOException, SAXException, OsmTransferException {
            try {
                if (isCanceled())
                    return;
                dataSet = parseDataSet();
            } catch(Exception e) {
                if (isCanceled()) {
                    Main.info(tr("Ignoring exception because download has been canceled. Exception was: {0}", e.toString()));
                    return;
                }
                if (e instanceof OsmTransferCanceledException) {
                    setCanceled(true);
                    return;
                } else if (e instanceof OsmTransferException) {
                    rememberException(e);
                } else {
                    rememberException(new OsmTransferException(e));
                }
                DownloadOsmTask.this.setFailed(true);
            }
        }

        protected OsmDataLayer getEditLayer() {
            if (!Main.isDisplayingMapView()) return null;
            return Main.main.getEditLayer();
        }

        protected int getNumDataLayers() {
            int count = 0;
            if (!Main.isDisplayingMapView()) return 0;
            Collection<Layer> layers = Main.map.mapView.getAllLayers();
            for (Layer layer : layers) {
                if (layer instanceof OsmDataLayer) {
                    count++;
                }
            }
            return count;
        }

        protected OsmDataLayer getFirstDataLayer() {
            if (!Main.isDisplayingMapView()) return null;
            Collection<Layer> layers = Main.map.mapView.getAllLayersAsList();
            for (Layer layer : layers) {
                if (layer instanceof OsmDataLayer)
                    return (OsmDataLayer) layer;
            }
            return null;
        }

        protected OsmDataLayer createNewLayer(String layerName) {
            if (layerName == null || layerName.isEmpty()) {
                layerName = OsmDataLayer.createNewName();
            }
            return new OsmDataLayer(dataSet, layerName, null);
        }

        protected OsmDataLayer createNewLayer() {
            return createNewLayer(null);
        }

        @Override protected void finish() {
            if (isFailed() || isCanceled())
                return;
            if (dataSet == null)
                return; // user canceled download or error occurred
            if (dataSet.allPrimitives().isEmpty()) {
                rememberErrorMessage(tr("No data found in this area."));
                // need to synthesize a download bounds lest the visual indication of downloaded
                // area doesn't work
                dataSet.dataSources.add(new DataSource(currentBounds != null ? currentBounds : new Bounds(new LatLon(0, 0)), "OpenStreetMap server"));
            }

            rememberDownloadedData(dataSet);
            int numDataLayers = getNumDataLayers();
            if (newLayer || numDataLayers == 0 || (numDataLayers > 1 && getEditLayer() == null)) {
                // the user explicitly wants a new layer, we don't have any layer at all
                // or it is not clear which layer to merge to
                //
                targetLayer = createNewLayer(newLayerName);
                final boolean isDisplayingMapView = Main.isDisplayingMapView();

                Main.main.addLayer(targetLayer);

                // If the mapView is not there yet, we cannot calculate the bounds (see constructor of MapView).
                // Otherwise jump to the current download.
                if (isDisplayingMapView) {
                    computeBboxAndCenterScale();
                }
            } else {
                targetLayer = getEditLayer();
                if (targetLayer == null) {
                    targetLayer = getFirstDataLayer();
                }
                targetLayer.mergeFrom(dataSet);
                computeBboxAndCenterScale();
                targetLayer.onPostDownloadFromServer();
            }
        }

        protected void computeBboxAndCenterScale() {
            BoundingXYVisitor v = new BoundingXYVisitor();
            if (currentBounds != null) {
                v.visit(currentBounds);
            } else {
                v.computeBoundingBox(dataSet.getNodes());
            }
            Main.map.mapView.recalculateCenterScale(v);
        }

        @Override protected void cancel() {
            setCanceled(true);
            if (reader != null) {
                reader.cancel();
            }
        }
    }

    @Override
    public String getConfirmationMessage(URL url) {
        if (url != null) {
            String urlString = url.toExternalForm();
            if (urlString.matches(PATTERN_OSM_API_URL)) {
                // TODO: proper i18n after stabilization
                Collection<String> items = new ArrayList<String>();
                items.add(tr("OSM Server URL:") + " " + url.getHost());
                items.add(tr("Command")+": "+url.getPath());
                if (url.getQuery() != null) {
                    items.add(tr("Request details: {0}", url.getQuery().replaceAll(",\\s*", ", ")));
                }
                return Utils.joinAsHtmlUnorderedList(items);
            }
            // TODO: other APIs
        }
        return null;
    }
}
