// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.openstreetmap.josm.data.UserIdentityManager;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.notes.Note;
import org.openstreetmap.josm.data.notes.Note.State;
import org.openstreetmap.josm.data.notes.NoteComment;
import org.openstreetmap.josm.tools.ListenerList;
import org.openstreetmap.josm.tools.Logging;

/**
 * Class to hold and perform operations on a set of notes
 */
public class NoteData {

    /**
     * A listener that can be informed on note data changes.
     * @author Michael Zangl
     * @since 12343
     */
    public interface NoteDataUpdateListener {
        /**
         * Called when the note data is updated
         * @param data The data that was changed
         */
        void noteDataUpdated(NoteData data);

        /**
         * The selected node was changed
         * @param noteData The data of which the selected node was changed
         */
        void selectedNoteChanged(NoteData noteData);
    }

    private long newNoteId = -1;

    private final Storage<Note> noteList;
    private Note selectedNote;
    private Comparator<Note> comparator = Note.DEFAULT_COMPARATOR;

    private final ListenerList<NoteDataUpdateListener> listeners = ListenerList.create();

    /**
     * Construct a new note container with a given list of notes
     * @since 14101
     */
    public NoteData() {
        this(null);
    }

    /**
     * Construct a new note container with a given list of notes
     * @param notes The list of notes to populate the container with
     */
    public NoteData(Collection<Note> notes) {
        noteList = new Storage<>();
        if (notes != null) {
            for (Note note : notes) {
                noteList.add(note);
                if (note.getId() <= newNoteId) {
                    newNoteId = note.getId() - 1;
                }
            }
        }
    }

    /**
     * Returns the notes stored in this layer
     * @return collection of notes
     */
    public Collection<Note> getNotes() {
        return Collections.unmodifiableCollection(noteList);
    }

    /**
     * Returns the notes stored in this layer sorted according to {@link #comparator}
     * @return sorted collection of notes
     */
    public Collection<Note> getSortedNotes() {
        final List<Note> list = new ArrayList<>(noteList);
        list.sort(comparator);
        return list;
    }

    /**
     * Returns the currently selected note
     * @return currently selected note
     */
    public Note getSelectedNote() {
        return selectedNote;
    }

    /**
     * Set a selected note. Causes the dialog to select the note and
     * the note layer to draw the selected note's comments.
     * @param note Selected note. Null indicates no selection
     */
    public void setSelectedNote(Note note) {
        selectedNote = note;
        listeners.fireEvent(l -> l.selectedNoteChanged(this));
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
                if (comment.isNew()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Merge notes from an existing note data.
     * @param from existing note data
     * @since 13437
     */
    public synchronized void mergeFrom(NoteData from) {
        if (this != from) {
            addNotes(from.noteList);
        }
    }

    /**
     * Add notes to the data set. It only adds a note if the ID is not already present
     * @param newNotes A list of notes to add
     */
    public synchronized void addNotes(Collection<Note> newNotes) {
        for (Note newNote : newNotes) {
            if (!noteList.contains(newNote)) {
                noteList.add(newNote);
            } else {
                final Note existingNote = noteList.get(newNote);
                final boolean isDirty = existingNote.getComments().stream().anyMatch(NoteComment::isNew);
                if (!isDirty) {
                    noteList.put(newNote);
                } else {
                    // TODO merge comments?
                    Logging.info("Keeping existing note id={0} with uncommitted changes", String.valueOf(newNote.getId()));
                }
            }
            if (newNote.getId() <= newNoteId) {
                newNoteId = newNote.getId() - 1;
            }
        }
        dataUpdated();
    }

    /**
     * Create a new note
     * @param location Location of note
     * @param text Required comment with which to open the note
     */
    public synchronized void createNote(LatLon location, String text) {
        if (text == null || text.isEmpty()) {
            throw new IllegalArgumentException("Comment can not be blank when creating a note");
        }
        Note note = new Note(location);
        note.setCreatedAt(new Date());
        note.setState(State.OPEN);
        note.setId(newNoteId--);
        NoteComment comment = new NoteComment(new Date(), getCurrentUser(), text, NoteComment.Action.OPENED, true);
        note.addComment(comment);
        if (Logging.isDebugEnabled()) {
            Logging.debug("Created note {0} with comment: {1}", note.getId(), text);
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
        if (note.getState() == State.CLOSED) {
            throw new IllegalStateException("Cannot add a comment to a closed note");
        }
        if (Logging.isDebugEnabled()) {
            Logging.debug("Adding comment to note {0}: {1}", note.getId(), text);
        }
        NoteComment comment = new NoteComment(new Date(), getCurrentUser(), text, NoteComment.Action.COMMENTED, true);
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
        if (note.getState() != State.OPEN) {
            throw new IllegalStateException("Cannot close a note that isn't open");
        }
        if (Logging.isDebugEnabled()) {
            Logging.debug("closing note {0} with comment: {1}", note.getId(), text);
        }
        NoteComment comment = new NoteComment(new Date(), getCurrentUser(), text, NoteComment.Action.CLOSED, true);
        note.addComment(comment);
        note.setState(State.CLOSED);
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
        if (note.getState() != State.CLOSED) {
            throw new IllegalStateException("Cannot reopen a note that isn't closed");
        }
        Logging.debug("reopening note {0} with comment: {1}", note.getId(), text);
        NoteComment comment = new NoteComment(new Date(), getCurrentUser(), text, NoteComment.Action.REOPENED, true);
        note.addComment(comment);
        note.setState(State.OPEN);
        dataUpdated();
    }

    private void dataUpdated() {
        listeners.fireEvent(l -> l.noteDataUpdated(this));
    }

    private static User getCurrentUser() {
        UserIdentityManager userMgr = UserIdentityManager.getInstance();
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
            boolean reindex = oldNote.hashCode() != newNote.hashCode();
            if (reindex) {
                noteList.removeElem(oldNote);
            }
            oldNote.updateWith(newNote);
            if (reindex) {
                noteList.add(oldNote);
            }
        }
        dataUpdated();
    }

    /**
     * Returns the current comparator being used to sort the note list.
     * @return The current comparator being used to sort the note list
     */
    public Comparator<Note> getCurrentSortMethod() {
        return comparator;
    }

    /** Set the comparator to be used to sort the note list. Several are available
     * as public static members of this class.
     * @param comparator - The Note comparator to sort by
     */
    public void setSortMethod(Comparator<Note> comparator) {
        this.comparator = comparator;
        dataUpdated();
    }

    /**
     * Adds a listener that listens to node data changes
     * @param listener The listener
     */
    public void addNoteDataUpdateListener(NoteDataUpdateListener listener) {
        listeners.addListener(listener);
    }

    /**
     * Removes a listener that listens to node data changes
     * @param listener The listener
     */
    public void removeNoteDataUpdateListener(NoteDataUpdateListener listener) {
        listeners.removeListener(listener);
    }
}
