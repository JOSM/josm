// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.upload;

import java.util.Map;

import org.openstreetmap.josm.data.APIDataSet;

/**
 * Change, or block, the upload.
 *
 * The UploadHook may modify the uploaded data silently, it may display a
 * warning message to the user or prevent the upload altogether.
 *
 * The tags of the changeset can also be changed with modifyChangesetTags method.
 */
public interface UploadHook {

    /**
     * Check, and/or change, the data to be uploaded.
     * Default implementation is to approve the upload.
     * @param apiDataSet the data to upload, modify this to change the data.
     * @return {@code true} if upload is possible, {@code false} to block the upload.
     */
    default boolean checkUpload(APIDataSet apiDataSet) {
        return true;
    }

    /**
     * Modify the changeset tags (in place) before upload.
     * Default implementation is to do no changes.
     * @param tags The current tags to change
     * @since 13028
     */
    default void modifyChangesetTags(Map<String, String> tags) {
    }
}
