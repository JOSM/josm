// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.imagery.vectortile;

import java.util.Collection;

import org.openstreetmap.josm.data.imagery.vectortile.mapbox.Layer;

/**
 * An interface that is used to draw vector tiles, instead of using images
 * @author Taylor Smock
 * @since xxx
 */
public interface VectorTile {
    /**
     * Get the layers for this vector tile
     * @return A collection of layers
     */
    Collection<Layer> getLayers();

    /**
     * Get the extent of the tile (in pixels)
     * @return The tile extent (pixels)
     */
    int getExtent();
}
