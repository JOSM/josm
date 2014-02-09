// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.history;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.PrimitiveId;
import org.openstreetmap.josm.data.osm.SimplePrimitiveId;
import org.openstreetmap.josm.data.osm.history.History;
import org.openstreetmap.josm.data.osm.history.HistoryDataSet;
import org.openstreetmap.josm.data.osm.history.HistoryOsmPrimitive;
import org.openstreetmap.josm.gui.ExceptionDialogUtil;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.io.ChangesetQuery;
import org.openstreetmap.josm.io.OsmServerChangesetReader;
import org.openstreetmap.josm.io.OsmServerHistoryReader;
import org.openstreetmap.josm.io.OsmTransferException;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.xml.sax.SAXException;

/**
 * Loads the object history of an collection of objects from the
 * server.
 *
 * It provides a fluent API for configuration.
 *
 * Sample usage:
 *
 * <pre>
 *   HistoryLoadTask task  = new HistoryLoadTask()
 *      .add(1, OsmPrimitiveType.NODE)
 *      .add(1233, OsmPrimitiveType.WAY)
 *      .add(37234, OsmPrimitveType.RELATION)
 *      .add(aHistoryItem);
 *
 *   Main.worker.execute(task);
 *
 * </pre>
 */
public class HistoryLoadTask extends PleaseWaitRunnable {

    private boolean canceled = false;
    private Exception lastException  = null;
    private Set<PrimitiveId> toLoad;
    private HistoryDataSet loadedData;
    private OsmServerHistoryReader reader = null;

    /**
     * Constructs a new {@code HistoryLoadTask}.
     */
    public HistoryLoadTask() {
        super(tr("Load history"), true);
        toLoad = new HashSet<PrimitiveId>();
    }

    /**
     * Constructs a new {@code HistoryLoadTask}.
     *
     * @param parent the component to be used as reference to find the
     * parent for {@link org.openstreetmap.josm.gui.PleaseWaitDialog}.
     * Must not be <code>null</code>.
     * @throws IllegalArgumentException thrown if parent is <code>null</code>
     */
    public HistoryLoadTask(Component parent) {
        super(parent, tr("Load history"), true);
        CheckParameterUtil.ensureParameterNotNull(parent, "parent");
        toLoad = new HashSet<PrimitiveId>();
    }

    /**
     * Adds an object whose history is to be loaded.
     *
     * @param id the object id
     * @param type the object type
     * @return this task
     */
    public HistoryLoadTask add(long id, OsmPrimitiveType type) throws IllegalArgumentException {
        if (id <= 0)
            throw new IllegalArgumentException(MessageFormat.format("Parameter ''{0}'' > 0 expected. Got {1}.", "id", id));
        CheckParameterUtil.ensureParameterNotNull(type, "type");
        SimplePrimitiveId pid = new SimplePrimitiveId(id, type);
        toLoad.add(pid);
        return this;
    }

    /**
     * Adds an object whose history is to be loaded.
     *
     * @param pid  the primitive id. Must not be null. Id &gt; 0 required.
     * @return this task
     */
    public HistoryLoadTask add(PrimitiveId pid) {
        CheckParameterUtil.ensureValidPrimitiveId(pid, "pid");
        toLoad.add(pid);
        return this;
    }

    /**
     * Adds an object to be loaded, the object is specified by a history item.
     *
     * @param primitive the history item
     * @return this task
     * @throws IllegalArgumentException thrown if primitive is null
     */
    public HistoryLoadTask add(HistoryOsmPrimitive primitive) {
        CheckParameterUtil.ensureParameterNotNull(primitive, "primitive");
        toLoad.add(primitive.getPrimitiveId());
        return this;
    }

    /**
     * Adds an object to be loaded, the object is specified by an already loaded object history.
     *
     * @param history the history. Must not be null.
     * @return this task
     * @throws IllegalArgumentException thrown if history is null
     */
    public HistoryLoadTask add(History history) {
        CheckParameterUtil.ensureParameterNotNull(history, "history");
        toLoad.add(history.getPrimitiveId());
        return this;
    }

    /**
     * Adds an object to be loaded, the object is specified by an OSM primitive.
     *
     * @param primitive the OSM primitive. Must not be null. primitive.getId() &gt; 0 required.
     * @return this task
     * @throws IllegalArgumentException thrown if the primitive is null
     * @throws IllegalArgumentException thrown if primitive.getId() &lt;= 0
     */
    public HistoryLoadTask add(OsmPrimitive primitive) {
        CheckParameterUtil.ensureValidPrimitiveId(primitive, "primitive");
        toLoad.add(primitive.getPrimitiveId());
        return this;
    }

    /**
     * Adds a collection of objects to loaded, specified by a collection of OSM primitives.
     *
     * @param primitives the OSM primitives. Must not be <code>null</code>.
     * <code>primitive.getId() &gt; 0</code> required.
     * @return this task
     * @throws IllegalArgumentException thrown if primitives is <code>null</code>
     * @throws IllegalArgumentException thrown if one of the ids in the collection &lt;= 0
     */
    public HistoryLoadTask add(Collection<? extends OsmPrimitive> primitives) {
        CheckParameterUtil.ensureParameterNotNull(primitives, "primitives");
        for (OsmPrimitive primitive: primitives) {
            if (primitive == null) {
                continue;
            }
            add(primitive);
        }
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
        try {
            progressMonitor.setTicksCount(toLoad.size());
            for(PrimitiveId pid: toLoad) {
                if (canceled) {
                    break;
                }
                String msg = "";
                switch(pid.getType()) {
                case NODE: msg = marktr("Loading history for node {0}"); break;
                case WAY: msg = marktr("Loading history for way {0}"); break;
                case RELATION: msg = marktr("Loading history for relation {0}"); break;
                }
                progressMonitor.indeterminateSubTask(tr(msg,
                        Long.toString(pid.getUniqueId())));
                reader = null;
                HistoryDataSet ds = null;
                try {
                    reader = new OsmServerHistoryReader(pid.getType(), pid.getUniqueId());
                    ds = reader.parseHistory(progressMonitor.createSubTaskMonitor(1, false));
                    // load corresponding changesets (mostly for changeset comment)
                    for (final Changeset i : new OsmServerChangesetReader().queryChangesets(
                            new ChangesetQuery().forChangesetIds(ds.getChangesetIds()), progressMonitor.createSubTaskMonitor(1, false))) {
                        ds.putChangeset(i);
                    }
                } catch(OsmTransferException e) {
                    if (canceled)
                        return;
                    throw e;
                }
                loadedData.mergeInto(ds);
            }
        } catch(OsmTransferException e) {
            lastException = e;
            return;
        }
    }

    public boolean isCanceled() {
        return canceled;
    }

    public Exception getLastException() {
        return lastException;
    }
}
