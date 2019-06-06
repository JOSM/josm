// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Rectangle;
import java.awt.geom.Area;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.openstreetmap.josm.data.validation.tests.MultipolygonTest;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.Geometry;
import org.openstreetmap.josm.tools.MultiMap;
import org.openstreetmap.josm.tools.Pair;

/**
 * Helper class to build multipolygons from multiple ways.
 * @author viesturs
 * @since 7392 (rename)
 * @since 3704
 */
public class MultipolygonBuilder {

    /**
     * Represents one polygon that consists of multiple ways.
     */
    public static class JoinedPolygon {
        /** list of ways building this polygon */
        public final List<Way> ways;
        /** list of flags that indicate if the nodes of the way in the same position where reversed */
        public final List<Boolean> reversed;
        /** the nodes of the polygon, first node is not duplicated as last node. */
        public final List<Node> nodes;
        /** the area in east/north space */
        public final Area area;
        /** the integer bounds,
         * @deprecated better use area.getBounds2D()
         *  */
        @Deprecated
        public final Rectangle bounds;

        /**
         * Constructs a new {@code JoinedPolygon} from given list of ways.
         * @param ways The ways used to build joined polygon
         * @param reversed list of reversed states
         */
        public JoinedPolygon(List<Way> ways, List<Boolean> reversed) {
            this.ways = ways;
            this.reversed = reversed;
            this.nodes = this.getNodes();
            this.area = Geometry.getArea(nodes);
            this.bounds = area.getBounds();
        }

        /**
         * Creates a polygon from single way.
         * @param way the way to form the polygon
         */
        public JoinedPolygon(Way way) {
            this(Collections.singletonList(way), Collections.singletonList(Boolean.FALSE));
        }

        /**
         * Builds a list of nodes for this polygon. First node is not duplicated as last node.
         * @return list of nodes
         */
        public List<Node> getNodes() {
            List<Node> ringNodes = new ArrayList<>();

            for (int waypos = 0; waypos < this.ways.size(); waypos++) {
                Way way = this.ways.get(waypos);

                if (!this.reversed.get(waypos)) {
                    for (int pos = 0; pos < way.getNodesCount() - 1; pos++) {
                        ringNodes.add(way.getNode(pos));
                    }
                } else {
                    for (int pos = way.getNodesCount() - 1; pos > 0; pos--) {
                        ringNodes.add(way.getNode(pos));
                    }
                }
            }

            return ringNodes;
        }
    }

    /** List of outer ways **/
    public final List<JoinedPolygon> outerWays;
    /** List of inner ways **/
    public final List<JoinedPolygon> innerWays;

    /**
     * Constructs a new {@code MultipolygonBuilder} initialized with given ways.
     * @param outerWays The outer ways
     * @param innerWays The inner ways
     */
    public MultipolygonBuilder(List<JoinedPolygon> outerWays, List<JoinedPolygon> innerWays) {
        this.outerWays = outerWays;
        this.innerWays = innerWays;
    }

    /**
     * Constructs a new empty {@code MultipolygonBuilder}.
     */
    public MultipolygonBuilder() {
        this.outerWays = new ArrayList<>(0);
        this.innerWays = new ArrayList<>(0);
    }

    /**
     * Splits ways into inner and outer JoinedWays. Sets {@link #innerWays} and {@link #outerWays} to the result.
     * Calculation is done in {@link MultipolygonTest#makeFromWays(Collection)} to ensure that the result is a valid multipolygon.
     * @param ways ways to analyze
     * @return error description if the ways cannot be split, {@code null} if all fine.
     */
    public String makeFromWays(Collection<Way> ways) {
        MultipolygonTest mpTest = new MultipolygonTest();
        Relation calculated = mpTest.makeFromWays(ways);
        if (!mpTest.getErrors().isEmpty()) {
            return mpTest.getErrors().iterator().next().getMessage();
        }
        Pair<List<JoinedPolygon>, List<JoinedPolygon>> outerInner = joinWays(calculated);
        this.outerWays.clear();
        this.innerWays.clear();
        this.outerWays.addAll(outerInner.a);
        this.innerWays.addAll(outerInner.b);
        return null;
    }

