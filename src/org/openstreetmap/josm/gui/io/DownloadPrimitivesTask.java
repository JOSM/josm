// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import static org.openstreetmap.josm.tools.CheckParameterUtil.ensureParameterNotNull;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.openstreetmap.josm.actions.AutoScaleAction;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.DataSetMerger;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.PrimitiveId;
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

public class DownloadPrimitivesTask extends PleaseWaitRunnable {
    private DataSet ds;
    private boolean canceled;
    private Exception lastException;
    private final List<PrimitiveId> ids;

    private Set<PrimitiveId> missingPrimitives;

    private final OsmDataLayer layer;
    private final boolean fullRelation;
    private MultiFetchServerObjectReader multiObjectReader;
    private OsmServerObjectReader objectReader;

    /**
     * Creates the  task
     *
     * @param layer the layer in which primitives are updated. Must not be null.
     * @param ids a collection of primitives to update from the server. Set to
     * the empty collection if null.
     * @param fullRelation true if a full download is required, i.e.,
     * a download including the immediate children of a relation.
     * @throws IllegalArgumentException thrown if layer is null.
     */
    public DownloadPrimitivesTask(OsmDataLayer layer, List<PrimitiveId> ids, boolean fullRelation) throws IllegalArgumentException {
        super(tr("Download objects"), false /* don't ignore exception */);
        ensureParameterNotNull(layer, "layer");
        this.ids = ids;
        this.layer = layer;
        this.fullRelation = fullRelation;
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
                AutoScaleAction.zoomTo(ds.allPrimitives());
                layer.onPostDownloadFromServer();
            }
        });
    }

    protected void initMultiFetchReader(MultiFetchServerObjectReader reader) {
        getProgressMonitor().indeterminateSubTask(tr("Initializing nodes to download ..."));
        for (PrimitiveId id : ids) {
            OsmPrimitive osm = layer.data.getPrimitiveById(id);
            if (osm == null) {
                switch (id.getType()) {
                    case NODE:
                        osm = new Node(id.getUniqueId());
                        break;
                    case WAY:
                        osm = new Way(id.getUniqueId());
                        break;
                    case RELATION:
                        osm = new Relation(id.getUniqueId());
                        break;
                    default: throw new AssertionError();
                }
            }
            reader.append(osm);
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
            initMultiFetchReader(multiObjectReader);
            theirDataSet = multiObjectReader.parseOsm(progressMonitor.createSubTaskMonitor(ProgressMonitor.ALL_TICKS, false));
            missingPrimitives = multiObjectReader.getMissingPrimitives();
            synchronized(this) {
                multiObjectReader = null;
            }
            DataSetMerger merger = new DataSetMerger(ds, theirDataSet);
            merger.merge();

            // if incomplete relation members exist, download them too
            for (Relation r : ds.getRelations()) {
                if (canceled) return;
                if (r.hasIncompleteMembers()) {
                    synchronized(this) {
                        if (canceled) return;
                        objectReader = new OsmServerObjectReader(r.getId(), OsmPrimitiveType.RELATION, fullRelation);
                    }
                    theirDataSet = objectReader.parseOsm(progressMonitor.createSubTaskMonitor(ProgressMonitor.ALL_TICKS, false));
                    synchronized (this) {
                        objectReader = null;
                    }
                    merger = new DataSetMerger(ds, theirDataSet);
                    merger.merge();
                }
            }

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
            if (canceled) return;
            lastException = e;
        }
    }

    /**
     * replies the set of ids of all primitives for which a fetch request to the
     * server was submitted but which are not available from the server (the server
     * replied a return code of 404)
     *
     * @return the set of ids of missing primitives
     */
    public Set<PrimitiveId> getMissingPrimitives() {
        return missingPrimitives;
    }

}
