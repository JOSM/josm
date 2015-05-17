// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.upload;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.notes.Note;
import org.openstreetmap.josm.data.notes.NoteComment;
import org.openstreetmap.josm.data.osm.NoteData;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.OsmApi;
import org.openstreetmap.josm.io.OsmTransferException;
import org.xml.sax.SAXException;

/**
 * Class for uploading note changes to the server
 */
public class UploadNotesTask {

    private UploadTask uploadTask;
    private NoteData noteData;

    /**
     * Upload notes with modifications to the server
     * @param noteData Note dataset with changes to upload
     * @param progressMonitor progress monitor for user feedback
     */
    public void uploadNotes(NoteData noteData, ProgressMonitor progressMonitor) {
        this.noteData = noteData;
        uploadTask = new UploadTask(tr("Uploading modified notes"), progressMonitor);
        Main.worker.submit(uploadTask);
    }

    private class UploadTask extends PleaseWaitRunnable {

        private boolean isCanceled = false;
        private Map<Note, Note> updatedNotes = new HashMap<>();
        private Map<Note, Exception> failedNotes = new HashMap<>();

        /**
         * Constructs a new {@code UploadTask}.
         * @param title message for the user
         * @param monitor progress monitor
         */
        public UploadTask(String title, ProgressMonitor monitor) {
            super(title, monitor, false);
        }

        @Override
        protected void cancel() {
            if (Main.isDebugEnabled()) {
                Main.debug("note upload canceled");
            }
            isCanceled = true;
        }

        @Override
        protected void realRun() throws SAXException, IOException, OsmTransferException {
            ProgressMonitor monitor = progressMonitor.createSubTaskMonitor(ProgressMonitor.ALL_TICKS, false);
            OsmApi api = OsmApi.getOsmApi();
            for (Note note : noteData.getNotes()) {
                if(isCanceled) {
                    Main.info("Note upload interrupted by user");
                    break;
                }
                for (NoteComment comment : note.getComments()) {
                    if (comment.isNew()) {
                        if (Main.isDebugEnabled()) {
                            Main.debug("found note change to upload");
                        }
                        processNoteComment(monitor, api, note, comment);
                    }
                }
            }
        }

        private void processNoteComment(ProgressMonitor monitor, OsmApi api, Note note, NoteComment comment) {
            try {
                Note newNote;
                switch (comment.getNoteAction()) {
                case opened:
                    if (Main.isDebugEnabled()) {
                        Main.debug("opening new note");
                    }
                    newNote = api.createNote(note.getLatLon(), comment.getText(), monitor);
                    note.setId(newNote.getId());
                    break;
                case closed:
                    if (Main.isDebugEnabled()) {
                        Main.debug("closing note " + note.getId());
                    }
                    newNote = api.closeNote(note, comment.getText(), monitor);
                    break;
                case commented:
                    if (Main.isDebugEnabled()) {
                        Main.debug("adding comment to note " + note.getId());
                    }
                    newNote = api.addCommentToNote(note, comment.getText(), monitor);
                    break;
                case reopened:
                    if (Main.isDebugEnabled()) {
                        Main.debug("reopening note " + note.getId());
                    }
                    newNote = api.reopenNote(note, comment.getText(), monitor);
                    break;
                default:
                    newNote = null;
                }
                updatedNotes.put(note, newNote);
            } catch (Exception e) {
                Main.error("Failed to upload note to server: " + note.getId());
                failedNotes.put(note, e);
            }
        }

        /** Updates the note layer with uploaded notes and notifies the user of any upload failures */
        @Override
        protected void finish() {
            if (Main.isDebugEnabled()) {
                Main.debug("finish called in notes upload task. Notes to update: " + updatedNotes.size());
            }
            noteData.updateNotes(updatedNotes);
            if (!failedNotes.isEmpty()) {
                Main.error("Some notes failed to upload");
                StringBuilder sb = new StringBuilder();
                for (Map.Entry<Note, Exception> entry : failedNotes.entrySet()) {
                    sb.append(tr("Note {0} failed: {1}", entry.getKey().getId(), entry.getValue().getMessage()));
                    sb.append("\n");
                }
                Main.error("Notes failed to upload: " + sb.toString());
                JOptionPane.showMessageDialog(Main.map, sb.toString(), tr("Notes failed to upload"), JOptionPane.ERROR_MESSAGE);
            }
        }

    }

}
