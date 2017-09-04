// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import org.openstreetmap.josm.gui.util.ChangeNotifier;

/**
 * ChangesetReviewModel is an observable model for the changeset review requested
 * in the {@link UploadDialog}.
 * @since 12719
 */
public class ChangesetReviewModel extends ChangeNotifier {
    private boolean review;

    /**
     * Sets the current changeset review request state and notifies observers if it has changed.
     *
     * @param review the new review request state
     */
    public void setReviewRequested(boolean review) {
        boolean oldValue = this.review;
        this.review = review;
        if (oldValue != this.review) {
            fireStateChanged();
        }
    }

    /**
     * Determines if a changeset review has been requested.
     *
     * @return {@code true} if a changeset review has been requested
     */
    public boolean isReviewRequested() {
        return review;
    }
}
