// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.AddCommand;
import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.BBox;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.NodePositionComparator;
import org.openstreetmap.josm.data.osm.Way;

/**
 * Some tools for geometry related tasks.
 *
 * @author viesturs
 */
public class Geometry {
    public enum PolygonIntersection {FIRST_INSIDE_SECOND, SECOND_INSIDE_FIRST, OUTSIDE, CROSSING}

    /**
     * Will find all intersection and add nodes there for list of given ways. Handles self-intersections too.
     * And make commands to add the intersection points to ways.
     * @param List<Way> - a list of ways to test
     * @return ArrayList<Node> List of new nodes
     * Prerequisite: no two nodes have the same coordinates.
     */
    public static Set<Node> addIntersections(List<Way> ways, boolean test, List<Command> cmds) {

        //stupid java, cannot instantiate array of generic classes..
        @SuppressWarnings("unchecked")
        ArrayList<Node>[] newNodes = new ArrayList[ways.size()];
        BBox[] wayBounds = new BBox[ways.size()];
        boolean[] changedWays = new boolean[ways.size()];

        Set<Node> intersectionNodes = new LinkedHashSet<Node>();

        //copy node arrays for local usage.
        for (int pos = 0; pos < ways.size(); pos ++) {
            newNodes[pos] = new ArrayList<Node>(ways.get(pos).getNodes());
            wayBounds[pos] = getNodesBounds(newNodes[pos]);
            changedWays[pos] = false;
        }

        //iterate over all way pairs and introduce the intersections
        Comparator<Node> coordsComparator = new NodePositionComparator();

        WayLoop: for (int seg1Way = 0; seg1Way < ways.size(); seg1Way ++) {
            for (int seg2Way = seg1Way; seg2Way < ways.size(); seg2Way ++) {

                //do not waste time on bounds that do not intersect
                if (!wayBounds[seg1Way].intersects(wayBounds[seg2Way])) {
                    continue;
                }

                ArrayList<Node> way1Nodes = newNodes[seg1Way];
                ArrayList<Node> way2Nodes = newNodes[seg2Way];

                //iterate over primary segmemt
                for (int seg1Pos = 0; seg1Pos + 1 < way1Nodes.size(); seg1Pos ++) {

                    //iterate over secondary segment
                    int seg2Start = seg1Way != seg2Way ? 0: seg1Pos + 2;//skip the adjacent segment

                    for (int seg2Pos = seg2Start; seg2Pos + 1< way2Nodes.size(); seg2Pos ++) {

                        //need to get them again every time, because other segments may be changed
                        Node seg1Node1 = way1Nodes.get(seg1Pos);
                        Node seg1Node2 = way1Nodes.get(seg1Pos + 1);
                        Node seg2Node1 = way2Nodes.get(seg2Pos);
                        Node seg2Node2 = way2Nodes.get(seg2Pos + 1);

                        int commonCount = 0;
                        //test if we have common nodes to add.
                        if (seg1Node1 == seg2Node1 || seg1Node1 == seg2Node2) {
                            commonCount ++;

                            if (seg1Way == seg2Way &&
                                    seg1Pos == 0 &&
                                    seg2Pos == way2Nodes.size() -2) {
                                //do not add - this is first and last segment of the same way.
                            } else {
                                intersectionNodes.add(seg1Node1);
                            }
                        }

                        if (seg1Node2 == seg2Node1 || seg1Node2 == seg2Node2) {
                            commonCount ++;

                            intersectionNodes.add(seg1Node2);
                        }

                        //no common nodes - find intersection
                        if (commonCount == 0) {
                            EastNorth intersection = getSegmentSegmentIntersection(
                                    seg1Node1.getEastNorth(), seg1Node2.getEastNorth(),
                                    seg2Node1.getEastNorth(), seg2Node2.getEastNorth());

                            if (intersection != null) {
                                if (test) {
                                    intersectionNodes.add(seg2Node1);
                                    return intersectionNodes;
                                }

                                Node newNode = new Node(Main.proj.eastNorth2latlon(intersection));
                                Node intNode = newNode;
                                boolean insertInSeg1 = false;
                                boolean insertInSeg2 = false;

                                //find if the intersection point is at end point of one of the segments, if so use that point

                                //segment 1
                                if (coordsComparator.compare(newNode, seg1Node1) == 0) {
                                    intNode = seg1Node1;
                                } else if (coordsComparator.compare(newNode, seg1Node2) == 0) {
                                    intNode = seg1Node2;
                                } else {
                                    insertInSeg1 = true;
                                }

                                //segment 2
                                if (coordsComparator.compare(newNode, seg2Node1) == 0) {
                                    intNode = seg2Node1;
                                } else if (coordsComparator.compare(newNode, seg2Node2) == 0) {
                                    intNode = seg2Node2;
                                } else {
                                    insertInSeg2 = true;
                                }

                                if (insertInSeg1) {
                                    way1Nodes.add(seg1Pos +1, intNode);
                                    changedWays[seg1Way] = true;

                                    //fix seg2 position, as indexes have changed, seg2Pos is always bigger than seg1Pos on the same segment.
                                    if (seg2Way == seg1Way) {
                                        seg2Pos ++;
                                    }
                                }

                                if (insertInSeg2) {
                                    way2Nodes.add(seg2Pos +1, intNode);
                                    changedWays[seg2Way] = true;

                                    //Do not need to compare again to already split segment
                                    seg2Pos ++;
                                }

                                intersectionNodes.add(intNode);

                                if (intNode == newNode) {
                                    cmds.add(new AddCommand(intNode));
                                }
                            }
                        }
                        else if (test && intersectionNodes.size() > 0)
                            return intersectionNodes;
                    }
                }
            }
        }


        for (int pos = 0; pos < ways.size(); pos ++) {
            if (changedWays[pos] == false) {
                continue;
            }

            Way way = ways.get(pos);
            Way newWay = new Way(way);
            newWay.setNodes(newNodes[pos]);

            cmds.add(new ChangeCommand(way, newWay));
        }

        return intersectionNodes;
    }

