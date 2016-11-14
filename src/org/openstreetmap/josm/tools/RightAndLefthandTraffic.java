// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import java.awt.geom.Area;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.JoinAreasAction;
import org.openstreetmap.josm.actions.JoinAreasAction.JoinAreasResult;
import org.openstreetmap.josm.actions.JoinAreasAction.Multipolygon;
import org.openstreetmap.josm.actions.PurgeAction;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.BBox;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.io.IllegalDataException;
import org.openstreetmap.josm.io.OsmReader;
import org.openstreetmap.josm.io.OsmWriter;
import org.openstreetmap.josm.io.OsmWriterFactory;
import org.openstreetmap.josm.tools.GeoPropertyIndex.GeoProperty;
import org.openstreetmap.josm.tools.Geometry.PolygonIntersection;

/**
 * Look up, if there is right- or left-hand traffic at a certain place.
 */
public final class RightAndLefthandTraffic {

    private static final String DRIVING_SIDE = "driving_side";
    private static final String LEFT = "left";
    private static final String RIGHT = "right";

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
     * @param ll the coordinates of the point
     * @return true if there is right-hand traffic, false if there is left-hand traffic
     */
    public static synchronized boolean isRightHandTraffic(LatLon ll) {
        return !rlCache.get(ll);
    }

    /**
     * Initializes Right and lefthand traffic data.
     * TODO: Synchronization can be refined inside the {@link GeoPropertyIndex} as most look-ups are read-only.
     */
    public static synchronized void initialize() {
        leftHandTrafficPolygons = new ArrayList<>();
        Collection<Way> optimizedWays = loadOptimizedBoundaries();
        if (optimizedWays.isEmpty()) {
            optimizedWays = computeOptimizedBoundaries();
            saveOptimizedBoundaries(optimizedWays);
        }
        for (Way w : optimizedWays) {
            leftHandTrafficPolygons.add(Geometry.getAreaLatLon(w.getNodes()));
        }
        rlCache = new GeoPropertyIndex<>(new RLTrafficGeoProperty(), 24);
    }

    private static Collection<Way> computeOptimizedBoundaries() {
        Collection<Way> ways = new ArrayList<>();
        Collection<OsmPrimitive> toPurge = new ArrayList<>();
        // Find all outer ways of left-driving countries. Many of them are adjacent (African and Asian states)
        DataSet data = Territories.getDataSet();
        Collection<Relation> allRelations = data.getRelations();
        Collection<Way> allWays = data.getWays();
        for (Way w : allWays) {
            if (LEFT.equals(w.get(DRIVING_SIDE))) {
                addWayIfNotInner(ways, w);
            }
        }
        for (Relation r : allRelations) {
            if (r.isMultipolygon() && LEFT.equals(r.get(DRIVING_SIDE))) {
                for (RelationMember rm : r.getMembers()) {
                    if (rm.isWay() && "outer".equals(rm.getRole()) && !RIGHT.equals(rm.getMember().get(DRIVING_SIDE))) {
                        addWayIfNotInner(ways, (Way) rm.getMember());
                    }
                }
            }
        }
        toPurge.addAll(allRelations);
        toPurge.addAll(allWays);
        toPurge.removeAll(ways);
        // Remove ways from parent relations for following optimizations
        for (Relation r : OsmPrimitive.getParentRelations(ways)) {
            r.setMembers(null);
        }
        // Remove all tags to avoid any conflict
        for (Way w : ways) {
            w.removeAll();
        }
        // Purge all other ways and relations so dataset only contains lefthand traffic data
        new PurgeAction().getPurgeCommand(toPurge).executeCommand();
        // Combine adjacent countries into a single polygon
        Collection<Way> optimizedWays = new ArrayList<>();
        List<Multipolygon> areas = JoinAreasAction.collectMultipolygons(ways);
        if (areas != null) {
            try {
                JoinAreasResult result = new JoinAreasAction().joinAreas(areas);
                if (result.hasChanges) {
                    for (Multipolygon mp : result.polygons) {
                        optimizedWays.add(mp.outerWay);
                    }
                }
            } catch (UserCancelException ex) {
                Main.warn(ex);
            }
        }
        if (optimizedWays.isEmpty()) {
            // Problem: don't optimize
            Main.warn("Unable to join left-driving countries polygons");
            optimizedWays.addAll(ways);
        }
        return optimizedWays;
    }

    /**
     * Adds w to ways, except if it is an inner way of another lefthand driving multipolygon,
     * as Lesotho in South Africa and Cyprus village in British Cyprus base.
     * @param ways ways
     * @param w way
     */
    private static void addWayIfNotInner(Collection<Way> ways, Way w) {
        Set<Way> s = Collections.singleton(w);
        for (Relation r : OsmPrimitive.getParentRelations(s)) {
            if (r.isMultipolygon() && LEFT.equals(r.get(DRIVING_SIDE)) &&
                "inner".equals(r.getMembersFor(s).iterator().next().getRole())) {
                if (Main.isDebugEnabled()) {
                    Main.debug("Skipping " + w.get("name:en") + " because inner part of " + r.get("name:en"));
                }
                return;
            }
        }
        ways.add(w);
    }

    private static void saveOptimizedBoundaries(Collection<Way> optimizedWays) {
        DataSet ds = optimizedWays.iterator().next().getDataSet();
        File file = new File(Main.pref.getCacheDirectory(), "left-right-hand-traffic.osm");
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8);
             OsmWriter w = OsmWriterFactory.createOsmWriter(new PrintWriter(writer), false, ds.getVersion())
            ) {
            w.header(false);
            w.writeContent(ds);
            w.footer();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static Collection<Way> loadOptimizedBoundaries() {
        try (InputStream is = new FileInputStream(new File(Main.pref.getCacheDirectory(), "left-right-hand-traffic.osm"))) {
           return OsmReader.parseDataSet(is, null).getWays();
        } catch (IllegalDataException | IOException ex) {
            Main.trace(ex);
            return Collections.emptyList();
        }
    }
}
