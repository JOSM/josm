// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import static org.openstreetmap.josm.tools.CheckParameterUtil.ensureParameterNotNull;

import java.io.IOException;
import java.util.Set;

import org.openstreetmap.josm.actions.AutoScaleAction;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.DataSetMerger;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.PrimitiveId;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.ExceptionDialogUtil;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.gui.progress.swing.PleaseWaitProgressMonitor;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.io.MultiFetchServerObjectReader;
import org.openstreetmap.josm.io.OsmServerObjectReader;
import org.openstreetmap.josm.io.OsmTransferException;
import org.xml.sax.SAXException;

/**
 * Abstract superclass of download/update primitives tasks.
 * @since 10129
 */
public abstract class AbstractPrimitiveTask extends PleaseWaitRunnable {

    protected final DataSet ds = new DataSet();
    protected boolean canceled;
    protected Exception lastException;
    private Set<PrimitiveId> missingPrimitives;

    protected final OsmDataLayer layer;
    protected MultiFetchServerObjectReader multiObjectReader;
    protected OsmServerObjectReader objectReader;

    private boolean zoom;
    protected boolean fullRelation;

    protected AbstractPrimitiveTask(String title, OsmDataLayer layer) {
        this(title, new PleaseWaitProgressMonitor(title), layer);
    }

    protected AbstractPrimitiveTask(String title, ProgressMonitor progressMonitor, OsmDataLayer layer) {
        super(title, progressMonitor, false);
        ensureParameterNotNull(layer, "layer");
        this.layer = layer;
        if (!layer.isDownloadable()) {
            throw new IllegalArgumentException("Non-downloadable layer: " + layer);
        }
    }

    protected abstract void initMultiFetchReader(MultiFetchServerObjectReader reader);

    /**
     * Sets whether the map view should zoom to impacted primitives at the end.
     * @param zoom {@code true} if the map view should zoom to impacted primitives at the end
     * @return {@code this}
     */
    public final AbstractPrimitiveTask setZoom(boolean zoom) {
        this.zoom = zoom;
        return this;
    }

    /**
     * Sets whether all members of the relation should be downloaded completely.
     * @param fullRelation {@code true} if a full download is required,
     *                     i.e., a download including the immediate children of a relation.
     * @return {@code this}
     * since 15811 (changed parameter list)
     */
    public final AbstractPrimitiveTask setDownloadRelations(boolean fullRelation) {
        this.fullRelation = fullRelation;
        return this;
    }

    /**
     * Replies the set of ids of all primitives for which a fetch request to the
     * server was submitted but which are not available from the server (the server
     * replied a return code of 404)
     *
     * @return the set of ids of missing primitives
     */
    public Set<PrimitiveId> getMissingPrimitives() {
        return missingPrimitives;
    }

    @Override
    protected void realRun() throws SAXException, IOException, OsmTransferException {
        DataSet theirDataSet;
        try {
            synchronized (this) {
                if (canceled)
                    return;
                multiObjectReader = MultiFetchServerObjectReader.create().setRecurseDownRelations(fullRelation);
            }
            initMultiFetchReader(multiObjectReader);
            theirDataSet = multiObjectReader.parseOsm(progressMonitor.createSubTaskMonitor(ProgressMonitor.ALL_TICKS, false));
            missingPrimitives = multiObjectReader.getMissingPrimitives();
            synchronized (this) {
                multiObjectReader = null;
            }
            new DataSetMerger(ds, theirDataSet).merge();

            loadIncompleteNodes();
        } catch (OsmTransferException e) {
            if (canceled)
                return;
            lastException = e;
        }
    }

    protected void loadIncompleteNodes() throws OsmTransferException {
        // a way loaded with MultiFetch may have incomplete nodes because at least one of its
        // nodes isn't present in the local data set. We therefore fully load all ways with incomplete nodes.
        for (Way w : ds.getWays()) {
            if (canceled)
                return;
            if (w.hasIncompleteNodes()) {
                synchronized (this) {
                    if (canceled)
                        return;
                    objectReader = new OsmServerObjectReader(w.getId(), OsmPrimitiveType.WAY, true /* full */);
                }
                DataSet theirDataSet = objectReader.parseOsm(progressMonitor.createSubTaskMonitor(ProgressMonitor.ALL_TICKS, false));
                synchronized (this) {
                    objectReader = null;
                }
                new DataSetMerger(ds, theirDataSet).merge();
            }
        }
    }

    @Override
    protected void cancel() {
        canceled = true;
        synchronized (this) {
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
        GuiHelper.runInEDTAndWait(() -> {
            layer.mergeFrom(ds);
            if (zoom && MainApplication.getMap() != null)
                AutoScaleAction.zoomTo(ds.allPrimitives());
            layer.onPostDownloadFromServer();
        });
    }
}
