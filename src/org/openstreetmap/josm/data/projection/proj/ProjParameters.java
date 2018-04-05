// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection.proj;

import org.openstreetmap.josm.data.projection.CustomProjection.Param;
import org.openstreetmap.josm.data.projection.Ellipsoid;

/**
 * Parameters to initialize a Proj object.
 * @since 5066
 */
public class ProjParameters {

    /** {@code +ellps} */
    public Ellipsoid ellps;

    /** {@link Param#lat_0} */
    public Double lat0;
    /** {@link Param#lat_1} */
    public Double lat1;
    /** {@link Param#lat_2} */
    public Double lat2;

    // Polar Stereographic and Mercator
    /** {@link Param#lat_ts} */
    public Double lat_ts;

    // Azimuthal Equidistant
    /** {@link Param#lon_0} */
    public Double lon0;

    // Oblique Mercator
    /** {@link Param#lonc} */
    public Double lonc;
    /** {@link Param#alpha} */
    public Double alpha;
    /** {@link Param#gamma} */
    public Double gamma;
    /** {@link Param#no_off} */
    public Boolean no_off;
    /** {@link Param#lon_1} */
    public Double lon1;
    /** {@link Param#lon_2} */
    public Double lon2;
}
