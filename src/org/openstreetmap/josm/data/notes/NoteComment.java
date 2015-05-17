// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.notes;

import java.util.Date;

import org.openstreetmap.josm.data.osm.User;

/**
 * Represents a comment made on a note. All notes have at least on comment
 * which is the comment the note was opened with. Comments are immutable.
 */
public class NoteComment {

    private String text;
    private User user;
    private Date commentTimestamp;
    private Action action;

    //not currently used. I'm planning on using this to keep track of new actions that need to be uploaded
    private boolean isNew;

    /**
     * Every comment has an associated action. Some comments are just comments
     * while others indicate the note being opened, closed or reopened
     */
    public enum Action {opened, closed, reopened, commented}

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
        this.commentTimestamp = createDate;
        this.action = action;
        this.isNew = isNew;
    }

    /** @return Plain text of user's comment */
    public String getText() {
        return text;
    }

    /** @return JOSM's User object for the user who made this comment */
    public User getUser() {
        return user;
    }

    /** @return The time at which this comment was created */
    public Date getCommentTimestamp() {
        return commentTimestamp;
    }

    /** @return the action associated with this note */
    public Action getNoteAction() {
        return action;
    }

    public void setNew(boolean isNew) {
        this.isNew = isNew;
    }

    /** @return true if this is a new comment/action and needs to be uploaded to the API */
    public boolean isNew() {
        return isNew;
    }
}
