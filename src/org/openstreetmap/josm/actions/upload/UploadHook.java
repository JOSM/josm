// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.upload;

import org.openstreetmap.josm.data.APIDataSet;

public interface UploadHook {
    /**
     * Checks the upload.
     * @param apiDataSet the data to upload
     */
    public boolean checkUpload(APIDataSet apiDataSet);
}
