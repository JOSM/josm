// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.openstreetmap.josm.data.projection.Ellipsoid.WGS84;

import java.awt.geom.Area;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.openstreetmap.josm.command.AddCommand;
import org.openstreetmap.josm.command.ChangeNodesCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.ILatLon;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.BBox;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.INode;
import org.openstreetmap.josm.data.osm.IPrimitive;
import org.openstreetmap.josm.data.osm.IRelation;
import org.openstreetmap.josm.data.osm.IWay;
import org.openstreetmap.josm.data.osm.MultipolygonBuilder;
import org.openstreetmap.josm.data.osm.MultipolygonBuilder.JoinedPolygon;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.NodePositionComparator;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.WaySegment;
import org.openstreetmap.josm.data.osm.visitor.paint.relations.Multipolygon;
import org.openstreetmap.josm.data.osm.visitor.paint.relations.Multipolygon.PolyData;
import org.openstreetmap.josm.data.osm.visitor.paint.relations.MultipolygonCache;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.data.projection.ProjectionRegistry;
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

    /** threshold value for size of intersection area given in east/north space */
    public static final double INTERSECTION_EPS_EAST_NORTH = 1e-4;

    /**
     * Will find all intersection and add nodes there for list of given ways.
     * Handles self-intersections too.
     * And makes commands to add the intersection points to ways.
     * <p>
     * Prerequisite: no two nodes have the same coordinates.
     *
     * @param ways  a list of ways to test
     * @param test  if true, do not build list of Commands, just return nodes
     * @param cmds  list of commands, typically empty when handed to this method.
     *              Will be filled with commands that add intersection nodes to
     *              the ways.
     * @return set of new nodes, if test is true the list might not contain all intersections
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
            wayBounds[pos] = ways.get(pos).getBBox();
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

                //iterate over primary segment
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
                                Node newNode = new Node(ProjectionRegistry.getProjection().eastNorth2latlon(intersection));
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

                                if (test) {
                                    intersectionNodes.add(intNode);
                                    return intersectionNodes;
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
            if (changedWays[pos]) {
                cmds.add(new ChangeNodesCommand(dataset, ways.get(pos), newNodes[pos]));
            }
        }

        return intersectionNodes;
    }

    /**
     * Tests if given point is to the right side of path consisting of 3 points.
     * <p>
     * (Imagine the path is continued beyond the endpoints, so you get two rays
     * starting from lineP2 and going through lineP1 and lineP3 respectively
     * which divide the plane into two parts. The test returns true, if testPoint
     * lies in the part that is to the right when traveling in the direction
     * lineP1, lineP2, lineP3.)
     *
     * @param <N> type of node
     * @param lineP1 first point in path
     * @param lineP2 second point in path
     * @param lineP3 third point in path
     * @param testPoint point to test
     * @return true if to the right side, false otherwise
     */
    public static <N extends INode> boolean isToTheRightSideOfLine(N lineP1, N lineP2, N lineP3, N testPoint) {
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
     * @param <N> type of node
     * @param commonNode starting point for both vectors
     * @param firstNode first vector end node
     * @param secondNode second vector end node
     * @return true if first vector is clockwise before second vector.
     */
    public static <N extends INode> boolean angleIsClockwise(N commonNode, N firstNode, N secondNode) {
        return angleIsClockwise(commonNode.getEastNorth(), firstNode.getEastNorth(), secondNode.getEastNorth());
    }

    /**
     * Finds the intersection of two line segments.
     * @param p1 the coordinates of the start point of the first specified line segment
     * @param p2 the coordinates of the end point of the first specified line segment
     * @param p3 the coordinates of the start point of the second specified line segment
     * @param p4 the coordinates of the end point of the second specified line segment
     * @return EastNorth null if no intersection was found, the EastNorth coordinates of the intersection otherwise
     * @see #getSegmentSegmentIntersection(ILatLon, ILatLon, ILatLon, ILatLon)
     */
    public static EastNorth getSegmentSegmentIntersection(EastNorth p1, EastNorth p2, EastNorth p3, EastNorth p4) {
        // see the ILatLon version for an explanation why the checks are in the if statement
        if (!(p1.isValid() && p2.isValid() && p3.isValid() && p4.isValid())) {
            CheckParameterUtil.ensureThat(p1.isValid(), () -> p1 + " invalid");
            CheckParameterUtil.ensureThat(p2.isValid(), () -> p2 + " invalid");
            CheckParameterUtil.ensureThat(p3.isValid(), () -> p3 + " invalid");
            CheckParameterUtil.ensureThat(p4.isValid(), () -> p4 + " invalid");
        }

        double x1 = p1.getX();
        double y1 = p1.getY();
        double x2 = p2.getX();
        double y2 = p2.getY();
        double x3 = p3.getX();
        double y3 = p3.getY();
        double x4 = p4.getX();
        double y4 = p4.getY();
        double[] en = getSegmentSegmentIntersection(x1, y1, x2, y2, x3, y3, x4, y4);
        if (en != null && en.length == 2) {
            return new EastNorth(en[0], en[1]);
        }
        return null;
    }

    /**
     * Finds the intersection of two line segments.
     * @param p1 the coordinates of the start point of the first specified line segment
     * @param p2 the coordinates of the end point of the first specified line segment
     * @param p3 the coordinates of the start point of the second specified line segment
     * @param p4 the coordinates of the end point of the second specified line segment
     * @return LatLon null if no intersection was found, the LatLon coordinates of the intersection otherwise
     * @see #getSegmentSegmentIntersection(EastNorth, EastNorth, EastNorth, EastNorth)
     * @since 18553
     */
    public static ILatLon getSegmentSegmentIntersection(ILatLon p1, ILatLon p2, ILatLon p3, ILatLon p4) {
        // Avoid lambda creation if at all possible -- this pretty much removes all memory allocations
        // from this method (11.4 GB to 0) when testing #20716 with Mesa County, CO (overpass download).
        // There was also a 2/3 decrease in CPU samples for the method.
        if (!(p1.isLatLonKnown() && p2.isLatLonKnown() && p3.isLatLonKnown() && p4.isLatLonKnown())) {
            CheckParameterUtil.ensureThat(p1.isLatLonKnown(), () -> p1 + " invalid");
            CheckParameterUtil.ensureThat(p2.isLatLonKnown(), () -> p2 + " invalid");
            CheckParameterUtil.ensureThat(p3.isLatLonKnown(), () -> p3 + " invalid");
            CheckParameterUtil.ensureThat(p4.isLatLonKnown(), () -> p4 + " invalid");
        }

        double x1 = p1.lon();
        double y1 = p1.lat();
        double x2 = p2.lon();
        double y2 = p2.lat();
        double x3 = p3.lon();
        double y3 = p3.lat();
        double x4 = p4.lon();
        double y4 = p4.lat();
        double[] en = getSegmentSegmentIntersection(x1, y1, x2, y2, x3, y3, x4, y4);
        if (en != null && en.length == 2) {
            return new LatLon(en[1], en[0]);
        }
        return null;
    }

    /**
     * Get the segment-segment intersection of two line segments
     * @param x1 The x coordinate of the first point (first segment)
     * @param y1 The y coordinate of the first point (first segment)
     * @param x2 The x coordinate of the second point (first segment)
     * @param y2 The y coordinate of the second point (first segment)
     * @param x3 The x coordinate of the third point (second segment)
     * @param y3 The y coordinate of the third point (second segment)
     * @param x4 The x coordinate of the fourth point (second segment)
     * @param y4 The y coordinate of the fourth point (second segment)
     * @return {@code null} if no intersection was found, otherwise [x, y]
     */
    private static double[] getSegmentSegmentIntersection(double x1, double y1, double x2, double y2, double x3, double y3,
            double x4, double y4) {

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
                return new double[] {x1+a1*u, y1+a2*u};
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

        CheckParameterUtil.ensureThat(p1.isValid(), () -> p1 + " invalid");
        CheckParameterUtil.ensureThat(p2.isValid(), () -> p2 + " invalid");
        CheckParameterUtil.ensureThat(p3.isValid(), () -> p3 + " invalid");
        CheckParameterUtil.ensureThat(p4.isValid(), () -> p4 + " invalid");

        // Basically, the formula from wikipedia is used:
        //  https://en.wikipedia.org/wiki/Line%E2%80%93line_intersection
        // However, large numbers lead to rounding errors (see #10286).
        // To avoid this, p1 is first subtracted from each of the points:
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

        CheckParameterUtil.ensureThat(p1.isValid(), () -> p1 + " invalid");
        CheckParameterUtil.ensureThat(p2.isValid(), () -> p2 + " invalid");
        CheckParameterUtil.ensureThat(p3.isValid(), () -> p3 + " invalid");
        CheckParameterUtil.ensureThat(p4.isValid(), () -> p4 + " invalid");

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
     * <p>
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

        CheckParameterUtil.ensureThat(commonNode.isValid(), () -> commonNode + " invalid");
        CheckParameterUtil.ensureThat(firstNode.isValid(), () -> firstNode + " invalid");
        CheckParameterUtil.ensureThat(secondNode.isValid(), () -> secondNode + " invalid");

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
    public static Area getArea(List<? extends INode> polygon) {
        Path2D path = new Path2D.Double(Path2D.WIND_NON_ZERO, polygon.size());

        boolean begin = true;
        for (INode n : polygon) {
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
     * @since 13638 (signature)
     */
    public static Path2D buildPath2DLatLon(List<? extends ILatLon> polygon, Path2D path2d) {
        Path2D path = path2d != null ? path2d : new Path2D.Double();
        boolean begin = true;
        for (ILatLon n : polygon) {
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
     * Calculate area in east/north space for given primitive. Uses {@link MultipolygonCache} for multipolygon relations.
     * @param p the primitive
     * @return the area in east/north space, might be empty if the primitive is incomplete or not closed or a node
     * since 15938
     */
    public static Area getAreaEastNorth(IPrimitive p) {
        if (p instanceof Way && ((Way) p).isClosed()) {
            return Geometry.getArea(((Way) p).getNodes());
        }
        if (p instanceof Relation && p.isMultipolygon() && !p.isIncomplete()) {
            Multipolygon mp = MultipolygonCache.getInstance().get((Relation) p);
            if (mp.getOpenEnds().isEmpty()) {
                Path2D path = new Path2D.Double();
                path.setWindingRule(Path2D.WIND_EVEN_ODD);
                for (PolyData pd : mp.getCombinedPolygons()) {
                    path.append(pd.get(), false);
                }
                return new Area(path);
            }
        }
        return new Area();
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
    public static PolygonIntersection polygonIntersection(List<? extends INode> first, List<? extends INode> second) {
        Area a1 = getArea(first);
        Area a2 = getArea(second);
        return polygonIntersection(a1, a2, INTERSECTION_EPS_EAST_NORTH);
    }

    /**
     * Tests if two polygons intersect. It is assumed that the area is given in East North points.
     * @param a1 Area of first polygon
     * @param a2 Area of second polygon
     * @return intersection kind
     * @since 6841
     */
    public static PolygonIntersection polygonIntersection(Area a1, Area a2) {
        return polygonIntersection(a1, a2, INTERSECTION_EPS_EAST_NORTH);
    }

    /**
     * Tests if two polygons intersect.
     * @param a1 Area of first polygon
     * @param a2 Area of second polygon
     * @param eps an area threshold, everything below is considered an empty intersection
     * @return intersection kind
     */
    public static PolygonIntersection polygonIntersection(Area a1, Area a2, double eps) {
        return polygonIntersectionResult(a1, a2, eps).a;
    }

    /**
     * Calculate intersection area and kind of intersection between two polygons.
     * @param a1 Area of first polygon
     * @param a2 Area of second polygon
     * @param eps an area threshold, everything below is considered an empty intersection
     * @return pair with intersection kind and intersection area (never null, but maybe empty)
     * @since 15938
     */
    public static Pair<PolygonIntersection, Area> polygonIntersectionResult(Area a1, Area a2, double eps) {
        // Simple intersect check (if their bounds don't intersect, don't bother going further; there will be no intersection)
        // This avoids the more expensive Area#intersect call some of the time (decreases CPU and memory allocation by ~95%)
        // in Mesa County, CO geometry validator test runs.
        final Rectangle2D a12d = a1.getBounds2D();
        final Rectangle2D a22d = a2.getBounds2D();
        if (!a12d.intersects(a22d) || !a1.intersects(a22d) || !a2.intersects(a12d)) {
            return new Pair<>(PolygonIntersection.OUTSIDE, new Area());
        }
        Area inter = new Area(a1);
        inter.intersect(a2);

        // Note: Area has an equals method that takes Area; it does _not_ override the Object.equals method.
        if (inter.isEmpty() || !checkIntersection(inter, eps)) {
            return new Pair<>(PolygonIntersection.OUTSIDE, inter);
        } else if (a22d.contains(a12d) && inter.equals(a1)) {
            return new Pair<>(PolygonIntersection.FIRST_INSIDE_SECOND, inter);
        } else if (a12d.contains(a22d) && inter.equals(a2)) {
            return new Pair<>(PolygonIntersection.SECOND_INSIDE_FIRST, inter);
        } else {
            return new Pair<>(PolygonIntersection.CROSSING, inter);
        }
    }

    /**
     * Check an intersection area which might describe multiple small polygons.
     * Return true if any of the polygons is bigger than the given threshold.
     * @param inter the intersection area
     * @param eps an area threshold, everything below is considered an empty intersection
     * @return true if any of the polygons is bigger than the given threshold
     */
    private static boolean checkIntersection(Area inter, double eps) {
        PathIterator pit = inter.getPathIterator(null);
        double[] res = new double[6];
        Rectangle2D r = new Rectangle2D.Double();
        while (!pit.isDone()) {
            int type = pit.currentSegment(res);
            switch (type) {
            case PathIterator.SEG_MOVETO:
                r = new Rectangle2D.Double(res[0], res[1], 0, 0);
                break;
            case PathIterator.SEG_LINETO:
                r.add(res[0], res[1]);
                break;
            case PathIterator.SEG_CLOSE:
                if (r.getWidth() > eps || r.getHeight() > eps)
                    return true;
                break;
            default:
                break;
            }
            pit.next();
        }
        return false;
    }

    /**
     * Tests if point is inside a polygon. The polygon can be self-intersecting. In such case the contains function works in xor-like manner.
     * @param polygonNodes list of nodes from polygon path.
     * @param point the point to test
     * @return true if the point is inside polygon.
     */
    public static boolean nodeInsidePolygon(INode point, List<? extends INode> polygonNodes) {
        if (polygonNodes.size() < 2)
            return false;

        //iterate each side of the polygon, start with the last segment
        INode oldPoint = polygonNodes.get(polygonNodes.size() - 1);

        if (!oldPoint.isLatLonKnown()) {
            return false;
        }

        boolean inside = false;
        INode p1, p2;

        for (INode newPoint : polygonNodes) {
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
        final Multipolygon mp = MultipolygonCache.getInstance().get(multipolygon);
        return mp.getCombinedPolygons().stream()
                .mapToDouble(pd -> pd.getAreaAndPerimeter(Projections.getProjectionByCode("EPSG:54008")).getArea())
                .sum();
    }

    /**
     * Computes the area of a closed way and multipolygon in square meters, or {@code null} for other primitives
     *
     * @param osm the primitive to measure
     * @return area of the primitive, or {@code null}
     * @since 13638 (signature)
     */
    public static Double computeArea(IPrimitive osm) {
        if (osm instanceof Way && ((Way) osm).isClosed()) {
            return closedWayArea((Way) osm);
        } else if (osm instanceof Relation && osm.isMultipolygon() && !((Relation) osm).hasIncompleteMembers()) {
            return multipolygonArea((Relation) osm);
        } else {
            return null;
        }
    }

    /**
     * Determines whether a way is oriented clockwise.
     * <p>
     * Internals: Assuming a closed non-looping way, compute twice the area
     * of the polygon using the formula {@code 2 * area = sum (X[n] * Y[n+1] - X[n+1] * Y[n])}.
     * If the area is negative the way is ordered in a clockwise direction.
     * <p>
     * See <a href="https://web.archive.org/web/20120722100030/http://paulbourke.net/geometry/polyarea/">
     *     https://paulbourke.net/geometry/polyarea/
     *     </a>
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
    public static boolean isClockwise(List<? extends INode> nodes) {
        int nodesCount = nodes.size();
        if (nodesCount < 3 || nodes.get(0) != nodes.get(nodesCount - 1)) {
            throw new IllegalArgumentException("Way must be closed to check orientation.");
        }
        double area2 = 0.;

        for (int node = 1; node <= /*sic! consider last-first as well*/ nodesCount; node++) {
            INode coorPrev = nodes.get(node - 1);
            INode coorCurr = nodes.get(node % nodesCount);
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

        CheckParameterUtil.ensureThat(p1.isValid(), () -> p1 + " invalid");
        CheckParameterUtil.ensureThat(p2.isValid(), () -> p2 + " invalid");

        return Math.atan2(p2.north() - p1.north(), p2.east() - p1.east());
    }

    /**
     * Returns angle of a corner defined with 3 point coordinates.
     *
     * @param p1 first point
     * @param common Common end point
     * @param p3 third point
     * @return Angle in radians (-pi, pi]
     */
    public static double getCornerAngle(EastNorth p1, EastNorth common, EastNorth p3) {

        CheckParameterUtil.ensureThat(p1.isValid(), () -> p1 + " invalid");
        CheckParameterUtil.ensureThat(common.isValid(), () -> common + " invalid");
        CheckParameterUtil.ensureThat(p3.isValid(), () -> p3 + " invalid");

        double result = getSegmentAngle(common, p1) - getSegmentAngle(common, p3);
        if (result <= -Math.PI) {
            result += 2 * Math.PI;
        }

        if (result > Math.PI) {
            result -= 2 * Math.PI;
        }

        return result;
    }

    /**
     * Get angles in radians and return its value in range [0, 180].
     *
     * @param angle the angle in radians
     * @return normalized angle in degrees
     * @since 13670
     */
    public static double getNormalizedAngleInDegrees(double angle) {
        return Math.abs(180 * angle / Math.PI);
    }

    /**
     * Compute the centroid/barycenter of nodes
     * @param nodes Nodes for which the centroid is wanted
     * @return the centroid of nodes
     * @see Geometry#getCenter
     */
    public static EastNorth getCentroid(List<? extends INode> nodes) {
        return getCentroidEN(nodes.stream().filter(INode::isLatLonKnown).map(INode::getEastNorth).collect(Collectors.toList()));
    }

    /**
     * Compute the centroid/barycenter of nodes
     * @param nodes Coordinates for which the centroid is wanted
     * @return the centroid of nodes
     * @since 13712
     */
    public static EastNorth getCentroidEN(List<EastNorth> nodes) {

        final int size = nodes.size();
        if (size == 1) {
            return nodes.get(0);
        } else if (size == 2) {
            return nodes.get(0).getCenter(nodes.get(1));
        } else if (size == 0) {
            return null;
        }

        BigDecimal area = BigDecimal.ZERO;
        BigDecimal north = BigDecimal.ZERO;
        BigDecimal east = BigDecimal.ZERO;

        // See https://en.wikipedia.org/wiki/Centroid#Of_a_polygon for the equation used here
        for (int i = 0; i < size; i++) {
            EastNorth n0 = nodes.get(i);
            EastNorth n1 = nodes.get((i+1) % size);

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
     * Compute the center of the circle closest to different nodes.
     * <p>
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
    public static EastNorth getCenter(List<? extends INode> nodes) {
        int nc = nodes.size();
        if (nc < 3) return null;
        /*
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
     * For repeated tests against {@code multiPolygon} better use {@link Geometry#filterInsideMultipolygon}.
     * @param node node
     * @param multiPolygon multipolygon
     * @param isOuterWayAMatch allows to decide if the immediate {@code outer} way of the multipolygon is a match
     * @return {@code true} if the node is inside the multipolygon
     */
    public static boolean isNodeInsideMultiPolygon(INode node, Relation multiPolygon, Predicate<Way> isOuterWayAMatch) {
        return isPolygonInsideMultiPolygon(Collections.singletonList(node), multiPolygon, isOuterWayAMatch);
    }

    /**
     * Tests if the polygon formed by {@code nodes} is inside the multipolygon {@code multiPolygon}. The nullable argument
     * {@code isOuterWayAMatch} allows to decide if the immediate {@code outer} way of the multipolygon is a match.
     * For repeated tests against {@code multiPolygon} better use {@link Geometry#filterInsideMultipolygon}.
     * <p>
     * If {@code nodes} contains exactly one element, then it is checked whether that one node is inside the multipolygon.
     * @param nodes nodes forming the polygon
     * @param multiPolygon multipolygon
     * @param isOuterWayAMatch allows to decide if the immediate {@code outer} way of the multipolygon is a match
     * @return {@code true} if the multipolygon is valid and the polygon formed by nodes is inside the multipolygon
     */
    public static boolean isPolygonInsideMultiPolygon(List<? extends INode> nodes, Relation multiPolygon, Predicate<Way> isOuterWayAMatch) {
        try {
            return isPolygonInsideMultiPolygon(nodes, MultipolygonBuilder.joinWays(multiPolygon), isOuterWayAMatch);
        } catch (MultipolygonBuilder.JoinedPolygonCreationException ex) {
            Logging.trace(ex);
            Logging.debug("Invalid multipolygon " + multiPolygon);
            return false;
        }
    }

    /**
     * Tests if the polygon formed by {@code nodes} is inside the multipolygon {@code multiPolygon}. The nullable argument
     * {@code isOuterWayAMatch} allows to decide if the immediate {@code outer} way of the multipolygon is a match.
     * For repeated tests against {@code multiPolygon} better use {@link Geometry#filterInsideMultipolygon}.
     * <p>
     * If {@code nodes} contains exactly one element, then it is checked whether that one node is inside the multipolygon.
     * @param nodes nodes forming the polygon
     * @param outerInner result of {@link MultipolygonBuilder#joinWays(Relation)}
     * @param isOuterWayAMatch allows to decide if the immediate {@code outer} way of the multipolygon is a match
     * @return {@code true} if the multipolygon is valid and the polygon formed by nodes is inside the multipolygon
     * @since 15069
     */
    public static boolean isPolygonInsideMultiPolygon(List<? extends INode> nodes, Pair<List<JoinedPolygon>,
            List<JoinedPolygon>> outerInner, Predicate<Way> isOuterWayAMatch) {
        Area a1 = nodes.size() == 1 ? null : getArea(nodes);
        // Test if object is inside an outer member
        for (JoinedPolygon out : outerInner.a) {
            if (a1 == null
                    ? nodeInsidePolygon(nodes.get(0), out.nodes)
                    : PolygonIntersection.FIRST_INSIDE_SECOND == polygonIntersection(a1, out.area)) {
                // If inside an outer, check it is not inside an inner
                boolean insideInner = outerInner.b.stream().anyMatch(in -> a1 == null
                        ? nodeInsidePolygon(nodes.get(0), in.nodes)
                        : in.area.getBounds2D().contains(a1.getBounds2D())
                        && polygonIntersection(a1, in.area) == PolygonIntersection.FIRST_INSIDE_SECOND
                        && polygonIntersection(in.area, out.area) == PolygonIntersection.FIRST_INSIDE_SECOND);
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
     * Find all primitives in the given collection which are inside the given polygon.
     *
     * @param primitives the primitives
     * @param polygon the closed way or multipolygon relation
     * @return a new list containing the found primitives, empty if polygon is invalid or nothing was found.
     * @see Geometry#filterInsidePolygon
     * @see Geometry#filterInsideMultipolygon
     * @since 15730
     */
    public static List<IPrimitive> filterInsideAnyPolygon(Collection<IPrimitive> primitives, IPrimitive polygon) {
        if (polygon instanceof IWay<?>) {
            return filterInsidePolygon(primitives, (IWay<?>) polygon);
        } else if (polygon instanceof Relation && polygon.isMultipolygon()) {
            return filterInsideMultipolygon(primitives, (Relation) polygon);
        }
        return Collections.emptyList();
    }

    /**
     * Find all primitives in the given collection which are inside the given polygon.
     * Unclosed ways and multipolygon relations with unclosed outer rings are ignored.
     *
     * @param primitives the primitives
     * @param polygon the polygon
     * @return a new list containing the found primitives, empty if polygon is invalid or nothing was found.
     * @since 15069 (for {@link List} of {@code primitives}, 15730 for a {@link Collection} of {@code primitives})
     */
    public static List<IPrimitive> filterInsidePolygon(Collection<IPrimitive> primitives, IWay<?> polygon) {
        List<IPrimitive> res = new ArrayList<>();
        if (!polygon.isClosed() || polygon.getNodesCount() <= 3)
            return res;
        /* polygon area in east north space, calculated only when really needed */
        Area polygonArea = null;
        for (IPrimitive p : primitives) {
            if (p instanceof INode) {
                if (nodeInsidePolygon((INode) p, polygon.getNodes())) {
                    res.add(p);
                }
            } else if (p instanceof IWay) {
                if (((IWay<?>) p).isClosed()) {
                    if (polygonArea == null) {
                        polygonArea = getArea(polygon.getNodes());
                    }
                    if (PolygonIntersection.FIRST_INSIDE_SECOND == polygonIntersection(getArea(((IWay<?>) p).getNodes()),
                            polygonArea)) {
                        res.add(p);
                    }
                }
            } else if (p.isMultipolygon()) {
                if (polygonArea == null) {
                    polygonArea = getArea(polygon.getNodes());
                }
                Multipolygon mp = p.getDataSet() != null ? MultipolygonCache.getInstance().get((Relation) p) : new Multipolygon((Relation) p);
                boolean inside = true;
                // a (valid) multipolygon is inside the polygon if all outer rings are inside
                for (PolyData outer : mp.getOuterPolygons()) {
                    if (!outer.isClosed()
                            || !polygonArea.getBounds2D().contains(outer.getBounds())
                            || PolygonIntersection.FIRST_INSIDE_SECOND != polygonIntersection(getArea(outer.getNodes()),
                                    polygonArea)) {
                        inside = false;
                        break;
                    }
                }
                if (inside) {
                    res.add(p);
                }
            }
        }
        return res;
    }

    /**
     * Find all primitives in the given collection which are inside the given multipolygon. Members of the multipolygon are
     * ignored. Unclosed ways and multipolygon relations with unclosed outer rings are ignored.
     * @param primitives the primitives
     * @param multiPolygon the multipolygon relation
     * @return a new list containing the found primitives, empty if multipolygon is invalid or nothing was found.
     * @since 15069
     */
    public static List<IPrimitive> filterInsideMultipolygon(Collection<IPrimitive> primitives, Relation multiPolygon) {
        return filterInsideMultipolygon(primitives, multiPolygon, null);
    }

    /**
     * Find all primitives in the given collection which are inside the given multipolygon. Members of the multipolygon are
     * ignored. Unclosed ways and multipolygon relations with unclosed outer rings are ignored.
     * @param primitives the primitives
     * @param multiPolygon the multipolygon relation
     * @param cache The cache to avoid calculating joined inner/outer ways multiple times (see {@link MultipolygonBuilder#joinWays(Relation)})
     * @return a new list containing the found primitives, empty if multipolygon is invalid or nothing was found.
     * @since 19336
     */
    public static List<IPrimitive> filterInsideMultipolygon(Collection<IPrimitive> primitives, Relation multiPolygon,
                                                            Map<IRelation<?>, Pair<List<JoinedPolygon>, List<JoinedPolygon>>> cache) {
        List<IPrimitive> res = new ArrayList<>();
        if (primitives.isEmpty())
            return res;

        final Pair<List<JoinedPolygon>, List<JoinedPolygon>> outerInner;
        try {
            outerInner = MultipolygonBuilder.joinWays(cache, multiPolygon);
        } catch (MultipolygonBuilder.JoinedPolygonCreationException ex) {
            Logging.trace(ex);
            Logging.debug("Invalid multipolygon " + multiPolygon);
            return res;
        }

        Set<OsmPrimitive> members = multiPolygon.getMemberPrimitives();
        for (IPrimitive p : primitives) {
            if (members.contains(p))
                continue;
            if (p instanceof Node) {
                if (isPolygonInsideMultiPolygon(Collections.singletonList((Node) p), outerInner, null)) {
                    res.add(p);
                }
            } else if (p instanceof Way) {
                if (((IWay<?>) p).isClosed() && isPolygonInsideMultiPolygon(((Way) p).getNodes(), outerInner, null)) {
                    res.add(p);
                }
            } else if (p.isMultipolygon()) {
                Multipolygon mp = new Multipolygon((Relation) p);
                // a (valid) multipolygon is inside multiPolygon if all outer rings are inside
                boolean inside = mp.getOuterPolygons().stream()
                        .allMatch(outer -> outer.isClosed() && isPolygonInsideMultiPolygon(outer.getNodes(), outerInner, null));
                if (inside) {
                    res.add(p);
                }
            }
        }
        return res;
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
     * <p>
     * Uses current projection; units are that of the projected coordinates.
     *
     * @param nodes the list of nodes representing the polygon
     * @return area and perimeter
     */
    public static AreaAndPerimeter getAreaAndPerimeter(List<? extends ILatLon> nodes) {
        return getAreaAndPerimeter(nodes, null);
    }

    /**
     * Calculate area and perimeter length of a polygon in the given projection.
     *
     * @param nodes the list of nodes representing the polygon
     * @param projection the projection to use for the calculation, {@code null} defaults to {@link ProjectionRegistry#getProjection()}
     * @return area and perimeter
     * @since 13638 (signature)
     */
    public static AreaAndPerimeter getAreaAndPerimeter(List<? extends ILatLon> nodes, Projection projection) {
        CheckParameterUtil.ensureParameterNotNull(nodes, "nodes");
        double area = 0;
        double perimeter = 0;
        Projection useProjection = projection == null ? ProjectionRegistry.getProjection() : projection;

        if (!nodes.isEmpty()) {
            boolean closed = nodes.get(0) == nodes.get(nodes.size() - 1);
            int numSegments = closed ? nodes.size() - 1 : nodes.size();
            EastNorth p1 = nodes.get(0).getEastNorth(useProjection);
            for (int i = 1; i <= numSegments; i++) {
                final ILatLon node = nodes.get(i == numSegments ? 0 : i);
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

    /**
     * Get the closest primitive to {@code osm} from the collection of
     * OsmPrimitive {@code primitives}
     * <p>
     * The {@code primitives} should be fully downloaded to ensure accuracy.
     * <p>
     * Note: The complexity of this method is O(n*m), where n is the number of
     * children {@code osm} has plus 1, m is the number of children the
     * collection of primitives have plus the number of primitives in the
     * collection.
     *
     * @param <T> The return type of the primitive
     * @param osm The primitive to get the distances from
     * @param primitives The collection of primitives to get the distance to
     * @return The closest {@link OsmPrimitive}. This is not determinative.
     * To get all primitives that share the same distance, use
     * {@link Geometry#getClosestPrimitives}.
     * @since 15035
     */
    public static <T extends OsmPrimitive> T getClosestPrimitive(OsmPrimitive osm, Collection<T> primitives) {
        Collection<T> collection = getClosestPrimitives(osm, primitives);
        return collection.iterator().next();
    }

    /**
     * Get the closest primitives to {@code osm} from the collection of
     * OsmPrimitive {@code primitives}
     * <p>
     * The {@code primitives} should be fully downloaded to ensure accuracy.
     * <p>
     * Note: The complexity of this method is O(n*m), where n is the number of
     * children {@code osm} has plus 1, m is the number of children the
     * collection of primitives have plus the number of primitives in the
     * collection.
     *
     * @param <T> The return type of the primitive
     * @param osm The primitive to get the distances from
     * @param primitives The collection of primitives to get the distance to
     * @return The closest {@link OsmPrimitive}s. May be empty.
     * @since 15035
     */
    public static <T extends OsmPrimitive> Collection<T> getClosestPrimitives(OsmPrimitive osm, Collection<T> primitives) {
        double lowestDistance = Double.MAX_VALUE;
        TreeSet<T> closest = new TreeSet<>();
        for (T primitive : primitives) {
            double distance = getDistance(osm, primitive);
            if (Double.isNaN(distance)) continue;
            int comp = Double.compare(distance, lowestDistance);
            if (comp < 0) {
                closest.clear();
                lowestDistance = distance;
                closest.add(primitive);
            } else if (comp == 0) {
                closest.add(primitive);
            }
        }
        return closest;
    }

    /**
     * Get the furthest primitive to {@code osm} from the collection of
     * OsmPrimitive {@code primitives}
     * <p>
     * The {@code primitives} should be fully downloaded to ensure accuracy.
     * <p>
     * It does NOT give the furthest primitive based off of the furthest
     * part of that primitive
     * <p>
     * Note: The complexity of this method is O(n*m), where n is the number of
     * children {@code osm} has plus 1, m is the number of children the
     * collection of primitives have plus the number of primitives in the
     * collection.
     *
     * @param <T> The return type of the primitive
     * @param osm The primitive to get the distances from
     * @param primitives The collection of primitives to get the distance to
     * @return The furthest {@link OsmPrimitive}.  This is not determinative.
     * To get all primitives that share the same distance, use
     * {@link Geometry#getFurthestPrimitives}
     * @since 15035
     */
    public static <T extends OsmPrimitive> T getFurthestPrimitive(OsmPrimitive osm, Collection<T> primitives) {
        return getFurthestPrimitives(osm, primitives).iterator().next();
    }

    /**
     * Get the furthest primitives to {@code osm} from the collection of
     * OsmPrimitive {@code primitives}
     * <p>
     * The {@code primitives} should be fully downloaded to ensure accuracy.
     * <p>
     * It does NOT give the furthest primitive based off of the furthest
     * part of that primitive
     * <p>
     * Note: The complexity of this method is O(n*m), where n is the number of
     * children {@code osm} has plus 1, m is the number of children the
     * collection of primitives have plus the number of primitives in the
     * collection.
     *
     * @param <T> The return type of the primitive
     * @param osm The primitive to get the distances from
     * @param primitives The collection of primitives to get the distance to
     * @return The furthest {@link OsmPrimitive}s. It may return an empty collection.
     * @since 15035
     */
    public static <T extends OsmPrimitive> Collection<T> getFurthestPrimitives(OsmPrimitive osm, Collection<T> primitives) {
        double furthestDistance = Double.NEGATIVE_INFINITY;
        TreeSet<T> furthest = new TreeSet<>();
        for (T primitive : primitives) {
            double distance = getDistance(osm, primitive);
            if (Double.isNaN(distance)) continue;
            int comp = Double.compare(distance, furthestDistance);
            if (comp > 0) {
                furthest.clear();
                furthestDistance = distance;
                furthest.add(primitive);
            } else if (comp == 0) {
                furthest.add(primitive);
            }
        }
        return furthest;
    }

    /**
     * Get the distance between different {@link OsmPrimitive}s
     * @param one The primitive to get the distance from
     * @param two The primitive to get the distance to
     * @return The distance between the primitives in meters
     * (or the unit of the current projection, see {@link Projection}).
     * May return {@link Double#NaN} if one of the primitives is incomplete.
     * <p>
     * Note: The complexity is O(n*m), where (n,m) are the number of child
     * objects the {@link OsmPrimitive}s have.
     * @since 15035
     */
    public static double getDistance(OsmPrimitive one, OsmPrimitive two) {
        double rValue = Double.MAX_VALUE;
        if (one == null || two == null || one.isIncomplete()
                || two.isIncomplete()) return Double.NaN;
        if (one instanceof ILatLon && two instanceof ILatLon) {
            rValue = ((ILatLon) one).greatCircleDistance(((ILatLon) two));
        } else if (one instanceof Node && two instanceof Way) {
            rValue = getDistanceWayNode((Way) two, (Node) one);
        } else if (one instanceof Way && two instanceof Node) {
            rValue = getDistanceWayNode((Way) one, (Node) two);
        } else if (one instanceof Way && two instanceof Way) {
            rValue = getDistanceWayWay((Way) one, (Way) two);
        } else if (one instanceof Relation && !(two instanceof Relation)) {
            for (OsmPrimitive osmPrimitive: ((Relation) one).getMemberPrimitives()) {
                double currentDistance = getDistance(osmPrimitive, two);
                if (currentDistance < rValue) rValue = currentDistance;
            }
        } else if (!(one instanceof Relation) && two instanceof Relation) {
            rValue = getDistance(two, one);
        } else if (one instanceof Relation && two instanceof Relation) {
            for (OsmPrimitive osmPrimitive1 : ((Relation) one).getMemberPrimitives()) {
                for (OsmPrimitive osmPrimitive2 : ((Relation) two).getMemberPrimitives()) {
                    double currentDistance = getDistance(osmPrimitive1, osmPrimitive2);
                    if (currentDistance < rValue) rValue = currentDistance;
                }
            }
        }
        return rValue != Double.MAX_VALUE ? rValue : Double.NaN;
    }

    /**
     * Get the distance between a way and a node
     * @param way The way to get the distance from
     * @param node The node to get the distance to
     * @return The distance between the {@code way} and the {@code node} in
     * meters (or the unit of the current projection, see {@link Projection}).
     * May return {@link Double#NaN} if the primitives are incomplete.
     * @since 15035
     */
    public static double getDistanceWayNode(Way way, Node node) {
        if (way == null || node == null || way.getNodesCount() < 2 || !node.isLatLonKnown())
            return Double.NaN;

        double smallest = Double.MAX_VALUE;
        EastNorth en0 = node.getEastNorth();
        // go through the nodes as if they were paired
        Iterator<Node> iter = way.getNodes().iterator();
        EastNorth en1 = iter.next().getEastNorth();
        while (iter.hasNext()) {
            EastNorth en2 = iter.next().getEastNorth();
            double distance = getSegmentNodeDistSq(en1, en2, en0);
            if (distance < smallest)
                smallest = distance;
            en1 = en2;
        }
        return smallest != Double.MAX_VALUE ? Math.sqrt(smallest) : Double.NaN;
    }

    /**
     * Get the closest {@link WaySegment} from a way to a primitive.
     * @param way The {@link Way} to get the distance from and the {@link WaySegment}
     * @param primitive The {@link OsmPrimitive} to get the distance to
     * @return The {@link WaySegment} that is closest to {@code primitive} from {@code way}.
     * If there are multiple {@link WaySegment}s with the same distance, the last
     * {@link WaySegment} with the same distance will be returned.
     * May return {@code null} if the way has fewer than two nodes or one
     * of the primitives is incomplete.
     * @since 15035
     */
    public static WaySegment getClosestWaySegment(Way way, OsmPrimitive primitive) {
        if (way == null || primitive == null || way.isIncomplete()
                || primitive.isIncomplete()) return null;
        double lowestDistance = Double.MAX_VALUE;
        Pair<Node, Node> closestNodes = null;
        for (Pair<Node, Node> nodes : way.getNodePairs(false)) {
            Way tWay = new Way();
            tWay.addNode(nodes.a);
            tWay.addNode(nodes.b);
            double distance = getDistance(tWay, primitive);
            if (distance < lowestDistance) {
                lowestDistance = distance;
                closestNodes = nodes;
            }
        }
        if (closestNodes == null) return null;
        return lowestDistance != Double.MAX_VALUE ? WaySegment.forNodePair(way, closestNodes.a, closestNodes.b) : null;
    }

    /**
     * Get the distance between different ways. Iterates over the nodes of the ways, complexity is O(n*m)
     * (n,m giving the number of nodes)
     * @param w1 The first {@link Way}
     * @param w2 The second {@link Way}
     * @return The shortest distance between the ways in meters
     * (or the unit of the current projection, see {@link Projection}).
     * May return {@link Double#NaN}.
     * @since 15035
     */
    public static double getDistanceWayWay(Way w1, Way w2) {
        if (w1 == null || w2 == null || w1.getNodesCount() < 2 || w2.getNodesCount() < 2)
            return Double.NaN;
        double rValue = Double.MAX_VALUE;
        Iterator<Node> iter1 = w1.getNodes().iterator();
        List<Node> w2Nodes = w2.getNodes();
        Node w1N1 = iter1.next();
        while (iter1.hasNext()) {
            Node w1N2 = iter1.next();
            Iterator<Node> iter2 = w2Nodes.iterator();
            Node w2N1 = iter2.next();
            while (iter2.hasNext()) {
                Node w2N2 = iter2.next();
                double distance = getDistanceSegmentSegment(w1N1, w1N2, w2N1, w2N2);
                if (distance < rValue)
                    rValue = distance;
                w2N1 = w2N2;
            }
            w1N1 = w1N2;
        }
        return rValue != Double.MAX_VALUE ? rValue : Double.NaN;
    }

    /**
     * Get the distance between different {@link WaySegment}s
     * @param ws1 A {@link WaySegment}
     * @param ws2 A {@link WaySegment}
     * @return The distance between the two {@link WaySegment}s in meters
     * (or the unit of the current projection, see {@link Projection}).
     * May return {@link Double#NaN}.
     * @since 15035
     */
    public static double getDistanceSegmentSegment(WaySegment ws1, WaySegment ws2) {
        return getDistanceSegmentSegment(ws1.getFirstNode(), ws1.getSecondNode(), ws2.getFirstNode(), ws2.getSecondNode());
    }

    /**
     * Get the distance between different {@link WaySegment}s
     * @param ws1Node1 The first node of the first WaySegment
     * @param ws1Node2 The second node of the second WaySegment
     * @param ws2Node1 The first node of the second WaySegment
     * @param ws2Node2 The second node of the second WaySegment
     * @return The distance between the two {@link WaySegment}s in meters
     * (or the unit of the current projection, see {@link Projection}).
     * May return {@link Double#NaN}.
     * @since 15035
     */
    public static double getDistanceSegmentSegment(Node ws1Node1, Node ws1Node2, Node ws2Node1, Node ws2Node2) {
        if (!ws1Node1.isLatLonKnown() || !ws1Node2.isLatLonKnown() || !ws2Node1.isLatLonKnown() || !ws2Node2.isLatLonKnown()) {
            return Double.NaN;
        }
        EastNorth enWs1Node1 = ws1Node1.getEastNorth();
        EastNorth enWs1Node2 = ws1Node2.getEastNorth();
        EastNorth enWs2Node1 = ws2Node1.getEastNorth();
        EastNorth enWs2Node2 = ws2Node2.getEastNorth();
        if (getSegmentSegmentIntersection(enWs1Node1, enWs1Node2, enWs2Node1, enWs2Node2) != null)
            return 0;

        double dist1sq = getSegmentNodeDistSq(enWs1Node1, enWs1Node2, enWs2Node1);
        double dist2sq = getSegmentNodeDistSq(enWs1Node1, enWs1Node2, enWs2Node2);
        double dist3sq = getSegmentNodeDistSq(enWs2Node1, enWs2Node2, enWs1Node1);
        double dist4sq = getSegmentNodeDistSq(enWs2Node1, enWs2Node2, enWs1Node2);
        double smallest = Math.min(Math.min(dist1sq, dist2sq), Math.min(dist3sq, dist4sq));
        return smallest != Double.MAX_VALUE ? Math.sqrt(smallest) : Double.NaN;
    }

    /**
     * Create a new LatLon at a specified distance. Currently uses WGS84, but may change randomly in the future.
     * This does not currently attempt to be hugely accurate. The actual location may be off
     * depending upon the distance and the elevation, but should be within 0.0002 meters.
     *
     * @param original The originating point
     * @param angle The angle (from true north) in radians
     * @param offset The distance to the new point in the current projection's units
     * @return The location at the specified angle and distance from the originating point
     * @since 18109
     */
    public static ILatLon getLatLonFrom(final ILatLon original, final double angle, final double offset) {
        final double meterOffset = ProjectionRegistry.getProjection().getMetersPerUnit() * offset;
        final double radianLat = Math.toRadians(original.lat());
        final double radianLon = Math.toRadians(original.lon());
        final double angularDistance = meterOffset / WGS84.a;
        final double lat = Math.asin(Math.sin(radianLat) * Math.cos(angularDistance)
                + Math.cos(radianLat) * Math.sin(angularDistance) * Math.cos(angle));
        final double lon = radianLon + Math.atan2(Math.sin(angle) * Math.sin(angularDistance) * Math.cos(radianLat),
                Math.cos(angularDistance) - Math.sin(radianLat) * Math.sin(lat));
        return new LatLon(Math.toDegrees(lat), Math.toDegrees(lon));
    }

    /**
     * Calculate closest distance between a line segment s1-s2 and a point p
     * @param s1 start of segment
     * @param s2 end of segment
     * @param p the point
     * @return the square of the euclidean distance from p to the closest point on the segment
     */
    private static double getSegmentNodeDistSq(EastNorth s1, EastNorth s2, EastNorth p) {
        EastNorth c1 = closestPointTo(s1, s2, p, true);
        return c1.distanceSq(p);
    }
}
