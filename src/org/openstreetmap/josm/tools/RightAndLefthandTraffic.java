// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;

/**
 * Look up, if there is right- or left-hand traffic at a certain place.
 * See <a href="https://en.wikipedia.org/wiki/Left-_and_right-hand_traffic">Left- and right-hand traffic</a>
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
        Boolean value = rlCache.get(ll);
        return value == null || !value;
    }

    /**
     * Initializes Right and lefthand traffic data.
     * @param geoProperty the property containing the traffic data
     * TODO: Synchronization can be refined inside the {@link GeoPropertyIndex} as most look-ups are read-only.
     */
    static synchronized void initialize(DefaultGeoProperty geoProperty) {
        rlCache = new GeoPropertyIndex<>(geoProperty, 24);
    }

    static void appendLeftDrivingBoundaries(OsmPrimitive osm, Collection<Way> ways) {
        // Find all outer ways of left-driving countries. Many of them are adjacent (African and Asian states)
        if (LEFT.equals(osm.get(DRIVING_SIDE))) {
            if (osm instanceof Way) {
                Way w = (Way) osm;
                addWayIfNotInner(ways, w);
            } else if (osm instanceof Relation && osm.isMultipolygon()) {
                Relation r = (Relation) osm;
                for (RelationMember rm : r.getMembers()) {
                    if (rm.isWay() && "outer".equals(rm.getRole()) && !RIGHT.equals(rm.getMember().get(DRIVING_SIDE))) {
                        addWayIfNotInner(ways, (Way) rm.getMember());
                    }
                }
            }
        }
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
}
