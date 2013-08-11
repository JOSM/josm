// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import static org.openstreetmap.josm.tools.CheckParameterUtil.ensureParameterNotNull;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.DataSetMerger;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.ExceptionDialogUtil;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.io.MultiFetchServerObjectReader;
import org.openstreetmap.josm.io.OsmServerObjectReader;
import org.openstreetmap.josm.io.OsmTransferException;
import org.xml.sax.SAXException;

/**
 * The asynchronous task for updating a collection of objects using multi fetch.
 *
 */
public class UpdatePrimitivesTask extends PleaseWaitRunnable {
    private DataSet ds;
    private boolean canceled;
    private Exception lastException;
    private Collection<? extends OsmPrimitive> toUpdate;
    private OsmDataLayer layer;
    private MultiFetchServerObjectReader multiObjectReader;
    private OsmServerObjectReader objectReader;

    /**
     * Creates the  task
     *
     * @param layer the layer in which primitives are updated. Must not be null.
     * @param toUpdate a collection of primitives to update from the server. Set to
     * the empty collection if null.
     * @throws IllegalArgumentException thrown if layer is null.
     */
    public UpdatePrimitivesTask(OsmDataLayer layer, Collection<? extends OsmPrimitive> toUpdate) throws IllegalArgumentException{
        super(tr("Update objects"), false /* don't ignore exception */);
        ensureParameterNotNull(layer, "layer");
        if (toUpdate == null) {
            toUpdate = Collections.emptyList();
        }
        this.layer = layer;
        this.toUpdate = toUpdate;
    }

    @Override
    protected void cancel() {
        canceled = true;
        synchronized(this) {
            if (multiObjectReader != null) {
                multiObjectReader.cancel();
            }
            if (objectReader != null) {
                objectReader.cancel();
            }
        }
    }

    @Override
    protected void finish() {
        if (canceled)
            return;
        if (lastException != null) {
            ExceptionDialogUtil.explainException(lastException);
            return;
        }
        GuiHelper.runInEDTAndWait(new Runnable() {
            @Override
            public void run() {
                layer.mergeFrom(ds);
                layer.onPostDownloadFromServer();
            }
        });
    }

    protected void initMultiFetchReaderWithNodes(MultiFetchServerObjectReader reader) {
        getProgressMonitor().indeterminateSubTask(tr("Initializing nodes to update ..."));
        for (OsmPrimitive primitive : toUpdate) {
            if (primitive instanceof Node && !primitive.isNew()) {
                reader.append(primitive);
            } else if (primitive instanceof Way) {
                Way way = (Way)primitive;
                for (Node node: way.getNodes()) {
                    if (!node.isNew()) {
                        reader.append(node);
                    }
                }
            }
        }
    }

    protected void initMultiFetchReaderWithWays(MultiFetchServerObjectReader reader) {
        getProgressMonitor().indeterminateSubTask(tr("Initializing ways to update ..."));
        for (OsmPrimitive primitive : toUpdate) {
            if (primitive instanceof Way && !primitive.isNew()) {
                reader.append(primitive);
            }
        }
    }

    protected void initMultiFetchReaderWithRelations(MultiFetchServerObjectReader reader) {
        getProgressMonitor().indeterminateSubTask(tr("Initializing relations to update ..."));
        for (OsmPrimitive primitive : toUpdate) {
            if (primitive instanceof Relation && !primitive.isNew()) {
                reader.append(primitive);
            }
        }
    }

    @Override
    protected void realRun() throws SAXException, IOException, OsmTransferException {
        this.ds = new DataSet();
        DataSet theirDataSet;
        try {
            synchronized(this) {
                if (canceled) return;
                multiObjectReader = new MultiFetchServerObjectReader();
            }
            initMultiFetchReaderWithNodes(multiObjectReader);
            initMultiFetchReaderWithWays(multiObjectReader);
            initMultiFetchReaderWithRelations(multiObjectReader);
            theirDataSet = multiObjectReader.parseOsm(progressMonitor.createSubTaskMonitor(ProgressMonitor.ALL_TICKS, false));
            synchronized(this) {
                multiObjectReader = null;
            }
            DataSetMerger merger = new DataSetMerger(ds, theirDataSet);
            merger.merge();
            // a way loaded with MultiFetch may have incomplete nodes because at least one of its
            // nodes isn't present in the local data set. We therefore fully load all
            // ways with incomplete nodes.
            //
            for (Way w : ds.getWays()) {
                if (canceled) return;
                if (w.hasIncompleteNodes()) {
                    synchronized(this) {
                        if (canceled) return;
                        objectReader = new OsmServerObjectReader(w.getId(), OsmPrimitiveType.WAY, true /* full */);
                    }
                    theirDataSet = objectReader.parseOsm(progressMonitor.createSubTaskMonitor(ProgressMonitor.ALL_TICKS, false));
                    synchronized (this) {
                        objectReader = null;
                    }
                    merger = new DataSetMerger(ds, theirDataSet);
                    merger.merge();
                }
            }
        } catch(Exception e) {
            if (canceled)
                return;
            lastException = e;
        }
    }
}
