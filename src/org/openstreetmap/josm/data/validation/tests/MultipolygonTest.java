// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.openstreetmap.josm.command.ChangeMembersCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.DefaultNameFormatter;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.WaySegment;
import org.openstreetmap.josm.data.osm.visitor.paint.relations.Multipolygon;
import org.openstreetmap.josm.data.osm.visitor.paint.relations.Multipolygon.PolyData;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.Test;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.gui.mappaint.ElemStyles;
import org.openstreetmap.josm.gui.mappaint.MapPaintStyles;
import org.openstreetmap.josm.gui.mappaint.styleelement.AreaElement;
import org.openstreetmap.josm.tools.Geometry;
import org.openstreetmap.josm.tools.Geometry.PolygonIntersection;
import org.openstreetmap.josm.tools.Utils;

/**
 * Checks if multipolygons are valid
 * @since 3669
 */
public class MultipolygonTest extends Test {

    /** Non-Way in multipolygon */
    public static final int WRONG_MEMBER_TYPE = 1601;
    /** No useful role for multipolygon member */
    public static final int WRONG_MEMBER_ROLE = 1602;
    /** Multipolygon is not closed */
    public static final int NON_CLOSED_WAY = 1603;
    /** Multipolygon inner way is outside */
    public static final int INNER_WAY_OUTSIDE = 1605;
    /** Intersection between multipolygon ways */
    public static final int CROSSING_WAYS = 1606;
    /** Style for outer way mismatches / With the currently used mappaint style(s) the style for outer way mismatches the area style */
    public static final int OUTER_STYLE_MISMATCH = 1607;
    /** With the currently used mappaint style the style for inner way equals the multipolygon style */
    public static final int INNER_STYLE_MISMATCH = 1608;
    // no longer used: Area style way is not closed NOT_CLOSED = 1609
    /** No area style for multipolygon */
    public static final int NO_STYLE = 1610;
    // no longer used: Multipolygon relation should be tagged with area tags and not the outer way(s) NO_STYLE_POLYGON = 1611;
    /** Area style on outer way */
    public static final int OUTER_STYLE = 1613;
    /** Multipolygon member repeated (same primitive, same role */
    public static final int REPEATED_MEMBER_SAME_ROLE = 1614;
    /** Multipolygon member repeated (same primitive, different role) */
    public static final int REPEATED_MEMBER_DIFF_ROLE = 1615;
    /** Multipolygon ring is equal to another ring */
    public static final int EQUAL_RINGS = 1616;
    /** Multipolygon rings share nodes */
    public static final int RINGS_SHARE_NODES = 1617;
    /** Incomplete multipolygon was modified */
    public static final int MODIFIED_INCOMPLETE = 1618;

    private static final int FOUND_INSIDE = 1;
    private static final int FOUND_OUTSIDE = 2;

    /** set when used to build a multipolygon relation */
    private Relation createdRelation;
    /** might be set when creating a relation and touching rings were found. */
    private boolean repeatCheck;

    /**
     * Constructs a new {@code MultipolygonTest}.
     */
    public MultipolygonTest() {
        super(tr("Multipolygon"),
                tr("This test checks if multipolygons are valid."));
    }

    @Override
    public void visit(Relation r) {
        if (r.isMultipolygon() && !r.isEmpty()) {
            List<TestError> tmpErrors = new ArrayList<>(30);
            boolean hasUnexpectedWayRoles = checkMembersAndRoles(r, tmpErrors);
            boolean hasRepeatedMembers = checkRepeatedWayMembers(r);
            if (r.isModified() && r.hasIncompleteMembers()) {
                errors.add(TestError.builder(this, Severity.WARNING, MODIFIED_INCOMPLETE)
                        .message(tr("Incomplete multipolygon relation was modified"))
                        .primitives(r)
                        .build());
            }
            // Rest of checks is only for complete multipolygon
            if (!hasUnexpectedWayRoles && !hasRepeatedMembers) {
                if (r.hasIncompleteMembers()) {
                    findIntersectingWaysIncomplete(r);
                } else {
                    Multipolygon polygon = new Multipolygon(r);
                    checkStyleConsistency(r, polygon);
                    checkGeometryAndRoles(r, polygon);
                    // see #17010: don't report problems twice
                    tmpErrors.removeIf(e -> e.getCode() == WRONG_MEMBER_ROLE);
                }
            }
            errors.addAll(tmpErrors);
        }
    }

