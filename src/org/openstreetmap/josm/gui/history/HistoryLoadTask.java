// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.history;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.history.History;
import org.openstreetmap.josm.data.osm.history.HistoryDataSet;
import org.openstreetmap.josm.data.osm.history.HistoryOsmPrimitive;
import org.openstreetmap.josm.gui.ExceptionDialogUtil;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.io.OsmApi;
import org.openstreetmap.josm.io.OsmServerHistoryReader;
import org.openstreetmap.josm.io.OsmTransferException;
import org.xml.sax.SAXException;

/**
 * Loads the the object history of an collection of objects from the
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

    private boolean cancelled = false;
    private Exception lastException  = null;
    private Map<Long, OsmPrimitiveType> toLoad;
    private HistoryDataSet loadedData;

    public HistoryLoadTask() {
        super(tr("Load history"), true);
        toLoad = new HashMap<Long, OsmPrimitiveType>();
    }

    /**
     * Adds an object whose history is to be loaded.
     * 
     * @param id the object id
     * @param type the object type
     * @return this task
     */
    public HistoryLoadTask add(long id, OsmPrimitiveType type) {
        if (id <= 0)
            throw new IllegalArgumentException(tr("ID > 0 expected. Got {0}.", id));
        if (type == null)
            throw new IllegalArgumentException(tr("Parameter ''{0}'' must not be null.", "type"));
        if (!toLoad.containsKey(id)) {
            toLoad.put(id, type);
        }
        return this;
    }

    /**
     * Adds an object to be loaded, the object is specified by a history item.
     * 
     * @param primitive the history item
     * @return this task
     * @throws IllegalArgumentException thrown if primitive is null
     */
    public HistoryLoadTask add(HistoryOsmPrimitive primitive) throws IllegalArgumentException  {
        if (primitive == null)
            throw new IllegalArgumentException(tr("Parameter ''{0}'' must not be null.", "primitive"));
        if (!toLoad.containsKey(primitive.getId())) {
            toLoad.put(primitive.getId(), primitive.getType());
        }
        return this;
    }

    /**
     * Adds an object to be loaded, the object is specified by an already loaded object history.
     * 
     * @param history the history. Must not be null.
     * @return this task
     * @throws IllegalArgumentException thrown if history is null
     */
    public HistoryLoadTask add(History history)throws IllegalArgumentException {
        if (history == null)
            throw new IllegalArgumentException(tr("Parameter ''{0}'' must not be null.", "history"));
        if (!toLoad.containsKey(history.getId())) {
            toLoad.put(history.getId(), history.getEarliest().getType());
        }
        return this;
    }

    /**
     * Adds an object to be loaded, the object is specified by an OSM primitive.
     * 
     * @param primitive the OSM primitive. Must not be null. primitive.getId() > 0 required.
     * @return this task
     * @throws IllegalArgumentException thrown if the primitive is null
     * @throws IllegalArgumentException thrown if primitive.getId() <= 0
     */
    public HistoryLoadTask add(OsmPrimitive primitive) throws IllegalArgumentException {
        if (primitive == null)
            throw new IllegalArgumentException(tr("Parameter ''{0}'' must not be null.", "primitive"));
        if (primitive.getId() <= 0)
            throw new IllegalArgumentException(tr("Object id > 0 expected. Got {0}", primitive.getId()));

        return add(primitive.getId(), OsmPrimitiveType.from(primitive));
    }

    /**
     * Adds a collection of objects to loaded, specified by a collection of OSM primitives.
     * 
     * @param primitive the OSM primitive. Must not be null. primitive.getId() > 0 required.
     * @return this task
     * @throws IllegalArgumentException thrown if primitives is null
     * @throws IllegalArgumentException thrown if one of the ids in the collection <= 0
     */
    public HistoryLoadTask add(Collection<? extends OsmPrimitive> primitives) throws IllegalArgumentException{
        if (primitives == null)
            throw new IllegalArgumentException(tr("Parameter ''{0}'' must not be null.", "primitives"));
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
        OsmApi.getOsmApi().cancel();
        cancelled = true;
    }

    @Override
    protected void finish() {
        if (isCancelled())
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
            for(Map.Entry<Long, OsmPrimitiveType> entry: toLoad.entrySet()) {
                if (cancelled) {
                    break;
                }
                if (entry.getKey() == 0) {
                    continue;
                }
                String msg = "";
                switch(entry.getValue()) {
                    case NODE: msg = marktr("Loading history for node {0}"); break;
                    case WAY: msg = marktr("Loading history for way {0}"); break;
                    case RELATION: msg = marktr("Loading history for relation {0}"); break;
                }
                progressMonitor.indeterminateSubTask(tr(msg,
                        Long.toString(entry.getKey())));
                OsmServerHistoryReader reader = null;
                HistoryDataSet ds = null;
                try {
                    reader = new OsmServerHistoryReader(entry.getValue(), entry.getKey());
                    ds = reader.parseHistory(progressMonitor.createSubTaskMonitor(1, false));
                } catch(OsmTransferException e) {
                    if (cancelled)
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

    public boolean isCancelled() {
        return cancelled;
    }

    public Exception getLastException() {
        return lastException;
    }
}
