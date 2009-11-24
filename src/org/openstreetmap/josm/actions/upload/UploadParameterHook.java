// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.upload;

import org.openstreetmap.josm.data.APIDataSet;
import org.openstreetmap.josm.gui.io.UploadDialog;

public class UploadParameterHook implements UploadHook {

    public boolean checkUpload(APIDataSet apiData) {
        final UploadDialog dialog = UploadDialog.getUploadDialog();
        dialog.setUploadedPrimitives(apiData.getPrimitivesToAdd(),apiData.getPrimitivesToUpdate(), apiData.getPrimitivesToDelete());
        dialog.setVisible(true);
        if (dialog.isCanceled())
            return false;
        dialog.rememberUserInput();
        return true;
    }
}