    /**
     * Various style-related checks:<ul>
     * <li>{@link #NO_STYLE}: No area style for multipolygon</li>
     * <li>{@link #INNER_STYLE_MISMATCH}: With the currently used mappaint style the style for inner way equals the multipolygon style</li>
     * <li>{@link #OUTER_STYLE_MISMATCH}: With the currently used mappaint style the style for outer way mismatches the area style</li>
     * <li>{@link #OUTER_STYLE}: Area style on outer way</li>
     * </ul>
     * @param r relation
     * @param polygon multipolygon
     */
    private void checkStyleConsistency(Relation r, Multipolygon polygon) {
        if (MapPaintStyles.getStyles() != null && !r.isBoundary()) {
            AreaElement area = ElemStyles.getAreaElemStyle(r, false);
            if (area == null) {
                errors.add(TestError.builder(this, Severity.OTHER, NO_STYLE)
                        .message(tr("No area style for multipolygon"))
                        .primitives(r)
                        .build());
            } else {
                for (Way wInner : polygon.getInnerWays()) {
                    if (wInner.isClosed() && area.equals(ElemStyles.getAreaElemStyle(wInner, false))) {
                        errors.add(TestError.builder(this, Severity.OTHER, INNER_STYLE_MISMATCH)
                                .message(tr("With the currently used mappaint style the style for inner way equals the multipolygon style"))
                                .primitives(Arrays.asList(r, wInner))
                                .highlight(wInner)
                                .build());
                    }
                }
                for (Way wOuter : polygon.getOuterWays()) {
                    if (!wOuter.isArea())
                        continue;
                    AreaElement areaOuter = ElemStyles.getAreaElemStyle(wOuter, false);
                    if (areaOuter != null) {
                        if (!area.equals(areaOuter)) {
                            errors.add(TestError.builder(this, Severity.OTHER, OUTER_STYLE_MISMATCH)
                                    .message(tr("With the currently used mappaint style the style for outer way mismatches the area style"))
                                    .primitives(Arrays.asList(r, wOuter))
                                    .highlight(wOuter)
                                    .build());
                        } else { /* style on outer way of multipolygon, but equal to polygon */
                            errors.add(TestError.builder(this, Severity.WARNING, OUTER_STYLE)
                                    .message(tr("Area style on outer way"))
                                    .primitives(Arrays.asList(r, wOuter))
                                    .highlight(wOuter)
                                    .build());
                        }
                    }
                }
            }
        }
    }

    /**
     * Various geometry-related checks:<ul>
     * <li>{@link #NON_CLOSED_WAY}: Multipolygon is not closed</li>
     * <li>{@link #INNER_WAY_OUTSIDE}: Multipolygon inner way is outside</li>
     * <li>{@link #CROSSING_WAYS}: Intersection between multipolygon ways</li>
     * </ul>
     * @param r relation
     * @param polygon multipolygon
     */
    private void checkGeometryAndRoles(Relation r, Multipolygon polygon) {
        int oldErrorsSize = errors.size();

        Map<Long, RelationMember> wayMap = r.getMembers().stream()
                .filter(RelationMember::isWay)
                .collect(Collectors.toMap(mem -> mem.getWay().getUniqueId(), mem -> mem, (a, b) -> b));
        List<Node> openNodes = polygon.getOpenEnds();
        if (!openNodes.isEmpty() || wayMap.isEmpty()) {
            errors.add(TestError.builder(this, Severity.ERROR, NON_CLOSED_WAY)
                    .message(tr("Multipolygon is not closed"))
                    .primitives(combineRelAndPrimitives(r, openNodes))
                    .highlight(openNodes)
                    .build());
        }

        // duplicate members were checked before
        if (wayMap.isEmpty())
            return;

        Set<Node> sharedNodes = new HashSet<>();
        Set<Way> intersectionWays = new HashSet<>();
        findIntersectionNodes(r, sharedNodes, intersectionWays);

        List<PolyData> innerPolygons = polygon.getInnerPolygons();
        List<PolyData> outerPolygons = polygon.getOuterPolygons();
        List<PolyData> allPolygons = new ArrayList<>();
        allPolygons.addAll(outerPolygons);
        allPolygons.addAll(innerPolygons);

        Map<PolyData, List<PolyData>> crossingPolyMap = findIntersectingWays(r, innerPolygons, outerPolygons);

        if (!sharedNodes.isEmpty()) {
            for (int i = 0; i < allPolygons.size(); i++) {
                PolyData pd1 = allPolygons.get(i);
                checkPolygonForSelfIntersection(r, pd1);
                // check if this ring has a way that is known to intersect with another way

                if (!hasIntersectionWay(pd1, intersectionWays))
                    continue;

                for (int j = i + 1; j < allPolygons.size(); j++) {
                    PolyData pd2 = allPolygons.get(j);
                    if (!checkProblemMap(crossingPolyMap, pd1, pd2) && hasIntersectionWay(pd2, intersectionWays)) {
                        checkPolygonsForSharedNodes(r, pd1, pd2, sharedNodes);
                    }
                }
            }
        }
        boolean checkRoles = IntStream.range(oldErrorsSize, errors.size())
                .noneMatch(i -> errors.get(i).getSeverity() != Severity.OTHER);
        if (checkRoles) {
            // we found no intersection or crossing between the polygons and they are closed
            // now we can calculate the nesting level to verify the roles with some simple node checks
            checkOrSetRoles(r, allPolygons, wayMap, sharedNodes);
        }
    }

