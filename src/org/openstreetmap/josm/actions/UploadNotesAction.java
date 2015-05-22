// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.util.List;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.upload.UploadNotesTask;
import org.openstreetmap.josm.data.osm.NoteData;
import org.openstreetmap.josm.gui.layer.NoteLayer;
import org.openstreetmap.josm.gui.progress.PleaseWaitProgressMonitor;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Action to initiate uploading changed notes to the OSM server.
 * On click, it finds the note layer and fires off an upload task
 * with the note data contained in the layer.
 *
 */
public class UploadNotesAction extends JosmAction {

    /** Create a new action to upload notes */
    public UploadNotesAction() {
        putValue(SHORT_DESCRIPTION,tr("Upload note changes to server"));
        putValue(NAME, tr("Upload notes"));
        putValue(SMALL_ICON, ImageProvider.get("upload"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        List<NoteLayer> noteLayers = null;
        if (Main.map != null) {
            noteLayers = Main.map.mapView.getLayersOfType(NoteLayer.class);
        }
        NoteLayer layer;
        if (noteLayers != null && !noteLayers.isEmpty()) {
            layer = noteLayers.get(0);
        } else {
            Main.error("No note layer found");
            return;
        }
        if (Main.isDebugEnabled()) {
            Main.debug("uploading note changes");
        }
        NoteData noteData = layer.getNoteData();

        if(noteData == null || !noteData.isModified()) {
            if (Main.isDebugEnabled()) {
                Main.debug("No changed notes to upload");
            }
            return;
        }
        UploadNotesTask uploadTask = new UploadNotesTask();
        uploadTask.uploadNotes(noteData, new PleaseWaitProgressMonitor(tr("Uploading notes to server")));
    }
}
