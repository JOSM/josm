// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.notes.Note;
import org.openstreetmap.josm.data.notes.Note.State;
import org.openstreetmap.josm.data.notes.NoteComment;
import org.openstreetmap.josm.gui.JosmUserIdentityManager;

/**
 * Class to hold and perform operations on a set of notes
 */
public class NoteData {

    private long newNoteId = -1;

    private final List<Note> noteList;
    private Note selectedNote = null;

    /**
     * Construct a new note container with an empty note list
     */
    public NoteData() {
        noteList = new ArrayList<>();
    }

    /**
     * Construct a new note container with a given list of notes
     * @param notes The list of notes to populate the container with
     */
    public NoteData(List<Note> notes) {
        noteList = notes;
    }

    /**
     * Returns the notes stored in this layer
     * @return List of Note objects
     */
    public List<Note> getNotes() {
        return noteList;
    }

    /** Returns the currently selected note
     * @return currently selected note
     */
    public Note getSelectedNote() {
        return selectedNote;
    }

    /** Set a selected note. Causes the dialog to select the note and
     * the note layer to draw the selected note's comments.
     * @param note Selected note. Null indicates no selection
     */
    public void setSelectedNote(Note note) {
        selectedNote = note;
        if (Main.map != null) {
            Main.map.noteDialog.selectionChanged();
            Main.map.mapView.repaint();
        }
    }

    /**
     * Return whether or not there are any changes in the note data set.
     * These changes may need to be either uploaded or saved.
     * @return true if local modifications have been made to the note data set. False otherwise.
     */
    public synchronized boolean isModified() {
        for (Note note : noteList) {
            if (note.getId() < 0) { //notes with negative IDs are new
                return true;
            }
            for (NoteComment comment : note.getComments()) {
                if (comment.getIsNew()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Add notes to the data set. It only adds a note if the ID is not already present
     * @param newNotes A list of notes to add
     */
    public synchronized void addNotes(List<Note> newNotes) {
        for (Note newNote : newNotes) {
            if (!noteList.contains(newNote)) {
                noteList.add(newNote);
            }
            if (newNote.getId() <= newNoteId) {
                newNoteId = newNote.getId() - 1;
            }
        }
        dataUpdated();
        if (Main.isDebugEnabled()) {
            Main.debug("notes in current set: " + noteList.size());
        }
    }

    /**
     * Create a new note
     * @param location Location of note
     * @param text Required comment with which to open the note
     */
    public synchronized void createNote(LatLon location, String text) {
        if(text == null || text.isEmpty()) {
            throw new IllegalArgumentException("Comment can not be blank when creating a note");
        }
        Note note = new Note(location);
        note.setCreatedAt(new Date());
        note.setState(State.open);
        note.setId(newNoteId--);
        NoteComment comment = new NoteComment(new Date(), getCurrentUser(), text, NoteComment.Action.opened, true);
        note.addComment(comment);
        if (Main.isDebugEnabled()) {
            Main.debug("Created note {0} with comment: {1}", note.getId(), text);
        }
        noteList.add(note);
        dataUpdated();
    }

    /**
     * Add a new comment to an existing note
     * @param note Note to add comment to. Must already exist in the layer
     * @param text Comment to add
     */
    public synchronized void addCommentToNote(Note note, String text) {
        if (!noteList.contains(note)) {
            throw new IllegalArgumentException("Note to modify must be in layer");
        }
        if (note.getState() == State.closed) {
            throw new IllegalStateException("Cannot add a comment to a closed note");
        }
        if (Main.isDebugEnabled()) {
            Main.debug("Adding comment to note {0}: {1}", note.getId(), text);
        }
        NoteComment comment = new NoteComment(new Date(), getCurrentUser(), text, NoteComment.Action.commented, true);
        note.addComment(comment);
        dataUpdated();
    }

    /**
     * Close note with comment
     * @param note Note to close. Must already exist in the layer
     * @param text Comment to attach to close action, if desired
     */
    public synchronized void closeNote(Note note, String text) {
        if (!noteList.contains(note)) {
            throw new IllegalArgumentException("Note to close must be in layer");
        }
        if (note.getState() != State.open) {
            throw new IllegalStateException("Cannot close a note that isn't open");
        }
        if (Main.isDebugEnabled()) {
            Main.debug("closing note {0} with comment: {1}", note.getId(), text);
        }
        NoteComment comment = new NoteComment(new Date(), getCurrentUser(), text, NoteComment.Action.closed, true);
        note.addComment(comment);
        note.setState(State.closed);
        note.setClosedAt(new Date());
        dataUpdated();
    }

    /**
     * Reopen a closed note.
     * @param note Note to reopen. Must already exist in the layer
     * @param text Comment to attach to the reopen action, if desired
     */
    public synchronized void reOpenNote(Note note, String text) {
        if (!noteList.contains(note)) {
            throw new IllegalArgumentException("Note to reopen must be in layer");
        }
        if (note.getState() != State.closed) {
            throw new IllegalStateException("Cannot reopen a note that isn't closed");
        }
        if (Main.isDebugEnabled()) {
            Main.debug("reopening note {0} with comment: {1}", note.getId(), text);
        }
        NoteComment comment = new NoteComment(new Date(), getCurrentUser(), text, NoteComment.Action.reopened, true);
        note.addComment(comment);
        note.setState(State.open);
        dataUpdated();
    }

    private void dataUpdated() {
        Main.map.noteDialog.setNoteList(noteList);
        Main.map.mapView.repaint();
    }

    private User getCurrentUser() {
        JosmUserIdentityManager userMgr = JosmUserIdentityManager.getInstance();
        return User.createOsmUser(userMgr.getUserId(), userMgr.getUserName());
    }

    /**
     * Updates notes with new state. Primarily to be used when updating the
     * note layer after uploading note changes to the server.
     * @param updatedNotes Map containing the original note as the key and the updated note as the value
     */
    public synchronized void updateNotes(Map<Note, Note> updatedNotes) {
        for (Map.Entry<Note, Note> entry : updatedNotes.entrySet()) {
            Note oldNote = entry.getKey();
            Note newNote = entry.getValue();
            oldNote.updateWith(newNote);
        }
        dataUpdated();
    }
}
