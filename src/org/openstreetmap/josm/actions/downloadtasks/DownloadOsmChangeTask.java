// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.downloadtasks;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.NodeData;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.PrimitiveData;
import org.openstreetmap.josm.data.osm.PrimitiveId;
import org.openstreetmap.josm.data.osm.RelationData;
import org.openstreetmap.josm.data.osm.RelationMemberData;
import org.openstreetmap.josm.data.osm.WayData;
import org.openstreetmap.josm.data.osm.history.History;
import org.openstreetmap.josm.data.osm.history.HistoryDataSet;
import org.openstreetmap.josm.data.osm.history.HistoryDataSetListener;
import org.openstreetmap.josm.data.osm.history.HistoryNode;
import org.openstreetmap.josm.data.osm.history.HistoryOsmPrimitive;
import org.openstreetmap.josm.data.osm.history.HistoryRelation;
import org.openstreetmap.josm.data.osm.history.HistoryWay;
import org.openstreetmap.josm.gui.history.HistoryLoadTask;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.OsmServerLocationReader;
import org.openstreetmap.josm.io.OsmServerReader;
import org.openstreetmap.josm.io.OsmTransferException;

/**
 * Task allowing to download OsmChange data (http://wiki.openstreetmap.org/wiki/OsmChange).
 * @since 4530
 */
public class DownloadOsmChangeTask extends DownloadOsmTask {

    @Override
    public String[] getPatterns() {
        return new String[]{"http://.*/api/0.6/changeset/\\p{Digit}+/download", // OSM API 0.6 changesets
            "https?://.*/.*\\.osc" // Remote .osc files
        };
    }

    @Override
    public String getTitle() {
        return tr("Download OSM Change");
    }
        
    /* (non-Javadoc)
     * @see org.openstreetmap.josm.actions.downloadtasks.DownloadOsmTask#download(boolean, org.openstreetmap.josm.data.Bounds, org.openstreetmap.josm.gui.progress.ProgressMonitor)
     */
    @Override
    public Future<?> download(boolean newLayer, Bounds downloadArea,
            ProgressMonitor progressMonitor) {
        return null;
    }

    /* (non-Javadoc)
     * @see org.openstreetmap.josm.actions.downloadtasks.DownloadOsmTask#loadUrl(boolean, java.lang.String, org.openstreetmap.josm.gui.progress.ProgressMonitor)
     */
    @Override
    public Future<?> loadUrl(boolean new_layer, String url,
            ProgressMonitor progressMonitor) {
        downloadTask = new DownloadTask(new_layer,
                new OsmServerLocationReader(url),
                progressMonitor);
        // Extract .osc filename from URL to set the new layer name
        extractOsmFilename("https?://.*/(.*\\.osc)", url);
        return Main.worker.submit(downloadTask);
    }

    protected class DownloadTask extends DownloadOsmTask.DownloadTask {

        public DownloadTask(boolean newLayer, OsmServerReader reader,
                ProgressMonitor progressMonitor) {
            super(newLayer, reader, progressMonitor);
        }

        /* (non-Javadoc)
         * @see org.openstreetmap.josm.actions.downloadtasks.DownloadOsmTask.DownloadTask#parseDataSet()
         */
        @Override
        protected DataSet parseDataSet() throws OsmTransferException {
            return reader.parseOsmChange(progressMonitor.createSubTaskMonitor(ProgressMonitor.ALL_TICKS, false));
        }

        /* (non-Javadoc)
         * @see org.openstreetmap.josm.actions.downloadtasks.DownloadOsmTask.DownloadTask#finish()
         */
        @Override
        protected void finish() {
            super.finish();
            if (isFailed() || isCanceled() || downloadedData == null)
                return; // user canceled download or error occurred
            try {
                // A changeset does not contain all referred primitives, this is the map of incomplete ones
                // For each incomplete primitive, we'll have to get its state at date it was referred
                Map<OsmPrimitive, Date> toLoad = new HashMap<OsmPrimitive, Date>();
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
                Main.worker.submit(new HistoryLoaderAndListener(toLoad));
            } catch (Exception e) {
                rememberException(e);
                setFailed(true);
            }
        }
    }
    
    /**
     * Loads history and updates incomplete primitives.
     */
    private static class HistoryLoaderAndListener extends HistoryLoadTask implements HistoryDataSetListener {

        private final Map<OsmPrimitive, Date> toLoad;

        public HistoryLoaderAndListener(Map<OsmPrimitive, Date> toLoad) {
            this.toLoad = toLoad;
            add(toLoad.keySet());
            // Updating process is done after all history requests have been made
            HistoryDataSet.getInstance().addHistoryDataSetListener(this);
        }

        @Override
        public void historyUpdated(HistoryDataSet source, PrimitiveId id) {
            Map<OsmPrimitive, Date> toLoadNext = new HashMap<OsmPrimitive, Date>();
            for (Iterator<OsmPrimitive> it = toLoad.keySet().iterator(); it.hasNext();) {
                OsmPrimitive p = it.next();
                History history = source.getHistory(p.getPrimitiveId());
                Date date = toLoad.get(p);
                // If the history has been loaded and a timestamp is known
                if (history != null && date != null) {
                    // Lookup for the primitive version at the specified timestamp
                    HistoryOsmPrimitive hp = history.getByDate(date);
                    if (hp != null) {
                        PrimitiveData data = null;

                        switch (p.getType()) {
                        case NODE:
                            data = new NodeData();
                            ((NodeData)data).setCoor(((HistoryNode)hp).getCoords());
                            break;
                        case WAY:
                            data = new WayData();
                            List<Long> nodeIds = ((HistoryWay)hp).getNodes();
                            ((WayData)data).setNodes(nodeIds);
                            // Find incomplete nodes to load at next run
                            for (Long nodeId : nodeIds) {
                                if (p.getDataSet().getPrimitiveById(nodeId, OsmPrimitiveType.NODE) == null) {
                                    Node n = new Node(nodeId);
                                    p.getDataSet().addPrimitive(n);
                                    toLoadNext.put(n, date);
                                }
                            }
                            break;
                        case RELATION:
                            data = new RelationData();
                            List<RelationMemberData> members = ((HistoryRelation)hp).getMembers();
                            ((RelationData)data).setMembers(members);
                            break;
                        default: throw new AssertionError("Unknown primitive type");
                        }

                        data.setUser(hp.getUser());
                        try {
                            data.setVisible(hp.isVisible());
                        } catch (IllegalStateException e) {
                            Main.error("Cannot change visibility for "+p+": "+e.getMessage());
                        }
                        data.setTimestamp(hp.getTimestamp());
                        data.setKeys(hp.getTags());
                        data.setOsmId(hp.getId(), (int) hp.getVersion());

                        // Load the history data
                        try {
                            p.load(data);
                            // Forget this primitive
                            it.remove();
                        } catch (AssertionError e) {
                            Main.error("Cannot load "+p + ": " + e.getMessage());
                        }
                    }
                }
            }
            source.removeHistoryDataSetListener(this);
            if (toLoadNext.isEmpty()) {
                // No more primitive to update. Processing is finished
                // Be sure all updated primitives are correctly drawn
                Main.map.repaint();
            } else {
                // Some primitives still need to be loaded
                // Let's load all required history
                Main.worker.submit(new HistoryLoaderAndListener(toLoadNext));
            }
        }

        @Override
        public void historyDataSetCleared(HistoryDataSet source) {
        }
    }
}
