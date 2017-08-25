// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.download;


import org.openstreetmap.josm.data.Bounds;

/**
 * Defines an interface for different download sources.
 * @param <T> The type of the data that a download source uses.
 */
public interface DownloadSource<T> {

    /**
     * Creates a panel with GUI specific for the download source.
     * @return Returns {@link AbstractDownloadSourcePanel}.
     */
    AbstractDownloadSourcePanel<T> createPanel();

    /**
     * Downloads the data.
     * @param bbox The bounding box. Can be null if no bounding box selected.
     * @param data The required data for the download source.
     * @param settings The global settings of the download dialog, see {@link DownloadDialog}.
     */
    void doDownload(Bounds bbox, T data, DownloadSettings settings);

    /**
     * Returns a string representation of this download source.
     * @return A string representation of this download source.
     */
    String getLabel();

    /**
     * Add a download source to the dialog, see {@link DownloadDialog}.
     * @param dialog The download dialog.
     */
    void addGui(DownloadDialog dialog);

    /**
     * Defines whether this download source should be visible only in the expert mode.
     * @return Returns {@code true} if the download source should be visible only in the
     * expert mode, {@code false} otherwise.
     */
    boolean onlyExpert();
}
