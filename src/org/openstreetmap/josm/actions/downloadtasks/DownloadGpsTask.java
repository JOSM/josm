// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.downloadtasks;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.Bounds.ParseMethod;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.layer.GpxLayer;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.markerlayer.MarkerLayer;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.gui.progress.ProgressTaskId;
import org.openstreetmap.josm.gui.progress.ProgressTaskIds;
import org.openstreetmap.josm.io.BoundingBoxDownloader;
import org.openstreetmap.josm.io.GpxImporter;
import org.openstreetmap.josm.io.GpxImporter.GpxImporterData;
import org.openstreetmap.josm.io.OsmServerLocationReader;
import org.openstreetmap.josm.io.OsmServerReader;
import org.openstreetmap.josm.io.OsmTransferException;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.xml.sax.SAXException;

/**
 * Task allowing to download GPS data.
 */
public class DownloadGpsTask extends AbstractDownloadTask {

    private DownloadTask downloadTask;

    private static final String PATTERN_TRACE_ID = "http://.*(osm|openstreetmap).org/trace/\\p{Digit}+/data";

    private static final String PATTERN_TRACKPOINTS_BBOX = "http://.*/api/0.6/trackpoints\\?bbox=.*,.*,.*,.*";

    private static final String PATTERN_EXTERNAL_GPX_SCRIPT = "https?://.*exportgpx.*";
    private static final String PATTERN_EXTERNAL_GPX_FILE = "https?://.*/(.*\\.gpx)";

    protected String newLayerName = null;

    @Override
    public String[] getPatterns() {
        return new String[] {PATTERN_EXTERNAL_GPX_FILE, PATTERN_EXTERNAL_GPX_SCRIPT, PATTERN_TRACE_ID, PATTERN_TRACKPOINTS_BBOX};
    }

    @Override
    public String getTitle() {
        return tr("Download GPS");
    }

    @Override
    public Future<?> download(boolean newLayer, Bounds downloadArea, ProgressMonitor progressMonitor) {
        downloadTask = new DownloadTask(newLayer,
                new BoundingBoxDownloader(downloadArea), progressMonitor);
        // We need submit instead of execute so we can wait for it to finish and get the error
        // message if necessary. If no one calls getErrorMessage() it just behaves like execute.
        return Main.worker.submit(downloadTask);
    }

    @Override
    public Future<?> loadUrl(boolean newLayer, String url, ProgressMonitor progressMonitor) {
        CheckParameterUtil.ensureParameterNotNull(url, "url");
        if (url.matches(PATTERN_TRACE_ID) || url.matches(PATTERN_EXTERNAL_GPX_SCRIPT) || url.matches(PATTERN_EXTERNAL_GPX_FILE)) {
            downloadTask = new DownloadTask(newLayer,
                    new OsmServerLocationReader(url), progressMonitor);
            // Extract .gpx filename from URL to set the new layer name
            Matcher matcher = Pattern.compile(PATTERN_EXTERNAL_GPX_FILE).matcher(url);
            newLayerName = matcher.matches() ? matcher.group(1) : null;
            // We need submit instead of execute so we can wait for it to finish and get the error
            // message if necessary. If no one calls getErrorMessage() it just behaves like execute.
            return Main.worker.submit(downloadTask);

        } else if (url.matches(PATTERN_TRACKPOINTS_BBOX)) {
            String[] table = url.split("\\?|=|&");
            for (int i = 0; i<table.length; i++) {
                if (table[i].equals("bbox") && i<table.length-1 )
                    return download(newLayer, new Bounds(table[i+1], ",", ParseMethod.LEFT_BOTTOM_RIGHT_TOP), progressMonitor);
            }
        }
        return null;
    }

    @Override
    public void cancel() {
        if (downloadTask != null) {
            downloadTask.cancel();
        }
    }

    class DownloadTask extends PleaseWaitRunnable {
        private OsmServerReader reader;
        private GpxData rawData;
        private final boolean newLayer;

        public DownloadTask(boolean newLayer, OsmServerReader reader, ProgressMonitor progressMonitor) {
            super(tr("Downloading GPS data"));
            this.reader = reader;
            this.newLayer = newLayer;
        }

        @Override public void realRun() throws IOException, SAXException, OsmTransferException {
            try {
                if (isCanceled())
                    return;
                ProgressMonitor subMonitor = progressMonitor.createSubTaskMonitor(ProgressMonitor.ALL_TICKS, false);
                rawData = reader.parseRawGps(subMonitor);
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
            String name = newLayerName != null ? newLayerName : tr("Downloaded GPX Data");

            GpxImporterData layers = GpxImporter.loadLayers(rawData, reader.isGpxParsedProperly(), name, tr("Markers from {0}", name));

            GpxLayer gpxLayer = addOrMergeLayer(layers.getGpxLayer(), findGpxMergeLayer());
            addOrMergeLayer(layers.getMarkerLayer(), findMarkerMergeLayer(gpxLayer));

            layers.getPostLayerTask().run();
        }

        private <L extends Layer> L addOrMergeLayer(L layer, L mergeLayer) {
            if (layer == null) return null;
            if (newLayer || mergeLayer == null) {
                Main.main.addLayer(layer);
                return layer;
            } else {
                mergeLayer.mergeFrom(layer);
                Main.map.repaint();
                return mergeLayer;
            }
        }

        private GpxLayer findGpxMergeLayer() {
            if (!Main.isDisplayingMapView())
                return null;
            boolean merge = Main.pref.getBoolean("download.gps.mergeWithLocal", false);
            Layer active = Main.map.mapView.getActiveLayer();
            if (active instanceof GpxLayer && (merge || ((GpxLayer)active).data.fromServer))
                return (GpxLayer) active;
            for (GpxLayer l : Main.map.mapView.getLayersOfType(GpxLayer.class)) {
                if (merge || l.data.fromServer)
                    return l;
            }
            return null;
        }

        private MarkerLayer findMarkerMergeLayer(GpxLayer fromLayer) {
            if (!Main.isDisplayingMapView())
                return null;
            for (MarkerLayer l : Main.map.mapView.getLayersOfType(MarkerLayer.class)) {
                if (fromLayer != null && l.fromLayer == fromLayer)
                    return l;
            }
            return null;
        }

        @Override protected void cancel() {
            setCanceled(true);
            if (reader != null) {
                reader.cancel();
            }
        }

        @Override
        public ProgressTaskId canRunInBackground() {
            return ProgressTaskIds.DOWNLOAD_GPS;
        }
    }

    @Override
    public String getConfirmationMessage(URL url) {
        // TODO
        return null;
    }

    /**
     * Determines if the given URL denotes an OSM gpx-related API call.
     * @param url The url to check
     * @return true if the url matches "Trace ID" API call or "Trackpoints bbox" API call, false otherwise
     * @see GpxData#fromServer
     * @since 5745
     */
    public static final boolean isFromServer(String url) {
        return url != null && (url.matches(PATTERN_TRACE_ID) || url.matches(PATTERN_TRACKPOINTS_BBOX));
    }
}
