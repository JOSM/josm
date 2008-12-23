// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions.downloadtasks;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;

import javax.swing.JCheckBox;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.DownloadAction;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.DataSource;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.download.DownloadDialog.DownloadTask;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.io.BoundingBoxDownloader;
import org.openstreetmap.josm.io.OsmServerLocationReader;
import org.openstreetmap.josm.io.OsmServerReader;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;
import org.xml.sax.SAXException;


/**
 * Open the download dialog and download the data.
 * Run in the worker thread.
 */
public class DownloadOsmTask implements DownloadTask {

    private static Bounds currentBounds;

    private static class Task extends PleaseWaitRunnable {
        private OsmServerReader reader;
        private DataSet dataSet;
        private boolean newLayer;

        public Task(boolean newLayer, OsmServerReader reader) {
            super(tr("Downloading data"));
            this.reader = reader;
            this.newLayer = newLayer;
        }

        @Override public void realRun() throws IOException, SAXException {
            dataSet = reader.parseOsm();
        }

        @Override protected void finish() {
            if (dataSet == null)
                return; // user cancelled download or error occoured
            if (dataSet.allPrimitives().isEmpty()) {
                errorMessage = tr("No data imported.");
                // need to synthesize a download bounds lest the visual indication of downloaded
                // area doesn't work
                dataSet.dataSources.add(new DataSource(currentBounds, "OpenStreetMap server"));
            }

            OsmDataLayer layer = new OsmDataLayer(dataSet, tr("Data Layer"), null);
            if (newLayer)
                Main.main.addLayer(layer);
            else
                Main.main.editLayer().mergeFrom(layer);
        }

        @Override protected void cancel() {
            if (reader != null)
                reader.cancel();
        }
    }
    private JCheckBox checkBox = new JCheckBox(tr("OpenStreetMap data"), true);

    public void download(DownloadAction action, double minlat, double minlon, double maxlat, double maxlon) {
        // Swap min and max if user has specified them the wrong way round
        // (easy to do if you are crossing 0, for example)
        // FIXME should perhaps be done in download dialog?
        if (minlat > maxlat) {
            double t = minlat; minlat = maxlat; maxlat = t;
        }
        if (minlon > maxlon) {
            double t = minlon; minlon = maxlon; maxlon = t;
        }

        Task task = new Task(action != null && (action.dialog == null || action.dialog.newLayer.isSelected()), new BoundingBoxDownloader(minlat, minlon, maxlat, maxlon));
        currentBounds = new Bounds(new LatLon(minlat, minlon), new LatLon(maxlat, maxlon));
        Main.worker.execute(task);
    }

    public void loadUrl(boolean new_layer, String url) {
        Task task = new Task(new_layer, new OsmServerLocationReader(url));
        Main.worker.execute(task);
    }




    public JCheckBox getCheckBox() {
        return checkBox;
    }

    public String getPreferencesSuffix() {
        return "osm";
    }
}
