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
}
