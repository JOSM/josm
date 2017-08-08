// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.upload;

import org.openstreetmap.josm.data.APIDataSet;

/**
 * A check right before the upload. The UploadHook may modify the uploaded data
 * silently, it may display a warning message to the user or prevent the upload
 * altogether.
 */
@FunctionalInterface
public interface UploadHook {

    /**
     * Checks the upload.
     * @param apiDataSet the data to upload
     * @return {@code true} if upload is possible
     */
    boolean checkUpload(APIDataSet apiDataSet);
}
