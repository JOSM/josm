// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions.downloadtasks;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.util.concurrent.Future;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.layer.GpxLayer;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.BoundingBoxDownloader;
import org.openstreetmap.josm.io.OsmTransferException;
import org.xml.sax.SAXException;

public class DownloadGpsTask extends AbstractDownloadTask {

    private DownloadTask downloadTask;

    public Future<?> download(boolean newLayer, Bounds downloadArea, ProgressMonitor progressMonitor) {
        downloadTask = new DownloadTask(newLayer,
                new BoundingBoxDownloader(downloadArea), progressMonitor);
        // We need submit instead of execute so we can wait for it to finish and get the error
        // message if necessary. If no one calls getErrorMessage() it just behaves like execute.
        return Main.worker.submit(downloadTask);
    }

    public Future<?> loadUrl(boolean a,java.lang.String b,  ProgressMonitor progressMonitor) {
        return null;
        // FIXME this is not currently used
    }

    public void cancel() {
        if (downloadTask != null) {
            downloadTask.cancel();
        }
    }

    class DownloadTask extends PleaseWaitRunnable {
        private BoundingBoxDownloader reader;
        private GpxData rawData;
        private final boolean newLayer;

        public DownloadTask(boolean newLayer, BoundingBoxDownloader reader, ProgressMonitor progressMonitor) {
            super(tr("Downloading GPS data"));
            this.reader = reader;
            this.newLayer = newLayer;
        }

        @Override public void realRun() throws IOException, SAXException, OsmTransferException {
            try {
                if (isCanceled())
                    return;
                rawData = reader.parseRawGps(progressMonitor.createSubTaskMonitor(ProgressMonitor.ALL_TICKS, false));
            } catch(Exception e) {
                if (isCanceled())
                    return;
                if (e instanceof OsmTransferException) {
                    rememberException(e);
                } else {
                    rememberException(new OsmTransferException(e));
                }
            }
        }

        @Override protected void finish() {
            if (isCanceled() || isFailed())
                return;
            if (rawData == null)
                return;
            rawData.recalculateBounds();
            String name = tr("Downloaded GPX Data");
            GpxLayer layer = new GpxLayer(rawData, name);
            Layer x = findMergeLayer();
            if (newLayer || x == null) {
                Main.main.addLayer(layer);
            } else {
                x.mergeFrom(layer);
            }
        }

        private Layer findMergeLayer() {
            boolean merge = Main.pref.getBoolean("download.gps.mergeWithLocal", false);
            if (!Main.isDisplayingMapView())
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
            setCanceled(true);
            if (reader != null) {
                reader.cancel();
            }
        }
    }
}