    /**
     * Simple check if given ring contains way that is known to intersect.
     * @param pd the ring
     * @param intersectionWays the known intersection ways
     * @return true if one or more ways are in the set of known ways
     */
    private static boolean hasIntersectionWay(PolyData pd, Set<Way> intersectionWays) {
        return intersectionWays.stream().anyMatch(w -> pd.getWayIds().contains(w.getUniqueId()));
    }

    /**
     * Check if a polygon ring is self-intersecting when the ring was build from multiple ways.
     * An self intersection in a single way is checked in {@link SelfIntersectingWay}.
     * @param r the relation
     * @param pd the ring
     */
    private void checkPolygonForSelfIntersection(Relation r, PolyData pd) {
        if (pd.getWayIds().size() == 1)
            return;
        List<Node> wayNodes = pd.getNodes();
        int num = wayNodes.size();
        Set<Node> nodes = new HashSet<>();
        Node firstNode = wayNodes.get(0);
        nodes.add(firstNode);
        List<Node> isNodes = new ArrayList<>();
        for (int i = 1; i < num - 1; i++) {
            Node n = wayNodes.get(i);
            if (nodes.contains(n)) {
                isNodes.add(n);
            } else {
                nodes.add(n);
            }
        }
        if (!isNodes.isEmpty()) {
            List<OsmPrimitive> prims = new ArrayList<>();
            prims.add(r);
            prims.addAll(isNodes);
            errors.add(TestError.builder(this, Severity.WARNING, CROSSING_WAYS)
                    .message(tr("Self-intersecting polygon ring"))
                    .primitives(prims)
                    .highlight(isNodes)
                    .build());

        }
    }

    /**
     * Detect intersections of multipolygon ways at nodes. If any way node is used by more than two ways
     * or two times in one way and at least once in another way we found an intersection.
     * @param r the relation
     * @param sharedNodes We be filled with shared nodes
     * @param intersectionWays We be filled with ways that have a shared node
     */
    private static void findIntersectionNodes(Relation r, Set<Node> sharedNodes, Set<Way> intersectionWays) {
        Map<Node, List<Way>> nodeMap = new HashMap<>();
        for (RelationMember rm : r.getMembers()) {
            if (!rm.isWay())
                continue;
            int numNodes = rm.getWay().getNodesCount();
            for (int i = 0; i < numNodes; i++) {
                Node n = rm.getWay().getNode(i);
                if (n.getReferrers().size() <= 1) {
                    continue; // cannot be a problem node
                }
                List<Way> ways = nodeMap.computeIfAbsent(n, k -> new ArrayList<>());
                ways.add(rm.getWay());
                if (ways.size() > 2 || (ways.size() == 2 && i != 0 && i + 1 != numNodes)) {
                    sharedNodes.add(n);
                    intersectionWays.addAll(ways);
                }
            }
        }
    }

    private enum ExtPolygonIntersection {
        EQUAL,
        FIRST_INSIDE_SECOND,
        SECOND_INSIDE_FIRST,
        OUTSIDE,
        CROSSING
    }

    private void checkPolygonsForSharedNodes(Relation r, PolyData pd1, PolyData pd2, Set<Node> allSharedNodes) {
        Set<Node> sharedByPolygons = new HashSet<>(allSharedNodes);
        sharedByPolygons.retainAll(pd1.getNodes());
        sharedByPolygons.retainAll(pd2.getNodes());
        if (sharedByPolygons.isEmpty())
            return;

        // the two polygons share one or more nodes
        // 1st might be equal to 2nd (same nodes, same or different direction) --> error shared way segments
        // they overlap --> error
        // 1st and 2nd share segments
        // 1st fully inside 2nd --> okay
        // 2nd fully inside 1st --> okay
        int errorCode = RINGS_SHARE_NODES;
        ExtPolygonIntersection res = checkOverlapAtSharedNodes(sharedByPolygons, pd1, pd2);
        if (res == ExtPolygonIntersection.CROSSING) {
            errorCode = CROSSING_WAYS;
        } else if (res == ExtPolygonIntersection.EQUAL) {
            errorCode = EQUAL_RINGS;
        }
        if (errorCode != 0) {
            Set<OsmPrimitive> prims = new HashSet<>();
            prims.add(r);
            for (Node n : sharedByPolygons) {
                for (OsmPrimitive p : n.getReferrers()) {
                    if (p instanceof Way && (pd1.getWayIds().contains(p.getUniqueId()) || pd2.getWayIds().contains(p.getUniqueId()))) {
                        prims.add(p);
                    }
                }
            }
            if (errorCode == RINGS_SHARE_NODES) {
                errors.add(TestError.builder(this, Severity.OTHER, errorCode)
                        .message(tr("Multipolygon rings share node"))
                        .primitives(prims)
                        .highlight(sharedByPolygons)
                        .build());
            } else {
                errors.add(TestError.builder(this, Severity.WARNING, errorCode)
                        .message(errorCode == CROSSING_WAYS ? tr("Intersection between multipolygon ways") : tr("Multipolygon rings are equal"))
                        .primitives(prims)
                        .highlight(sharedByPolygons)
                        .build());
            }
        }
    }

