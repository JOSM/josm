// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions.downloadtasks;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.DataSource;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.BoundingBoxDownloader;
import org.openstreetmap.josm.io.OsmServerLocationReader;
import org.openstreetmap.josm.io.OsmServerReader;
import org.openstreetmap.josm.io.OsmTransferCancelledException;
import org.openstreetmap.josm.io.OsmTransferException;
import org.xml.sax.SAXException;

/**
 * Open the download dialog and download the data.
 * Run in the worker thread.
 */
public class DownloadOsmTask extends AbstractDownloadTask {
    private static final Logger logger = Logger.getLogger(DownloadOsmTask.class.getName());

    private Bounds currentBounds;
    private DataSet downloadedData;
    private DownloadTask downloadTask;

    private void rememberDownloadedData(DataSet ds) {
        this.downloadedData = ds;
    }

    public DataSet getDownloadedData() {
        return downloadedData;
    }

    public Future<?> download(boolean newLayer, Bounds downloadArea, ProgressMonitor progressMonitor) {

        downloadTask = new DownloadTask(newLayer,
                new BoundingBoxDownloader(downloadArea), progressMonitor);
        currentBounds = new Bounds(downloadArea);
        // We need submit instead of execute so we can wait for it to finish and get the error
        // message if necessary. If no one calls getErrorMessage() it just behaves like execute.
        return Main.worker.submit(downloadTask);
    }

    /**
     * Loads a given URL from the OSM Server
     * @param True if the data should be saved to a new layer
     * @param The URL as String
     */
    public Future<?> loadUrl(boolean new_layer, String url, ProgressMonitor progressMonitor) {
        downloadTask = new DownloadTask(new_layer,
                new OsmServerLocationReader(url),
                progressMonitor);
        currentBounds = new Bounds(new LatLon(0,0), new LatLon(0,0));
        return Main.worker.submit(downloadTask);
    }

    public void cancel() {
        if (downloadTask != null) {
            downloadTask.cancel();
        }
    }

    private class DownloadTask extends PleaseWaitRunnable {
        private OsmServerReader reader;
        private DataSet dataSet;
        private boolean newLayer;

        public DownloadTask(boolean newLayer, OsmServerReader reader, ProgressMonitor progressMonitor) {
            super(tr("Downloading data"), progressMonitor, false);
            this.reader = reader;
            this.newLayer = newLayer;
        }

        @Override public void realRun() throws IOException, SAXException, OsmTransferException {
            try {
                if (isCanceled())
                    return;
                dataSet = reader.parseOsm(progressMonitor.createSubTaskMonitor(ProgressMonitor.ALL_TICKS, false));
            } catch(Exception e) {
                if (isCanceled()) {
                    logger.warning(tr("Ignoring exception because download has been cancelled. Exception was: {0}", e.toString()));
                    return;
                }
                if (e instanceof OsmTransferCancelledException) {
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
            return Main.map.mapView.getEditLayer();
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

        @Override protected void finish() {
            if (isFailed() || isCanceled())
                return;
            if (dataSet == null)
                return; // user canceled download or error occurred
            if (currentBounds == null)
                return; // no data retrieved
            if (dataSet.allPrimitives().isEmpty()) {
                rememberErrorMessage(tr("No data found in this area."));
                // need to synthesize a download bounds lest the visual indication of downloaded
                // area doesn't work
                dataSet.dataSources.add(new DataSource(currentBounds, "OpenStreetMap server"));
            }

            rememberDownloadedData(dataSet);
            int numDataLayers = getNumDataLayers();
            if (newLayer || numDataLayers == 0 || (numDataLayers > 1 && getEditLayer() == null)) {
                // the user explicitly wants a new layer, we don't have any layer at all
                // or it is not clear which layer to merge to
                //
                OsmDataLayer layer = new OsmDataLayer(dataSet, OsmDataLayer.createNewName(), null);
                final boolean isDisplayingMapView = Main.isDisplayingMapView();
                
                Main.main.addLayer(layer);

                // If the mapView is not there yet, we cannot calculate the bounds (see constructor of MapView).
                // Otherwise jump to the current download.
                if (isDisplayingMapView) {
                    System.err.println("TRUE!");
                    BoundingXYVisitor v = new BoundingXYVisitor();
                    v.visit(currentBounds);
                    Main.map.mapView.recalculateCenterScale(v);
                }
            } else {
                OsmDataLayer target;
                target = getEditLayer();
                if (target == null) {
                    target = getFirstDataLayer();
                }
                target.mergeFrom(dataSet);
                BoundingXYVisitor v = new BoundingXYVisitor();
                v.visit(currentBounds);
                Main.map.mapView.recalculateCenterScale(v);
                target.onPostDownloadFromServer();
            }
        }

        @Override protected void cancel() {
            setCanceled(true);
            if (reader != null) {
                reader.cancel();
            }
        }
    }
}
