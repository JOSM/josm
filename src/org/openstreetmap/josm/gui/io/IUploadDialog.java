// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import java.util.Map;

import org.openstreetmap.josm.io.UploadStrategySpecification;

/**
 * Upload dialog super interface.
 * @since 9685
 */
public interface IUploadDialog {

    /**
     * Returns true if the dialog was canceled
     *
     * @return true if the dialog was canceled
     */
    boolean isCanceled();

    /**
     * Remembers the user input in the preference settings
     */
    void rememberUserInput();

    /**
     * Returns the current value for the upload comment
     *
     * @return the current value for the upload comment
     */
    String getUploadComment();

    /**
     * Returns the current value for the changeset source
     *
     * @return the current value for the changeset source
     */
    String getUploadSource();

    /**
     * Replies the {@link UploadStrategySpecification} the user entered in the dialog.
     *
     * @return the {@link UploadStrategySpecification} the user entered in the dialog.
     */
    UploadStrategySpecification getUploadStrategySpecification();

    /**
     * Replies the map with the current tags in the tag editor model.
     * @param keepEmpty {@code true} to keep empty tags
     * @return the map with the current tags in the tag editor model.
     */
    Map<String, String> getTags(boolean keepEmpty);

    /**
     * Handles missing comment.
     */
    void handleMissingComment();

    /**
     * Handles missing source.
     */
    void handleMissingSource();

    /**
     * Handles illegal chunk size.
     */
    void handleIllegalChunkSize();
}
