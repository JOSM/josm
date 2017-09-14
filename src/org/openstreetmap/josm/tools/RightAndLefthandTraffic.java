// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

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

import org.openstreetmap.josm.actions.JoinAreasAction;
import org.openstreetmap.josm.actions.JoinAreasAction.JoinAreasResult;
import org.openstreetmap.josm.actions.JoinAreasAction.Multipolygon;
import org.openstreetmap.josm.command.PurgeCommand;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.io.IllegalDataException;
import org.openstreetmap.josm.io.OsmReader;
import org.openstreetmap.josm.io.OsmWriter;
import org.openstreetmap.josm.io.OsmWriterFactory;
import org.openstreetmap.josm.spi.preferences.Config;

/**
 * Look up, if there is right- or left-hand traffic at a certain place.
 */
public final class RightAndLefthandTraffic {

    private static final String DRIVING_SIDE = "driving_side";
    private static final String LEFT = "left";
    private static final String RIGHT = "right";

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
        Collection<Way> optimizedWays = loadOptimizedBoundaries();
        if (optimizedWays.isEmpty()) {
            optimizedWays = computeOptimizedBoundaries();
            saveOptimizedBoundaries(optimizedWays);
        }
        rlCache = new GeoPropertyIndex<>(new DefaultGeoProperty(optimizedWays), 24);
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
        PurgeCommand.build(toPurge, null).executeCommand();
        // Combine adjacent countries into a single polygon
        Collection<Way> optimizedWays = new ArrayList<>();
        List<Multipolygon> areas = JoinAreasAction.collectMultipolygons(ways);
        if (areas != null) {
            try {
                JoinAreasResult result = new JoinAreasAction(false).joinAreas(areas);
                if (result.hasChanges()) {
                    for (Multipolygon mp : result.getPolygons()) {
                        optimizedWays.add(mp.getOuterWay());
                    }
                }
            } catch (UserCancelException ex) {
                Logging.warn(ex);
            } catch (JosmRuntimeException ex) {
                // Workaround to #10511 / #14185. To remove when #10511 is solved
                Logging.error(ex);
            }
        }
        if (optimizedWays.isEmpty()) {
            // Problem: don't optimize
            Logging.warn("Unable to join left-driving countries polygons");
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
                if (Logging.isDebugEnabled()) {
                    Logging.debug("Skipping {0} because inner part of {1}", w.get("name:en"), r.get("name:en"));
                }
                return;
            }
        }
        ways.add(w);
    }

    private static void saveOptimizedBoundaries(Collection<Way> optimizedWays) {
        DataSet ds = optimizedWays.iterator().next().getDataSet();
        File file = new File(Config.getDirs().getCacheDirectory(true), "left-right-hand-traffic.osm");
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8);
             OsmWriter w = OsmWriterFactory.createOsmWriter(new PrintWriter(writer), false, ds.getVersion())
            ) {
            w.header(DataSet.UploadPolicy.DISCOURAGED);
            w.writeContent(ds);
            w.footer();
        } catch (IOException ex) {
            throw new JosmRuntimeException(ex);
        }
    }

    private static Collection<Way> loadOptimizedBoundaries() {
        try (InputStream is = new FileInputStream(new File(
                Config.getDirs().getCacheDirectory(false), "left-right-hand-traffic.osm"))) {
           return OsmReader.parseDataSet(is, null).getWays();
        } catch (IllegalDataException | IOException ex) {
            Logging.trace(ex);
            return Collections.emptyList();
        }
    }
}
