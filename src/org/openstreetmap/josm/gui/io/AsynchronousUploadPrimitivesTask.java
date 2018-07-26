// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Optional;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.data.APIDataSet;
import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.dialogs.LayerListDialog;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.progress.ProgressTaskId;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.io.UploadStrategySpecification;

/**
 * Task for uploading primitives using background worker threads. The actual upload is delegated to the
 * {@link UploadPrimitivesTask}. This class is a wrapper over that to make the background upload process safe. There
 * can only be one instance of this class, hence background uploads are limited to one at a time. This class also
 * changes the editLayer of {@link org.openstreetmap.josm.gui.layer.MainLayerManager} to null during upload so that
 * any changes to the uploading layer are prohibited.
 *
 * @author udit
 * @since 13133
 */
public final class AsynchronousUploadPrimitivesTask extends UploadPrimitivesTask {

    /**
     * Static instance
     */
    private static AsynchronousUploadPrimitivesTask asynchronousUploadPrimitivesTask;

    /**
     * Member fields
     */
    private final ProgressTaskId taskId;
    private final OsmDataLayer uploadDataLayer;

    /**
     * Private constructor to restrict creating more Asynchronous upload tasks
     *
     * @param uploadStrategySpecification UploadStrategySpecification for the DataLayer
     * @param osmDataLayer Datalayer to be uploaded
     * @param apiDataSet ApiDataSet that contains the primitives to be uploaded
     * @param changeset Changeset for the datalayer
     *
     * @throws IllegalArgumentException if layer is null
     * @throws IllegalArgumentException if toUpload is null
     * @throws IllegalArgumentException if strategy is null
     * @throws IllegalArgumentException if changeset is null
     */
    private AsynchronousUploadPrimitivesTask(UploadStrategySpecification uploadStrategySpecification,
                                             OsmDataLayer osmDataLayer, APIDataSet apiDataSet, Changeset changeset) {
        super(uploadStrategySpecification,
                osmDataLayer,
                apiDataSet,
                changeset);

        uploadDataLayer = osmDataLayer;
        // Create a ProgressTaskId for background upload
        taskId = new ProgressTaskId("core", "async-upload");
    }

    /**
     * Creates an instance of AsynchronousUploadPrimitiveTask
     *
     * @param uploadStrategySpecification UploadStrategySpecification for the DataLayer
     * @param dataLayer Datalayer to be uploaded
     * @param apiDataSet ApiDataSet that contains the primitives to be uploaded
     * @param changeset Changeset for the datalayer
     * @return Returns an {@literal Optional<AsynchronousUploadPrimitivesTask> } if there is no
     * background upload in progress. Otherwise returns an {@literal Optional.empty()}
     *
     * @throws IllegalArgumentException if layer is null
     * @throws IllegalArgumentException if toUpload is null
     * @throws IllegalArgumentException if strategy is null
     * @throws IllegalArgumentException if changeset is null
     */
    public static Optional<AsynchronousUploadPrimitivesTask> createAsynchronousUploadTask(
            UploadStrategySpecification uploadStrategySpecification,
             OsmDataLayer dataLayer, APIDataSet apiDataSet, Changeset changeset) {
        synchronized (AsynchronousUploadPrimitivesTask.class) {
            if (asynchronousUploadPrimitivesTask != null) {
                GuiHelper.runInEDTAndWait(() ->
                        JOptionPane.showMessageDialog(MainApplication.parent,
                                tr("A background upload is already in progress. " +
                                        "Kindly wait for it to finish before uploading new changes")));
                return Optional.empty();
            } else {
                // Create an asynchronous upload task
                asynchronousUploadPrimitivesTask = new AsynchronousUploadPrimitivesTask(
                        uploadStrategySpecification,
                        dataLayer,
                        apiDataSet,
                        changeset);
                return Optional.ofNullable(asynchronousUploadPrimitivesTask);
            }
        }
    }

    /**
     * Get the current upload task
     * @return {@literal Optional<AsynchronousUploadPrimitivesTask> }
     */
    public static Optional<AsynchronousUploadPrimitivesTask> getCurrentAsynchronousUploadTask() {
        return Optional.ofNullable(asynchronousUploadPrimitivesTask);
    }

    @Override
    public ProgressTaskId canRunInBackground() {
        return taskId;
    }

    @Override
    protected void realRun() {
        // Lock the data layer before upload in EDT
        GuiHelper.runInEDTAndWait(() -> {
            // Remove the commands from the undo stack
            MainApplication.undoRedo.clean(uploadDataLayer.getDataSet());
            MainApplication.getLayerManager().prepareLayerForUpload(uploadDataLayer);

            // Repainting the Layer List dialog to update the icon of the active layer
            LayerListDialog.getInstance().repaint();
        });
        super.realRun();
    }

    @Override
    protected void cancel() {
        super.cancel();
        asynchronousUploadPrimitivesTask = null;
    }

    @Override
    protected void finish() {
        try {
            // Unlock the data layer in EDT
            GuiHelper.runInEDTAndWait(() -> {
                MainApplication.getLayerManager().processLayerAfterUpload(uploadDataLayer);
                LayerListDialog.getInstance().repaint();
            });
            super.finish();
        } finally {
            asynchronousUploadPrimitivesTask = null;
        }
    }
}
