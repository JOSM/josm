// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.downloadtasks;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.DataSource;
import org.openstreetmap.josm.data.ProjectionBounds;
import org.openstreetmap.josm.data.ViewportData;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.io.UpdatePrimitivesTask;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.BoundingBoxDownloader;
import org.openstreetmap.josm.io.OsmServerLocationReader;
import org.openstreetmap.josm.io.OsmServerLocationReader.OsmUrlPattern;
import org.openstreetmap.josm.io.OsmServerReader;
import org.openstreetmap.josm.io.OsmTransferCanceledException;
import org.openstreetmap.josm.io.OsmTransferException;
import org.openstreetmap.josm.io.OverpassDownloadReader;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Utils;
import org.xml.sax.SAXException;

/**
 * Open the download dialog and download the data.
 * Run in the worker thread.
 */
public class DownloadOsmTask extends AbstractDownloadTask<DataSet> {

    protected Bounds currentBounds;
    protected DownloadTask downloadTask;

    protected String newLayerName;

    /** This allows subclasses to ignore this warning */
    protected boolean warnAboutEmptyArea = true;

    protected static final String OVERPASS_INTERPRETER_DATA = "interpreter?data=";

    @Override
    public String[] getPatterns() {
        if (this.getClass() == DownloadOsmTask.class) {
            return Arrays.stream(OsmUrlPattern.values()).map(OsmUrlPattern::pattern).toArray(String[]::new);
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

    @Override
    public Future<?> download(DownloadParams settings, Bounds downloadArea, ProgressMonitor progressMonitor) {
        return download(new BoundingBoxDownloader(downloadArea), settings, downloadArea, progressMonitor);
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
     *    MainApplication.worker.submit(runAfterTask);
     * </pre>
     * @param reader the reader used to parse OSM data (see {@link OsmServerReader#parseOsm})
     * @param settings download settings
     * @param downloadArea the area to download
     * @param progressMonitor the progressMonitor
     * @return the future representing the asynchronous task
     * @since 13927
     */
    public Future<?> download(OsmServerReader reader, DownloadParams settings, Bounds downloadArea, ProgressMonitor progressMonitor) {
        return download(new DownloadTask(settings, reader, progressMonitor, zoomAfterDownload), downloadArea);
    }

    protected Future<?> download(DownloadTask downloadTask, Bounds downloadArea) {
        this.downloadTask = downloadTask;
        this.currentBounds = new Bounds(downloadArea);
        // We need submit instead of execute so we can wait for it to finish and get the error
        // message if necessary. If no one calls getErrorMessage() it just behaves like execute.
        return MainApplication.worker.submit(downloadTask);
    }

    /**
     * This allows subclasses to perform operations on the URL before {@link #loadUrl} is performed.
     * @param url the original URL
     * @return the modified URL
     */
    protected String modifyUrlBeforeLoad(String url) {
        return url;
    }

    /**
     * Loads a given URL from the OSM Server
     * @param settings download settings
     * @param url The URL as String
     */
    @Override
    public Future<?> loadUrl(DownloadParams settings, String url, ProgressMonitor progressMonitor) {
        String newUrl = modifyUrlBeforeLoad(url);
        downloadTask = new DownloadTask(settings, getOsmServerReader(newUrl), progressMonitor);
        currentBounds = null;
        // Extract .osm filename from URL to set the new layer name
        extractOsmFilename(settings, "https?://.*/(.*\\.osm)", newUrl);
        return MainApplication.worker.submit(downloadTask);
    }

    protected OsmServerReader getOsmServerReader(String url) {
        try {
            String host = new URL(url).getHost();
            for (String knownOverpassServer : OverpassDownloadReader.OVERPASS_SERVER_HISTORY.get()) {
                if (host.equals(new URL(knownOverpassServer).getHost())) {
                    int index = url.indexOf(OVERPASS_INTERPRETER_DATA);
                    if (index > 0) {
                        return new OverpassDownloadReader(new Bounds(LatLon.ZERO), knownOverpassServer,
                                Utils.decodeUrl(url.substring(index + OVERPASS_INTERPRETER_DATA.length())));
                    }
                }
            }
        } catch (MalformedURLException e) {
            Logging.error(e);
        }
        return new OsmServerLocationReader(url);
    }

    protected final void extractOsmFilename(DownloadParams settings, String pattern, String url) {
        newLayerName = settings.getLayerName();
        if (newLayerName == null || newLayerName.isEmpty()) {
            Matcher matcher = Pattern.compile(pattern).matcher(url);
            newLayerName = matcher.matches() ? matcher.group(1) : null;
        }
    }

    @Override
    public void cancel() {
        if (downloadTask != null) {
            downloadTask.cancel();
        }
    }

    @Override
    public boolean isSafeForRemotecontrolRequests() {
        return true;
    }

    @Override
    public ProjectionBounds getDownloadProjectionBounds() {
        return downloadTask != null ? downloadTask.computeBbox(currentBounds).orElse(null) : null;
    }

    /**
     * Superclass of internal download task.
     * @since 7636
     */
    public abstract static class AbstractInternalTask extends PleaseWaitRunnable {

        protected final DownloadParams settings;
        protected final boolean zoomAfterDownload;
        protected DataSet dataSet;

        /**
         * Constructs a new {@code AbstractInternalTask}.
         * @param settings download settings
         * @param title message for the user
         * @param ignoreException If true, exception will be propagated to calling code. If false then
         * exception will be thrown directly in EDT. When this runnable is executed using executor framework
         * then use false unless you read result of task (because exception will get lost if you don't)
         * @param zoomAfterDownload If true, the map view will zoom to download area after download
         */
        public AbstractInternalTask(DownloadParams settings, String title, boolean ignoreException, boolean zoomAfterDownload) {
            super(title, ignoreException);
            this.settings = Objects.requireNonNull(settings);
            this.zoomAfterDownload = zoomAfterDownload;
        }

        /**
         * Constructs a new {@code AbstractInternalTask}.
         * @param settings download settings
         * @param title message for the user
         * @param progressMonitor progress monitor
         * @param ignoreException If true, exception will be propagated to calling code. If false then
         * exception will be thrown directly in EDT. When this runnable is executed using executor framework
         * then use false unless you read result of task (because exception will get lost if you don't)
         * @param zoomAfterDownload If true, the map view will zoom to download area after download
         */
        public AbstractInternalTask(DownloadParams settings, String title, ProgressMonitor progressMonitor, boolean ignoreException,
                boolean zoomAfterDownload) {
            super(title, progressMonitor, ignoreException);
            this.settings = Objects.requireNonNull(settings);
            this.zoomAfterDownload = zoomAfterDownload;
        }

        protected OsmDataLayer getEditLayer() {
            return MainApplication.getLayerManager().getEditLayer();
        }

        private static Stream<OsmDataLayer> getModifiableDataLayers() {
            return MainApplication.getLayerManager().getLayersOfType(OsmDataLayer.class)
                    .stream().filter(OsmDataLayer::isDownloadable);
        }

        /**
         * Returns the number of modifiable data layers
         * @return number of modifiable data layers
         * @since 13434
         */
        protected long getNumModifiableDataLayers() {
            return getModifiableDataLayers().count();
        }

        /**
         * Returns the first modifiable data layer
         * @return the first modifiable data layer
         * @since 13434
         */
        protected OsmDataLayer getFirstModifiableDataLayer() {
            return getModifiableDataLayers().findFirst().orElse(null);
        }

        /**
         * Creates a name for a new layer by utilizing the settings ({@link DownloadParams#getLayerName()}) or
         * {@link OsmDataLayer#createNewName()} if the former option is {@code null}.
         *
         * @return a name for a new layer
         * @since 14347
         */
        protected String generateLayerName() {
            return Optional.ofNullable(settings.getLayerName())
                .filter(layerName -> !Utils.isStripEmpty(layerName))
                .orElse(OsmDataLayer.createNewName());
        }

        /**
         * Can be overridden (e.g. by plugins) if a subclass of {@link OsmDataLayer} is needed.
         * If you want to change how the name is determined, consider overriding
         * {@link #generateLayerName()} instead.
         *
         * @param ds the dataset on which the layer is based, must be non-null
         * @param layerName the name of the new layer, must be either non-blank or non-present
         * @return a new instance of {@link OsmDataLayer} constructed with the given arguments
         * @since 14347
         */
        protected OsmDataLayer createNewLayer(final DataSet ds, final Optional<String> layerName) {
            if (layerName.filter(Utils::isStripEmpty).isPresent()) {
                throw new IllegalArgumentException("Blank layer name!");
            }
            return new OsmDataLayer(
                Objects.requireNonNull(ds, "dataset parameter"),
                layerName.orElseGet(this::generateLayerName),
                null
            );
        }

        /**
         * Convenience method for {@link #createNewLayer(DataSet, Optional)}, uses the dataset
         * from field {@link #dataSet} and applies the settings from field {@link #settings}.
         *
         * @param layerName an optional layer name, must be non-blank if the [Optional] is present
         * @return a newly constructed layer
         * @since 14347
         */
        protected final OsmDataLayer createNewLayer(final Optional<String> layerName) {
            Optional.ofNullable(settings.getDownloadPolicy())
                .ifPresent(dataSet::setDownloadPolicy);
            Optional.ofNullable(settings.getUploadPolicy())
                .ifPresent(dataSet::setUploadPolicy);
            if (dataSet.isLocked() && !settings.isLocked()) {
                dataSet.unlock();
            } else if (!dataSet.isLocked() && settings.isLocked()) {
                dataSet.lock();
            }
            return createNewLayer(dataSet, layerName);
        }

        protected Optional<ProjectionBounds> computeBbox(Bounds bounds) {
            BoundingXYVisitor v = new BoundingXYVisitor();
            if (bounds != null) {
                v.visit(bounds);
            } else {
                v.computeBoundingBox(dataSet.getNodes());
            }
            return Optional.ofNullable(v.getBounds());
        }

        protected OsmDataLayer addNewLayerIfRequired(String newLayerName) {
            long numDataLayers = getNumModifiableDataLayers();
            if (settings.isNewLayer() || numDataLayers == 0 || (numDataLayers > 1 && getEditLayer() == null)) {
                // the user explicitly wants a new layer, we don't have any layer at all
                // or it is not clear which layer to merge to
                final OsmDataLayer layer = createNewLayer(Optional.ofNullable(newLayerName).filter(it -> !Utils.isStripEmpty(it)));
                MainApplication.getLayerManager().addLayer(layer, zoomAfterDownload);
                return layer;
            }
            return null;
        }

        protected void loadData(String newLayerName, Bounds bounds) {
            OsmDataLayer layer = addNewLayerIfRequired(newLayerName);
            if (layer == null) {
                layer = getEditLayer();
                if (layer == null || !layer.isDownloadable()) {
                    layer = getFirstModifiableDataLayer();
                }
                Collection<OsmPrimitive> primitivesToUpdate = searchPrimitivesToUpdate(bounds, layer.getDataSet());
                layer.mergeFrom(dataSet);
                MapFrame map = MainApplication.getMap();
                if (map != null && zoomAfterDownload) {
                    computeBbox(bounds).map(ViewportData::new).ifPresent(map.mapView::zoomTo);
                }
                if (!primitivesToUpdate.isEmpty()) {
                    MainApplication.worker.submit(new UpdatePrimitivesTask(layer, primitivesToUpdate));
                }
                layer.onPostDownloadFromServer();
            }
        }

        /**
         * Look for primitives deleted on server (thus absent from downloaded data)
         * but still present in existing data layer
         * @param bounds download bounds
         * @param ds existing data set
         * @return the primitives to update
         */
        private Collection<OsmPrimitive> searchPrimitivesToUpdate(Bounds bounds, DataSet ds) {
            if (bounds == null)
                return Collections.emptySet();
            Collection<OsmPrimitive> col = new ArrayList<>();
            ds.searchNodes(bounds.toBBox()).stream().filter(n -> !n.isNew() && !dataSet.containsNode(n)).forEachOrdered(col::add);
            if (!col.isEmpty()) {
                Set<Way> ways = new HashSet<>();
                Set<Relation> rels = new HashSet<>();
                for (OsmPrimitive n : col) {
                    for (OsmPrimitive ref : n.getReferrers()) {
                        if (ref.isNew()) {
                            continue;
                        } else if (ref instanceof Way) {
                            ways.add((Way) ref);
                        } else if (ref instanceof Relation) {
                            rels.add((Relation) ref);
                        }
                    }
                }
                ways.stream().filter(w -> !dataSet.containsWay(w)).forEachOrdered(col::add);
                rels.stream().filter(r -> !dataSet.containsRelation(r)).forEachOrdered(col::add);
            }
            return col;
        }
    }

    protected class DownloadTask extends AbstractInternalTask {
        protected final OsmServerReader reader;

        /**
         * Constructs a new {@code DownloadTask}.
         * @param settings download settings
         * @param reader OSM data reader
         * @param progressMonitor progress monitor
         * @since 13927
         */
        public DownloadTask(DownloadParams settings, OsmServerReader reader, ProgressMonitor progressMonitor) {
            this(settings, reader, progressMonitor, true);
        }

        /**
         * Constructs a new {@code DownloadTask}.
         * @param settings download settings
         * @param reader OSM data reader
         * @param progressMonitor progress monitor
         * @param zoomAfterDownload If true, the map view will zoom to download area after download
         * @since 13927
         */
        public DownloadTask(DownloadParams settings, OsmServerReader reader, ProgressMonitor progressMonitor, boolean zoomAfterDownload) {
            super(settings, tr("Downloading data"), progressMonitor, false, zoomAfterDownload);
            this.reader = reader;
        }

        protected DataSet parseDataSet() throws OsmTransferException {
            return reader.parseOsm(progressMonitor.createSubTaskMonitor(ProgressMonitor.ALL_TICKS, false));
        }

        @Override
        public void realRun() throws IOException, SAXException, OsmTransferException {
            try {
                if (isCanceled())
                    return;
                dataSet = parseDataSet();
            } catch (OsmTransferException e) {
                if (isCanceled()) {
                    Logging.info(tr("Ignoring exception because download has been canceled. Exception was: {0}", e.toString()));
                    return;
                }
                if (e instanceof OsmTransferCanceledException) {
                    setCanceled(true);
                    return;
                } else {
                    rememberException(e);
                }
                DownloadOsmTask.this.setFailed(true);
            }
        }

        @Override
        protected void finish() {
            if (isFailed() || isCanceled())
                return;
            if (dataSet == null)
                return; // user canceled download or error occurred
            if (dataSet.allPrimitives().isEmpty()) {
                if (warnAboutEmptyArea) {
                    rememberErrorMessage(tr("No data found in this area."));
                }
                String remark = dataSet.getRemark();
                if (remark != null && !remark.isEmpty()) {
                    rememberErrorMessage(remark);
                }
                // need to synthesize a download bounds lest the visual indication of downloaded area doesn't work
                dataSet.addDataSource(new DataSource(currentBounds != null ? currentBounds :
                    new Bounds(LatLon.ZERO), "OpenStreetMap server"));
            }

            rememberDownloadedData(dataSet);
            loadData(newLayerName, currentBounds);
        }

        @Override
        protected void cancel() {
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
            if (urlString.matches(OsmUrlPattern.OSM_API_URL.pattern())) {
                // TODO: proper i18n after stabilization
                Collection<String> items = new ArrayList<>();
                items.add(tr("OSM Server URL:") + ' ' + url.getHost());
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
