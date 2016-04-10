// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.notes;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import org.openstreetmap.josm.data.coor.LatLon;

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

    private long id;
    private LatLon latLon;
    private Date createdAt;
    private Date closedAt;
    private State state;
    private List<NoteComment> comments = new ArrayList<>();

    /**
     * Create a note with a given location
     * @param latLon Geographic location of this note
     */
    public Note(LatLon latLon) {
        this.latLon = latLon;
    }

    /** @return The unique OSM ID of this note */
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

    /** @return The geographic location of the note */
    public LatLon getLatLon() {
        return latLon;
    }

    /** @return Date that this note was submitted */
    public Date getCreatedAt() {
        return createdAt;
    }

    /**
     * Sets date at which this note has been created.
     * @param createdAt date at which this note has been created
     */
    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    /** @return Date that this note was closed. Null if it is still open. */
    public Date getClosedAt() {
        return closedAt;
    }

    /**
     * Sets date at which this note has been closed.
     * @param closedAt date at which this note has been closed
     */
    public void setClosedAt(Date closedAt) {
        this.closedAt = closedAt;
    }

    /** @return The open or closed state of this note */
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

    /** @return An ordered list of comments associated with this note */
    public List<NoteComment> getComments() {
        return comments;
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
    public NoteComment getFirstComment() {
        return comments.isEmpty() ? null : comments.get(0);
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
        return tr("Note") + " " + id + ": " + getFirstComment();
    }
}
