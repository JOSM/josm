// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.SaveAction;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.tools.CheckParameterUtil;

/**
 * SaveLayerTask saves the data managed by an {@link org.openstreetmap.josm.gui.layer.OsmDataLayer} to the
 * {@link org.openstreetmap.josm.gui.layer.OsmDataLayer#getAssociatedFile()}.
 *
 * <pre>
 *     ExecutorService executorService = ...
 *     SaveLayerTask task = new SaveLayerTask(layer, monitor);
 *     Future&lt;?&gt; taskFuture = executorServce.submit(task)
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
     * @param monitor the monitor. Set to {@link NullProgressMonitor#INSTANCE} if null
     * @throws IllegalArgumentException thrown if layer is null
     */
    protected SaveLayerTask(SaveLayerInfo layerInfo, ProgressMonitor monitor) {
        CheckParameterUtil.ensureParameterNotNull(layerInfo, "layerInfo");
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
            if (!SaveAction.doSave(layerInfo.getLayer(), layerInfo.getFile())) {
                setFailed(true);
                return;
            }
            if (!isCanceled()) {
                layerInfo.getLayer().onPostSaveToFile();
            }
        } catch(Exception e) {
            Main.error(e);
            setLastException(e);
        }
    }

    @Override
    public void cancel() {
        setCanceled(true);
    }
}
