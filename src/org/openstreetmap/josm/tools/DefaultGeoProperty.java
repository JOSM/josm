// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import java.awt.Rectangle;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
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
    private LatLon random;

    /**
     * Create DefaultGeoProperty based on a collection of closed ways.
     *
     * @param ways the ways forming the area
     */
    public DefaultGeoProperty(Collection<Way> ways) {
        Path2D path = new Path2D.Double();
        path.setWindingRule(Path2D.WIND_EVEN_ODD);
        for (Way w : ways) {
            Geometry.buildPath2DLatLon(w.getNodes(), path);
        }
        this.area = new Area(path);
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

    /**
     * Returns the area.
     * @return the area
     * @since 14484
     */
    public final Area getArea() {
        return area;
    }

    /**
     * Returns a random lat/lon in the area.
     * @return a random lat/lon in the area
     * @since 15359
     */
    public final synchronized LatLon getRandomLatLon() {
        if (random == null) {
            Rectangle r = area.getBounds();
            double x, y;
            do {
                x = r.getX() + r.getWidth() * Math.random();
                y = r.getY() + r.getHeight() * Math.random();
            } while (!area.contains(x, y));

            random = new LatLon(y, x);
        }
        return random;
    }
}
