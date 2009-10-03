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

public class HistoryLoadTask extends PleaseWaitRunnable {

    private boolean cancelled = false;
    private Exception lastException  = null;
    private Map<Long, OsmPrimitiveType> toLoad;
    private HistoryDataSet loadedData;

    public HistoryLoadTask() {
        super(tr("Load history"), true);
        toLoad = new HashMap<Long, OsmPrimitiveType>();
    }

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

    public HistoryLoadTask add(HistoryOsmPrimitive primitive) {
        if (primitive == null)
            throw new IllegalArgumentException(tr("Parameter ''{0}'' must not be null.", "primitive"));
        if (!toLoad.containsKey(primitive.getId())) {
            toLoad.put(primitive.getId(), primitive.getType());
        }
        return this;
    }

    public HistoryLoadTask add(History history) {
        if (history == null)
            throw new IllegalArgumentException(tr("Parameter ''{0}'' must not be null.", "history"));
        if (!toLoad.containsKey(history.getId())) {
            toLoad.put(history.getId(), history.getEarliest().getType());
        }
        return this;
    }

    public HistoryLoadTask add(OsmPrimitive primitive) {
        if (primitive == null)
            throw new IllegalArgumentException(tr("Parameter ''{0}'' must not be null.", "primitive"));
        return add(primitive.getId(), OsmPrimitiveType.from(primitive));
    }

    public HistoryLoadTask add(Collection<? extends OsmPrimitive> primitives) {
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
