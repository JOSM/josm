// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import java.awt.geom.Area;
import java.util.Collection;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.BBox;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;

/**
 * Implementation of simple boolean {@link GeoProperty}.
 */
public class DefaultGeoProperty implements GeoProperty<Boolean> {

    private final Area area;

    /**
     * Create DefaultGeoProperty based on a collection of closed ways.
     *
     * @param ways the ways forming the area
     */
    public DefaultGeoProperty(Collection<Way> ways) {
        Area area = null;
        for (Way w : ways) {
            Area wayArea = Geometry.getAreaLatLon(w.getNodes());
            if (area == null) {
                area = wayArea;
            } else {
                area.add(wayArea);
            }
        }
        this.area = area;
    }

    /**
     * Create DefaultGeoProperty based on a multipolygon relation.
     *
     * @param multipolygon the multipolygon
     */
    public DefaultGeoProperty(Relation multipolygon) {
        this.area = Geometry.getAreaLatLon(multipolygon);
    }

    @Override
    public Boolean get(LatLon ll) {
        return area.contains(ll.lon(), ll.lat());
    }

    @Override
    public Boolean get(BBox box) {
        Area abox = new Area(box.toRectangle());
        Geometry.PolygonIntersection is = Geometry.polygonIntersection(abox, area, 1e-10 /* using deg and not meters */);
        switch (is) {
            case FIRST_INSIDE_SECOND:
                return Boolean.TRUE;
            case OUTSIDE:
                return Boolean.FALSE;
            default:
                return null;
        }
    }

}
