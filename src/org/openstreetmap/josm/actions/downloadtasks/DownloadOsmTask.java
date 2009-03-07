// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions.downloadtasks;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.util.concurrent.Future;

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
import org.openstreetmap.josm.io.BoundingBoxDownloader;
import org.openstreetmap.josm.io.OsmServerLocationReader;
import org.openstreetmap.josm.io.OsmServerReader;
import org.xml.sax.SAXException;


/**
 * Open the download dialog and download the data.
 * Run in the worker thread.
 */
public class DownloadOsmTask implements DownloadTask {
    private static Bounds currentBounds;
    private Future<Task> task = null;

    private static class Task extends PleaseWaitRunnable {
        private OsmServerReader reader;
        private DataSet dataSet;
        private boolean newLayer;
        private int num = 1;
        private String msg = "";
        
        public Task(boolean newLayer, OsmServerReader reader, boolean silent,
                int numLayers, String msg) {
            super(tr("Downloading data"));
            this.msg = msg;
            this.reader = reader;
            this.newLayer = newLayer;
            this.silent = silent;
        }

        @Override public void realRun() throws IOException, SAXException {
            Main.pleaseWaitDlg.setCustomText(msg);
            dataSet = reader.parseOsm();
        }

        @Override protected void finish() {
            if (dataSet == null)
                return; // user canceled download or error occurred
            if (dataSet.allPrimitives().isEmpty()) {
                // If silent is set to true, we don't want to see information messages
                if(!silent)
                    errorMessage = tr("No data imported.");
                // need to synthesize a download bounds lest the visual indication of downloaded
                // area doesn't work
                dataSet.dataSources.add(new DataSource(currentBounds, "OpenStreetMap server"));
            }
            
            OsmDataLayer layer = new OsmDataLayer(dataSet, tr("Data Layer {0}", num), null);
            if (newLayer)
                Main.main.addLayer(layer);
            else
                Main.main.editLayer().mergeFrom(layer);
            
            Main.pleaseWaitDlg.setCustomText("");
        }

        @Override protected void cancel() {
            if (reader != null)
                reader.cancel();
            Main.pleaseWaitDlg.cancel.setEnabled(false);
        }
    }
    private JCheckBox checkBox = new JCheckBox(tr("OpenStreetMap data"), true);

    public void download(DownloadAction action, double minlat, double minlon,
            double maxlat, double maxlon, boolean silent, String message) {
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
                new BoundingBoxDownloader(minlat, minlon, maxlat, maxlon),
                silent,
                getDataLayersCount(),
                message);
        currentBounds = new Bounds(new LatLon(minlat, minlon), new LatLon(maxlat, maxlon));
        // We need submit instead of execute so we can wait for it to finish and get the error 
        // message if necessary. If no one calls getErrorMessage() it just behaves like execute.
        task = Main.worker.submit(t, t);       
    }
    
    public void download(DownloadAction action, double minlat, double minlon,
            double maxlat, double maxlon) {
        download(action, minlat, minlon, maxlat, maxlon, false, "");
    }

    /**
     * Loads a given URL from the OSM Server
     * @param True if the data should be saved to a new layer
     * @param The URL as String
     */
    public void loadUrl(boolean new_layer, String url) {
        Task t = new Task(new_layer,
                new OsmServerLocationReader(url),
                false,
                getDataLayersCount(),
                "");
        task = Main.worker.submit(t, t);
    }

    public JCheckBox getCheckBox() {
        return checkBox;
    }

    public String getPreferencesSuffix() {
        return "osm";
    }
    
    /**
     * Finds the number of data layers currently opened
     * @return Number of data layers
     */
    private int getDataLayersCount() {
        if(Main.map == null || Main.map.mapView == null)
            return 0;
        int num = 0;
        for(Layer l : Main.map.mapView.getAllLayers())
            if(l instanceof OsmDataLayer)
                num++;
        return num;
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
            return t.errorMessage == null
                ? ""
                : t.errorMessage;
        } catch (Exception e) {
            return "";
        }
    }
}