    /**
     * An exception indicating an error while joining ways to multipolygon rings.
     */
    public static class JoinedPolygonCreationException extends RuntimeException {
        /**
         * Constructs a new {@code JoinedPolygonCreationException}.
         * @param message the detail message. The detail message is saved for
         *                later retrieval by the {@link #getMessage()} method
         */
        public JoinedPolygonCreationException(String message) {
            super(message);
        }
    }

    /**
     * Joins the given {@code multipolygon} to a pair of outer and inner multipolygon rings.
     *
     * @param multipolygon the multipolygon to join.
     * @return a pair of outer and inner multipolygon rings.
     * @throws JoinedPolygonCreationException if the creation fails.
     */
    public static Pair<List<JoinedPolygon>, List<JoinedPolygon>> joinWays(Relation multipolygon) {
        CheckParameterUtil.ensureThat(multipolygon.isMultipolygon(), "multipolygon.isMultipolygon");
        final Map<String, Set<Way>> members = multipolygon.getMembers().stream()
                .filter(RelationMember::isWay)
                .collect(Collectors.groupingBy(RelationMember::getRole, Collectors.mapping(RelationMember::getWay, Collectors.toSet())));
        final List<JoinedPolygon> outerRings = joinWays(members.getOrDefault("outer", Collections.emptySet()));
        final List<JoinedPolygon> innerRings = joinWays(members.getOrDefault("inner", Collections.emptySet()));
        return Pair.create(outerRings, innerRings);
    }

    /**
     * Joins the given {@code ways} to multipolygon rings.
     * @param ways the ways to join.
     * @return a list of multipolygon rings.
     * @throws JoinedPolygonCreationException if the creation fails.
     */
    public static List<JoinedPolygon> joinWays(Collection<Way> ways) {
        List<JoinedPolygon> joinedWays = new ArrayList<>();

        //collect ways connecting to each node.
        MultiMap<Node, Way> nodesWithConnectedWays = new MultiMap<>();
        Set<Way> usedWays = new HashSet<>();

        for (Way w: ways) {
            if (w.getNodesCount() < 2) {
                throw new JoinedPolygonCreationException(tr("Cannot add a way with only {0} nodes.", w.getNodesCount()));
            }

            if (w.isClosed()) {
                //closed way, add as is.
                JoinedPolygon jw = new JoinedPolygon(w);
                joinedWays.add(jw);
                usedWays.add(w);
            } else {
                nodesWithConnectedWays.put(w.lastNode(), w);
                nodesWithConnectedWays.put(w.firstNode(), w);
            }
        }

        //process unclosed ways
        for (Way startWay: ways) {
            if (usedWays.contains(startWay)) {
                continue;
            }

            Node startNode = startWay.firstNode();
            List<Way> collectedWays = new ArrayList<>();
            List<Boolean> collectedWaysReverse = new ArrayList<>();
            Way curWay = startWay;
            Node prevNode = startNode;

            //find polygon ways
            while (true) {
                boolean curWayReverse = prevNode == curWay.lastNode();
                Node nextNode = curWayReverse ? curWay.firstNode() : curWay.lastNode();

                //add cur way to the list
                collectedWays.add(curWay);
                collectedWaysReverse.add(Boolean.valueOf(curWayReverse));

                if (nextNode == startNode) {
                    //way finished
                    break;
                }

                //find next way
                Collection<Way> adjacentWays = nodesWithConnectedWays.get(nextNode);

                if (adjacentWays.size() != 2) {
                    throw new JoinedPolygonCreationException(tr("Each node must connect exactly 2 ways"));
                }

                Way nextWay = null;
                for (Way way: adjacentWays) {
                    if (way != curWay) {
                        nextWay = way;
                    }
                }

                //move to the next way
                curWay = nextWay;
                prevNode = nextNode;
            }

            usedWays.addAll(collectedWays);
            joinedWays.add(new JoinedPolygon(collectedWays, collectedWaysReverse));
        }

        return joinedWays;
    }
}
