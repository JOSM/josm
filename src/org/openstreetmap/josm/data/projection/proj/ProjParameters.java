// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection.proj;

import org.openstreetmap.josm.data.projection.Ellipsoid;

/**
 * Parameters to initialize a Proj object.
 */
public class ProjParameters {

    public Ellipsoid ellps;

    public Double lat_0;
    public Double lat_1;
    public Double lat_2;

    /* for LambertConformalConic */
    public Double lcc_n;
    public Double lcc_F;
    public Double lcc_r0;

}
