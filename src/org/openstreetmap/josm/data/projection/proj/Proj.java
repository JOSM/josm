// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection.proj;

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
     * A Human readable name of this projection.
     */
    String getName();

    /**
     * The Proj.4 identifier.
     * 
     * (as reported by cs2cs -lp)
     * If no id exists, return null.
     */
    String getProj4Id();
    
    /**
     * Convert lat/lon to east/north.
     * 
     * @param lat_rad the latitude in radians
     * @param lon_rad the longitude in radians
     * @return array of length 2, containing east and north value in meters,
     * divided by the semi major axis of the ellipsoid.
     */
    double[] project(double lat_rad, double lon_rad);
    
    /**
     * Convert east/north to lat/lon.
     * 
     * @param east east value in meters, divided by the semi major axis of the ellipsoid
     * @param north north value in meters, divided by the semi major axis of the ellipsoid
     * @return array of length 2, containing lat and lon in radians.
     */
    double[] invproject(double east, double north);
}
