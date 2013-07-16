// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import java.util.Observable;

/**
 * ChangesetCommentModel is an observable model for the changeset comment edited
 * in the {@link UploadDialog}.
 *
 */
public class ChangesetCommentModel extends Observable {
    private String comment = "";

    /**
     * Sets the current changeset comment and notifies observers if the comment
     * has changed.
     *
     * @param comment the new upload comment. Empty string assumed if null.
     */
    public void setComment(String comment) {
        String oldValue = this.comment;
        this.comment = comment == null ? "" : comment;
        if (!oldValue.equals(this.comment)) {
            setChanged();
            notifyObservers(this.comment);
        }
    }

    /**
     * Replies the current changeset comment in this model.
     *
     * @return the current changeset comment in this model.
     */
    public String getComment() {
        return comment == null ? "": comment;
    }
}
