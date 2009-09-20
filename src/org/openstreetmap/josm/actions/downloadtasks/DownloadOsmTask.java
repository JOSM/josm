// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions.downloadtasks;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import javax.swing.JCheckBox;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.DownloadAction;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.DataSource;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.download.DownloadDialog.DownloadTask;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.BoundingBoxDownloader;
import org.openstreetmap.josm.io.OsmServerLocationReader;
import org.openstreetmap.josm.io.OsmServerReader;
import org.openstreetmap.josm.io.OsmTransferException;
import org.openstreetmap.josm.tools.ExceptionUtil;
import org.xml.sax.SAXException;


/**
 * Open the download dialog and download the data.
 * Run in the worker thread.
 */
public class DownloadOsmTask implements DownloadTask {
    private static final Logger logger = Logger.getLogger(DownloadOsmTask.class.getName());

    private static Bounds currentBounds;
    private Future<Task> task = null;
    private DataSet downloadedData;
    private boolean canceled = false;
    private boolean failed = false;

    private class Task extends PleaseWaitRunnable {
        private OsmServerReader reader;
        private DataSet dataSet;
        private boolean newLayer;
        private boolean canceled;
        private Exception lastException;

        public Task(boolean newLayer, OsmServerReader reader, ProgressMonitor progressMonitor) {
            super(tr("Downloading data"), progressMonitor, false);
            this.reader = reader;
            this.newLayer = newLayer;
        }

        @Override public void realRun() throws IOException, SAXException, OsmTransferException {
            try {
                dataSet = reader.parseOsm(progressMonitor.createSubTaskMonitor(ProgressMonitor.ALL_TICKS, false));
            } catch(Exception e) {
                if (canceled) {
                    logger.warning(tr("Ignoring exception because download has been cancelled. Exception was: {0}" + e.toString()));
                    return;
                }
                if (e instanceof OsmTransferException) {
                    lastException = e;
                } else {
                    lastException = new OsmTransferException(e);
                }
            }
        }

        protected OsmDataLayer getEditLayer() {
            if (Main.map == null) return null;
            if (Main.map.mapView == null) return null;
            return Main.map.mapView.getEditLayer();
        }

        protected int getNumDataLayers() {
            int count = 0;
            if (Main.map == null) return 0;
            if (Main.map.mapView == null) return 0;
            Collection<Layer> layers = Main.map.mapView.getAllLayers();
            for (Layer layer : layers) {
                if (layer instanceof OsmDataLayer) {
                    count++;
                }
            }
            return count;
        }

        protected OsmDataLayer getFirstDataLayer() {
            if (Main.map == null) return null;
            if (Main.map.mapView == null) return null;
            Collection<Layer> layers = Main.map.mapView.getAllLayersAsList();
            for (Layer layer : layers) {
                if (layer instanceof OsmDataLayer)
                    return (OsmDataLayer) layer;
            }
            return null;
        }

        @Override protected void finish() {
            if (canceled)
                return;
            if (lastException != null) {
                getProgressMonitor().setErrorMessage(ExceptionUtil.explainException(lastException));
                DownloadOsmTask.this.setFailed(true);
                return;
            }
            if (dataSet == null)
                return; // user canceled download or error occurred
            if (currentBounds == null)
                return; // no data retrieved
            if (dataSet.allPrimitives().isEmpty()) {
                //progressMonitor.setErrorMessage(tr("No data imported."));
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
                Main.main.addLayer(layer);
            } else {
                OsmDataLayer target;
                target = getEditLayer();
                if (target == null) {
                    target = getFirstDataLayer();
                }
                target.mergeFrom(dataSet);
            }
        }

        @Override protected void cancel() {
            this.canceled = true;
            if (reader != null) {
                reader.cancel();
            }
            DownloadOsmTask.this.setCanceled(true);
        }
    }
    private JCheckBox checkBox = new JCheckBox(tr("OpenStreetMap data"), true);

    private void rememberDownloadedData(DataSet ds) {
        this.downloadedData = ds;
    }

    public boolean isCanceled() {
        return canceled;
    }

    public void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }

    public boolean isFailed() {
        return failed;
    }

    public void setFailed(boolean failed) {
        this.failed = failed;
    }

    public DataSet getDownloadedData() {
        return downloadedData;
    }

    public void download(DownloadAction action, double minlat, double minlon,
            double maxlat, double maxlon, ProgressMonitor progressMonitor) {
        // Swap min and max if user has specified them the wrong way round
        // (easy to do if you are crossing 0, for example)
        // FIXME should perhaps be done in download dialog?
        if (minlat > maxlat) {
            double t = minlat; minlat = maxlat; maxlat = t;
        }
        if (minlon > maxlon) {
            double t = minlon; minlon = maxlon; maxlon = t;
        }

        boolean newLayer = action != null
        && (action.dialog == null || action.dialog.newLayer.isSelected());

        Task t = new Task(newLayer,
                new BoundingBoxDownloader(minlat, minlon, maxlat, maxlon), progressMonitor);
        currentBounds = new Bounds(new LatLon(minlat, minlon), new LatLon(maxlat, maxlon));
        // We need submit instead of execute so we can wait for it to finish and get the error
        // message if necessary. If no one calls getErrorMessage() it just behaves like execute.
        task = Main.worker.submit(t, t);
    }

    /**
     * Loads a given URL from the OSM Server
     * @param True if the data should be saved to a new layer
     * @param The URL as String
     */
    public void loadUrl(boolean new_layer, String url) {
        Task t = new Task(new_layer,
                new OsmServerLocationReader(url),
                NullProgressMonitor.INSTANCE);
        task = Main.worker.submit(t, t);
    }

    public JCheckBox getCheckBox() {
        return checkBox;
    }

    public String getPreferencesSuffix() {
        return "osm";
    }

    /*
     * (non-Javadoc)
     * @see org.openstreetmap.josm.gui.download.DownloadDialog.DownloadTask#getErrorMessage()
     */
    public String getErrorMessage() {
        if(task == null)
            return "";

        try {
            Task t = task.get();
            return t.getProgressMonitor().getErrorMessage() == null
            ? ""
                    : t.getProgressMonitor().getErrorMessage();
        } catch (Exception e) {
            return "";
        }
    }
}
