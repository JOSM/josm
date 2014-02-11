// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import java.awt.Rectangle;
import java.awt.geom.Area;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.AddCommand;
import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.BBox;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.NodePositionComparator;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;

/**
 * Some tools for geometry related tasks.
 *
 * @author viesturs
 */
public final class Geometry {

    private Geometry() {
        // Hide default constructor for utils classes
    }

    public enum PolygonIntersection {FIRST_INSIDE_SECOND, SECOND_INSIDE_FIRST, OUTSIDE, CROSSING}

    /**
     * Will find all intersection and add nodes there for list of given ways.
     * Handles self-intersections too.
     * And makes commands to add the intersection points to ways.
     *
     * Prerequisite: no two nodes have the same coordinates.
     *
     * @param ways  a list of ways to test
     * @param test  if false, do not build list of Commands, just return nodes
     * @param cmds  list of commands, typically empty when handed to this method.
     *              Will be filled with commands that add intersection nodes to
     *              the ways.
     * @return list of new nodes
     */
    public static Set<Node> addIntersections(List<Way> ways, boolean test, List<Command> cmds) {

        int n = ways.size();
        @SuppressWarnings("unchecked")
        List<Node>[] newNodes = new ArrayList[n];
        BBox[] wayBounds = new BBox[n];
        boolean[] changedWays = new boolean[n];

        Set<Node> intersectionNodes = new LinkedHashSet<Node>();

        //copy node arrays for local usage.
        for (int pos = 0; pos < n; pos ++) {
            newNodes[pos] = new ArrayList<Node>(ways.get(pos).getNodes());
            wayBounds[pos] = getNodesBounds(newNodes[pos]);
            changedWays[pos] = false;
        }

        //iterate over all way pairs and introduce the intersections
        Comparator<Node> coordsComparator = new NodePositionComparator();
        for (int seg1Way = 0; seg1Way < n; seg1Way ++) {
            for (int seg2Way = seg1Way; seg2Way < n; seg2Way ++) {

                //do not waste time on bounds that do not intersect
                if (!wayBounds[seg1Way].intersects(wayBounds[seg2Way])) {
                    continue;
                }

                List<Node> way1Nodes = newNodes[seg1Way];
                List<Node> way2Nodes = newNodes[seg2Way];

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

                                Node newNode = new Node(Main.getProjection().eastNorth2latlon(intersection));
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
                        else if (test && !intersectionNodes.isEmpty())
                            return intersectionNodes;
                    }
                }
            }
        }


        for (int pos = 0; pos < ways.size(); pos ++) {
            if (!changedWays[pos]) {
                continue;
            }

            Way way = ways.get(pos);
            Way newWay = new Way(way);
            newWay.setNodes(newNodes[pos]);

            cmds.add(new ChangeCommand(way, newWay));
        }

        return intersectionNodes;
    }

    private static BBox getNodesBounds(List<Node> nodes) {

        BBox bounds = new BBox(nodes.get(0));
        for (Node n: nodes) {
            bounds.add(n.getCoor());
        }
        return bounds;
    }

    /**
     * Tests if given point is to the right side of path consisting of 3 points.
     *
     * (Imagine the path is continued beyond the endpoints, so you get two rays
     * starting from lineP2 and going through lineP1 and lineP3 respectively
     * which divide the plane into two parts. The test returns true, if testPoint
     * lies in the part that is to the right when traveling in the direction
     * lineP1, lineP2, lineP3.)
     *
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
        return angleIsClockwise(commonNode.getEastNorth(), firstNode.getEastNorth(), secondNode.getEastNorth());
    }

    /**
     * Finds the intersection of two line segments
     * @return EastNorth null if no intersection was found, the EastNorth coordinates of the intersection otherwise
     */
    public static EastNorth getSegmentSegmentIntersection(EastNorth p1, EastNorth p2, EastNorth p3, EastNorth p4) {

        CheckParameterUtil.ensureValidCoordinates(p1, "p1");
        CheckParameterUtil.ensureValidCoordinates(p2, "p2");
        CheckParameterUtil.ensureValidCoordinates(p3, "p3");
        CheckParameterUtil.ensureValidCoordinates(p4, "p4");

        double x1 = p1.getX();
        double y1 = p1.getY();
        double x2 = p2.getX();
        double y2 = p2.getY();
        double x3 = p3.getX();
        double y3 = p3.getY();
        double x4 = p4.getX();
        double y4 = p4.getY();

        //TODO: do this locally.
        //TODO: remove this check after careful testing
        if (!Line2D.linesIntersect(x1, y1, x2, y2, x3, y3, x4, y4)) return null;

        // solve line-line intersection in parametric form:
        // (x1,y1) + (x2-x1,y2-y1)* u  = (x3,y3) + (x4-x3,y4-y3)* v
        // (x2-x1,y2-y1)*u - (x4-x3,y4-y3)*v = (x3-x1,y3-y1)
        // if 0<= u,v <=1, intersection exists at ( x1+ (x2-x1)*u, y1 + (y2-y1)*u )

        double a1 = x2 - x1;
        double b1 = x3 - x4;
        double c1 = x3 - x1;

        double a2 = y2 - y1;
        double b2 = y3 - y4;
        double c2 = y3 - y1;

        // Solve the equations
        double det = a1*b2 - a2*b1;

        double uu = b2*c1 - b1*c2 ;
        double vv = a1*c2 - a2*c1;
        double mag = Math.abs(uu)+Math.abs(vv);

        if (Math.abs(det) > 1e-12 * mag) {
            double u = uu/det, v = vv/det;
            if (u>-1e-8 && u < 1+1e-8 && v>-1e-8 && v < 1+1e-8 ) {
                if (u<0) u=0;
                if (u>1) u=1.0;
                return new EastNorth(x1+a1*u, y1+a2*u);
            } else {
                return null;
            }
        } else {
            // parallel lines
            return null;
        }
    }

    /**
     * Finds the intersection of two lines of infinite length.
     * @return EastNorth null if no intersection was found, the coordinates of the intersection otherwise
     * @throws IllegalArgumentException if a parameter is null or without valid coordinates
     */
    public static EastNorth getLineLineIntersection(EastNorth p1, EastNorth p2, EastNorth p3, EastNorth p4) {

        CheckParameterUtil.ensureValidCoordinates(p1, "p1");
        CheckParameterUtil.ensureValidCoordinates(p2, "p2");
        CheckParameterUtil.ensureValidCoordinates(p3, "p3");
        CheckParameterUtil.ensureValidCoordinates(p4, "p4");

        if (!p1.isValid()) throw new IllegalArgumentException();

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

    public static boolean segmentsParallel(EastNorth p1, EastNorth p2, EastNorth p3, EastNorth p4) {

        CheckParameterUtil.ensureValidCoordinates(p1, "p1");
        CheckParameterUtil.ensureValidCoordinates(p2, "p2");
        CheckParameterUtil.ensureValidCoordinates(p3, "p3");
        CheckParameterUtil.ensureValidCoordinates(p4, "p4");

        // Convert line from (point, point) form to ax+by=c
        double a1 = p2.getY() - p1.getY();
        double b1 = p1.getX() - p2.getX();

        double a2 = p4.getY() - p3.getY();
        double b2 = p3.getX() - p4.getX();

        // Solve the equations
        double det = a1 * b2 - a2 * b1;
        // remove influence of of scaling factor
        det /= Math.sqrt(a1*a1 + b1*b1) * Math.sqrt(a2*a2 + b2*b2);
        return Math.abs(det) < 1e-3;
    }

    private static EastNorth closestPointTo(EastNorth p1, EastNorth p2, EastNorth point, boolean segmentOnly) {
        CheckParameterUtil.ensureParameterNotNull(p1, "p1");
        CheckParameterUtil.ensureParameterNotNull(p2, "p2");
        CheckParameterUtil.ensureParameterNotNull(point, "point");

        double ldx = p2.getX() - p1.getX();
        double ldy = p2.getY() - p1.getY();

        if (ldx == 0 && ldy == 0) //segment zero length
            return p1;

        double pdx = point.getX() - p1.getX();
        double pdy = point.getY() - p1.getY();

        double offset = (pdx * ldx + pdy * ldy) / (ldx * ldx + ldy * ldy);

        if (segmentOnly && offset <= 0)
            return p1;
        else if (segmentOnly && offset >= 1)
            return p2;
        else
            return new EastNorth(p1.getX() + ldx * offset, p1.getY() + ldy * offset);
    }

    /**
     * Calculates closest point to a line segment.
     * @param segmentP1 First point determining line segment
     * @param segmentP2 Second point determining line segment
     * @param point Point for which a closest point is searched on line segment [P1,P2]
     * @return segmentP1 if it is the closest point, segmentP2 if it is the closest point,
     * a new point if closest point is between segmentP1 and segmentP2.
     * @since 3650
     * @see #closestPointToLine
     */
    public static EastNorth closestPointToSegment(EastNorth segmentP1, EastNorth segmentP2, EastNorth point) {
        return closestPointTo(segmentP1, segmentP2, point, true);
    }

    /**
     * Calculates closest point to a line.
     * @param lineP1 First point determining line
     * @param lineP2 Second point determining line
     * @param point Point for which a closest point is searched on line (P1,P2)
     * @return The closest point found on line. It may be outside the segment [P1,P2].
     * @since 4134
     * @see #closestPointToSegment
     */
    public static EastNorth closestPointToLine(EastNorth lineP1, EastNorth lineP2, EastNorth point) {
        return closestPointTo(lineP1, lineP2, point, false);
    }

    /**
     * This method tests if secondNode is clockwise to first node.
     *
     * The line through the two points commonNode and firstNode divides the
     * plane into two parts. The test returns true, if secondNode lies in
     * the part that is to the right when traveling in the direction from
     * commonNode to firstNode.
     *
     * @param commonNode starting point for both vectors
     * @param firstNode first vector end node
     * @param secondNode second vector end node
     * @return true if first vector is clockwise before second vector.
     */
    public static boolean angleIsClockwise(EastNorth commonNode, EastNorth firstNode, EastNorth secondNode) {

        CheckParameterUtil.ensureValidCoordinates(commonNode, "commonNode");
        CheckParameterUtil.ensureValidCoordinates(firstNode, "firstNode");
        CheckParameterUtil.ensureValidCoordinates(secondNode, "secondNode");

        double dy1 = (firstNode.getY() - commonNode.getY());
        double dy2 = (secondNode.getY() - commonNode.getY());
        double dx1 = (firstNode.getX() - commonNode.getX());
        double dx2 = (secondNode.getX() - commonNode.getX());

        return dy1 * dx2 - dx1 * dy2 > 0;
    }

    /**
     * Returns the Area of a polygon, from its list of nodes.
     * @param polygon List of nodes forming polygon
     * @return Area for the given list of nodes
     * @since 6841
     */
    public static Area getArea(List<Node> polygon) {
        Path2D path = new Path2D.Double();

        boolean begin = true;
        for (Node n : polygon) {
            if (begin) {
                path.moveTo(n.getEastNorth().getX(), n.getEastNorth().getY());
                begin = false;
            } else {
                path.lineTo(n.getEastNorth().getX(), n.getEastNorth().getY());
            }
        }
        if (!begin) {
            path.closePath();
        }

        return new Area(path);
    }

    /**
     * Tests if two polygons intersect.
     * @param first List of nodes forming first polygon
     * @param second List of nodes forming second polygon
     * @return intersection kind
     */
    public static PolygonIntersection polygonIntersection(List<Node> first, List<Node> second) {
        Area a1 = getArea(first);
        Area a2 = getArea(second);
        return polygonIntersection(a1, a2);
    }

    /**
     * Tests if two polygons intersect.
     * @param a1 Area of first polygon
     * @param a2 Area of second polygon
     * @return intersection kind
     * @since 6841
     */
    public static PolygonIntersection polygonIntersection(Area a1, Area a2) {

        Area inter = new Area(a1);
        inter.intersect(a2);

        Rectangle bounds = inter.getBounds();

        if (inter.isEmpty() || bounds.getHeight()*bounds.getWidth() <= 1.0) {
            return PolygonIntersection.OUTSIDE;
        } else if (inter.equals(a1)) {
            return PolygonIntersection.FIRST_INSIDE_SECOND;
        } else if (inter.equals(a2)) {
            return PolygonIntersection.SECOND_INSIDE_FIRST;
        } else {
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

            //order points so p1.lat <= p2.lat
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

    /**
     * Returns area of a closed way in square meters.
     * (approximate(?), but should be OK for small areas)
     *
     * Relies on the current projection: Works correctly, when
     * one unit in projected coordinates corresponds to one meter.
     * This is true for most projections, but not for WGS84 and
     * Mercator (EPSG:3857).
     *
     * @param way Way to measure, should be closed (first node is the same as last node)
     * @return area of the closed way.
     */
    public static double closedWayArea(Way way) {

        //http://local.wasp.uwa.edu.au/~pbourke/geometry/polyarea/
        double area = 0;
        Node lastN = null;
        for (Node n : way.getNodes()) {
            if (lastN != null) {
                n.getEastNorth().getX();

                area += (calcX(n) * calcY(lastN)) - (calcY(n) * calcX(lastN));
            }
            lastN = n;
        }
        return Math.abs(area/2);
    }

    protected static double calcX(Node p1){
        double lat1, lon1, lat2, lon2;
        double dlon, dlat;

        lat1 = p1.getCoor().lat() * Math.PI / 180.0;
        lon1 = p1.getCoor().lon() * Math.PI / 180.0;
        lat2 = lat1;
        lon2 = 0;

        dlon = lon2 - lon1;
        dlat = lat2 - lat1;

        double a = (Math.pow(Math.sin(dlat/2), 2) + Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(dlon/2), 2));
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return 6367000 * c;
    }

    protected static double calcY(Node p1){
        double lat1, lon1, lat2, lon2;
        double dlon, dlat;

        lat1 = p1.getCoor().lat() * Math.PI / 180.0;
        lon1 = p1.getCoor().lon() * Math.PI / 180.0;
        lat2 = 0;
        lon2 = lon1;

        dlon = lon2 - lon1;
        dlat = lat2 - lat1;

        double a = (Math.pow(Math.sin(dlat/2), 2) + Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(dlon/2), 2));
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return 6367000 * c;
    }

    /**
     * Determines whether a way is oriented clockwise.
     *
     * Internals: Assuming a closed non-looping way, compute twice the area
     * of the polygon using the formula {@code 2 * area = sum (X[n] * Y[n+1] - X[n+1] * Y[n])}.
     * If the area is negative the way is ordered in a clockwise direction.
     *
     * See http://paulbourke.net/geometry/polyarea/
     *
     * @param w the way to be checked.
     * @return true if and only if way is oriented clockwise.
     * @throws IllegalArgumentException if way is not closed (see {@link Way#isClosed}).
     */
    public static boolean isClockwise(Way w) {
        if (!w.isClosed()) {
            throw new IllegalArgumentException("Way must be closed to check orientation.");
        }

        double area2 = 0.;
        int nodesCount = w.getNodesCount();

        for (int node = 1; node <= /*sic! consider last-first as well*/ nodesCount; node++) {
            LatLon coorPrev = w.getNode(node - 1).getCoor();
            LatLon coorCurr = w.getNode(node % nodesCount).getCoor();
            area2 += coorPrev.lon() * coorCurr.lat();
            area2 -= coorCurr.lon() * coorPrev.lat();
        }
        return area2 < 0;
    }

    /**
     * Returns angle of a segment defined with 2 point coordinates.
     *
     * @param p1
     * @param p2
     * @return Angle in radians (-pi, pi]
     */
    public static double getSegmentAngle(EastNorth p1, EastNorth p2) {

        CheckParameterUtil.ensureValidCoordinates(p1, "p1");
        CheckParameterUtil.ensureValidCoordinates(p2, "p2");

        return Math.atan2(p2.north() - p1.north(), p2.east() - p1.east());
    }

    /**
     * Returns angle of a corner defined with 3 point coordinates.
     *
     * @param p1
     * @param p2 Common endpoint
     * @param p3
     * @return Angle in radians (-pi, pi]
     */
    public static double getCornerAngle(EastNorth p1, EastNorth p2, EastNorth p3) {

        CheckParameterUtil.ensureValidCoordinates(p1, "p1");
        CheckParameterUtil.ensureValidCoordinates(p2, "p2");
        CheckParameterUtil.ensureValidCoordinates(p3, "p3");

        Double result = getSegmentAngle(p2, p1) - getSegmentAngle(p2, p3);
        if (result <= -Math.PI) {
            result += 2 * Math.PI;
        }

        if (result > Math.PI) {
            result -= 2 * Math.PI;
        }

        return result;
    }

    /**
     * Compute the centroid/barycenter of nodes
     * @param nodes Nodes for which the centroid is wanted
     * @return the centroid of nodes
     */
    public static EastNorth getCentroid(List<Node> nodes) {

        BigDecimal area = BigDecimal.ZERO;
        BigDecimal north = BigDecimal.ZERO;
        BigDecimal east = BigDecimal.ZERO;

        // See http://en.wikipedia.org/w/index.php?title=Centroid&oldid=294224857#Centroid_of_polygon for the equation used here
        for (int i = 0; i < nodes.size(); i++) {
            EastNorth n0 = nodes.get(i).getEastNorth();
            EastNorth n1 = nodes.get((i+1) % nodes.size()).getEastNorth();

            if (n0.isValid() && n1.isValid()) {
                BigDecimal x0 = new BigDecimal(n0.east());
                BigDecimal y0 = new BigDecimal(n0.north());
                BigDecimal x1 = new BigDecimal(n1.east());
                BigDecimal y1 = new BigDecimal(n1.north());

                BigDecimal k = x0.multiply(y1, MathContext.DECIMAL128).subtract(y0.multiply(x1, MathContext.DECIMAL128));

                area = area.add(k, MathContext.DECIMAL128);
                east = east.add(k.multiply(x0.add(x1, MathContext.DECIMAL128), MathContext.DECIMAL128));
                north = north.add(k.multiply(y0.add(y1, MathContext.DECIMAL128), MathContext.DECIMAL128));
            }
        }

        BigDecimal d = new BigDecimal(3, MathContext.DECIMAL128); // 1/2 * 6 = 3
        area  = area.multiply(d, MathContext.DECIMAL128);
        if (area.compareTo(BigDecimal.ZERO) != 0) {
            north = north.divide(area, MathContext.DECIMAL128);
            east = east.divide(area, MathContext.DECIMAL128);
        }

        return new EastNorth(east.doubleValue(), north.doubleValue());
    }

    /**
     * Returns the coordinate of intersection of segment sp1-sp2 and an altitude
     * to it starting at point ap. If the line defined with sp1-sp2 intersects
     * its altitude out of sp1-sp2, null is returned.
     *
     * @param sp1
     * @param sp2
     * @param ap
     * @return Intersection coordinate or null
     */
    public static EastNorth getSegmentAltituteIntersection(EastNorth sp1, EastNorth sp2, EastNorth ap) {

        CheckParameterUtil.ensureValidCoordinates(sp1, "sp1");
        CheckParameterUtil.ensureValidCoordinates(sp2, "sp2");
        CheckParameterUtil.ensureValidCoordinates(ap, "ap");

        Double segmentLenght = sp1.distance(sp2);
        Double altitudeAngle = getSegmentAngle(sp1, sp2) + Math.PI / 2;

        // Taking a random point on the altitude line (angle is known).
        EastNorth ap2 = new EastNorth(ap.east() + 1000
                * Math.cos(altitudeAngle), ap.north() + 1000
                * Math.sin(altitudeAngle));

        // Finding the intersection of two lines
        EastNorth resultCandidate = Geometry.getLineLineIntersection(sp1, sp2,
                ap, ap2);

        // Filtering result
        if (resultCandidate != null
                && resultCandidate.distance(sp1) * .999 < segmentLenght
                && resultCandidate.distance(sp2) * .999 < segmentLenght) {
            return resultCandidate;
        } else {
            return null;
        }
    }

    public static class MultiPolygonMembers {
        public final Set<Way> outers = new HashSet<Way>();
        public final Set<Way> inners = new HashSet<Way>();

        public MultiPolygonMembers(Relation multiPolygon) {
            for (RelationMember m : multiPolygon.getMembers()) {
                if (m.getType().equals(OsmPrimitiveType.WAY)) {
                    if (m.getRole().equals("outer")) {
                        outers.add(m.getWay());
                    } else if (m.getRole().equals("inner")) {
                        inners.add(m.getWay());
                    }
                }
            }
        }
    }

    /**
     * Tests if the {@code node} is inside the multipolygon {@code multiPolygon}. The nullable argument
     * {@code isOuterWayAMatch} allows to decide if the immediate {@code outer} way of the multipolygon is a match.
     */
    public static boolean isNodeInsideMultiPolygon(Node node, Relation multiPolygon, Predicate<Way> isOuterWayAMatch) {
        return isPolygonInsideMultiPolygon(Collections.singletonList(node), multiPolygon, isOuterWayAMatch);
    }

    /**
     * Tests if the polygon formed by {@code nodes} is inside the multipolygon {@code multiPolygon}. The nullable argument
     * {@code isOuterWayAMatch} allows to decide if the immediate {@code outer} way of the multipolygon is a match.
     * <p>
     * If {@code nodes} contains exactly one element, then it is checked whether that one node is inside the multipolygon.
     */
    public static boolean isPolygonInsideMultiPolygon(List<Node> nodes, Relation multiPolygon, Predicate<Way> isOuterWayAMatch) {
        // Extract outer/inner members from multipolygon
        MultiPolygonMembers mpm = new MultiPolygonMembers(multiPolygon);
        // Test if object is inside an outer member
        for (Way out : mpm.outers) {
            if (nodes.size() == 1
                    ? nodeInsidePolygon(nodes.get(0), out.getNodes())
                    : EnumSet.of(PolygonIntersection.FIRST_INSIDE_SECOND, PolygonIntersection.CROSSING).contains(polygonIntersection(nodes, out.getNodes()))) {
                boolean insideInner = false;
                // If inside an outer, check it is not inside an inner
                for (Way in : mpm.inners) {
                    if (polygonIntersection(in.getNodes(), out.getNodes()) == PolygonIntersection.FIRST_INSIDE_SECOND
                            && (nodes.size() == 1
                            ? nodeInsidePolygon(nodes.get(0), in.getNodes())
                            : polygonIntersection(nodes, in.getNodes()) == PolygonIntersection.FIRST_INSIDE_SECOND)) {
                        insideInner = true;
                        break;
                    }
                }
                // Inside outer but not inside inner -> the polygon appears to be inside a the multipolygon
                if (!insideInner) {
                    // Final check using predicate
                    if (isOuterWayAMatch == null || isOuterWayAMatch.evaluate(out)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
