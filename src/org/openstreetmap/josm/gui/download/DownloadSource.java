// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.download;

/**
 * Defines an interface for different download sources.
 * <p>
 * Plugins may implement this to provide new download sources to the main download dialog.
 * @param <T> The type of the data that a download source uses.
 * @since 12652
 */
public interface DownloadSource<T> {

    /**
     * Creates a panel with GUI specific for the download source.
     * @param dialog the parent download dialog, as {@code DownloadDialog.getInstance()} might not be initialized yet
     * @return Returns {@link AbstractDownloadSourcePanel}.
     * @since 12900
     */
    AbstractDownloadSourcePanel<T> createPanel(DownloadDialog dialog);

    /**
     * Downloads the data.
     * @param data The required data for the download source.
     * @param settings The global settings of the download dialog, see {@link DownloadDialog}.
     */
    void doDownload(T data, DownloadSettings settings);

    /**
     * Returns a string representation of this download source.
     * @return A string representation of this download source.
     */
    String getLabel();

    /**
     * Defines whether this download source should be visible only in the expert mode.
     * @return Returns {@code true} if the download source should be visible only in the
     * expert mode, {@code false} otherwise.
     */
    boolean onlyExpert();
}
