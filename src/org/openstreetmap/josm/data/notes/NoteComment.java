// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.notes;

import java.util.Comparator;
import java.util.Date;

import org.openstreetmap.josm.data.osm.User;
import org.openstreetmap.josm.tools.date.DateUtils;

/**
 * Represents a comment made on a note. All notes have at least on comment
 * which is the comment the note was opened with. Comments are immutable.
 * @since 7451
 */
public class NoteComment {

    private final String text;
    private final User user;
    private final Date commentTimestamp;
    private final Action action;

    //not currently used. I'm planning on using this to keep track of new actions that need to be uploaded
    private boolean isNew;

    /**
     * Every comment has an associated action. Some comments are just comments
     * while others indicate the note being opened, closed or reopened
     */
    public enum Action {
        /** note has been opened */
        OPENED,
        /** note has been closed */
        CLOSED,
        /** note has been reopened */
        REOPENED,
        /** note has been commented */
        COMMENTED,
        /** note has been hidden */
        HIDDEN
    }

    /** Sorts note comments strictly by creation date */
    public static final Comparator<NoteComment> DATE_COMPARATOR = Comparator.comparing(n -> n.commentTimestamp);

    /**
     * @param createDate The time at which this comment was added
     * @param user JOSM User object of the user who created the comment
     * @param commentText The text left by the user. Is sometimes blank
     * @param action The action associated with this comment
     * @param isNew Whether or not this comment is new and needs to be uploaded
     */
    public NoteComment(Date createDate, User user, String commentText, Action action, boolean isNew) {
        this.text = commentText;
        this.user = user;
        this.commentTimestamp = DateUtils.cloneDate(createDate);
        this.action = action;
        this.isNew = isNew;
    }

    /**
     * Returns Plain text of user's comment.
     * @return Plain text of user's comment
     */
    public String getText() {
        return text;
    }

    /**
     * Returns the user who made this comment.
     * @return JOSM's User object for the user who made this comment
     */
    public User getUser() {
        return user;
    }

    /**
     * Returns the time at which this comment was created.
     * @return The time at which this comment was created
     */
    public Date getCommentTimestamp() {
        return DateUtils.cloneDate(commentTimestamp);
    }

    /**
     * Returns the action associated with this note.
     * @return the action associated with this note
     */
    public Action getNoteAction() {
        return action;
    }

    /**
     * Sets whether this is a new comment/action and needs to be uploaded to the API
     * @param isNew {@code true} if this is a new comment/action and needs to be uploaded to the API
     */
    public void setNew(boolean isNew) {
        this.isNew = isNew;
    }

    /**
     * Determines if this is a new comment/action and needs to be uploaded to the API
     * @return true if this is a new comment/action and needs to be uploaded to the API
     */
    public boolean isNew() {
        return isNew;
    }

    @Override
    public String toString() {
        return text;
    }
}