    private static BBox getNodesBounds(ArrayList<Node> nodes) {

        BBox bounds = new BBox(nodes.get(0));
        for(Node n: nodes) {
            bounds.add(n.getCoor());
        }
        return bounds;
    }

    /**
     * Tests if given point is to the right side of path consisting of 3 points.
     * @param lineP1 first point in path
     * @param lineP2 second point in path
     * @param lineP3 third point in path
     * @param testPoint
     * @return true if to the right side, false otherwise
     */
    public static boolean isToTheRightSideOfLine(Node lineP1, Node lineP2, Node lineP3, Node testPoint) {
        boolean pathBendToRight = angleIsClockwise(lineP1, lineP2, lineP3);
        boolean rightOfSeg1 = angleIsClockwise(lineP1, lineP2, testPoint);
        boolean rightOfSeg2 = angleIsClockwise(lineP2, lineP3, testPoint);

        if (pathBendToRight)
            return rightOfSeg1 && rightOfSeg2;
        else
            return !(!rightOfSeg1 && !rightOfSeg2);
    }

    /**
     * This method tests if secondNode is clockwise to first node.
     * @param commonNode starting point for both vectors
     * @param firstNode first vector end node
     * @param secondNode second vector end node
     * @return true if first vector is clockwise before second vector.
     */
    public static boolean angleIsClockwise(Node commonNode, Node firstNode, Node secondNode) {
        double dy1 = (firstNode.getEastNorth().getY() - commonNode.getEastNorth().getY());
        double dy2 = (secondNode.getEastNorth().getY() - commonNode.getEastNorth().getY());
        double dx1 = (firstNode.getEastNorth().getX() - commonNode.getEastNorth().getX());
        double dx2 = (secondNode.getEastNorth().getX() - commonNode.getEastNorth().getX());

        return dy1 * dx2 - dx1 * dy2 > 0;
    }

