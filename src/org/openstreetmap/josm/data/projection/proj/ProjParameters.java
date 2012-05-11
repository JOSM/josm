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

}
