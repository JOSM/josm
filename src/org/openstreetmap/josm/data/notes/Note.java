// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.notes;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.openstreetmap.josm.data.coor.LatLon;

import javax.annotation.Nullable;

/**
 * A map note. It always has at least one comment since a comment is required to create a note on osm.org.
 * @since 7451
 */
public class Note {

    /** Note state */
    public enum State {
        /** Note is open */
        OPEN,
        /** Note is closed */
        CLOSED
    }

    /**
     * Sorts notes in the following order:
     * <ol>
     *     <li>Open notes</li>
     *     <li>Closed notes</li>
     *     <li>New notes</li>
     * </ol>
     * Within each subgroup it sorts by ID
     */
    public static final Comparator<Note> DEFAULT_COMPARATOR = (n1, n2) -> {
        if (n1.getId() < 0 && n2.getId() > 0) {
            return 1;
        }
        if (n1.getId() > 0 && n2.getId() < 0) {
            return -1;
        }
        if (n1.getState() == State.CLOSED && n2.getState() == State.OPEN) {
            return 1;
        }
        if (n1.getState() == State.OPEN && n2.getState() == State.CLOSED) {
            return -1;
        }
        return Long.compare(Math.abs(n1.getId()), Math.abs(n2.getId()));
    };

    /** Sorts notes strictly by creation date */
    public static final Comparator<Note> DATE_COMPARATOR = Comparator.comparing(n -> n.createdAt);

    /** Sorts notes by user, then creation date */
    public static final Comparator<Note> USER_COMPARATOR =
            Comparator.comparing(Note::getUserName, Comparator.nullsLast(Comparator.naturalOrder())).thenComparing(DATE_COMPARATOR);

    /** Sorts notes by the last modified date */
    public static final Comparator<Note> LAST_ACTION_COMPARATOR = Comparator.comparing(Note::getLastComment, NoteComment.DATE_COMPARATOR);

    private long id;
    private LatLon latLon;
    private Instant createdAt;
    private Instant closedAt;
    private State state;
    private List<NoteComment> comments = new ArrayList<>();

    /**
     * Create a note with a given location
     * @param latLon Geographic location of this note
     */
    public Note(LatLon latLon) {
        this.latLon = latLon;
    }

    /**
     * Returns the unique OSM ID of this note.
     * @return The unique OSM ID of this note
     */
    public long getId() {
        return id;
    }

    /**
     * Sets note id.
     * @param id OSM ID of this note
     */
    public void setId(long id) {
        this.id = id;
    }

    /**
     * Returns the geographic location of the note.
     * @return The geographic location of the note
     */
    public LatLon getLatLon() {
        return latLon;
    }

    /**
     * Returns the date at which this note was submitted.
     * @return Date that this note was submitted
     */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * Sets date at which this note has been created.
     * @param createdAt date at which this note has been created
     */
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Returns the date at which this note was closed.
     * @return Date that this note was closed. Null if it is still open.
     */
    public Instant getClosedAt() {
        return closedAt;
    }

    /**
     * Sets date at which this note has been closed.
     * @param closedAt date at which this note has been closed
     */
    public void setClosedAt(Instant closedAt) {
        this.closedAt = closedAt;
    }

    /**
     * Returns the open or closed state of this note.
     * @return The open or closed state of this note
     */
    public State getState() {
        return state;
    }

    /**
     * Sets the note state.
     * @param state note state (open or closed)
     */
    public void setState(State state) {
        this.state = state;
    }

    /**
     * Returns the list of comments associated with this note.
     * @return An ordered list of comments associated with this note
     */
    public List<NoteComment> getComments() {
        return comments;
    }

    /**
     * Returns the last comment, or {@code null}.
     * @return the last comment, or {@code null}
     * @since 11821
     */
    @Nullable
    public NoteComment getLastComment() {
        return comments.isEmpty() ? null : comments.get(comments.size()-1);
    }

    /**
     * Adds a comment.
     * @param comment note comment
     */
    public void addComment(NoteComment comment) {
        comments.add(comment);
    }

    /**
     * Returns the comment that was submitted by the user when creating the note
     * @return First comment object
     */
    @Nullable
    public NoteComment getFirstComment() {
        return comments.isEmpty() ? null : comments.get(0);
    }

    @Nullable
    private String getUserName() {
        return getFirstComment() == null ? null : getFirstComment().getUser().getName();
    }

    /**
     * Copies values from a new note into an existing one. Used after a note
     * has been updated on the server and the local copy needs refreshing.
     * @param note New values to copy
     */
    public void updateWith(Note note) {
        this.comments = note.comments;
        this.createdAt = note.createdAt;
        this.id = note.id;
        this.state = note.state;
        this.latLon = note.latLon;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        Note note = (Note) obj;
        return id == note.id;
    }

    @Override
    public String toString() {
        return tr("Note") + ' ' + id + ": " + getFirstComment();
    }
}
