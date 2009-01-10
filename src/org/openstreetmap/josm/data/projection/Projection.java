// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.data.projection;

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
     * Maximum latitude representable.
     */
    public static final double MAX_LAT = 85.05112877980659; // Mercator squares the world

    /**
     * Maximum longditude representable.
     */
    public static final double MAX_LON = 180;

    /**
     * Minimum difference in location to not be represented as the same position.
     */
    public static final double MAX_SERVER_PRECISION = 1e12;

    /**
     * List of all available projections.
     */
    public static Projection[] allProjections = new Projection[]{
        new Epsg4326(),
        new Mercator(),
        new Lambert(),
        new LambertEST()
    };

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
     * Get a filename compatible string (for the cache directory)
     */
    String getCacheDirectoryName();

    /**
     * The factor to multiply with an easting coordinate to get from "easting
     * units per pixel" to "meters per pixel"
     */
    double scaleFactor();
}
