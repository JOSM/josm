// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.geom.Area;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openstreetmap.josm.tools.Geometry;
import org.openstreetmap.josm.tools.Geometry.PolygonIntersection;
import org.openstreetmap.josm.tools.MultiMap;

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
        public final List<Way> ways;
        public final List<Boolean> reversed;
        public final List<Node> nodes;
        public final Area area;

        /**
         * Constructs a new {@code JoinedPolygon} from given list of ways.
         * @param ways The ways used to build joined polygon
         */
        public JoinedPolygon(List<Way> ways, List<Boolean> reversed) {
            this.ways = ways;
            this.reversed = reversed;
            this.nodes = this.getNodes();
            this.area = Geometry.getArea(nodes);
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
            List<Node> nodes = new ArrayList<>();

            for (int waypos = 0; waypos < this.ways.size(); waypos ++) {
                Way way = this.ways.get(waypos);
                boolean reversed = this.reversed.get(waypos).booleanValue();

                if (!reversed) {
                    for (int pos = 0; pos < way.getNodesCount() - 1; pos++) {
                        nodes.add(way.getNode(pos));
                    }
                } else {
                    for (int pos = way.getNodesCount() - 1; pos > 0; pos--) {
                        nodes.add(way.getNode(pos));
                    }
                }
            }

            return nodes;
        }
    }

    /**
     * Helper storage class for finding findOuterWays
     */
    static class PolygonLevel {
        public final int level; //nesting level , even for outer, odd for inner polygons.
        public final JoinedPolygon outerWay;

        public List<JoinedPolygon> innerWays;

        public PolygonLevel(JoinedPolygon pol, int level) {
            this.outerWay = pol;
            this.level = level;
            this.innerWays = new ArrayList<>();
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
     * TODO: Currently cannot process touching polygons. See code in JoinAreasAction.
     * @param ways ways to analyze
     * @return error description if the ways cannot be split, {@code null} if all fine.
     */
    public String makeFromWays(Collection<Way> ways) {
        try {
            List<JoinedPolygon> joinedWays = joinWays(ways);
            //analyze witch way is inside witch outside.
            return makeFromPolygons(joinedWays);
        } catch (JoinedPolygonCreationException ex) {
            return ex.getMessage();
        }
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
     * Joins the given {@code ways} to multipolygon rings.
     * @param ways the ways to join.
     * @return a list of multipolygon rings.
     * @throws JoinedPolygonCreationException if the creation fails.
     */
    public static List<JoinedPolygon> joinWays(Collection<Way> ways) throws JoinedPolygonCreationException {
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
                Node nextNode = (curWayReverse) ? curWay.firstNode(): curWay.lastNode();

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
                for(Way way: adjacentWays) {
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

    /**
     * This method analyzes which ways are inner and which outer. Sets {@link #innerWays} and {@link #outerWays} to the result.
     * @param polygons polygons to analyze
     * @return error description if the ways cannot be split, {@code null} if all fine.
     */
    private String makeFromPolygons(List<JoinedPolygon> polygons) {
        List<PolygonLevel> list = findOuterWaysRecursive(0, polygons);

        if (list == null) {
            return tr("There is an intersection between ways.");
        }

        this.outerWays.clear();
        this.innerWays.clear();

        //take every other level
        for (PolygonLevel pol : list) {
            if (pol.level % 2 == 0) {
                this.outerWays.add(pol.outerWay);
            } else {
                this.innerWays.add(pol.outerWay);
            }
        }

        return null;
    }

    /**
     * Collects outer way and corresponding inner ways from all boundaries.
     * @param boundaryWays
     * @return the outermostWay, or {@code null} if intersection found.
     */
    private List<PolygonLevel> findOuterWaysRecursive(int level, Collection<JoinedPolygon> boundaryWays) {

        //TODO: bad performance for deep nesting...
        List<PolygonLevel> result = new ArrayList<>();

        for (JoinedPolygon outerWay : boundaryWays) {

            boolean outerGood = true;
            List<JoinedPolygon> innerCandidates = new ArrayList<>();

            for (JoinedPolygon innerWay : boundaryWays) {
                if (innerWay == outerWay) {
                    continue;
                }

                PolygonIntersection intersection = Geometry.polygonIntersection(outerWay.area, innerWay.area);

                if (intersection == PolygonIntersection.FIRST_INSIDE_SECOND) {
                    outerGood = false;  // outer is inside another polygon
                    break;
                } else if (intersection == PolygonIntersection.SECOND_INSIDE_FIRST) {
                    innerCandidates.add(innerWay);
                } else if (intersection == PolygonIntersection.CROSSING) {
                    //ways intersect
                    return null;
                }
            }

            if (!outerGood) {
                continue;
            }

            //add new outer polygon
            PolygonLevel pol = new PolygonLevel(outerWay, level);

            //process inner ways
            if (!innerCandidates.isEmpty()) {
                List<PolygonLevel> innerList = this.findOuterWaysRecursive(level + 1, innerCandidates);
                if (innerList == null) {
                    return null; //intersection found
                }

                result.addAll(innerList);

                for (PolygonLevel pl : innerList) {
                    if (pl.level == level + 1) {
                        pol.innerWays.add(pl.outerWay);
                    }
                }
            }

            result.add(pol);
        }

        return result;
    }
}
