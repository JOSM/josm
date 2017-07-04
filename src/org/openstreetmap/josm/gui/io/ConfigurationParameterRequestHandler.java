// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

/**
 * Handler, that will take action when the user clicks one of two hyperlinks
 * in the upload dialog.
 */
public interface ConfigurationParameterRequestHandler {
    /**
     * Handle the event when user clicks the "configure changeset" hyperlink.
     */
    void handleChangesetConfigurationRequest();

    /**
     * Handle the event when user clicks the "advanced configuration" hyperlink.
     * The advanced configuration tab contains upload strategy parameters.
     */
    void handleUploadStrategyConfigurationRequest();
}
