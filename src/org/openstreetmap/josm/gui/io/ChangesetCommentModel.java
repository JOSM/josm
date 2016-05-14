// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import java.util.Objects;

import org.openstreetmap.josm.gui.util.ChangeNotifier;

/**
 * ChangesetCommentModel is an observable model for the changeset comment edited
 * in the {@link UploadDialog}.
 * @since 3133
 */
public class ChangesetCommentModel extends ChangeNotifier {
    private String comment = "";

    /**
     * Sets the current changeset comment and notifies observers if the comment has changed.
     *
     * @param comment the new upload comment. Empty string assumed if null.
     */
    public void setComment(String comment) {
        String oldValue = this.comment;
        this.comment = comment == null ? "" : comment;
        if (!Objects.equals(oldValue, this.comment)) {
            fireStateChanged();
        }
    }

    /**
     * Replies the current changeset comment in this model.
     *
     * @return the current changeset comment in this model.
     */
    public String getComment() {
        return comment == null ? "" : comment;
    }
}
