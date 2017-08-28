// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

/**
 * This determines what to do when the max changeset size was exceeded by a upload.
 * @since 12687 (moved from {@code gui.io} package)
 */
public enum MaxChangesetSizeExceededPolicy {
    /**
     * Abort uploading. Send the user back to map editing.
     */
    ABORT,
    /**
     * Fill one changeset. If it is full send the user back to the
     * upload dialog where he can choose another changeset or another
     * upload strategy if he or she wants to.
     */
    FILL_ONE_CHANGESET_AND_RETURN_TO_UPLOAD_DIALOG,

    /**
     * Automatically open as many new changesets as necessary to upload
     * the data.
     */
    AUTOMATICALLY_OPEN_NEW_CHANGESETS
}
