// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.history;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.PrimitiveId;
import org.openstreetmap.josm.data.osm.history.History;
import org.openstreetmap.josm.data.osm.history.HistoryDataSet;
import org.openstreetmap.josm.data.osm.history.HistoryOsmPrimitive;
import org.openstreetmap.josm.gui.ExceptionDialogUtil;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.ChangesetQuery;
import org.openstreetmap.josm.io.OsmApiException;
import org.openstreetmap.josm.io.OsmServerChangesetReader;
import org.openstreetmap.josm.io.OsmServerHistoryReader;
import org.openstreetmap.josm.io.OsmTransferException;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.xml.sax.SAXException;

/**
 * Loads the object history of a collection of objects from the server.
 *
 * It provides a fluent API for configuration.
 *
 * Sample usage:
 *
 * <pre>
 *   HistoryLoadTask task = new HistoryLoadTask()
 *      .add(node)
 *      .add(way)
 *      .add(relation)
 *      .add(aHistoryItem);
 *
 *   MainApplication.worker.execute(task);
 * </pre>
 */
public class HistoryLoadTask extends PleaseWaitRunnable {

    private boolean canceled;
    private Exception lastException;
    private final Set<PrimitiveId> toLoad = new LinkedHashSet<>();
    private HistoryDataSet loadedData;
    private OsmServerHistoryReader reader;
    private boolean getChangesetData = true;
    private boolean collectMissing;
    private final Set<PrimitiveId> missingPrimitives = new LinkedHashSet<>();

    /**
     * Constructs a new {@code HistoryLoadTask}.
     */
    public HistoryLoadTask() {
        super(tr("Load history"), true);
    }

    /**
     * Constructs a new {@code HistoryLoadTask}.
     *
     * @param parent the component to be used as reference to find the
     * parent for {@link org.openstreetmap.josm.gui.PleaseWaitDialog}.
     * Must not be <code>null</code>.
     * @throws NullPointerException if parent is <code>null</code>
     */
    public HistoryLoadTask(Component parent) {
        super(Objects.requireNonNull(parent, "parent"), tr("Load history"), true);
    }

    /**
     * Adds an object whose history is to be loaded.
     *
     * @param pid  the primitive id. Must not be null. Id &gt; 0 required.
     * @return this task
     */
    public HistoryLoadTask add(PrimitiveId pid) {
        CheckParameterUtil.ensureThat(pid.getUniqueId() > 0, "id > 0");
        toLoad.add(pid);
        return this;
    }

    /**
     * Adds an object to be loaded, the object is specified by a history item.
     *
     * @param primitive the history item
     * @return this task
     * @throws NullPointerException if primitive is null
     */
    public HistoryLoadTask add(HistoryOsmPrimitive primitive) {
        return add(primitive.getPrimitiveId());
    }

    /**
     * Adds an object to be loaded, the object is specified by an already loaded object history.
     *
     * @param history the history. Must not be null.
     * @return this task
     * @throws NullPointerException if history is null
     */
    public HistoryLoadTask add(History history) {
        return add(history.getPrimitiveId());
    }

    /**
     * Adds an object to be loaded, the object is specified by an OSM primitive.
     *
     * @param primitive the OSM primitive. Must not be null. primitive.getOsmId() &gt; 0 required.
     * @return this task
     * @throws NullPointerException if the primitive is null
     * @throws IllegalArgumentException if primitive.getOsmId() &lt;= 0
     */
    public HistoryLoadTask add(OsmPrimitive primitive) {
        CheckParameterUtil.ensureThat(primitive.getOsmId() > 0, "id > 0");
        return add(primitive.getOsmPrimitiveId());
    }

    /**
     * Adds a collection of objects to loaded, specified by a collection of OSM primitives.
     *
     * @param primitives the OSM primitives. Must not be <code>null</code>.
     * <code>primitive.getId() &gt; 0</code> required.
     * @return this task
     * @throws NullPointerException if primitives is null
     * @throws IllegalArgumentException if one of the ids in the collection &lt;= 0
     * @since 16123
     */
    public HistoryLoadTask addPrimitiveIds(Collection<? extends PrimitiveId> primitives) {
        primitives.forEach(this::add);
        return this;
    }

    /**
     * Adds a collection of objects to loaded, specified by a collection of OSM primitives.
     *
     * @param primitives the OSM primitives. Must not be <code>null</code>.
     * <code>primitive.getId() &gt; 0</code> required.
     * @return this task
     * @throws NullPointerException if primitives is null
     * @throws IllegalArgumentException if one of the ids in the collection &lt;= 0
     * @since 16123
     */
    public HistoryLoadTask addOsmPrimitives(Collection<? extends OsmPrimitive> primitives) {
        primitives.forEach(this::add);
        return this;
    }

