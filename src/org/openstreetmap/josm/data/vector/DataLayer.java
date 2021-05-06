// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.vector;

/**
 * An interface for objects that are part of a data layer
 * @param <T> The type used to identify a layer, typically a string
 */
public interface DataLayer<T> {
    /**
     * Get the layer
     * @return The layer
     */
    T getLayer();

    /**
     * Set the layer
     * @param layer The layer to set
     * @return {@code true} if the layer was set -- some objects may never change layers.
     */
    default boolean setLayer(T layer) {
        return layer != null && layer.equals(getLayer());
    }
}
