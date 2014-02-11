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

public class MultipolygonCreate {

    /**
     * Represents one polygon that consists of multiple ways.
     * @author Viesturs
     *
     */
    public static class JoinedPolygon {
        public final List<Way> ways;
        public final List<Boolean> reversed;
        public final List<Node> nodes;
        public final Area area;

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
        private List<Node> getNodes() {
            List<Node> nodes = new ArrayList<Node>();

            for(int waypos = 0; waypos < this.ways.size(); waypos ++) {
                Way way = this.ways.get(waypos);
                boolean reversed = this.reversed.get(waypos).booleanValue();

                if (!reversed){
                    for (int pos = 0; pos < way.getNodesCount() - 1; pos++) {
                        nodes.add(way.getNode(pos));
                    }
                }
                else {
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
     * @author viesturs
     */
    static class PolygonLevel {
        public final int level; //nesting level , even for outer, odd for inner polygons.
        public final JoinedPolygon outerWay;

        public List<JoinedPolygon> innerWays;

        public PolygonLevel(JoinedPolygon _pol, int _level) {
            this.outerWay = _pol;
            this.level = _level;
            this.innerWays = new ArrayList<JoinedPolygon>();
        }
    }

    public List<JoinedPolygon> outerWays;
    public List<JoinedPolygon> innerWays;

    public MultipolygonCreate(List<JoinedPolygon> outerWays, List<JoinedPolygon> innerWays){
        this.outerWays = outerWays;
        this.innerWays = innerWays;
    }

    public MultipolygonCreate(){
        this.outerWays = new ArrayList<JoinedPolygon>(0);
        this.innerWays = new ArrayList<JoinedPolygon>(0);
    }

    /**
     * Splits ways into inner and outer JoinedWays. Sets {@link #innerWays} and {@link #outerWays} to the result.
     * TODO: Currently cannot process touching polygons. See code in JoinAreasAction.
     * @param ways ways to analyze
     * @return error description if the ways cannot be split, {@code null} if all fine.
     */
    public String makeFromWays(Collection<Way> ways){
        List<JoinedPolygon> joinedWays = new ArrayList<JoinedPolygon>();

        //collect ways connecting to each node.
        MultiMap<Node, Way> nodesWithConnectedWays = new MultiMap<Node, Way>();
        Set<Way> usedWays = new HashSet<Way>();

        for(Way w: ways) {
            if (w.getNodesCount() < 2) {
                return tr("Cannot add a way with only {0} nodes.", w.getNodesCount());
            }

            if (w.isClosed()) {
                //closed way, add as is.
                JoinedPolygon jw = new JoinedPolygon(w);
                joinedWays.add(jw);
                usedWays.add(w);
            }
            else {
                nodesWithConnectedWays.put(w.lastNode(), w);
                nodesWithConnectedWays.put(w.firstNode(), w);
            }
        }

        //process unclosed ways
        for (Way startWay: ways) {
            if (usedWays.contains(startWay)){
                continue;
            }

            Node startNode = startWay.firstNode();
            List<Way> collectedWays = new ArrayList<Way>();
            List<Boolean> collectedWaysReverse = new ArrayList<Boolean>();
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
                    return tr("Each node must connect exactly 2 ways");
                }

                Way nextWay = null;
                for(Way way: adjacentWays){
                    if (way != curWay){
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

        //analyze witch way is inside witch outside.
        return makeFromPolygons(joinedWays);
    }

    /**
     * This method analyzes which ways are inner and which outer. Sets {@link #innerWays} and {@link #outerWays} to the result.
     * @param polygons polygons to analyze
     * @return error description if the ways cannot be split, {@code null} if all fine.
     */
    private String makeFromPolygons(List<JoinedPolygon> polygons) {
        List<PolygonLevel> list = findOuterWaysRecursive(0, polygons);

        if (list == null){
            return tr("There is an intersection between ways.");
        }

        this.outerWays = new ArrayList<JoinedPolygon>(0);
        this.innerWays = new ArrayList<JoinedPolygon>(0);

        //take every other level
        for (PolygonLevel pol : list) {
            if (pol.level % 2 == 0) {
                this.outerWays.add(pol.outerWay);
            }
            else {
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
        List<PolygonLevel> result = new ArrayList<PolygonLevel>();

        for (JoinedPolygon outerWay : boundaryWays) {

            boolean outerGood = true;
            List<JoinedPolygon> innerCandidates = new ArrayList<JoinedPolygon>();

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
                }
                else if (intersection == PolygonIntersection.CROSSING)
                {
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