    @Override
    protected void cancel() {
        if (reader != null) {
            reader.cancel();
        }
        canceled = true;
    }

    @Override
    protected void finish() {
        if (isCanceled())
            return;
        if (lastException != null) {
            ExceptionDialogUtil.explainException(lastException);
            return;
        }
        HistoryDataSet.getInstance().mergeInto(loadedData);
    }

    @Override
    protected void realRun() throws SAXException, IOException, OsmTransferException {
        loadedData = new HistoryDataSet();
        int ticks = toLoad.size();
        if (getChangesetData)
            ticks *= 2;
        try {
            progressMonitor.setTicksCount(ticks);
            for (PrimitiveId pid: toLoad) {
                if (canceled) {
                    break;
                }
                loadHistory(pid);
            }
        } catch (OsmTransferException e) {
            lastException = e;
        }
    }

    private void loadHistory(PrimitiveId pid) throws OsmTransferException {
        String msg = getLoadingMessage(pid);
        progressMonitor.indeterminateSubTask(tr(msg, Long.toString(pid.getUniqueId())));
        reader = null;
        HistoryDataSet ds = null;
        try {
            reader = new OsmServerHistoryReader(pid.getType(), pid.getUniqueId());
            if (getChangesetData) {
                ds = loadHistory(reader, progressMonitor);
            } else {
                ds = reader.parseHistory(progressMonitor.createSubTaskMonitor(1, false));
            }
        } catch (OsmApiException e) {
            if (canceled)
                return;
            if (e.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND && collectMissing) {
                missingPrimitives.add(pid);
            } else {
                throw e;
            }
        } catch (OsmTransferException e) {
            if (canceled)
                return;
            throw e;
        }
        if (ds != null) {
            loadedData.mergeInto(ds);
        }
    }

    protected static HistoryDataSet loadHistory(OsmServerHistoryReader reader, ProgressMonitor progressMonitor) throws OsmTransferException {
        HistoryDataSet ds = reader.parseHistory(progressMonitor.createSubTaskMonitor(1, false));
        if (ds != null) {
            // load corresponding changesets (mostly for changeset comment)
            OsmServerChangesetReader changesetReader = new OsmServerChangesetReader();
            List<Long> changesetIds = new ArrayList<>(ds.getChangesetIds());

            // query changesets 100 by 100 (OSM API limit)
            int n = ChangesetQuery.MAX_CHANGESETS_NUMBER;
            for (int i = 0; i < changesetIds.size(); i += n) {
                for (Changeset c : changesetReader.queryChangesets(
                        new ChangesetQuery().forChangesetIds(changesetIds.subList(i, Math.min(i + n, changesetIds.size()))),
                        progressMonitor.createSubTaskMonitor(1, false))) {
                    ds.putChangeset(c);
                }
            }
        }
        return ds;
    }

    protected static String getLoadingMessage(PrimitiveId pid) {
        switch (pid.getType()) {
        case NODE:
            return marktr("Loading history for node {0}");
        case WAY:
            return marktr("Loading history for way {0}");
        case RELATION:
            return marktr("Loading history for relation {0}");
        default:
            return "";
        }
    }

    /**
     * Determines if this task has ben canceled.
     * @return {@code true} if this task has ben canceled
     */
    public boolean isCanceled() {
        return canceled;
    }

    /**
     * Returns the last exception that occurred during loading, if any.
     * @return the last exception that occurred during loading, or {@code null}
     */
    public Exception getLastException() {
        return lastException;
    }

    /**
     * Determine if changeset information is needed. By default it is retrieved.
     * @param b false means don't retrieve changeset data.
     * @since 14763
     */
    public void setChangesetDataNeeded(boolean b) {
        getChangesetData = b;
    }

    /**
     * Determine if missing primitives should be collected. By default they are not collected
     * and the first missing object terminates the task.
     * @param b true means collect missing data and continue.
     * @since 16205
     */
    public void setCollectMissing(boolean b) {
        collectMissing = b;
    }

    /**
     * replies the set of ids of all primitives for which a fetch request to the
     * server was submitted but which are not available from the server (the server
     * replied a return code of 404)
     * @return the set of ids of missing primitives
     * @since 16205
     */
    public Set<PrimitiveId> getMissingPrimitives() {
        return missingPrimitives;
    }

}
