// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.geoimage;

import java.util.Collections;
import java.util.List;

import org.openstreetmap.josm.data.imagery.street_level.IImageEntry;

/**
 * An interface for layers which want to show images
 * @since 18613
 */
public interface IGeoImageLayer {
    /**
     * Clear the selection of the layer
     */
    void clearSelection();

    /**
     * Get the current selection
     * @return The currently selected images
     */
    List<? extends IImageEntry<?>> getSelection();

    /**
     * Get the invalid geo images for this layer (specifically, those that <i>cannot</i> be displayed on the map)
     * @return The list of invalid geo images
     */
    default List<IImageEntry<?>> getInvalidGeoImages() {
        return Collections.emptyList();
    }

    /**
     * Check if the layer contains the specified image
     * @param imageEntry The entry to look for
     * @return {@code true} if this layer contains the image
     */
    boolean containsImage(IImageEntry<?> imageEntry);

    /**
     * Add a listener for when images change
     * @param listener The listener to call
     */
    void addImageChangeListener(ImageChangeListener listener);

    /**
     * Remove a listener for when images change
     * @param listener The listener to remove
     */
    void removeImageChangeListener(ImageChangeListener listener);

    /**
     * Listen for image changes
     */
    interface ImageChangeListener {
        /**
         * Called when the selected image(s) change
         * @param source The source of the change
         * @param oldImages The previously selected image(s)
         * @param newImages The newly selected image(s)
         */
        void imageChanged(IGeoImageLayer source, List<? extends IImageEntry<?>> oldImages, List<? extends IImageEntry<?>> newImages);
    }
}
