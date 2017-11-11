// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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

    /**
     * Extracts the list of hashtags from the comment text.
     * @return the list of hashtags from the comment text. Can be empty, but not null.
     * @since 13109
     */
    public List<String> findHashTags() {
        return Arrays.stream(comment.split("\\s")).filter(s -> s.length() >= 2 && s.charAt(0) == '#').collect(Collectors.toList());
    }
}
