// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.upload;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.data.notes.Note;
import org.openstreetmap.josm.data.notes.NoteComment;
import org.openstreetmap.josm.data.osm.NoteData;
import org.openstreetmap.josm.gui.ExceptionDialogUtil;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.OsmApi;
import org.openstreetmap.josm.io.OsmTransferCanceledException;
import org.openstreetmap.josm.io.OsmTransferException;
import org.openstreetmap.josm.tools.Logging;
import org.xml.sax.SAXException;

/**
 * Class for uploading note changes to the server
 */
public class UploadNotesTask {

    private NoteData noteData;

    /**
     * Upload notes with modifications to the server
     * @param noteData Note dataset with changes to upload
     * @param progressMonitor progress monitor for user feedback
     */
    public void uploadNotes(NoteData noteData, ProgressMonitor progressMonitor) {
        this.noteData = noteData;
        MainApplication.worker.submit(new UploadTask(tr("Uploading modified notes"), progressMonitor));
    }

    private class UploadTask extends PleaseWaitRunnable {

        private boolean isCanceled;
        private final Map<Note, Note> updatedNotes = new HashMap<>();
        private final Map<Note, Exception> failedNotes = new HashMap<>();

        /**
         * Constructs a new {@code UploadTask}.
         * @param title message for the user
         * @param monitor progress monitor
         */
        UploadTask(String title, ProgressMonitor monitor) {
            super(title, monitor, false);
        }

        @Override
        protected void cancel() {
            Logging.debug("note upload canceled");
            isCanceled = true;
        }

        @Override
        protected void realRun() throws SAXException, IOException, OsmTransferException {
            ProgressMonitor monitor = progressMonitor.createSubTaskMonitor(ProgressMonitor.ALL_TICKS, false);
            OsmApi api = OsmApi.getOsmApi();
            for (Note note : noteData.getNotes()) {
                if (isCanceled) {
                    Logging.info("Note upload interrupted by user");
                    break;
                }
                for (NoteComment comment : note.getComments()) {
                    if (comment.isNew()) {
                        Logging.debug("found note change to upload");
                        processNoteComment(monitor, api, note, comment);
                    }
                }
            }
        }

        private void processNoteComment(ProgressMonitor monitor, OsmApi api, Note note, NoteComment comment) {
            try {
                if (updatedNotes.containsKey(note)) {
                    // if note has been created earlier in this task, obtain its real id and not use the placeholder id
                    note = updatedNotes.get(note);
                }
                Note newNote;
                switch (comment.getNoteAction()) {
                case OPENED:
                    Logging.debug("opening new note");
                    newNote = api.createNote(note.getLatLon(), comment.getText(), monitor);
                    break;
                case CLOSED:
                    Logging.debug("closing note {0}", note.getId());
                    newNote = api.closeNote(note, comment.getText(), monitor);
                    break;
                case COMMENTED:
                    Logging.debug("adding comment to note {0}", note.getId());
                    newNote = api.addCommentToNote(note, comment.getText(), monitor);
                    break;
                case REOPENED:
                    Logging.debug("reopening note {0}", note.getId());
                    newNote = api.reopenNote(note, comment.getText(), monitor);
                    break;
                default:
                    newNote = null;
                }
                updatedNotes.put(note, newNote);
            } catch (OsmTransferException e) {
                Logging.error("Failed to upload note to server: {0}", note.getId());
                Logging.error(e);
                if (!(e instanceof OsmTransferCanceledException)) {
                    failedNotes.put(note, e);
                }
            }
        }

        /** Updates the note layer with uploaded notes and notifies the user of any upload failures */
        @Override
        protected void finish() {
            if (Logging.isDebugEnabled()) {
                Logging.debug("finish called in notes upload task. Notes to update: {0}", updatedNotes.size());
            }
            noteData.updateNotes(updatedNotes);
            if (!failedNotes.isEmpty()) {
                String message = failedNotes.entrySet().stream()
                        .map(entry -> tr("Note {0} failed: {1}", entry.getKey().getId(), entry.getValue().getMessage()))
                        .collect(Collectors.joining("\n"));
                Logging.error("Notes failed to upload: " + message);
                JOptionPane.showMessageDialog(MainApplication.getMap(), message,
                        tr("Notes failed to upload"), JOptionPane.ERROR_MESSAGE);
                ExceptionDialogUtil.explainException(failedNotes.values().iterator().next());
            }
        }
    }
}