    private static ExtPolygonIntersection checkOverlapAtSharedNodes(Set<Node> shared, PolyData pd1, PolyData pd2) {
        // Idea: if two polygons share one or more nodes they can either just touch or share segments or intersect.
        // The insideness test is complex, so we try to reduce the number of these tests.
        // There is no need to check all nodes, we only have to check the node following a shared node.

        int[] flags = new int[2];
        for (int loop = 0; loop < flags.length; loop++) {
            List<Node> nodes2Test = loop == 0 ? pd1.getNodes() : pd2.getNodes();
            int num = nodes2Test.size() - 1; // ignore closing duplicate node


            int lenShared = 0;
            for (int i = 0; i < num; i++) {
                Node n = nodes2Test.get(i);
                if (shared.contains(n)) {
                    ++lenShared;
                } else {
                    if (i == 0 || lenShared > 0) {
                        // do we have to treat lenShared > 1 special ?
                        lenShared = 0;
                        boolean inside = checkIfNodeIsInsidePolygon(n, loop == 0 ? pd2 : pd1);
                        flags[loop] |= inside ? FOUND_INSIDE : FOUND_OUTSIDE;
                        if (flags[loop] == (FOUND_INSIDE | FOUND_OUTSIDE)) {
                            return ExtPolygonIntersection.CROSSING;
                        }
                    }
                }
            }
        }

        if ((flags[0] & FOUND_INSIDE) != 0)
            return ExtPolygonIntersection.FIRST_INSIDE_SECOND;
        if ((flags[1] & FOUND_INSIDE) != 0)
            return ExtPolygonIntersection.SECOND_INSIDE_FIRST;
        if ((flags[0] & FOUND_OUTSIDE) != (flags[1] & FOUND_OUTSIDE)) {
            return (flags[0] & FOUND_OUTSIDE) != 0 ?
                ExtPolygonIntersection.SECOND_INSIDE_FIRST : ExtPolygonIntersection.FIRST_INSIDE_SECOND;
        }
        if ((flags[0] & FOUND_OUTSIDE) != 0 && (flags[1] & FOUND_OUTSIDE) != 0) {
            // the two polygons may only share one or more segments but they may also intersect
            Area a1 = new Area(pd1.get());
            Area a2 = new Area(pd2.get());
            PolygonIntersection areaRes = Geometry.polygonIntersection(a1, a2);
            if (areaRes == PolygonIntersection.OUTSIDE)
                return ExtPolygonIntersection.OUTSIDE;
            return ExtPolygonIntersection.CROSSING;
        }
        return ExtPolygonIntersection.EQUAL;
    }

    /**
     * Helper class for calculation of nesting levels
     */
    private static class PolygonLevel {
        final int level; // nesting level, even for outer, odd for inner polygons.
        final PolyData outerWay;

        PolygonLevel(PolyData pd, int level) {
            this.outerWay = pd;
            this.level = level;
        }
    }