    /**
     * Finds the intersection of two line segments
     * @return EastNorth null if no intersection was found, the EastNorth coordinates of the intersection otherwise
     */
    public static EastNorth getSegmentSegmentIntersection(
            EastNorth p1, EastNorth p2,
            EastNorth p3, EastNorth p4) {
        double x1 = p1.getX();
        double y1 = p1.getY();
        double x2 = p2.getX();
        double y2 = p2.getY();
        double x3 = p3.getX();
        double y3 = p3.getY();
        double x4 = p4.getX();
        double y4 = p4.getY();

        //TODO: do this locally.
        if (!Line2D.linesIntersect(x1, y1, x2, y2, x3, y3, x4, y4)) return null;

        // Convert line from (point, point) form to ax+by=c
        double a1 = y2 - y1;
        double b1 = x1 - x2;
        double c1 = x2*y1 - x1*y2;

        double a2 = y4 - y3;
        double b2 = x3 - x4;
        double c2 = x4*y3 - x3*y4;

        // Solve the equations
        double det = a1*b2 - a2*b1;
        if (det == 0) return null; // Lines are parallel

        double x = (b1*c2 - b2*c1)/det;
        double y = (a2*c1 -a1*c2)/det;

        return new EastNorth(x, y);
    }

    /**
     * Finds the intersection of two lines of infinite length.
     * @return EastNorth null if no intersection was found, the coordinates of the intersection otherwise
     */
    public static EastNorth getLineLineIntersection(EastNorth p1, EastNorth p2, EastNorth p3, EastNorth p4) {

        // Convert line from (point, point) form to ax+by=c
        double a1 = p2.getY() - p1.getY();
        double b1 = p1.getX() - p2.getX();
        double c1 = p2.getX() * p1.getY() - p1.getX() * p2.getY();

        double a2 = p4.getY() - p3.getY();
        double b2 = p3.getX() - p4.getX();
        double c2 = p4.getX() * p3.getY() - p3.getX() * p4.getY();

        // Solve the equations
        double det = a1 * b2 - a2 * b1;
        if (det == 0)
            return null; // Lines are parallel

        return new EastNorth((b1 * c2 - b2 * c1) / det, (a2 * c1 - a1 * c2) / det);
    }

    public static boolean segmentsParralel(EastNorth p1, EastNorth p2, EastNorth p3, EastNorth p4) {
        // Convert line from (point, point) form to ax+by=c
        double a1 = p2.getY() - p1.getY();
        double b1 = p1.getX() - p2.getX();

        double a2 = p4.getY() - p3.getY();
        double b2 = p3.getX() - p4.getX();

        // Solve the equations
        double det = a1 * b2 - a2 * b1;
        return Math.abs(det) < 1e-13;
    }

    /**
     * Calculates closest point to a line segment.
     * @param segmentP1
     * @param segmentP2
     * @param point
     * @return segmentP1 if it is the closest point, segmentP2 if it is the closest point,
     * a new point if closest point is between segmentP1 and segmentP2.
     */
    public static EastNorth closestPointToSegment(EastNorth segmentP1, EastNorth segmentP2, EastNorth point) {

        double ldx = segmentP2.getX() - segmentP1.getX();
        double ldy = segmentP2.getY() - segmentP1.getY();

        if (ldx == 0 && ldy == 0) //segment zero length
            return segmentP1;

        double pdx = point.getX() - segmentP1.getX();
        double pdy = point.getY() - segmentP1.getY();

        double offset = (pdx * ldx + pdy * ldy) / (ldx * ldx + ldy * ldy);

        if (offset <= 0)
            return segmentP1;
        else if (offset >= 1)
            return segmentP2;
        else
            return new EastNorth(segmentP1.getX() + ldx * offset, segmentP1.getY() + ldy * offset);
    }

