// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.downloadtasks;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.regex.Matcher;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.NodeData;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.PrimitiveData;
import org.openstreetmap.josm.data.osm.PrimitiveId;
import org.openstreetmap.josm.data.osm.RelationData;
import org.openstreetmap.josm.data.osm.WayData;
import org.openstreetmap.josm.data.osm.history.History;
import org.openstreetmap.josm.data.osm.history.HistoryDataSet;
import org.openstreetmap.josm.data.osm.history.HistoryDataSetListener;
import org.openstreetmap.josm.data.osm.history.HistoryNode;
import org.openstreetmap.josm.data.osm.history.HistoryOsmPrimitive;
import org.openstreetmap.josm.data.osm.history.HistoryRelation;
import org.openstreetmap.josm.data.osm.history.HistoryWay;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.history.HistoryLoadTask;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.Compression;
import org.openstreetmap.josm.io.OsmApi;
import org.openstreetmap.josm.io.OsmServerLocationReader;
import org.openstreetmap.josm.io.OsmServerReader;
import org.openstreetmap.josm.io.OsmTransferException;
import org.openstreetmap.josm.io.UrlPatterns.OsmChangeUrlPattern;
import org.openstreetmap.josm.tools.Logging;

/**
 * Task allowing to download OsmChange data (http://wiki.openstreetmap.org/wiki/OsmChange).
 * @since 4530
 */
public class DownloadOsmChangeTask extends DownloadOsmTask {

    @Override
    public String[] getPatterns() {
        return patterns(OsmChangeUrlPattern.class);
    }

    @Override
    public String getTitle() {
        return tr("Download OSM Change");
    }

    @Override
    public Future<?> download(DownloadParams settings, Bounds downloadArea, ProgressMonitor progressMonitor) {
        return null;
    }

    @Override
    public Future<?> loadUrl(DownloadParams settings, final String url, ProgressMonitor progressMonitor) {
        Optional<OsmChangeUrlPattern> urlPattern = Arrays.stream(OsmChangeUrlPattern.values()).filter(p -> p.matches(url)).findFirst();
        String newUrl = url;
        final Matcher matcher = OsmChangeUrlPattern.OSM_WEBSITE.matcher(url);
        if (matcher.matches()) {
            newUrl = OsmApi.getOsmApi().getBaseUrl() + "changeset/" + Long.parseLong(matcher.group(2)) + "/download";
        }
        downloadTask = new DownloadTask(settings, new OsmServerLocationReader(newUrl), progressMonitor, true,
                Compression.byExtension(newUrl));
        // Extract .osc filename from URL to set the new layer name
        extractOsmFilename(settings, urlPattern.orElse(OsmChangeUrlPattern.EXTERNAL_OSC_FILE).pattern(), newUrl);
        return MainApplication.worker.submit(downloadTask);
    }

    /**
     * OsmChange download task.
     */
    protected class DownloadTask extends DownloadOsmTask.DownloadTask {

        /**
         * Constructs a new {@code DownloadTask}.
         * @param settings download settings
         * @param reader OSM data reader
         * @param progressMonitor progress monitor
         * @param zoomAfterDownload If true, the map view will zoom to download area after download
         * @param compression compression to use
         */
        public DownloadTask(DownloadParams settings, OsmServerReader reader, ProgressMonitor progressMonitor,
                boolean zoomAfterDownload, Compression compression) {
            super(settings, reader, progressMonitor, zoomAfterDownload, compression);
        }

        @Override
        protected DataSet parseDataSet() throws OsmTransferException {
            return reader.parseOsmChange(progressMonitor.createSubTaskMonitor(ProgressMonitor.ALL_TICKS, false),
                    compression);
        }

        @Override
        protected void finish() {
            super.finish();
            if (isFailed() || isCanceled() || downloadedData == null)
                return; // user canceled download or error occurred
            try {
                // A changeset does not contain all referred primitives, this is the map of incomplete ones
                // For each incomplete primitive, we'll have to get its state at date it was referred
                Map<OsmPrimitive, Date> toLoad = new HashMap<>();
                for (OsmPrimitive p : downloadedData.allNonDeletedPrimitives()) {
                    if (p.isIncomplete()) {
                        Date timestamp = null;
                        for (OsmPrimitive ref : p.getReferrers()) {
                            if (!ref.isTimestampEmpty()) {
                                timestamp = ref.getTimestamp();
                                break;
                            }
                        }
                        toLoad.put(p, timestamp);
                    }
                }
                if (isCanceled()) return;
                // Let's load all required history
                MainApplication.worker.submit(new HistoryLoaderAndListener(toLoad));
            } catch (RejectedExecutionException e) {
                rememberException(e);
                setFailed(true);
            }
        }
    }

    /**
     * Loads history and updates incomplete primitives.
     */
    private static final class HistoryLoaderAndListener extends HistoryLoadTask implements HistoryDataSetListener {

        private final Map<OsmPrimitive, Date> toLoad;

        private HistoryLoaderAndListener(Map<OsmPrimitive, Date> toLoad) {
            this.toLoad = toLoad;
            this.setChangesetDataNeeded(false);
            add(toLoad.keySet());
            // Updating process is done after all history requests have been made
            HistoryDataSet.getInstance().addHistoryDataSetListener(this);
        }

        @Override
        public void historyUpdated(HistoryDataSet source, PrimitiveId id) {
            Map<OsmPrimitive, Date> toLoadNext = new HashMap<>();
            for (Iterator<Entry<OsmPrimitive, Date>> it = toLoad.entrySet().iterator(); it.hasNext();) {
                Entry<OsmPrimitive, Date> entry = it.next();
                OsmPrimitive p = entry.getKey();
                History history = source.getHistory(p.getPrimitiveId());
                Date date = entry.getValue();
                // If the history has been loaded and a timestamp is known
                if (history != null && date != null) {
                    // Lookup for the primitive version at the specified timestamp
                    HistoryOsmPrimitive hp = history.getByDate(date);
                    if (hp != null) {
                        PrimitiveData data;

                        switch (p.getType()) {
                        case NODE:
                            data = ((HistoryNode) hp).fillPrimitiveData(new NodeData());
                            break;
                        case WAY:
                            data = ((HistoryWay) hp).fillPrimitiveData(new WayData());
                            // Find incomplete nodes to load at next run
                            for (Long nodeId : ((HistoryWay) hp).getNodes()) {
                                if (p.getDataSet().getPrimitiveById(nodeId, OsmPrimitiveType.NODE) == null) {
                                    Node n = new Node(nodeId);
                                    p.getDataSet().addPrimitive(n);
                                    toLoadNext.put(n, date);
                                }
                            }
                            break;
                        case RELATION:
                            data = ((HistoryRelation) hp).fillPrimitiveData(new RelationData());
                            break;
                        default: throw new AssertionError("Unknown primitive type");
                        }

                        // Load the history data
                        try {
                            p.load(data);
                            // Forget this primitive
                            it.remove();
                        } catch (AssertionError e) {
                            Logging.log(Logging.LEVEL_ERROR, "Cannot load "+p+':', e);
                        }
                    }
                }
            }
            source.removeHistoryDataSetListener(this);
            if (toLoadNext.isEmpty()) {
                // No more primitive to update. Processing is finished
                // Be sure all updated primitives are correctly drawn
                MainApplication.getMap().repaint();
            } else {
                // Some primitives still need to be loaded
                // Let's load all required history
                MainApplication.worker.submit(new HistoryLoaderAndListener(toLoadNext));
            }
        }

        @Override
        public void historyDataSetCleared(HistoryDataSet source) {
            // Do nothing
        }
    }
}