    /**
     * Calculate the nesting levels of the polygon rings and check if calculated role matches
     * @param r relation (for error reporting)
     * @param allPolygons list of polygon rings
     * @param wayMap maps way ids to relation members
     * @param sharedNodes all nodes shared by multiple ways of this multipolygon
     */
    private void checkOrSetRoles(Relation r, List<PolyData> allPolygons, Map<Long, RelationMember> wayMap, Set<Node> sharedNodes) {
        PolygonLevelFinder levelFinder = new PolygonLevelFinder(sharedNodes);
        List<PolygonLevel> list = levelFinder.findOuterWays(allPolygons);
        if (Utils.isEmpty(list)) {
            return;
        }
        if (r == createdRelation) {
            List<RelationMember> modMembers = new ArrayList<>();
            for (PolygonLevel pol : list) {
                final String calculatedRole = (pol.level % 2 == 0) ? "outer" : "inner";
                for (long wayId : pol.outerWay.getWayIds()) {
                    RelationMember member = wayMap.get(wayId);
                    modMembers.add(new RelationMember(calculatedRole, member.getMember()));
                }
            }
            r.setMembers(modMembers);
            return;
        }
        for (PolygonLevel pol : list) {
            final String calculatedRole = (pol.level % 2 == 0) ? "outer" : "inner";
            for (long wayId : pol.outerWay.getWayIds()) {
                RelationMember member = wayMap.get(wayId);
                if (!calculatedRole.equals(member.getRole())) {
                    errors.add(TestError.builder(this, Severity.ERROR, WRONG_MEMBER_ROLE)
                            .message(RelationChecker.ROLE_VERIF_PROBLEM_MSG,
                                    marktr("Role for ''{0}'' should be ''{1}''"),
                                    member.getMember().getDisplayName(DefaultNameFormatter.getInstance()),
                                    calculatedRole)
                            .primitives(Arrays.asList(r, member.getMember()))
                            .highlight(member.getMember())
                            .build());
                    if (pol.level == 0 && "inner".equals(member.getRole())) {
                        // maybe only add this error if we found an outer ring with correct role(s) ?
                        errors.add(TestError.builder(this, Severity.ERROR, INNER_WAY_OUTSIDE)
                                .message(tr("Multipolygon inner way is outside"))
                                .primitives(Arrays.asList(r, member.getMember()))
                                .highlight(member.getMember())
                                .build());
                    }
                }
            }
        }
    }

    /**
     * Check if a node is inside the polygon according to the insideness rules of Shape.
     * @param n the node
     * @param p the polygon
     * @return true if the node is inside the polygon
     */
    private static boolean checkIfNodeIsInsidePolygon(Node n, PolyData p) {
        EastNorth en = n.getEastNorth();
        return en != null && p.get().contains(en.getX(), en.getY());
    }

    /**
     * Determine multipolygon ways which are intersecting (crossing without a common node) or sharing one or more way segments.
     * See also {@link CrossingWays}
     * @param r the relation (for error reporting)
     * @param innerPolygons list of inner polygons
     * @param outerPolygons list of outer polygons
     * @return map with crossing polygons
     */
    private Map<PolyData, List<PolyData>> findIntersectingWays(Relation r, List<PolyData> innerPolygons,
            List<PolyData> outerPolygons) {
        HashMap<PolyData, List<PolyData>> crossingPolygonsMap = new HashMap<>();
        HashMap<PolyData, List<PolyData>> sharedWaySegmentsPolygonsMap = new HashMap<>();

        for (int loop = 0; loop < 2; loop++) {

            Map<List<Way>, List<WaySegment>> crossingWays = findIntersectingWays(r, loop == 1);

            if (!crossingWays.isEmpty()) {
                Map<PolyData, List<PolyData>> problemPolygonMap = (loop == 0) ? crossingPolygonsMap
                        : sharedWaySegmentsPolygonsMap;

                List<PolyData> allPolygons = new ArrayList<>(innerPolygons.size() + outerPolygons.size());
                allPolygons.addAll(innerPolygons);
                allPolygons.addAll(outerPolygons);

                for (Entry<List<Way>, List<WaySegment>> entry : crossingWays.entrySet()) {
                    List<Way> ways = entry.getKey();
                    if (ways.size() != 2)
                        continue;
                    PolyData[] crossingPolys = new PolyData[2];
                    boolean allInner = true;
                    for (int i = 0; i < 2; i++) {
                        Way w = ways.get(i);
                        for (int j = 0; j < allPolygons.size(); j++) {
                            PolyData pd = allPolygons.get(j);
                            if (pd.getWayIds().contains(w.getUniqueId())) {
                                crossingPolys[i] = pd;
                                if (j >= innerPolygons.size())
                                    allInner = false;
                                break;
                            }
                        }
                    }
                    boolean samePoly = false;
                    if (crossingPolys[0] != null && crossingPolys[1] != null) {
                        List<PolyData> crossingPolygons = problemPolygonMap.computeIfAbsent(crossingPolys[0], k -> new ArrayList<>());
                        crossingPolygons.add(crossingPolys[1]);
                        if (crossingPolys[0] == crossingPolys[1]) {
                            samePoly = true;
                        }
                    }
                    if (r == createdRelation && loop == 1 && !allInner) {
                        repeatCheck = true;
                    } else if (loop == 0 || samePoly || (loop == 1 && !allInner)) {
                        String msg = loop == 0 ? tr("Intersection between multipolygon ways")
                                : samePoly ? tr("Multipolygon ring contains segment twice")
                                        : tr("Multipolygon outer way shares segment with other ring");
                        errors.add(TestError.builder(this, Severity.ERROR, CROSSING_WAYS)
                                .message(msg)
                                .primitives(Arrays.asList(r, ways.get(0), ways.get(1)))
                                .highlightWaySegments(entry.getValue())
                                .build());
                    }
                }
            }
        }
        return crossingPolygonsMap;
    }

