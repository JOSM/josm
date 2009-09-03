// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Collection;

import org.openstreetmap.josm.data.APIDataSet;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.OsmServerWriter;

/**
 * UploadLayerTask uploads the data managed by an {@see OsmDataLayer} asynchronously.
 * 
 * <pre>
 *     ExecutorService executorService = ...
 *     UploadLayerTask task = new UploadLayerTask(layer, monitor);
 *     Future<?> taskFuture = executorServce.submit(task)
 *     try {
 *        // wait for the task to complete
 *        taskFuture.get();
 *     } catch(Exception e) {
 *        e.printStackTracek();
 *     }
 * </pre>
 */
class UploadLayerTask extends AbstractIOTask implements Runnable {
    private OsmServerWriter writer;
    private OsmDataLayer layer;
    private ProgressMonitor monitor;

    /**
     * 
     * @param layer the layer. Must not be null.
     * @param monitor  a progress monitor. If monitor is null, uses {@see NullProgressMonitor#INSTANCE}
     * @throws IllegalArgumentException thrown, if layer is null
     */
    public UploadLayerTask(OsmDataLayer layer, ProgressMonitor monitor) {
        if (layer == null)
            throw new IllegalArgumentException(tr("parameter ''{0}'' must not be null", layer));
        if (monitor == null) {
            monitor = NullProgressMonitor.INSTANCE;
        }
        this.layer = layer;
        this.monitor = monitor;
    }

    @Override
    public void run() {
        monitor.subTask(tr("Preparing primitives to upload ..."));
        Collection<OsmPrimitive> toUpload = new APIDataSet(layer.data).getPrimitives();
        if (toUpload.isEmpty())
            return;
        writer = new OsmServerWriter();
        try {
            ProgressMonitor m = monitor.createSubTaskMonitor(ProgressMonitor.ALL_TICKS, false);
            if (isCancelled()) return;
            writer.uploadOsm(layer.data.version, toUpload, m);
        } catch (Exception sxe) {
            if (isCancelled()) {
                System.out.println("Ignoring exception caught because upload is cancelled. Exception is: " + sxe.toString());
                return;
            }
            setLastException(sxe);
        }

        if (isCancelled())
            return;
        layer.cleanupAfterUpload(writer.getProcessedPrimitives());
        DataSet.fireSelectionChanged(layer.data.getSelected());
        layer.fireDataChange();
        layer.onPostUploadToServer();
    }

    @Override
    public void cancel() {
        // make sure the the softCancel operation is serialized with
        // blocks which can be interrupted.
        setCancelled(true);
        if (writer != null) {
            writer.cancel();
        }
    }
}