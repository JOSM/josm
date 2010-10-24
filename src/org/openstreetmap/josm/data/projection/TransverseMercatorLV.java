// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection;

import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;

/**
 * LKS-92/ Latvia TM projection. Based on data from spatialreference.org.
 * http://spatialreference.org/ref/epsg/3059/
 *
 * @author Viesturs Zarins
 */
public class TransverseMercatorLV extends TransverseMercator {

    public TransverseMercatorLV()
    {
        setProjectionParameters(24, 500000, -6000000);
    }

    @Override public String toString() {
        return tr("LKS-92 (Latvia TM)");
    }

    private int epsgCode() {
        return 3059;
    }

    @Override
    public String toCode() {
        return "EPSG:"+ epsgCode();
    }

    @Override
    public int hashCode() {
        return toCode().hashCode();
    }

    public String getCacheDirectoryName() {
        return "epsg"+ epsgCode();
    }

    @Override
    public Bounds getWorldBoundsLatLon() {
        return new Bounds(
                new LatLon(-90.0, -180.0),
                new LatLon(90.0, 180.0));
    }
}
