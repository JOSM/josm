// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.download;

import java.util.Optional;

import org.openstreetmap.josm.data.Bounds;

/**
 * The global settings of {@link DownloadDialog}.
 * <p>
 * This class is immutable
 * @since 12652
 */
public final class DownloadSettings {

    private final Bounds downloadBounds;
    private final boolean downloadAsNewLayer;
    private final boolean zoomToDownloadedData;

    /**
     * Initializes a new instance of {@code DownloadSettings}.
     * @param bbox The bounding box
     * @param downloadAsNewLayer The flag defining if a new layer must be created for the downloaded data.
     * @param zoomToDownloadedData The flag defining if the map view, see {@link SlippyMapChooser},
     *                             must zoom to the downloaded data.
     */
    public DownloadSettings(Bounds bbox, boolean downloadAsNewLayer, boolean zoomToDownloadedData) {
        this.downloadBounds = bbox;
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

    /**
     * Gets the download bounds that are requested
     * @return The bounds or an empty {@link Optional} if no bounds are selected
     */
    public Optional<Bounds> getDownloadBounds() {
        if (downloadBounds == null) {
            return Optional.empty();
        } else {
            return Optional.of(downloadBounds);
        }
    }
}
