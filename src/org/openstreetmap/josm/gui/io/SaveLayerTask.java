// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Optional;

import org.openstreetmap.josm.actions.SaveAction;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.JosmRuntimeException;
import org.openstreetmap.josm.tools.Logging;

/**
 * SaveLayerTask saves the data managed by an {@link org.openstreetmap.josm.gui.layer.AbstractModifiableLayer} to the
 * {@link org.openstreetmap.josm.gui.layer.Layer#getAssociatedFile()}.
 *
 * <pre>
 *     ExecutorService executorService = ...
 *     SaveLayerTask task = new SaveLayerTask(layer, monitor);
 *     Future&lt;?&gt; taskFuture = executorService.submit(task)
 *     try {
 *        // wait for the task to complete
 *        taskFuture.get();
 *     } catch (Exception e) {
 *        e.printStackTrace();
 *     }
 * </pre>
 */
public class SaveLayerTask extends AbstractIOTask {
    private final SaveLayerInfo layerInfo;
    private final ProgressMonitor parentMonitor;

    /**
     *
     * @param layerInfo information about the layer to be saved to save. Must not be null.
     * @param monitor the monitor. Set to {@link NullProgressMonitor#INSTANCE} if null
     * @throws IllegalArgumentException if layer is null
     */
    protected SaveLayerTask(SaveLayerInfo layerInfo, ProgressMonitor monitor) {
        CheckParameterUtil.ensureParameterNotNull(layerInfo, "layerInfo");
        this.layerInfo = layerInfo;
        this.parentMonitor = Optional.ofNullable(monitor).orElse(NullProgressMonitor.INSTANCE);
    }

    @Override
    public void run() {
        try {
            parentMonitor.subTask(tr("Saving layer to ''{0}'' ...", layerInfo.getFile().toString()));
            if (!SaveAction.doSave(layerInfo.getLayer(), layerInfo.getFile(), layerInfo.isDoCheckSaveConditions())) {
                setFailed(true);
                return;
            }
            if (!isCanceled()) {
                layerInfo.getLayer().onPostSaveToFile();
            }
        } catch (JosmRuntimeException | IllegalArgumentException | IllegalStateException e) {
            Logging.error(e);
            setLastException(e);
        }
    }

    @Override
    public void cancel() {
        setCanceled(true);
    }
}
