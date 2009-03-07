// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions.downloadtasks;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.util.concurrent.Future;

import javax.swing.JCheckBox;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.DownloadAction;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.download.DownloadDialog.DownloadTask;
import org.openstreetmap.josm.gui.layer.GpxLayer;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.io.BoundingBoxDownloader;
import org.xml.sax.SAXException;

public class DownloadGpsTask implements DownloadTask {
    private Future<Task> task = null;

    private static class Task extends PleaseWaitRunnable {
        private BoundingBoxDownloader reader;
        private GpxData rawData;
        private final boolean newLayer;
        private String msg = "";

        public Task(boolean newLayer, BoundingBoxDownloader reader, boolean silent, String msg) {
            super(tr("Downloading GPS data"));
            this.msg = msg;
            this.reader = reader;
            this.newLayer = newLayer;
            this.silent = silent;
        }

        @Override public void realRun() throws IOException, SAXException {
            Main.pleaseWaitDlg.setCustomText(msg);
            rawData = reader.parseRawGps();
        }

        @Override protected void finish() {
            if (rawData == null)
                return;
            rawData.recalculateBounds();
            String name = tr("Downloaded GPX Data");
            GpxLayer layer = new GpxLayer(rawData, name);
            Layer x = findMergeLayer();
            if (newLayer || x == null)
                Main.main.addLayer(layer);
            else
                x.mergeFrom(layer);

            Main.pleaseWaitDlg.setCustomText("");
        }

        private Layer findMergeLayer() {
            boolean merge = Main.pref.getBoolean("download.gps.mergeWithLocal", false);
            if (Main.map == null)
                return null;
            Layer active = Main.map.mapView.getActiveLayer();
            if (active != null && active instanceof GpxLayer && (merge || ((GpxLayer)active).data.fromServer))
                return active;
            for (Layer l : Main.map.mapView.getAllLayers())
                if (l instanceof GpxLayer &&  (merge || ((GpxLayer)l).data.fromServer))
                    return l;
            return null;
        }

        @Override protected void cancel() {
            if (reader != null)
                reader.cancel();
            Main.pleaseWaitDlg.cancel.setEnabled(false);
        }
    }

    private JCheckBox checkBox = new JCheckBox(tr("Raw GPS data"));

    public void download(DownloadAction action, double minlat, double minlon,
            double maxlat, double maxlon) {
        download(action, minlat, minlon, maxlat, maxlon, false, "");
    }

    public void download(DownloadAction action, double minlat, double minlon,
            double maxlat, double maxlon, boolean silent, String message) {
        Task t = new Task(action.dialog.newLayer.isSelected(),
                new BoundingBoxDownloader(minlat, minlon, maxlat, maxlon),
                silent,
                message);
        // We need submit instead of execute so we can wait for it to finish and get the error
        // message if necessary. If no one calls getErrorMessage() it just behaves like execute.
        task = Main.worker.submit(t, t);
    }

    public JCheckBox getCheckBox() {
        return checkBox;
    }

    public String getPreferencesSuffix() {
        return "gps";
    }

    public void loadUrl(boolean a,java.lang.String b) {
        // FIXME this is not currently used
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
