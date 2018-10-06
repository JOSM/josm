// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection.proj;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.projection.ProjectionConfigurationException;

/**
 * A projection (in the narrow sense).
 *
 * Converts lat/lon the east/north and the other way around.
 *
 * Datum conversion, false easting / northing, origin of longitude
 * and general scale factor is already applied when the projection is invoked.
 *
 * Lat/lon is not in degrees, but in radians (unlike other parts of JOSM).
 * Additional parameters in the constructor arguments are usually still in
 * degrees. So to avoid confusion, you can follow the convention, that
 * coordinates in radians are called lat_rad/lon_rad or phi/lambda.
 *
 * East/north values are not in meters, but in meters divided by the semi major
 * axis of the ellipsoid (earth radius). (Usually this is what you get anyway,
 * unless you multiply by 'a' somehow implicitly or explicitly.)
 *
 */
public interface Proj {

    /**
     * Replies a human readable name of this projection.
     * @return The projection name. must not be null.
     */
    String getName();

    /**
     * Replies the Proj.4 identifier.
     *
     * @return The Proj.4 identifier (as reported by cs2cs -lp).
     * If no id exists, return {@code null}.
     */
    String getProj4Id();

    /**
     * Initialize the projection using the provided parameters.
     * @param params The projection parameters
     *
     * @throws ProjectionConfigurationException in case parameters are not suitable
     */
    void initialize(ProjParameters params) throws ProjectionConfigurationException;

    /**
     * Convert lat/lon to east/north.
     *
     * @param latRad the latitude in radians
     * @param lonRad the longitude in radians
     * @return array of length 2, containing east and north value in meters,
     * divided by the semi major axis of the ellipsoid.
     */
    double[] project(double latRad, double lonRad);

    /**
     * Convert east/north to lat/lon.
     *
     * @param east east value in meters, divided by the semi major axis of the ellipsoid
     * @param north north value in meters, divided by the semi major axis of the ellipsoid
     * @return array of length 2, containing lat and lon in radians.
     */
    double[] invproject(double east, double north);

    /**
     * Return the bounds where this projection is applicable.
     *
     * This is a fallback for when the projection bounds are not specified
     * explicitly.
     *
     * In this area, the round trip lat/lon -&gt; east/north -&gt; lat/lon should
     * return the starting value with small error. In addition, regions with
     * extreme distortions should be excluded, if possible.
     *
     * It need not be the absolute maximum, but rather an area that is safe to
     * display in JOSM and contain everything that one would expect to use.
     *
     * @return the bounds where this projection is applicable, null if unknown
     */
    Bounds getAlgorithmBounds();

    /**
     * Return true, if a geographic coordinate reference system is represented.
     *
     * I.e. if it returns latitude/longitude values rather than Cartesian
     * east/north coordinates on a flat surface.
     * @return true, if it is geographic
     */
    boolean isGeographic();

    /**
     * Checks whether the result of projecting a lon coordinate only has a linear relation to the east coordinate and
     * is not related to lat/north at all.
     * @return <code>true</code> if lon has a linear relationship to east only.
     * @since 10805
     */
    default boolean lonIsLinearToEast() {
        return false;
    }
}
