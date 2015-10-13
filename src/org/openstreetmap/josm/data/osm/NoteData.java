// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.notes.Note;
import org.openstreetmap.josm.data.notes.Note.State;
import org.openstreetmap.josm.data.notes.NoteComment;
import org.openstreetmap.josm.gui.JosmUserIdentityManager;
import org.openstreetmap.josm.tools.Predicate;
import org.openstreetmap.josm.tools.Utils;

/**
 * Class to hold and perform operations on a set of notes
 */
public class NoteData {

    private long newNoteId = -1;

    private final Storage<Note> noteList;
    private Note selectedNote;
    private Comparator<Note> comparator = DEFAULT_COMPARATOR;

    /**
     * Sorts notes in the following order:
     * 1) Open notes
     * 2) Closed notes
     * 3) New notes
     * Within each subgroup it sorts by ID
     */
    public static final Comparator<Note> DEFAULT_COMPARATOR = new Comparator<Note>() {
        @Override
        public int compare(Note n1, Note n2) {
            if (n1.getId() < 0 && n2.getId() > 0) {
                return 1;
            }
            if (n1.getId() > 0 && n2.getId() < 0) {
                return -1;
            }
            if (n1.getState() == State.closed && n2.getState() == State.open) {
                return 1;
            }
            if (n1.getState() == State.open && n2.getState() == State.closed) {
                return -1;
            }
            return Long.compare(Math.abs(n1.getId()), Math.abs(n2.getId()));
        }
    };

    /** Sorts notes strictly by creation date */
    public static final Comparator<Note> DATE_COMPARATOR = new Comparator<Note>() {
        @Override
        public int compare(Note n1, Note n2) {
            return n1.getCreatedAt().compareTo(n2.getCreatedAt());
        }
    };

    /** Sorts notes by user, then creation date */
    public static final Comparator<Note> USER_COMPARATOR = new Comparator<Note>() {
        @Override
        public int compare(Note n1, Note n2) {
            String n1User = n1.getFirstComment().getUser().getName();
            String n2User = n2.getFirstComment().getUser().getName();
            if (n1User.equals(n2User)) {
                return n1.getCreatedAt().compareTo(n2.getCreatedAt());
            }
            return n1.getFirstComment().getUser().getName().compareTo(n2.getFirstComment().getUser().getName());
        }
    };

    /** Sorts notes by the last modified date */
    public static final Comparator<Note> LAST_ACTION_COMPARATOR = new Comparator<Note>() {
        @Override
        public int compare(Note n1, Note n2) {
            Date n1Date = n1.getComments().get(n1.getComments().size()-1).getCommentTimestamp();
            Date n2Date = n2.getComments().get(n2.getComments().size()-1).getCommentTimestamp();
            return n1Date.compareTo(n2Date);
        }
    };

    /**
     * Construct a new note container with a given list of notes
     * @param notes The list of notes to populate the container with
     */
    public NoteData(Collection<Note> notes) {
        noteList = new Storage<>();
        for (Note note : notes) {
            noteList.add(note);
            if (note.getId() <= newNoteId) {
                newNoteId = note.getId() - 1;
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
        Collections.sort(list, comparator);
        return list;
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
                if (comment.isNew()) {
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
    public synchronized void addNotes(Collection<Note> newNotes) {
        for (Note newNote : newNotes) {
            if (!noteList.contains(newNote)) {
                noteList.add(newNote);
            } else {
                final Note existingNote = noteList.get(newNote);
                final boolean isDirty = Utils.exists(existingNote.getComments(), new Predicate<NoteComment>() {
                    @Override
                    public boolean evaluate(NoteComment object) {
                        return object.isNew();
                    }
                });
                if (!isDirty) {
                    noteList.put(newNote);
                } else {
                    // TODO merge comments?
                    Main.info("Keeping existing note id={0} with uncommitted changes", String.valueOf(newNote.getId()));
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
        if (Main.isDisplayingMapView()) {
            Main.map.noteDialog.setNotes(getSortedNotes());
            Main.map.mapView.repaint();
        }
    }

    private static User getCurrentUser() {
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

    /** @return The current comparator being used to sort the note list */
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
}
