// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;

/**
 * A projection, i.e.&nbsp;a class that supports conversion from lat/lon
 * to east/north and back.
 *
 * The conversion from east/north to the screen coordinates is simply a scale
 * factor and x/y offset.
 */
public interface Projection {
    /**
     * The default scale factor in east/north units per pixel
     * ({@link org.openstreetmap.josm.gui.NavigatableComponent#scale})).
     * FIXME: misnomer
     * @return the scale factor
     */
    double getDefaultZoomInPPD();

    /**
     * Convert from lat/lon to easting/northing.
     *
     * @param ll the geographical point to convert (in WGS84 lat/lon)
     * @return the corresponding east/north coordinates
     */
    EastNorth latlon2eastNorth(LatLon ll);

    /**
     * Convert from easting/norting to lat/lon.
     *
     * @param en the geographical point to convert (in projected coordinates)
     * @return the corresponding lat/lon (WGS84)
     */
    LatLon eastNorth2latlon(EastNorth en);

    /**
     * Describe the projection in one or two words.
     * @return the name / description
     */
    String toString();

    /**
     * Return projection code.
     *
     * This should be a unique identifier.
     * If projection supports parameters, return a different code
     * for each set of parameters.
     *
     * The EPSG code can be used (if defined for the projection).
     *
     * @return the projection identifier
     */
    String toCode();

    /**
     * Get a filename compatible string (for the cache directory).
     * @return the cache directory name (base name)
     */
    String getCacheDirectoryName();

    /**
     * Get the bounds of the world.
     * @return the supported lat/lon rectangle for this projection
     */
    Bounds getWorldBoundsLatLon();
}
