// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

public enum UploadOrSaveState {
    /**
     * a data layer was successfully saved or upload to the server
     */
    OK,
    /**
     * uploading or saving a data layer has failed
     */
    FAILED,
    /**
     * uploading or saving a data layer was canceled
     */
    CANCELED
}
