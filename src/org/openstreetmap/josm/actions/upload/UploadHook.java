// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.upload;

import org.openstreetmap.josm.data.APIDataSet;

@FunctionalInterface
public interface UploadHook {

    /**
     * Checks the upload.
     * @param apiDataSet the data to upload
     * @return {@code true} if upload is possible
     */
    boolean checkUpload(APIDataSet apiDataSet);
}
