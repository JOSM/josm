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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.AddCommand;
import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.BBox;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.MultipolygonBuilder;
import org.openstreetmap.josm.data.osm.MultipolygonBuilder.JoinedPolygon;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.NodePositionComparator;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.paint.relations.Multipolygon;
import org.openstreetmap.josm.data.osm.visitor.paint.relations.MultipolygonCache;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.data.projection.Projections;

/**
 * Some tools for geometry related tasks.
 *
 * @author viesturs
 */
public final class Geometry {

    private Geometry() {
        // Hide default constructor for utils classes
    }

    /**
     * The result types for a {@link Geometry#polygonIntersection(Area, Area)} test
     */
    public enum PolygonIntersection {
        /**
         * The first polygon is inside the second one
         */
        FIRST_INSIDE_SECOND,
        /**
         * The second one is inside the first
         */
        SECOND_INSIDE_FIRST,
        /**
         * The polygons do not overlap
         */
        OUTSIDE,
        /**
         * The polygon borders cross each other
         */
        CROSSING
    }

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

        Set<Node> intersectionNodes = new LinkedHashSet<>();

        //copy node arrays for local usage.
        for (int pos = 0; pos < n; pos++) {
            newNodes[pos] = new ArrayList<>(ways.get(pos).getNodes());
            wayBounds[pos] = getNodesBounds(newNodes[pos]);
            changedWays[pos] = false;
        }

        DataSet dataset = ways.get(0).getDataSet();

