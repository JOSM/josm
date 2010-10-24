// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.AddCommand;
import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.NodePositionComparator;
import org.openstreetmap.josm.data.osm.Way;

/**
 * Some tools for geometry related tasks.
 *
 * @author viesturs
 */
public class Geometry {
    /**
     * Will find all intersection and add nodes there for list of given ways. Handles self-intersections too.
     * And make commands to add the intersection points to ways.
     * @param List<Way> - a list of ways to test
     * @return ArrayList<Node> List of new nodes
     * Prerequisite: no two nodes have the same coordinates.
     */
    public static ArrayList<Node> addIntersections(List<Way> ways, boolean test, List<Command> cmds) {
        //TODO: this is a bit slow - O( (number of nodes)^2 + numberOfIntersections * numberOfNodes )

        //stupid java, cannot instantiate array of generic classes..
        @SuppressWarnings("unchecked")
        ArrayList<Node>[] newNodes = new ArrayList[ways.size()];
        boolean[] changedWays = new boolean[ways.size()];

        Set<Node> intersectionNodes = new LinkedHashSet<Node>();

        for (int pos = 0; pos < ways.size(); pos ++) {
            newNodes[pos] = new ArrayList<Node>(ways.get(pos).getNodes());
            changedWays[pos] = false;
        }

        //iterate over all segment pairs and introduce the intersections

        Comparator<Node> coordsComparator = new NodePositionComparator();

        int seg1Way = 0;
        int seg1Pos = -1;

        while (true) {
            //advance to next segment
            seg1Pos++;
            if (seg1Pos > newNodes[seg1Way].size() - 2) {
                seg1Way++;
                seg1Pos = 0;

                if (seg1Way == ways.size()) { //finished
                    break;
                }
            }


            //iterate over secondary segment

            int seg2Way = seg1Way;
            int seg2Pos = seg1Pos + 1;//skip the adjacent segment

            while (true) {

                //advance to next segment
                seg2Pos++;
                if (seg2Pos > newNodes[seg2Way].size() - 2) {
                    seg2Way++;
                    seg2Pos = 0;

                    if (seg2Way == ways.size()) { //finished
                        break;
                    }
                }

                //need to get them again every time, because other segments may be changed
                Node seg1Node1 = newNodes[seg1Way].get(seg1Pos);
                Node seg1Node2 = newNodes[seg1Way].get(seg1Pos + 1);
                Node seg2Node1 = newNodes[seg2Way].get(seg2Pos);
                Node seg2Node2 = newNodes[seg2Way].get(seg2Pos + 1);

                int commonCount = 0;
                //test if we have common nodes to add.
                if (seg1Node1 == seg2Node1 || seg1Node1 == seg2Node2) {
                    commonCount ++;

                    if (seg1Way == seg2Way &&
                            seg1Pos == 0 &&
                            seg2Pos == newNodes[seg2Way].size() -2) {
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
                    LatLon intersection = getLineLineIntersection(
                            seg1Node1.getEastNorth().east(), seg1Node1.getEastNorth().north(),
                            seg1Node2.getEastNorth().east(), seg1Node2.getEastNorth().north(),
                            seg2Node1.getEastNorth().east(), seg2Node1.getEastNorth().north(),
                            seg2Node2.getEastNorth().east(), seg2Node2.getEastNorth().north());

                    if (intersection != null) {
                        if (test) {
                            intersectionNodes.add(seg2Node1);
                            return new ArrayList<Node>(intersectionNodes);
                        }

                        Node newNode = new Node(intersection);
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
                            newNodes[seg1Way].add(seg1Pos +1, intNode);
                            changedWays[seg1Way] = true;

                            //fix seg2 position, as indexes have changed, seg2Pos is always bigger than seg1Pos on the same segment.
                            if (seg2Way == seg1Way) {
                                seg2Pos ++;
                            }
                        }

                        if (insertInSeg2) {
                            newNodes[seg2Way].add(seg2Pos +1, intNode);
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
                    return new ArrayList<Node>(intersectionNodes);
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

        return new ArrayList<Node>(intersectionNodes);
    }

    /**
     * Finds the intersection of two lines
     * @return LatLon null if no intersection was found, the LatLon coordinates of the intersection otherwise
     */
    static private LatLon getLineLineIntersection(
            double x1, double y1, double x2, double y2,
            double x3, double y3, double x4, double y4) {

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

        return Main.proj.eastNorth2latlon(new EastNorth(
                (b1*c2 - b2*c1)/det,
                (a2*c1 -a1*c2)/det
        ));
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
     * Tests if point is inside a polygon. The polygon can be self-intersecting. In such case the contains function works in xor-like manner.
     * @param polygonNodes list of nodes from polygon path.
     * @param point the point to test
     * @return true if the point is inside polygon.
     */
    public static boolean nodeInsidePolygon(Node point, List<Node> polygonNodes) {
        if (polygonNodes.size() < 3)
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