    /**
    * Determine multipolygon ways which are intersecting (crossing without a common node) or sharing one or more way segments.
    * This should only be used for relations with incomplete members.
    * See also {@link CrossingWays}
    * @param r the relation (for error reporting)
     */
    private void findIntersectingWaysIncomplete(Relation r) {
        Set<OsmPrimitive> outerWays = r.getMembers().stream()
                .filter(m -> m.getRole().isEmpty() || "outer".equals(m.getRole()))
                .map(RelationMember::getMember)
                .collect(Collectors.toSet());
        for (int loop = 0; loop < 2; loop++) {
            for (Entry<List<Way>, List<WaySegment>> entry : findIntersectingWays(r, loop == 1).entrySet()) {
                List<Way> ways = entry.getKey();
                if (ways.size() != 2)
                    continue;
                if (loop == 0) {
                errors.add(TestError.builder(this, Severity.ERROR, CROSSING_WAYS)
                        .message(tr("Intersection between multipolygon ways"))
                        .primitives(Arrays.asList(r, ways.get(0), ways.get(1)))
                        .highlightWaySegments(entry.getValue())
                        .build());
                } else if (outerWays.contains(ways.get(0)) || outerWays.contains(ways.get(1))) {
                    errors.add(TestError.builder(this, Severity.ERROR, CROSSING_WAYS)
                            .message(tr("Multipolygon outer way shares segment with other ring"))
                            .primitives(Arrays.asList(r, ways.get(0), ways.get(1)))
                            .highlightWaySegments(entry.getValue()).build());
                }

            }
        }
    }

    /**
     * See {@link CrossingWays}
     * @param r the relation
     * @param findSharedWaySegments true: find shared way segments instead of crossings
     * @return map with crossing ways and the related segments
     */
    private static Map<List<Way>, List<WaySegment>> findIntersectingWays(Relation r, boolean findSharedWaySegments) {
        /* All way segments, grouped by cells */
        final Map<Point2D, List<WaySegment>> cellSegments = new HashMap<>(1000);
        /* The detected crossing ways */
        final Map<List<Way>, List<WaySegment>> crossingWays = new HashMap<>(50);

        for (Way w: r.getMemberPrimitives(Way.class)) {
            if (!w.hasIncompleteNodes()) {
                CrossingWays.findIntersectingWay(w, cellSegments, crossingWays, findSharedWaySegments);
            }
        }
        return crossingWays;
    }

    /**
     * Check if map contains combination of two given polygons.
     * @param problemPolyMap the map
     * @param pd1 1st polygon
     * @param pd2 2nd polygon
     * @return true if the combination of polygons is found in the map
     */
    private static boolean checkProblemMap(Map<PolyData, List<PolyData>> problemPolyMap, PolyData pd1, PolyData pd2) {
        List<PolyData> crossingWithFirst = problemPolyMap.get(pd1);
        if (crossingWithFirst != null && crossingWithFirst.contains(pd2)) {
            return true;
        }
        List<PolyData> crossingWith2nd = problemPolyMap.get(pd2);
        return crossingWith2nd != null && crossingWith2nd.contains(pd1);
    }

    /**
     * Check for:<ul>
     * <li>{@link #WRONG_MEMBER_ROLE}: No useful role for multipolygon member</li>
     * <li>{@link #WRONG_MEMBER_TYPE}: Non-Way in multipolygon</li>
     * </ul>
     * @param r relation
     * @param tmpErrors list that will contain found errors
     * @return true if ways with roles other than inner, outer or empty where found
     */
    private boolean checkMembersAndRoles(Relation r, List<TestError> tmpErrors) {
        boolean hasUnexpectedWayRole = false;
        for (RelationMember rm : r.getMembers()) {
            if (rm.isWay()) {
                if (rm.hasRole() && !rm.hasRole("inner", "outer"))
                    hasUnexpectedWayRole = true;
                if (!rm.hasRole("inner", "outer") || !rm.hasRole()) {
                    tmpErrors.add(TestError.builder(this, Severity.ERROR, WRONG_MEMBER_ROLE)
                            .message(tr("Role for multipolygon way member should be inner or outer"))
                            .primitives(Arrays.asList(r, rm.getMember()))
                            .build());
                }
            } else {
                if (!r.isBoundary() || !rm.hasRole("admin_centre", "label", "subarea", "land_area")) {
                    tmpErrors.add(TestError.builder(this, Severity.WARNING, WRONG_MEMBER_TYPE)
                            .message(r.isBoundary() ? tr("Non-Way in boundary") : tr("Non-Way in multipolygon"))
                            .primitives(Arrays.asList(r, rm.getMember()))
                            .build());
                }
            }
        }
        return hasUnexpectedWayRole;
    }