        //iterate over all way pairs and introduce the intersections
        Comparator<Node> coordsComparator = new NodePositionComparator();
        for (int seg1Way = 0; seg1Way < n; seg1Way++) {
            for (int seg2Way = seg1Way; seg2Way < n; seg2Way++) {

                //do not waste time on bounds that do not intersect
                if (!wayBounds[seg1Way].intersects(wayBounds[seg2Way])) {
                    continue;
                }

                List<Node> way1Nodes = newNodes[seg1Way];
                List<Node> way2Nodes = newNodes[seg2Way];

                //iterate over primary segmemt
                for (int seg1Pos = 0; seg1Pos + 1 < way1Nodes.size(); seg1Pos++) {

                    //iterate over secondary segment
                    int seg2Start = seg1Way != seg2Way ? 0 : seg1Pos + 2; //skip the adjacent segment

                    for (int seg2Pos = seg2Start; seg2Pos + 1 < way2Nodes.size(); seg2Pos++) {

                        //need to get them again every time, because other segments may be changed
                        Node seg1Node1 = way1Nodes.get(seg1Pos);
                        Node seg1Node2 = way1Nodes.get(seg1Pos + 1);
                        Node seg2Node1 = way2Nodes.get(seg2Pos);
                        Node seg2Node2 = way2Nodes.get(seg2Pos + 1);

                        int commonCount = 0;
                        //test if we have common nodes to add.
                        if (seg1Node1 == seg2Node1 || seg1Node1 == seg2Node2) {
                            commonCount++;

                            if (seg1Way == seg2Way &&
                                    seg1Pos == 0 &&
                                    seg2Pos == way2Nodes.size() -2) {
                                //do not add - this is first and last segment of the same way.
                            } else {
                                intersectionNodes.add(seg1Node1);
                            }
                        }

                        if (seg1Node2 == seg2Node1 || seg1Node2 == seg2Node2) {
                            commonCount++;

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
                                        seg2Pos++;
                                    }
                                }

                                if (insertInSeg2) {
                                    way2Nodes.add(seg2Pos +1, intNode);
                                    changedWays[seg2Way] = true;

                                    //Do not need to compare again to already split segment
                                    seg2Pos++;
                                }

                                intersectionNodes.add(intNode);

                                if (intNode == newNode) {
                                    cmds.add(new AddCommand(dataset, intNode));
                                }
                            }
                        } else if (test && !intersectionNodes.isEmpty())
                            return intersectionNodes;
                    }
                }
            }
        }


        for (int pos = 0; pos < ways.size(); pos++) {
            if (!changedWays[pos]) {
                continue;
            }

            Way way = ways.get(pos);
            Way newWay = new Way(way);
            newWay.setNodes(newNodes[pos]);

            cmds.add(new ChangeCommand(dataset, way, newWay));
        }

        return intersectionNodes;
    }

    private static BBox getNodesBounds(List<Node> nodes) {

        BBox bounds = new BBox(nodes.get(0));
        for (Node n: nodes) {
            bounds.add(n);
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
     * @param testPoint point to test
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
     * Finds the intersection of two line segments.
     * @param p1 the coordinates of the start point of the first specified line segment
     * @param p2 the coordinates of the end point of the first specified line segment
     * @param p3 the coordinates of the start point of the second specified line segment
     * @param p4 the coordinates of the end point of the second specified line segment
     * @return EastNorth null if no intersection was found, the EastNorth coordinates of the intersection otherwise
     */
    public static EastNorth getSegmentSegmentIntersection(EastNorth p1, EastNorth p2, EastNorth p3, EastNorth p4) {

        CheckParameterUtil.ensure(p1, "p1", EastNorth::isValid);
        CheckParameterUtil.ensure(p2, "p2", EastNorth::isValid);
        CheckParameterUtil.ensure(p3, "p3", EastNorth::isValid);
        CheckParameterUtil.ensure(p4, "p4", EastNorth::isValid);

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

        double uu = b2*c1 - b1*c2;
        double vv = a1*c2 - a2*c1;
        double mag = Math.abs(uu)+Math.abs(vv);

        if (Math.abs(det) > 1e-12 * mag) {
            double u = uu/det, v = vv/det;
            if (u > -1e-8 && u < 1+1e-8 && v > -1e-8 && v < 1+1e-8) {
                if (u < 0) u = 0;
                if (u > 1) u = 1.0;
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
     *
     * @param p1 first point on first line
     * @param p2 second point on first line
     * @param p3 first point on second line
     * @param p4 second point on second line
     * @return EastNorth null if no intersection was found, the coordinates of the intersection otherwise
     * @throws IllegalArgumentException if a parameter is null or without valid coordinates
     */
    public static EastNorth getLineLineIntersection(EastNorth p1, EastNorth p2, EastNorth p3, EastNorth p4) {

        CheckParameterUtil.ensure(p1, "p1", EastNorth::isValid);
        CheckParameterUtil.ensure(p2, "p2", EastNorth::isValid);
        CheckParameterUtil.ensure(p3, "p3", EastNorth::isValid);
        CheckParameterUtil.ensure(p4, "p4", EastNorth::isValid);

        // Basically, the formula from wikipedia is used:
        //  https://en.wikipedia.org/wiki/Line%E2%80%93line_intersection
        // However, large numbers lead to rounding errors (see #10286).
        // To avoid this, p1 is first substracted from each of the points:
        //  p1' = 0
        //  p2' = p2 - p1
        //  p3' = p3 - p1
        //  p4' = p4 - p1
        // In the end, p1 is added to the intersection point of segment p1'/p2'
        // and segment p3'/p4'.

        // Convert line from (point, point) form to ax+by=c
        double a1 = p2.getY() - p1.getY();
        double b1 = p1.getX() - p2.getX();

        double a2 = p4.getY() - p3.getY();
        double b2 = p3.getX() - p4.getX();

        // Solve the equations
        double det = a1 * b2 - a2 * b1;
        if (det == 0)
            return null; // Lines are parallel

        double c2 = (p4.getX() - p1.getX()) * (p3.getY() - p1.getY()) - (p3.getX() - p1.getX()) * (p4.getY() - p1.getY());

        return new EastNorth(b1 * c2 / det + p1.getX(), -a1 * c2 / det + p1.getY());
    }

    /**
     * Check if the segment p1 - p2 is parallel to p3 - p4
     * @param p1 First point for first segment
     * @param p2 Second point for first segment
     * @param p3 First point for second segment
     * @param p4 Second point for second segment
     * @return <code>true</code> if they are parallel or close to parallel
     */
    public static boolean segmentsParallel(EastNorth p1, EastNorth p2, EastNorth p3, EastNorth p4) {

        CheckParameterUtil.ensure(p1, "p1", EastNorth::isValid);
        CheckParameterUtil.ensure(p2, "p2", EastNorth::isValid);
        CheckParameterUtil.ensure(p3, "p3", EastNorth::isValid);
        CheckParameterUtil.ensure(p4, "p4", EastNorth::isValid);

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

        //segment zero length
        if (ldx == 0 && ldy == 0)
            return p1;

        double pdx = point.getX() - p1.getX();
        double pdy = point.getY() - p1.getY();

        double offset = (pdx * ldx + pdy * ldy) / (ldx * ldx + ldy * ldy);

        if (segmentOnly && offset <= 0)
            return p1;
        else if (segmentOnly && offset >= 1)
            return p2;
        else
            return p1.interpolate(p2, offset);
    }

    /**
     * Calculates closest point to a line segment.
     * @param segmentP1 First point determining line segment
     * @param segmentP2 Second point determining line segment
     * @param point Point for which a closest point is searched on line segment [P1,P2]
     * @return segmentP1 if it is the closest point, segmentP2 if it is the closest point,
     * a new point if closest point is between segmentP1 and segmentP2.
     * @see #closestPointToLine
     * @since 3650
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
     * @see #closestPointToSegment
     * @since 4134
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

        CheckParameterUtil.ensure(commonNode, "commonNode", EastNorth::isValid);
        CheckParameterUtil.ensure(firstNode, "firstNode", EastNorth::isValid);
        CheckParameterUtil.ensure(secondNode, "secondNode", EastNorth::isValid);

        double dy1 = firstNode.getY() - commonNode.getY();
        double dy2 = secondNode.getY() - commonNode.getY();
        double dx1 = firstNode.getX() - commonNode.getX();
        double dx2 = secondNode.getX() - commonNode.getX();

        return dy1 * dx2 - dx1 * dy2 > 0;
    }

    /**
     * Returns the Area of a polygon, from its list of nodes.
     * @param polygon List of nodes forming polygon
     * @return Area for the given list of nodes  (EastNorth coordinates)
     * @since 6841
     */
    public static Area getArea(List<Node> polygon) {
        Path2D path = new Path2D.Double();

        boolean begin = true;
        for (Node n : polygon) {
            EastNorth en = n.getEastNorth();
            if (en != null) {
                if (begin) {
                    path.moveTo(en.getX(), en.getY());
                    begin = false;
                } else {
                    path.lineTo(en.getX(), en.getY());
                }
            }
        }
        if (!begin) {
            path.closePath();
        }

        return new Area(path);
    }

    /**
     * Builds a path from a list of nodes
     * @param polygon Nodes, forming a closed polygon
     * @param path2d path to add to; can be null, then a new path is created
     * @return the path (LatLon coordinates)
     */
    public static Path2D buildPath2DLatLon(List<Node> polygon, Path2D path2d) {
        Path2D path = path2d != null ? path2d : new Path2D.Double();
        boolean begin = true;
        for (Node n : polygon) {
            if (begin) {
                path.moveTo(n.lon(), n.lat());
                begin = false;
            } else {
                path.lineTo(n.lon(), n.lat());
            }
        }
        if (!begin) {
            path.closePath();
        }
        return path;
    }

    /**
     * Returns the Area of a polygon, from the multipolygon relation.
     * @param multipolygon the multipolygon relation
     * @return Area for the multipolygon (LatLon coordinates)
     */
    public static Area getAreaLatLon(Relation multipolygon) {
        final Multipolygon mp = MultipolygonCache.getInstance().get(multipolygon);
        Path2D path = new Path2D.Double();
        path.setWindingRule(Path2D.WIND_EVEN_ODD);
        for (Multipolygon.PolyData pd : mp.getCombinedPolygons()) {
            buildPath2DLatLon(pd.getNodes(), path);
            for (Multipolygon.PolyData pdInner : pd.getInners()) {
                buildPath2DLatLon(pdInner.getNodes(), path);
            }
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
        return polygonIntersection(a1, a2, 1.0);
    }

    /**
     * Tests if two polygons intersect.
     * @param a1 Area of first polygon
     * @param a2 Area of second polygon
     * @param eps an area threshold, everything below is considered an empty intersection
     * @return intersection kind
     */
    public static PolygonIntersection polygonIntersection(Area a1, Area a2, double eps) {

        Area inter = new Area(a1);
        inter.intersect(a2);

        Rectangle bounds = inter.getBounds();

        if (inter.isEmpty() || bounds.getHeight()*bounds.getWidth() <= eps) {
            return PolygonIntersection.OUTSIDE;
        } else if (a2.getBounds2D().contains(a1.getBounds2D()) && inter.equals(a1)) {
            return PolygonIntersection.FIRST_INSIDE_SECOND;
        } else if (a1.getBounds2D().contains(a2.getBounds2D()) && inter.equals(a2)) {
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

        //iterate each side of the polygon, start with the last segment
        Node oldPoint = polygonNodes.get(polygonNodes.size() - 1);

        if (!oldPoint.isLatLonKnown()) {
            return false;
        }

        boolean inside = false;
        Node p1, p2;

        for (Node newPoint : polygonNodes) {
            //skip duplicate points
            if (newPoint.equals(oldPoint)) {
                continue;
            }

            if (!newPoint.isLatLonKnown()) {
                return false;
            }

            //order points so p1.lat <= p2.lat
            if (newPoint.getEastNorth().getY() > oldPoint.getEastNorth().getY()) {
                p1 = oldPoint;
                p2 = newPoint;
            } else {
                p1 = newPoint;
                p2 = oldPoint;
            }

            EastNorth pEN = point.getEastNorth();
            EastNorth opEN = oldPoint.getEastNorth();
            EastNorth npEN = newPoint.getEastNorth();
            EastNorth p1EN = p1.getEastNorth();
            EastNorth p2EN = p2.getEastNorth();

            if (pEN != null && opEN != null && npEN != null && p1EN != null && p2EN != null) {
                //test if the line is crossed and if so invert the inside flag.
                if ((npEN.getY() < pEN.getY()) == (pEN.getY() <= opEN.getY())
                        && (pEN.getX() - p1EN.getX()) * (p2EN.getY() - p1EN.getY())
                        < (p2EN.getX() - p1EN.getX()) * (pEN.getY() - p1EN.getY())) {
                    inside = !inside;
                }
            }

            oldPoint = newPoint;
        }

        return inside;
    }

    /**
     * Returns area of a closed way in square meters.
     *
     * @param way Way to measure, should be closed (first node is the same as last node)
     * @return area of the closed way.
     */
    public static double closedWayArea(Way way) {
        return getAreaAndPerimeter(way.getNodes(), Projections.getProjectionByCode("EPSG:54008")).getArea();
    }

    /**
     * Returns area of a multipolygon in square meters.
     *
     * @param multipolygon the multipolygon to measure
     * @return area of the multipolygon.
     */
    public static double multipolygonArea(Relation multipolygon) {
        double area = 0.0;
        final Multipolygon mp = MultipolygonCache.getInstance().get(multipolygon);
        for (Multipolygon.PolyData pd : mp.getCombinedPolygons()) {
            area += pd.getAreaAndPerimeter(Projections.getProjectionByCode("EPSG:54008")).getArea();
        }
        return area;
    }

    /**
     * Computes the area of a closed way and multipolygon in square meters, or {@code null} for other primitives
     *
     * @param osm the primitive to measure
     * @return area of the primitive, or {@code null}
     */
    public static Double computeArea(OsmPrimitive osm) {
        if (osm instanceof Way && ((Way) osm).isClosed()) {
            return closedWayArea((Way) osm);
        } else if (osm instanceof Relation && ((Relation) osm).isMultipolygon() && !((Relation) osm).hasIncompleteMembers()) {
            return multipolygonArea((Relation) osm);
        } else {
            return null;
        }
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
        return isClockwise(w.getNodes());
    }

    /**
     * Determines whether path from nodes list is oriented clockwise.
     * @param nodes Nodes list to be checked.
     * @return true if and only if way is oriented clockwise.
     * @throws IllegalArgumentException if way is not closed (see {@link Way#isClosed}).
     * @see #isClockwise(Way)
     */
    public static boolean isClockwise(List<Node> nodes) {
        int nodesCount = nodes.size();
        if (nodesCount < 3 || nodes.get(0) != nodes.get(nodesCount - 1)) {
            throw new IllegalArgumentException("Way must be closed to check orientation.");
        }
        double area2 = 0.;

        for (int node = 1; node <= /*sic! consider last-first as well*/ nodesCount; node++) {
            Node coorPrev = nodes.get(node - 1);
            Node coorCurr = nodes.get(node % nodesCount);
            area2 += coorPrev.lon() * coorCurr.lat();
            area2 -= coorCurr.lon() * coorPrev.lat();
        }
        return area2 < 0;
    }

    /**
     * Returns angle of a segment defined with 2 point coordinates.
     *
     * @param p1 first point
     * @param p2 second point
     * @return Angle in radians (-pi, pi]
     */
    public static double getSegmentAngle(EastNorth p1, EastNorth p2) {

        CheckParameterUtil.ensure(p1, "p1", EastNorth::isValid);
        CheckParameterUtil.ensure(p2, "p2", EastNorth::isValid);

        return Math.atan2(p2.north() - p1.north(), p2.east() - p1.east());
    }

    /**
     * Returns angle of a corner defined with 3 point coordinates.
     *
     * @param p1 first point
     * @param p2 Common endpoint
     * @param p3 third point
     * @return Angle in radians (-pi, pi]
     */
    public static double getCornerAngle(EastNorth p1, EastNorth p2, EastNorth p3) {

        CheckParameterUtil.ensure(p1, "p1", EastNorth::isValid);
        CheckParameterUtil.ensure(p2, "p2", EastNorth::isValid);
        CheckParameterUtil.ensure(p3, "p3", EastNorth::isValid);

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
     * @see Geometry#getCenter
     */
    public static EastNorth getCentroid(List<Node> nodes) {

        BigDecimal area = BigDecimal.ZERO;
        BigDecimal north = BigDecimal.ZERO;
        BigDecimal east = BigDecimal.ZERO;

        // See https://en.wikipedia.org/wiki/Centroid#Centroid_of_polygon for the equation used here
        for (int i = 0; i < nodes.size(); i++) {
            EastNorth n0 = nodes.get(i).getEastNorth();
            EastNorth n1 = nodes.get((i+1) % nodes.size()).getEastNorth();

            if (n0 != null && n1 != null && n0.isValid() && n1.isValid()) {
                BigDecimal x0 = BigDecimal.valueOf(n0.east());
                BigDecimal y0 = BigDecimal.valueOf(n0.north());
                BigDecimal x1 = BigDecimal.valueOf(n1.east());
                BigDecimal y1 = BigDecimal.valueOf(n1.north());

                BigDecimal k = x0.multiply(y1, MathContext.DECIMAL128).subtract(y0.multiply(x1, MathContext.DECIMAL128));

                area = area.add(k, MathContext.DECIMAL128);
                east = east.add(k.multiply(x0.add(x1, MathContext.DECIMAL128), MathContext.DECIMAL128));
                north = north.add(k.multiply(y0.add(y1, MathContext.DECIMAL128), MathContext.DECIMAL128));
            }
        }

        BigDecimal d = new BigDecimal(3, MathContext.DECIMAL128); // 1/2 * 6 = 3
        area = area.multiply(d, MathContext.DECIMAL128);
        if (area.compareTo(BigDecimal.ZERO) != 0) {
            north = north.divide(area, MathContext.DECIMAL128);
            east = east.divide(area, MathContext.DECIMAL128);
        }

        return new EastNorth(east.doubleValue(), north.doubleValue());
    }

    /**
     * Compute center of the circle closest to different nodes.
     *
     * Ensure exact center computation in case nodes are already aligned in circle.
     * This is done by least square method.
     * Let be a_i x + b_i y + c_i = 0 equations of bisectors of each edges.
     * Center must be intersection of all bisectors.
     * <pre>
     *          [ a1  b1  ]         [ -c1 ]
     * With A = [ ... ... ] and Y = [ ... ]
     *          [ an  bn  ]         [ -cn ]
     * </pre>
     * An approximation of center of circle is (At.A)^-1.At.Y
     * @param nodes Nodes parts of the circle (at least 3)
     * @return An approximation of the center, of null if there is no solution.
     * @see Geometry#getCentroid
     * @since 6934
     */
    public static EastNorth getCenter(List<Node> nodes) {
        int nc = nodes.size();
        if (nc < 3) return null;
        /**
         * Equation of each bisector ax + by + c = 0
         */
        double[] a = new double[nc];
        double[] b = new double[nc];
        double[] c = new double[nc];
        // Compute equation of bisector
        for (int i = 0; i < nc; i++) {
            EastNorth pt1 = nodes.get(i).getEastNorth();
            EastNorth pt2 = nodes.get((i+1) % nc).getEastNorth();
            a[i] = pt1.east() - pt2.east();
            b[i] = pt1.north() - pt2.north();
            double d = Math.sqrt(a[i]*a[i] + b[i]*b[i]);
            if (d == 0) return null;
            a[i] /= d;
            b[i] /= d;
            double xC = (pt1.east() + pt2.east()) / 2;
            double yC = (pt1.north() + pt2.north()) / 2;
            c[i] = -(a[i]*xC + b[i]*yC);
        }
        // At.A = [aij]
        double a11 = 0, a12 = 0, a22 = 0;
        // At.Y = [bi]
        double b1 = 0, b2 = 0;
        for (int i = 0; i < nc; i++) {
            a11 += a[i]*a[i];
            a12 += a[i]*b[i];
            a22 += b[i]*b[i];
            b1 -= a[i]*c[i];
            b2 -= b[i]*c[i];
        }
        // (At.A)^-1 = [invij]
        double det = a11*a22 - a12*a12;
        if (Math.abs(det) < 1e-5) return null;
        double inv11 = a22/det;
        double inv12 = -a12/det;
        double inv22 = a11/det;
        // center (xC, yC) = (At.A)^-1.At.y
        double xC = inv11*b1 + inv12*b2;
        double yC = inv12*b1 + inv22*b2;
        return new EastNorth(xC, yC);
    }

    /**
     * Tests if the {@code node} is inside the multipolygon {@code multiPolygon}. The nullable argument
     * {@code isOuterWayAMatch} allows to decide if the immediate {@code outer} way of the multipolygon is a match.
     * @param node node
     * @param multiPolygon multipolygon
     * @param isOuterWayAMatch allows to decide if the immediate {@code outer} way of the multipolygon is a match
     * @return {@code true} if the node is inside the multipolygon
     */
    public static boolean isNodeInsideMultiPolygon(Node node, Relation multiPolygon, Predicate<Way> isOuterWayAMatch) {
        return isPolygonInsideMultiPolygon(Collections.singletonList(node), multiPolygon, isOuterWayAMatch);
    }

    /**
     * Tests if the polygon formed by {@code nodes} is inside the multipolygon {@code multiPolygon}. The nullable argument
     * {@code isOuterWayAMatch} allows to decide if the immediate {@code outer} way of the multipolygon is a match.
     * <p>
     * If {@code nodes} contains exactly one element, then it is checked whether that one node is inside the multipolygon.
     * @param nodes nodes forming the polygon
     * @param multiPolygon multipolygon
     * @param isOuterWayAMatch allows to decide if the immediate {@code outer} way of the multipolygon is a match
     * @return {@code true} if the polygon formed by nodes is inside the multipolygon
     */
    public static boolean isPolygonInsideMultiPolygon(List<Node> nodes, Relation multiPolygon, Predicate<Way> isOuterWayAMatch) {
        // Extract outer/inner members from multipolygon
        final Pair<List<JoinedPolygon>, List<JoinedPolygon>> outerInner;
        try {
            outerInner = MultipolygonBuilder.joinWays(multiPolygon);
        } catch (MultipolygonBuilder.JoinedPolygonCreationException ex) {
            Logging.trace(ex);
            Logging.debug("Invalid multipolygon " + multiPolygon);
            return false;
        }
        // Test if object is inside an outer member
        for (JoinedPolygon out : outerInner.a) {
            if (nodes.size() == 1
                    ? nodeInsidePolygon(nodes.get(0), out.getNodes())
                    : EnumSet.of(PolygonIntersection.FIRST_INSIDE_SECOND, PolygonIntersection.CROSSING).contains(
                            polygonIntersection(nodes, out.getNodes()))) {
                boolean insideInner = false;
                // If inside an outer, check it is not inside an inner
                for (JoinedPolygon in : outerInner.b) {
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
                    if (isOuterWayAMatch == null || isOuterWayAMatch.test(out.ways.get(0)
                            /* TODO give a better representation of the outer ring to the predicate */)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Data class to hold two double values (area and perimeter of a polygon).
     */
    public static class AreaAndPerimeter {
        private final double area;
        private final double perimeter;

        /**
         * Create a new {@link AreaAndPerimeter}
         * @param area The area
         * @param perimeter The perimeter
         */
        public AreaAndPerimeter(double area, double perimeter) {
            this.area = area;
            this.perimeter = perimeter;
        }

        /**
         * Gets the area
         * @return The area size
         */
        public double getArea() {
            return area;
        }

        /**
         * Gets the perimeter
         * @return The perimeter length
         */
        public double getPerimeter() {
            return perimeter;
        }
    }

    /**
     * Calculate area and perimeter length of a polygon.
     *
     * Uses current projection; units are that of the projected coordinates.
     *
     * @param nodes the list of nodes representing the polygon
     * @return area and perimeter
     */
    public static AreaAndPerimeter getAreaAndPerimeter(List<Node> nodes) {
        return getAreaAndPerimeter(nodes, null);
    }

    /**
     * Calculate area and perimeter length of a polygon in the given projection.
     *
     * @param nodes the list of nodes representing the polygon
     * @param projection the projection to use for the calculation, {@code null} defaults to {@link Main#getProjection()}
     * @return area and perimeter
     */
    public static AreaAndPerimeter getAreaAndPerimeter(List<Node> nodes, Projection projection) {
        CheckParameterUtil.ensureParameterNotNull(nodes, "nodes");
        double area = 0;
        double perimeter = 0;
        Projection useProjection = projection == null ? Main.getProjection() : projection;

        if (!nodes.isEmpty()) {
            boolean closed = nodes.get(0) == nodes.get(nodes.size() - 1);
            int numSegments = closed ? nodes.size() - 1 : nodes.size();
            EastNorth p1 = nodes.get(0).getEastNorth(useProjection);
            for (int i = 1; i <= numSegments; i++) {
                final Node node = nodes.get(i == numSegments ? 0 : i);
                final EastNorth p2 = node.getEastNorth(useProjection);
                if (p1 != null && p2 != null) {
                    area += p1.east() * p2.north() - p2.east() * p1.north();
                    perimeter += p1.distance(p2);
                }
                p1 = p2;
            }
        }
        return new AreaAndPerimeter(Math.abs(area) / 2, perimeter);
    }
}