    /**
     * This method tests if secondNode is clockwise to first node.
     * @param commonNode starting point for both vectors
     * @param firstNode first vector end node
     * @param secondNode second vector end node
     * @return true if first vector is clockwise before second vector.
     */
    public static boolean angleIsClockwise(EastNorth commonNode, EastNorth firstNode, EastNorth secondNode) {
        double dy1 = (firstNode.getY() - commonNode.getY());
        double dy2 = (secondNode.getY() - commonNode.getY());
        double dx1 = (firstNode.getX() - commonNode.getX());
        double dx2 = (secondNode.getX() - commonNode.getX());

        return dy1 * dx2 - dx1 * dy2 > 0;
    }

    /**
     * Tests if two polygons intersect.
     * @param first
     * @param second
     * @return intersection kind
     * TODO: test segments, not only points
     * TODO: is O(N*M), should use sweep for better performance.
     */
    public static PolygonIntersection polygonIntersection(List<Node> first, List<Node> second) {
        Set<Node> firstSet = new HashSet<Node>(first);
        Set<Node> secondSet = new HashSet<Node>(second);

        int nodesInsideSecond = 0;
        int nodesOutsideSecond = 0;
        int nodesInsideFirst = 0;
        int nodesOutsideFirst = 0;

        for (Node insideNode : first) {
            if (secondSet.contains(insideNode)) {
                continue;
                //ignore touching nodes.
            }

            if (nodeInsidePolygon(insideNode, second)) {
                nodesInsideSecond ++;
            }
            else {
                nodesOutsideSecond ++;
            }
        }

        for (Node insideNode : second) {
            if (firstSet.contains(insideNode)) {
                continue;
                //ignore touching nodes.
            }

            if (nodeInsidePolygon(insideNode, first)) {
                nodesInsideFirst ++;
            }
            else {
                nodesOutsideFirst ++;
            }
        }

        if (nodesInsideFirst == 0) {
            if (nodesInsideSecond == 0){
                if (nodesOutsideFirst + nodesInsideSecond > 0)
                    return PolygonIntersection.OUTSIDE;
                else
                    //all nodes common
                    return PolygonIntersection.CROSSING;
            } else
                return PolygonIntersection.FIRST_INSIDE_SECOND;
        }
        else
        {
            if (nodesInsideSecond == 0)
                return PolygonIntersection.SECOND_INSIDE_FIRST;
            else
                return PolygonIntersection.CROSSING;
        }
    }

    /**
     * Tests if point is inside a polygon. The polygon can be self-intersecting. In such case the contains function works in xor-like manner.
     * @param polygonNodes list of nodes from polygon path.
     * @param point the point to test
     * @return true if the point is inside polygon.
     */
    public static boolean nodeInsidePolygon(Node point, List<Node> polygonNodes) {
        if (polygonNodes.size() < 2)
            return false;

        boolean inside = false;
        Node p1, p2;

        //iterate each side of the polygon, start with the last segment
        Node oldPoint = polygonNodes.get(polygonNodes.size() - 1);

        for (Node newPoint : polygonNodes) {
            //skip duplicate points
            if (newPoint.equals(oldPoint)) {
                continue;
            }

            //order points so p1.lat <= p2.lat;
            if (newPoint.getEastNorth().getY() > oldPoint.getEastNorth().getY()) {
                p1 = oldPoint;
                p2 = newPoint;
            } else {
                p1 = newPoint;
                p2 = oldPoint;
            }

            //test if the line is crossed and if so invert the inside flag.
            if ((newPoint.getEastNorth().getY() < point.getEastNorth().getY()) == (point.getEastNorth().getY() <= oldPoint.getEastNorth().getY())
                    && (point.getEastNorth().getX() - p1.getEastNorth().getX()) * (p2.getEastNorth().getY() - p1.getEastNorth().getY())
                    < (p2.getEastNorth().getX() - p1.getEastNorth().getX()) * (point.getEastNorth().getY() - p1.getEastNorth().getY()))
            {
                inside = !inside;
            }

            oldPoint = newPoint;
        }

        return inside;
    }
}
