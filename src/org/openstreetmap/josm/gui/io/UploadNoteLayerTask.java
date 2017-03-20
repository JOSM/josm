// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Optional;

import org.openstreetmap.josm.actions.upload.UploadNotesTask;
import org.openstreetmap.josm.gui.layer.NoteLayer;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.tools.CheckParameterUtil;

/**
 * UploadNoteLayerTask uploads the data managed by an {@link NoteLayer} asynchronously.
 * @since 8474
 */
public class UploadNoteLayerTask extends AbstractIOTask {

    private final NoteLayer layer;
    private final ProgressMonitor monitor;

    /**
     * Creates the upload task.
     *
     * @param layer the layer. Must not be null.
     * @param monitor  a progress monitor. If monitor is null, uses {@link NullProgressMonitor#INSTANCE}
     * @throws IllegalArgumentException if layer is null
     * @throws IllegalArgumentException if strategy is null
     */
    public UploadNoteLayerTask(NoteLayer layer, ProgressMonitor monitor) {
        CheckParameterUtil.ensureParameterNotNull(layer, "layer");
        this.layer = layer;
        this.monitor = Optional.ofNullable(monitor).orElse(NullProgressMonitor.INSTANCE);
    }

    @Override
    public void run() {
        monitor.indeterminateSubTask(tr("Uploading notes to server"));
        new UploadNotesTask().uploadNotes(layer.getNoteData(), monitor.createSubTaskMonitor(ProgressMonitor.ALL_TICKS, false));
        if (isCanceled())
            return;
        layer.onPostUploadToServer();
    }

    @Override
    public void cancel() {
        setCanceled(true);
    }
}
