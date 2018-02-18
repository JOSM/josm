// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;

import org.openstreetmap.josm.actions.upload.UploadNotesTask;
import org.openstreetmap.josm.data.osm.NoteData;
import org.openstreetmap.josm.gui.layer.NoteLayer;
import org.openstreetmap.josm.gui.progress.swing.PleaseWaitProgressMonitor;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Logging;

/**
 * Action to initiate uploading changed notes to the OSM server.
 * On click, it finds the note layer and fires off an upload task
 * with the note data contained in the layer.
 * @since 7699
 */
public class UploadNotesAction extends JosmAction {

    /** Create a new action to upload notes */
    public UploadNotesAction() {
        putValue(SHORT_DESCRIPTION, tr("Upload note changes to server"));
        putValue(NAME, tr("Upload notes"));
        new ImageProvider("upload").getResource().attachImageIcon(this, true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        NoteLayer layer = getLayerManager().getNoteLayer();
        if (layer == null) {
            Logging.error("No note layer found");
            return;
        }
        Logging.debug("uploading note changes");
        NoteData noteData = layer.getNoteData();

        if (noteData == null || !noteData.isModified()) {
            Logging.debug("No changed notes to upload");
            return;
        }
        new UploadNotesTask().uploadNotes(noteData, new PleaseWaitProgressMonitor(tr("Uploading notes to server")));
    }
}
