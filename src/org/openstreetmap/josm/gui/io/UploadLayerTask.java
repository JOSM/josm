// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.upload.CyclicUploadDependencyException;
import org.openstreetmap.josm.data.APIDataSet;
import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.data.osm.IPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.gui.DefaultNameFormatter;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.OsmApi;
import org.openstreetmap.josm.io.OsmApiPrimitiveGoneException;
import org.openstreetmap.josm.io.OsmServerWriter;
import org.openstreetmap.josm.io.OsmTransferException;
import org.openstreetmap.josm.tools.CheckParameterUtil;

/**
 * UploadLayerTask uploads the data managed by an {@link OsmDataLayer} asynchronously.
 *
 * <pre>
 *     ExecutorService executorService = ...
 *     UploadLayerTask task = new UploadLayerTask(layer, monitor);
 *     Future&lt;?&gt; taskFuture = executorServce.submit(task)
 *     try {
 *        // wait for the task to complete
 *        taskFuture.get();
 *     } catch(Exception e) {
 *        e.printStackTracek();
 *     }
 * </pre>
 */
public class UploadLayerTask extends AbstractIOTask implements Runnable {
    private OsmServerWriter writer;
    private OsmDataLayer layer;
    private ProgressMonitor monitor;
    private Changeset changeset;
    private Collection<OsmPrimitive> toUpload;
    private Set<IPrimitive> processedPrimitives;
    private UploadStrategySpecification strategy;

    /**
     * Creates the upload task
     *
     * @param strategy the upload strategy specification
     * @param layer the layer. Must not be null.
     * @param monitor  a progress monitor. If monitor is null, uses {@link NullProgressMonitor#INSTANCE}
     * @param changeset the changeset to be used
     * @throws IllegalArgumentException if layer is null
     * @throws IllegalArgumentException if strategy is null
     */
    public UploadLayerTask(UploadStrategySpecification strategy, OsmDataLayer layer, ProgressMonitor monitor, Changeset changeset) {
        CheckParameterUtil.ensureParameterNotNull(layer, "layer");
        CheckParameterUtil.ensureParameterNotNull(strategy, "strategy");
        if (monitor == null) {
            monitor = NullProgressMonitor.INSTANCE;
        }
        this.layer = layer;
        this.monitor = monitor;
        this.changeset = changeset;
        this.strategy = strategy;
        processedPrimitives = new HashSet<>();
    }

    protected OsmPrimitive getPrimitive(OsmPrimitiveType type, long id) {
        for (OsmPrimitive p: toUpload) {
            if (OsmPrimitiveType.from(p).equals(type) && p.getId() == id)
                return p;
        }
        return null;
    }

    /**
     * Retries to recover the upload operation from an exception which was thrown because
     * an uploaded primitive was already deleted on the server.
     *
     * @param e the exception throw by the API
     * @param monitor a progress monitor
     * @throws OsmTransferException if we can't recover from the exception
     */
    protected void recoverFromGoneOnServer(OsmApiPrimitiveGoneException e, ProgressMonitor monitor) throws OsmTransferException{
        if (!e.isKnownPrimitive()) throw e;
        OsmPrimitive p = getPrimitive(e.getPrimitiveType(), e.getPrimitiveId());
        if (p == null) throw e;
        if (p.isDeleted()) {
            // we tried to delete an already deleted primitive.
            //
            Main.warn(tr("Object ''{0}'' is already deleted on the server. Skipping this object and retrying to upload.", p.getDisplayName(DefaultNameFormatter.getInstance())));
            processedPrimitives.addAll(writer.getProcessedPrimitives());
            processedPrimitives.add(p);
            toUpload.removeAll(processedPrimitives);
            return;
        }
        // exception was thrown because we tried to *update* an already deleted
        // primitive. We can't resolve this automatically. Re-throw exception,
        // a conflict is going to be created later.
        throw e;
    }

    @Override
    public void run() {
        monitor.indeterminateSubTask(tr("Preparing objects to upload ..."));
        APIDataSet ds = new APIDataSet(layer.data);
        try {
            ds.adjustRelationUploadOrder();
        } catch(CyclicUploadDependencyException e) {
            setLastException(e);
            return;
        }
        toUpload = ds.getPrimitives();
        if (toUpload.isEmpty())
            return;
        writer = new OsmServerWriter();
        try {
            while(true) {
                try {
                    ProgressMonitor m = monitor.createSubTaskMonitor(ProgressMonitor.ALL_TICKS, false);
                    if (isCanceled()) return;
                    writer.uploadOsm(strategy, toUpload, changeset, m);
                    processedPrimitives.addAll(writer.getProcessedPrimitives()); // OsmPrimitive in => OsmPrimitive out
                    break;
                } catch(OsmApiPrimitiveGoneException e) {
                    recoverFromGoneOnServer(e, monitor);
                }
            }
            if (strategy.isCloseChangesetAfterUpload()) {
                if (changeset != null && changeset.getId() > 0) {
                    OsmApi.getOsmApi().closeChangeset(changeset, monitor.createSubTaskMonitor(0, false));
                }
            }
        } catch (Exception sxe) {
            if (isCanceled()) {
                Main.info("Ignoring exception caught because upload is canceled. Exception is: " + sxe);
                return;
            }
            setLastException(sxe);
        }

        if (isCanceled())
            return;
        layer.cleanupAfterUpload(processedPrimitives);
        layer.onPostUploadToServer();

        // don't process exceptions remembered with setLastException().
        // Caller is supposed to deal with them.
    }

    @Override
    public void cancel() {
        setCanceled(true);
        if (writer != null) {
            writer.cancel();
        }
    }
}
