// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.data.projection;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;

/**
 * Classes implementing this are able to convert lat/lon values to
 * planar screen coordinates.
 *
 * @author imi
 */
public interface Projection {
    /**
     * The default scale factor in east/north units per pixel ({@see #NavigatableComponent#scale}))
     * FIXME: misnomer
     */
    double getDefaultZoomInPPD();

    /**
     * Convert from lat/lon to northing/easting.
     *
     * @param p     The geo point to convert. x/y members of the point are filled.
     */
    EastNorth latlon2eastNorth(LatLon p);

    /**
     * Convert from norting/easting to lat/lon.
     *
     * @param p     The geo point to convert. lat/lon members of the point are filled.
     */
    LatLon eastNorth2latlon(EastNorth p);

    /**
     * Describe the projection converter in one or two words.
     */
    String toString();

    /**
     * Return projection code. This should be a unique identifier.
     * If projection supports parameters, return a different code
     * for each set of parameters.
     * 
     * The EPSG code can be used (if defined for the projection).
     */
    String toCode();

    /**
     * Get a filename compatible string (for the cache directory)
     */
    String getCacheDirectoryName();

    /**
     * Get the bounds of the world
     */
    Bounds getWorldBoundsLatLon();
}
