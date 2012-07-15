package org.openstreetmap.josm.actions.downloadtasks;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.NodeData;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
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
import org.openstreetmap.josm.gui.history.HistoryLoadTask;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.OsmServerLocationReader;
import org.openstreetmap.josm.io.OsmServerReader;
import org.openstreetmap.josm.io.OsmTransferException;
import org.openstreetmap.josm.tools.Pair;

public class DownloadOsmChangeTask extends DownloadOsmTask {

    @Override
    public boolean acceptsUrl(String url) {
        return url != null && (
                url.matches("http://.*/api/0.6/changeset/\\p{Digit}+/download") // OSM API 0.6 changesets
             || url.matches("http://.*/.*\\.osc")                               // Remote .osc files
                );
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
        extractOsmFilename("http://.*/(.*\\.osc)", url);
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
                // A changeset does not contain all referred primitives, this is the list of incomplete ones
                Set<OsmPrimitive> toLoad = new HashSet<OsmPrimitive>();
                // For each incomplete primitive, we'll have to get its state at date it was referred
                List<Pair<OsmPrimitive, Date>> toMonitor = new ArrayList<Pair<OsmPrimitive, Date>>();
                for (OsmPrimitive p : downloadedData.allNonDeletedPrimitives()) {
                    if (p.isIncomplete()) {
                        Date timestamp = null;
                        for (OsmPrimitive ref : p.getReferrers()) {
                            if (!ref.isTimestampEmpty()) {
                                timestamp = ref.getTimestamp();
                                break;
                            }
                        }
                        if (toLoad.add(p)) {
                            toMonitor.add(new Pair<OsmPrimitive, Date>(p, timestamp));
                        }
                    }
                }
                if (isCanceled()) return;
                // Updating process is asynchronous and done after each history request
                HistoryDataSet.getInstance().addHistoryDataSetListener(new HistoryListener(toMonitor));
                // Let's load all required history
                Main.worker.submit(new HistoryLoadTask().add(toLoad));
            } catch (Exception e) {
                rememberException(e);
                setFailed(true);
            }
        }
    }
    /**
     * Asynchroneous updater of incomplete primitives.
     *
     */
    private static class HistoryListener implements HistoryDataSetListener {

        private final List<Pair<OsmPrimitive, Date>> toMonitor;

        public HistoryListener(List<Pair<OsmPrimitive, Date>> toMonitor) {
            this.toMonitor = toMonitor;
        }

        @Override
        public void historyUpdated(HistoryDataSet source, PrimitiveId id) {
            for (Iterator<Pair<OsmPrimitive, Date>> it = toMonitor.iterator(); it.hasNext();) {
                Pair<OsmPrimitive, Date> pair = it.next();
                History history = source.getHistory(pair.a.getPrimitiveId());
                // If the history has been loaded and a timestamp is known
                if (history != null && pair.b != null) {
                    // Lookup for the primitive version at the specified timestamp
                    HistoryOsmPrimitive hp = history.getByDate(pair.b);
                    if (hp != null) {
                        PrimitiveData data = null;

                        switch (pair.a.getType()) {
                        case NODE:
                            data = new NodeData();
                            ((NodeData)data).setCoor(((HistoryNode)hp).getCoords());
                            break;
                        case WAY:
                            data = new WayData();
                            ((WayData)data).setNodes(((HistoryWay)hp).getNodes());
                            break;
                        case RELATION:
                            data = new RelationData();
                            ((RelationData)data).setMembers(((HistoryRelation)hp).getMembers());
                            break;
                        default: throw new AssertionError();
                        }

                        data.setUser(hp.getUser());
                        try {
                            data.setVisible(hp.isVisible());
                        } catch (IllegalStateException e) {
                            System.err.println("Cannot change visibility for "+pair.a+": "+e.getMessage());
                        }
                        data.setTimestamp(hp.getTimestamp());
                        data.setKeys(hp.getTags());
                        data.setOsmId(hp.getChangesetId(), (int) hp.getVersion());

                        // Load the history data
                        pair.a.load(data);
                        // Forget this primitive
                        it.remove();
                    }
                }
            }
            if (toMonitor.isEmpty()) {
                // No more primitive to update. Processing is finished
                source.removeHistoryDataSetListener(this);
                // Be sure all updated primitives are correctly drawn
                Main.map.repaint();
            }
        }

        @Override
        public void historyDataSetCleared(HistoryDataSet source) {
        }
    }
}
