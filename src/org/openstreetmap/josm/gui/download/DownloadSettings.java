// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.download;

/**
 * The global settings of {@link DownloadDialog}.
 */
public final class DownloadSettings {

    private boolean downloadAsNewLayer;
    private boolean zoomToDownloadedData;

    /**
     * Initializes a new instance of {@code DownloadSettings}.
     * @param downloadAsNewLayer The flag defining if a new layer must be created for the downloaded data.
     * @param zoomToDownloadedData The flag defining if the map view, see {@link SlippyMapChooser},
     *                             must zoom to the downloaded data.
     */
    public DownloadSettings(boolean downloadAsNewLayer, boolean zoomToDownloadedData) {
        this.downloadAsNewLayer = downloadAsNewLayer;
        this.zoomToDownloadedData = zoomToDownloadedData;
    }

    /**
     * Gets the flag defining if a new layer must be created for the downloaded data.
     * @return {@code true} if a new layer must be created, {@code false} otherwise.
     */
    public boolean asNewLayer() {
        return this.downloadAsNewLayer;
    }

    /**
     * Gets the flag defining if the map view must zoom to the downloaded data.
     * @return {@code true} if the view must zoom, {@code false} otherwise.
     */
    public boolean zoomToData() {
        return this.zoomToDownloadedData;
    }
}
