// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.CreateMultipolygonAction;
import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.paint.relations.Multipolygon;
import org.openstreetmap.josm.data.osm.visitor.paint.relations.Multipolygon.PolyData;
import org.openstreetmap.josm.data.osm.visitor.paint.relations.Multipolygon.PolyData.Intersection;
import org.openstreetmap.josm.data.osm.visitor.paint.relations.MultipolygonCache;
import org.openstreetmap.josm.data.validation.OsmValidator;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.Test;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.gui.DefaultNameFormatter;
import org.openstreetmap.josm.gui.mappaint.ElemStyles;
import org.openstreetmap.josm.gui.mappaint.MapPaintStyles;
import org.openstreetmap.josm.gui.mappaint.styleelement.AreaElement;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.tools.Pair;

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
    /** No outer way for multipolygon */
    public static final int MISSING_OUTER_WAY = 1604;
    /** Multipolygon inner way is outside */
    public static final int INNER_WAY_OUTSIDE = 1605;
    /** Intersection between multipolygon ways */
    public static final int CROSSING_WAYS = 1606;
    /** Style for outer way mismatches / With the currently used mappaint style(s) the style for outer way mismatches the area style */
    public static final int OUTER_STYLE_MISMATCH = 1607;
    /** With the currently used mappaint style the style for inner way equals the multipolygon style */
    public static final int INNER_STYLE_MISMATCH = 1608;
    /** Area style way is not closed */
    public static final int NOT_CLOSED = 1609;
    /** No area style for multipolygon */
    public static final int NO_STYLE = 1610;
    /** Multipolygon relation should be tagged with area tags and not the outer way(s) */
    public static final int NO_STYLE_POLYGON = 1611;
    /** Area style on outer way */
    public static final int OUTER_STYLE = 1613;
    /** Multipolygon member repeated (same primitive, same role */
    public static final int REPEATED_MEMBER_SAME_ROLE = 1614;
    /** Multipolygon member repeated (same primitive, different role) */
    public static final int REPEATED_MEMBER_DIFF_ROLE = 1615;

    private static volatile ElemStyles styles;

    private final Set<String> keysCheckedByAnotherTest = new HashSet<>();

    /**
     * Constructs a new {@code MultipolygonTest}.
     */
    public MultipolygonTest() {
        super(tr("Multipolygon"),
                tr("This test checks if multipolygons are valid."));
    }

    @Override
    public void initialize() {
        styles = MapPaintStyles.getStyles();
    }

    @Override
    public void startTest(ProgressMonitor progressMonitor) {
        super.startTest(progressMonitor);
        keysCheckedByAnotherTest.clear();
        for (Test t : OsmValidator.getEnabledTests(false)) {
            if (t instanceof UnclosedWays) {
                keysCheckedByAnotherTest.addAll(((UnclosedWays) t).getCheckedKeys());
                break;
            }
        }
    }

    @Override
    public void endTest() {
        keysCheckedByAnotherTest.clear();
        super.endTest();
    }

    private static GeneralPath createPath(List<Node> nodes) {
        GeneralPath result = new GeneralPath();
        result.moveTo((float) nodes.get(0).getCoor().lat(), (float) nodes.get(0).getCoor().lon());
        for (int i = 1; i < nodes.size(); i++) {
            Node n = nodes.get(i);
            result.lineTo((float) n.getCoor().lat(), (float) n.getCoor().lon());
        }
        return result;
    }

    private static List<GeneralPath> createPolygons(List<Multipolygon.PolyData> joinedWays) {
        List<GeneralPath> result = new ArrayList<>();
        for (Multipolygon.PolyData way : joinedWays) {
            result.add(createPath(way.getNodes()));
        }
        return result;
    }

    private static Intersection getPolygonIntersection(GeneralPath outer, List<Node> inner) {
        boolean inside = false;
        boolean outside = false;

        for (Node n : inner) {
            boolean contains = outer.contains(n.getCoor().lat(), n.getCoor().lon());
            inside = inside | contains;
            outside = outside | !contains;
            if (inside & outside) {
                return Intersection.CROSSING;
            }
        }

        return inside ? Intersection.INSIDE : Intersection.OUTSIDE;
    }

    @Override
    public void visit(Way w) {
        if (!w.isArea() && ElemStyles.hasOnlyAreaElemStyle(w)) {
            List<Node> nodes = w.getNodes();
            if (nodes.isEmpty()) return; // fix zero nodes bug
            for (String key : keysCheckedByAnotherTest) {
                if (w.hasKey(key)) {
                    return;
                }
            }
            errors.add(TestError.builder(this, Severity.WARNING, NOT_CLOSED)
                    .message(tr("Area style way is not closed"))
                    .primitives(w)
                    .highlight(Arrays.asList(nodes.get(0), nodes.get(nodes.size() - 1)))
                    .build());
        }
    }

    @Override
    public void visit(Relation r) {
        if (r.isMultipolygon()) {
            checkMembersAndRoles(r);
            checkOuterWay(r);
            checkRepeatedWayMembers(r);

            // Rest of checks is only for complete multipolygons
            if (!r.hasIncompleteMembers()) {
                Multipolygon polygon = new Multipolygon(r);

                // Create new multipolygon using the logics from CreateMultipolygonAction and see if roles match.
                checkMemberRoleCorrectness(r);
                checkStyleConsistency(r, polygon);
                checkGeometry(r, polygon);
            }
        }
    }

    /**
     * Checks that multipolygon has at least an outer way:<ul>
     * <li>{@link #MISSING_OUTER_WAY}: No outer way for multipolygon</li>
     * </ul>
     * @param r relation
     */
    private void checkOuterWay(Relation r) {
        boolean hasOuterWay = false;
        for (RelationMember m : r.getMembers()) {
            if ("outer".equals(m.getRole())) {
                hasOuterWay = true;
                break;
            }
        }
        if (!hasOuterWay) {
            errors.add(TestError.builder(this, Severity.WARNING, MISSING_OUTER_WAY)
                    .message(tr("No outer way for multipolygon"))
                    .primitives(r)
                    .build());
        }
    }

    /**
     * Create new multipolygon using the logics from CreateMultipolygonAction and see if roles match:<ul>
     * <li>{@link #WRONG_MEMBER_ROLE}: Role for ''{0}'' should be ''{1}''</li>
     * </ul>
     * @param r relation
     */
    private void checkMemberRoleCorrectness(Relation r) {
        final Pair<Relation, Relation> newMP = CreateMultipolygonAction.createMultipolygonRelation(r.getMemberPrimitives(Way.class), false);
        if (newMP != null) {
            for (RelationMember member : r.getMembers()) {
                final Collection<RelationMember> memberInNewMP = newMP.b.getMembersFor(Collections.singleton(member.getMember()));
                if (memberInNewMP != null && !memberInNewMP.isEmpty()) {
                    final String roleInNewMP = memberInNewMP.iterator().next().getRole();
                    if (!member.getRole().equals(roleInNewMP)) {
                        errors.add(TestError.builder(this, Severity.WARNING, WRONG_MEMBER_ROLE)
                                .message(RelationChecker.ROLE_VERIF_PROBLEM_MSG,
                                        marktr("Role for ''{0}'' should be ''{1}''"),
                                        member.getMember().getDisplayName(DefaultNameFormatter.getInstance()), roleInNewMP)
                                .primitives(addRelationIfNeeded(r, member.getMember()))
                                .highlight(member.getMember())
                                .build());
                    }
                }
            }
        }
    }

    /**
     * Various style-related checks:<ul>
     * <li>{@link #NO_STYLE_POLYGON}: Multipolygon relation should be tagged with area tags and not the outer way</li>
     * <li>{@link #INNER_STYLE_MISMATCH}: With the currently used mappaint style the style for inner way equals the multipolygon style</li>
     * <li>{@link #OUTER_STYLE_MISMATCH}: Style for outer way mismatches</li>
     * <li>{@link #OUTER_STYLE}: Area style on outer way</li>
     * </ul>
     * @param r relation
     * @param polygon multipolygon
     */
    private void checkStyleConsistency(Relation r, Multipolygon polygon) {
        if (styles != null && !"boundary".equals(r.get("type"))) {
            AreaElement area = ElemStyles.getAreaElemStyle(r, false);
            boolean areaStyle = area != null;
            // If area style was not found for relation then use style of ways
            if (area == null) {
                for (Way w : polygon.getOuterWays()) {
                    area = ElemStyles.getAreaElemStyle(w, true);
                    if (area != null) {
                        break;
                    }
                }
                if (area == null) {
                    errors.add(TestError.builder(this, Severity.OTHER, NO_STYLE)
                            .message(tr("No area style for multipolygon"))
                            .primitives(r)
                            .build());
                } else {
                    /* old style multipolygon - solve: copy tags from outer way to multipolygon */
                    errors.add(TestError.builder(this, Severity.WARNING, NO_STYLE_POLYGON)
                            .message(trn("Multipolygon relation should be tagged with area tags and not the outer way",
                                    "Multipolygon relation should be tagged with area tags and not the outer ways",
                                    polygon.getOuterWays().size()))
                            .primitives(r)
                            .build());
                }
            }

            if (area != null) {
                for (Way wInner : polygon.getInnerWays()) {
                    AreaElement areaInner = ElemStyles.getAreaElemStyle(wInner, false);

                    if (areaInner != null && area.equals(areaInner)) {
                        errors.add(TestError.builder(this, Severity.OTHER, INNER_STYLE_MISMATCH)
                                .message(tr("With the currently used mappaint style the style for inner way equals the multipolygon style"))
                                .primitives(addRelationIfNeeded(r, wInner))
                                .highlight(wInner)
                                .build());
                    }
                }
                for (Way wOuter : polygon.getOuterWays()) {
                    AreaElement areaOuter = ElemStyles.getAreaElemStyle(wOuter, false);
                    if (areaOuter != null) {
                        if (!area.equals(areaOuter)) {
                            String message = !areaStyle ? tr("Style for outer way mismatches")
                                    : tr("With the currently used mappaint style(s) the style for outer way mismatches the area style");
                            errors.add(TestError.builder(this, Severity.OTHER, OUTER_STYLE_MISMATCH)
                                    .message(message)
                                    .primitives(addRelationIfNeeded(r, wOuter))
                                    .highlight(wOuter)
                                    .build());
                        } else if (areaStyle) { /* style on outer way of multipolygon, but equal to polygon */
                            errors.add(TestError.builder(this, Severity.WARNING, OUTER_STYLE)
                                    .message(tr("Area style on outer way"))
                                    .primitives(addRelationIfNeeded(r, wOuter))
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
    private void checkGeometry(Relation r, Multipolygon polygon) {
        List<Node> openNodes = polygon.getOpenEnds();
        if (!openNodes.isEmpty()) {
            errors.add(TestError.builder(this, Severity.WARNING, NON_CLOSED_WAY)
                    .message(tr("Multipolygon is not closed"))
                    .primitives(addRelationIfNeeded(r, openNodes))
                    .highlight(openNodes)
                    .build());
        }

        // For painting is used Polygon class which works with ints only. For validation we need more precision
        List<PolyData> innerPolygons = polygon.getInnerPolygons();
        List<PolyData> outerPolygons = polygon.getOuterPolygons();
        List<GeneralPath> innerPolygonsPaths = innerPolygons.isEmpty() ? Collections.<GeneralPath>emptyList() : createPolygons(innerPolygons);
        List<GeneralPath> outerPolygonsPaths = createPolygons(outerPolygons);
        for (int i = 0; i < outerPolygons.size(); i++) {
            PolyData pdOuter = outerPolygons.get(i);
            // Check for intersection between outer members
            for (int j = i+1; j < outerPolygons.size(); j++) {
                checkCrossingWays(r, outerPolygons, outerPolygonsPaths, pdOuter, j);
            }
        }
        for (int i = 0; i < innerPolygons.size(); i++) {
            PolyData pdInner = innerPolygons.get(i);
            // Check for intersection between inner members
            for (int j = i+1; j < innerPolygons.size(); j++) {
                checkCrossingWays(r, innerPolygons, innerPolygonsPaths, pdInner, j);
            }
            // Check for intersection between inner and outer members
            boolean outside = true;
            for (int o = 0; o < outerPolygons.size(); o++) {
                outside &= checkCrossingWays(r, outerPolygons, outerPolygonsPaths, pdInner, o) == Intersection.OUTSIDE;
            }
            if (outside) {
                errors.add(TestError.builder(this, Severity.WARNING, INNER_WAY_OUTSIDE)
                        .message(tr("Multipolygon inner way is outside"))
                        .primitives(r)
                        .highlightNodePairs(Collections.singletonList(pdInner.getNodes()))
                        .build());
            }
        }
    }

    private Intersection checkCrossingWays(Relation r, List<PolyData> polygons, List<GeneralPath> polygonsPaths, PolyData pd, int idx) {
        Intersection intersection = getPolygonIntersection(polygonsPaths.get(idx), pd.getNodes());
        if (intersection == Intersection.CROSSING) {
            PolyData pdOther = polygons.get(idx);
            if (pdOther != null) {
                errors.add(TestError.builder(this, Severity.WARNING, CROSSING_WAYS)
                        .message(tr("Intersection between multipolygon ways"))
                        .primitives(r)
                        .highlightNodePairs(Arrays.asList(pd.getNodes(), pdOther.getNodes()))
                        .build());
            }
        }
        return intersection;
    }

    /**
     * Check for:<ul>
     * <li>{@link #WRONG_MEMBER_ROLE}: No useful role for multipolygon member</li>
     * <li>{@link #WRONG_MEMBER_TYPE}: Non-Way in multipolygon</li>
     * </ul>
     * @param r relation
     */
    private void checkMembersAndRoles(Relation r) {
        for (RelationMember rm : r.getMembers()) {
            if (rm.isWay()) {
                if (!(rm.hasRole("inner", "outer") || !rm.hasRole())) {
                    errors.add(TestError.builder(this, Severity.WARNING, WRONG_MEMBER_ROLE)
                            .message(tr("No useful role for multipolygon member"))
                            .primitives(addRelationIfNeeded(r, rm.getMember()))
                            .build());
                }
            } else {
                if (!rm.hasRole("admin_centre", "label", "subarea", "land_area")) {
                    errors.add(TestError.builder(this, Severity.WARNING, WRONG_MEMBER_TYPE)
                            .message(tr("Non-Way in multipolygon"))
                            .primitives(addRelationIfNeeded(r, rm.getMember()))
                            .build());
                }
            }
        }
    }

    private static Collection<? extends OsmPrimitive> addRelationIfNeeded(Relation r, OsmPrimitive primitive) {
        return addRelationIfNeeded(r, Collections.singleton(primitive));
    }

    private static Collection<? extends OsmPrimitive> addRelationIfNeeded(Relation r, Collection<? extends OsmPrimitive> primitives) {
        // Fix #8212 : if the error references only incomplete primitives,
        // add multipolygon in order to let user select something and fix the error
        if (!primitives.contains(r)) {
            for (OsmPrimitive p : primitives) {
                if (!p.isIncomplete()) {
                    return primitives;
                }
            }
            // Diamond operator does not work with Java 9 here
            @SuppressWarnings("unused")
            List<OsmPrimitive> newPrimitives = new ArrayList<OsmPrimitive>(primitives);
            newPrimitives.add(0, r);
            return newPrimitives;
        } else {
            return primitives;
        }
    }

    /**
     * Check for:<ul>
     * <li>{@link #REPEATED_MEMBER_DIFF_ROLE}: Multipolygon member(s) repeated with different role</li>
     * <li>{@link #REPEATED_MEMBER_SAME_ROLE}: Multipolygon member(s) repeated with same role</li>
     * </ul>
     * @param r relation
     * @return true if repeated members have been detected, false otherwise
     */
    private boolean checkRepeatedWayMembers(Relation r) {
        boolean hasDups = false;
        Map<OsmPrimitive, List<RelationMember>> seenMemberPrimitives = new HashMap<>();
        for (RelationMember rm : r.getMembers()) {
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
            addRepeatedMemberError(r, repeatedDiffRole, REPEATED_MEMBER_DIFF_ROLE, tr("Multipolygon member(s) repeated with different role"));
            addRepeatedMemberError(r, repeatedSameRole, REPEATED_MEMBER_SAME_ROLE, tr("Multipolygon member(s) repeated with same role"));
        }
        return hasDups;
    }

    private void addRepeatedMemberError(Relation r, List<OsmPrimitive> repeatedMembers, int errorCode, String msg) {
        if (!repeatedMembers.isEmpty()) {
            List<OsmPrimitive> prims = new ArrayList<>(1 + repeatedMembers.size());
            prims.add(r);
            prims.addAll(repeatedMembers);
            errors.add(TestError.builder(this, Severity.WARNING, errorCode)
                    .message(msg)
                    .primitives(prims)
                    .highlight(repeatedMembers)
                    .build());
        }
    }

    @Override
    public Command fixError(TestError testError) {
        if (testError.getCode() == REPEATED_MEMBER_SAME_ROLE) {
            ArrayList<OsmPrimitive> primitives = new ArrayList<>(testError.getPrimitives());
            if (primitives.size() >= 2) {
                if (primitives.get(0) instanceof Relation) {
                    Relation oldRel = (Relation) primitives.get(0);
                    Relation newRel = new Relation(oldRel);
                    List<OsmPrimitive> repeatedPrims = primitives.subList(1, primitives.size());
                    List<RelationMember> oldMembers = oldRel.getMembers();

                    List<RelationMember> newMembers = new ArrayList<>();
                    HashSet<OsmPrimitive> toRemove = new HashSet<>(repeatedPrims);
                    HashSet<OsmPrimitive> found = new HashSet<>(repeatedPrims.size());
                    for (RelationMember rm : oldMembers) {
                        if (toRemove.contains(rm.getMember())) {
                            if (found.contains(rm.getMember()) == false) {
                                found.add(rm.getMember());
                                newMembers.add(rm);
                            }
                        } else {
                            newMembers.add(rm);
                        }
                    }
                    newRel.setMembers(newMembers);
                    return new ChangeCommand (oldRel, newRel);
                }
            }
        }
        return null;
    }

    @Override
    public boolean isFixable(TestError testError) {
        if (testError.getCode() == REPEATED_MEMBER_SAME_ROLE)
            return true;
        return false;
    }
}
