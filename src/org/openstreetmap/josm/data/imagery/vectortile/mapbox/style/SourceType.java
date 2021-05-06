// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.imagery.vectortile.mapbox.style;

/**
 * The "source type" for the data (Mapbox Vector Style specification)
 *
 * @author Taylor Smock
 * @since 17862
 */
public enum SourceType {
    VECTOR,
    RASTER,
    RASTER_DEM,
    GEOJSON,
    IMAGE,
    VIDEO
}