    private static Collection<? extends OsmPrimitive> combineRelAndPrimitives(Relation r, Collection<? extends OsmPrimitive> primitives) {
        // add multipolygon in order to let user select something and fix the error
        if (!primitives.contains(r)) {
            List<OsmPrimitive> newPrimitives = new ArrayList<>(primitives);
            newPrimitives.add(0, r);
            return newPrimitives;
        } else {
            return primitives;
        }
    }

    /**
     * Check for:<ul>
     * <li>{@link #REPEATED_MEMBER_DIFF_ROLE}: Multipolygon member repeated with different role</li>
     * <li>{@link #REPEATED_MEMBER_SAME_ROLE}: Multipolygon member repeated with same role</li>
     * </ul>
     * @param r relation
     * @return true if repeated members have been detected, false otherwise
     */
    private boolean checkRepeatedWayMembers(Relation r) {
        boolean hasDups = false;
        Map<OsmPrimitive, List<RelationMember>> seenMemberPrimitives = new HashMap<>();
        for (RelationMember rm : r.getMembers()) {
            if (!rm.isWay())
                continue;
            List<RelationMember> list = seenMemberPrimitives.get(rm.getMember());
            if (list == null) {
                list = new ArrayList<>(2);
                seenMemberPrimitives.put(rm.getMember(), list);
            } else {
                hasDups = true;
            }
            list.add(rm);
        }
        if (hasDups) {
            List<OsmPrimitive> repeatedSameRole = new ArrayList<>();
            List<OsmPrimitive> repeatedDiffRole = new ArrayList<>();
            for (Entry<OsmPrimitive, List<RelationMember>> e : seenMemberPrimitives.entrySet()) {
                List<RelationMember> visited = e.getValue();
                if (e.getValue().size() == 1)
                    continue;
                // we found a duplicate member, check if the roles differ
                boolean rolesDiffer = false;
                RelationMember rm = visited.get(0);
                List<OsmPrimitive> primitives = new ArrayList<>();
                for (int i = 1; i < visited.size(); i++) {
                    RelationMember v = visited.get(i);
                    primitives.add(rm.getMember());
                    if (!v.getRole().equals(rm.getRole())) {
                        rolesDiffer = true;
                    }
                }
                if (rolesDiffer) {
                    repeatedDiffRole.addAll(primitives);
                } else {
                    repeatedSameRole.addAll(primitives);
                }
            }
            addRepeatedMemberError(r, repeatedDiffRole, REPEATED_MEMBER_DIFF_ROLE, tr("Multipolygon member repeated with different role"));
            addRepeatedMemberError(r, repeatedSameRole, REPEATED_MEMBER_SAME_ROLE, tr("Multipolygon member repeated with same role"));
        }
        return hasDups;
    }

    private void addRepeatedMemberError(Relation r, List<OsmPrimitive> repeatedMembers, int errorCode, String msg) {
        if (!repeatedMembers.isEmpty()) {
            errors.add(TestError.builder(this, Severity.ERROR, errorCode)
                    .message(msg)
                    .primitives(combineRelAndPrimitives(r, repeatedMembers))
                    .highlight(repeatedMembers)
                    .build());
        }
    }

    @Override
    public Command fixError(TestError testError) {
        if (testError.getCode() == REPEATED_MEMBER_SAME_ROLE) {
            ArrayList<OsmPrimitive> primitives = new ArrayList<>(testError.getPrimitives());
            if (primitives.size() >= 2 && primitives.get(0) instanceof Relation) {
                Relation oldRel = (Relation) primitives.get(0);
                List<OsmPrimitive> repeatedPrims = primitives.subList(1, primitives.size());
                List<RelationMember> oldMembers = oldRel.getMembers();

                List<RelationMember> newMembers = new ArrayList<>();
                HashSet<OsmPrimitive> toRemove = new HashSet<>(repeatedPrims);
                HashSet<OsmPrimitive> found = new HashSet<>(repeatedPrims.size());
                for (RelationMember rm : oldMembers) {
                    if (toRemove.contains(rm.getMember())) {
                        if (found.add(rm.getMember())) {
                            newMembers.add(rm);
                        }
                    } else {
                        newMembers.add(rm);
                    }
                }
                return new ChangeMembersCommand(oldRel, newMembers);
            }
        }
        return null;
    }

    @Override
    public boolean isFixable(TestError testError) {
        return testError.getCode() == REPEATED_MEMBER_SAME_ROLE;
    }

    /**
     * Find nesting levels of polygons. Logic taken from class MultipolygonBuilder, uses different structures.
     */
    private static class PolygonLevelFinder {
        private final Set<Node> sharedNodes;

        PolygonLevelFinder(Set<Node> sharedNodes) {
            this.sharedNodes = sharedNodes;
        }

