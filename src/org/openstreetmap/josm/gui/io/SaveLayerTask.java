// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.actions.SaveAction;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;

/**
 * SaveLayerTask saves the data managed by an {@see OsmDataLayer} to the
 * {@see OsmDataLayer#getAssociatedFile()}.
 *
 * <pre>
 *     ExecutorService executorService = ...
 *     SaveLayerTask task = new SaveLayerTask(layer, monitor);
 *     Future<?> taskFuture = executorServce.submit(task)
 *     try {
 *        // wait for the task to complete
 *        taskFuture.get();
 *     } catch(Exception e) {
 *        e.printStackTracek();
 *     }
 * </pre>
 */
class SaveLayerTask extends AbstractIOTask {
    private SaveLayerInfo layerInfo;
    private ProgressMonitor parentMonitor;

    /**
     *
     * @param layerInfo information about the layer to be saved to save. Must not be null.
     * @param monitor the monitor. Set to {@see NullProgressMonitor#INSTANCE} if null
     * @throws IllegalArgumentException thrown if layer is null
     */
    protected SaveLayerTask(SaveLayerInfo layerInfo, ProgressMonitor monitor) {
        if (layerInfo == null)
            throw new IllegalArgumentException(tr("Parameter ''{0}'' must not be null.", "layerInfo"));
        if (monitor == null) {
            monitor = NullProgressMonitor.INSTANCE;
        }
        this.layerInfo =  layerInfo;
        this.parentMonitor = monitor;
    }

    @Override
    public void run() {
        try {
            parentMonitor.subTask(tr("Saving layer to ''{0}'' ...", layerInfo.getFile().toString()));
            layerInfo.getLayer().setAssociatedFile(layerInfo.getFile());
            if (!new SaveAction().doSave(layerInfo.getLayer())) {
                setFailed(true);
                return;
            }
            if (!isCancelled()) {
                layerInfo.getLayer().onPostSaveToFile();
            }
        } catch(Exception e) {
            e.printStackTrace();
            setLastException(e);
        }
    }

    @Override
    public void cancel() {
        setCancelled(true);
    }
}
