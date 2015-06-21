// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import java.awt.geom.Area;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.BBox;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.io.CachedFile;
import org.openstreetmap.josm.io.IllegalDataException;
import org.openstreetmap.josm.io.OsmReader;
import org.openstreetmap.josm.tools.GeoPropertyIndex.GeoProperty;
import org.openstreetmap.josm.tools.Geometry.PolygonIntersection;

/**
 * Look up, if there is right- or left-hand traffic at a certain place.
 */
public final class RightAndLefthandTraffic {

    private static class RLTrafficGeoProperty implements GeoProperty<Boolean> {

        @Override
        public Boolean get(LatLon ll) {
            for (Area a : leftHandTrafficPolygons) {
                if (a.contains(ll.lon(), ll.lat()))
                    return Boolean.TRUE;
            }
            return Boolean.FALSE;
        }

        @Override
        public Boolean get(BBox box) {
            Area abox = new Area(box.toRectangle());
            for (Area a : leftHandTrafficPolygons) {
                PolygonIntersection is = Geometry.polygonIntersection(abox, a, 1e-10 /* using deg and not meters */);
                if (is == PolygonIntersection.FIRST_INSIDE_SECOND)
                    return Boolean.TRUE;
                if (is != PolygonIntersection.OUTSIDE)
                    return null;
            }
            return Boolean.FALSE;
        }
    }

    private static volatile Collection<Area> leftHandTrafficPolygons;
    private static volatile GeoPropertyIndex<Boolean> rlCache;

    private RightAndLefthandTraffic() {
        // Hide implicit public constructor for utility classes
    }

    /**
     * Check if there is right-hand traffic at a certain location.
     *
     * TODO: Synchronization can be refined inside the {@link GeoPropertyIndex}
     *       as most look-ups are read-only.
     * @param ll the coordinates of the point
     * @return true if there is right-hand traffic, false if there is left-hand traffic
     */
    public static synchronized boolean isRightHandTraffic(LatLon ll) {
        if (leftHandTrafficPolygons == null) {
            initialize();
        }
        return !rlCache.get(ll);
    }

    private static void initialize() {
        leftHandTrafficPolygons = new ArrayList<>();
        try (InputStream is = new CachedFile("resource://data/left-right-hand-traffic.osm").getInputStream()) {
            DataSet data = OsmReader.parseDataSet(is, null);
            for (Way w : data.getWays()) {
                leftHandTrafficPolygons.add(Geometry.getAreaLatLon(w.getNodes()));
            }
        } catch (IOException | IllegalDataException ex) {
            throw new RuntimeException(ex);
        }
        rlCache = new GeoPropertyIndex<Boolean>(new RLTrafficGeoProperty(), 24);
    }
}
