// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection.proj;

import org.openstreetmap.josm.data.projection.Ellipsoid;

/**
 * Parameters to initialize a Proj object.
 */
public class ProjParameters {

    public Ellipsoid ellps;

    public Double lat0;
    public Double lat1;
    public Double lat2;

    // Polar Stereographic and Mercator
    public Double lat_ts;

    // Oblique Mercator
    public Double lonc;
    public Double alpha;
    public Double gamma;
    public Boolean no_off;
    public Double lon1;
    public Double lon2;
}