        List<PolygonLevel> findOuterWays(List<PolyData> allPolygons) {
            return findOuterWaysRecursive(0, allPolygons);
        }

        private List<PolygonLevel> findOuterWaysRecursive(int level, List<PolyData> polygons) {
            final List<PolygonLevel> result = new ArrayList<>();

            for (PolyData pd : polygons) {
                processOuterWay(level, polygons, result, pd);
            }

            return result;
        }

        private void processOuterWay(int level, List<PolyData> polygons, List<PolygonLevel> result, PolyData pd) {
            List<PolyData> inners = findInnerWaysCandidates(pd, polygons);

            if (inners != null) {
                //add new outer polygon
                PolygonLevel pol = new PolygonLevel(pd, level);

                //process inner ways
                if (!inners.isEmpty()) {
                    List<PolygonLevel> innerList = findOuterWaysRecursive(level + 1, inners);
                    result.addAll(innerList);
                }

                result.add(pol);
            }
        }

        /**
         * Check if polygon is an out-most ring, if so, collect the inners
         * @param outerCandidate polygon which is checked
         * @param polygons all polygons
         * @return null if outerCandidate is inside any other polygon, else a list of inner polygons (which might be empty)
         */
        private List<PolyData> findInnerWaysCandidates(PolyData outerCandidate, List<PolyData> polygons) {
            List<PolyData> innerCandidates = new ArrayList<>();

            for (PolyData inner : polygons) {
                if (inner == outerCandidate) {
                    continue;
                }
                if (!outerCandidate.getBounds().intersects(inner.getBounds())) {
                    continue;
                }
                boolean useIntersectionTest = false;
                Node unsharedOuterNode = null;
                Node unsharedInnerNode = getNonIntersectingNode(outerCandidate, inner);
                if (unsharedInnerNode != null) {
                    if (checkIfNodeIsInsidePolygon(unsharedInnerNode, outerCandidate)) {
                        innerCandidates.add(inner);
                    } else {
                        // inner is not inside outerCandidate, check if it contains outerCandidate
                        unsharedOuterNode = getNonIntersectingNode(inner, outerCandidate);
                        if (unsharedOuterNode != null) {
                            if (checkIfNodeIsInsidePolygon(unsharedOuterNode, inner)) {
                                return null; // outer is inside inner
                            }
                        } else {
                            useIntersectionTest = true;
                        }
                    }
                } else {
                    // all nodes of inner are also nodes of outerCandidate
                    unsharedOuterNode = getNonIntersectingNode(inner, outerCandidate);
                    if (unsharedOuterNode == null) {
                        return null; // all nodes shared -> same ways, maybe different direction
                    } else {
                        if (checkIfNodeIsInsidePolygon(unsharedOuterNode, inner)) {
                            return null; // outer is inside inner
                        } else {
                            useIntersectionTest = true;
                        }
                    }
                }
                if (useIntersectionTest) {
                    PolygonIntersection res = Geometry.polygonIntersection(inner.getNodes(), outerCandidate.getNodes());
                    if (res == PolygonIntersection.FIRST_INSIDE_SECOND)
                        innerCandidates.add(inner);
                    else if (res == PolygonIntersection.SECOND_INSIDE_FIRST)
                        return null;
                }
            }
            return innerCandidates;
        }

        /**
         * Find node of pd2 which is not an intersection node with pd1.
         * @param pd1 1st polygon
         * @param pd2 2nd polygon
         * @return node of pd2 which is not an intersection node with pd1 or null if none is found
         */
        private Node getNonIntersectingNode(PolyData pd1, PolyData pd2) {
            return pd2.getNodes().stream()
                    .filter(n -> !sharedNodes.contains(n) || !pd1.getNodes().contains(n))
                    .findFirst().orElse(null);
        }
    }

    /**
     * Create a multipolygon relation from the given ways and test it.
     * @param ways the collection of ways
     * @return a pair of a {@link Multipolygon} instance and the relation.
     * @since 15160
     */
    public Relation makeFromWays(Collection<Way> ways) {
        Relation r = new Relation();
        createdRelation = r;
        r.put("type", "multipolygon");
        for (Way w : ways) {
            r.addMember(new RelationMember("", w));
        }
        do {
            repeatCheck = false;
            errors.clear();
            Multipolygon polygon = null;
            boolean hasRepeatedMembers = checkRepeatedWayMembers(r);
            if (!hasRepeatedMembers) {
                polygon = new Multipolygon(r);
                // don't check style consistency here
                checkGeometryAndRoles(r, polygon);
            }
            createdRelation = null; // makes sure that repeatCheck is only set once
        } while (repeatCheck);
        errors.removeIf(e->e.getSeverity() == Severity.OTHER);
        return r;
    }

}
