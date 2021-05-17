// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.openstreetmap.josm.data.validation.tests.CrossingWays.HIGHWAY;
import static org.openstreetmap.josm.data.validation.tests.CrossingWays.RAILWAY;
import static org.openstreetmap.josm.data.validation.tests.CrossingWays.WATERWAY;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.WaySegment;
import org.openstreetmap.josm.data.preferences.ListProperty;
import org.openstreetmap.josm.data.preferences.sources.ValidatorPrefHelper;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.Test;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.MultiMap;
import org.openstreetmap.josm.tools.Pair;

/**
 * Tests if there are overlapping ways.
 *
 * @author frsantos
 * @since 3669
 */
public class OverlappingWays extends Test {

    /** Bag of all way segments */
    private MultiMap<Pair<Node, Node>, WaySegment> nodePairs;

    private boolean onlyKnownLinear;
    private boolean includeOther;
    private boolean ignoreLayer;

    protected static final int OVERLAPPING_HIGHWAY = 101;
    protected static final int OVERLAPPING_RAILWAY = 102;
    protected static final int OVERLAPPING_WAY = 103;
    protected static final int OVERLAPPING_WATERWAY = 104;
    protected static final int OVERLAPPING_HIGHWAY_AREA = 111;
    protected static final int OVERLAPPING_RAILWAY_AREA = 112;
    protected static final int OVERLAPPING_WAY_AREA = 113;
    protected static final int OVERLAPPING_WATERWAY_AREA = 114;
    protected static final int DUPLICATE_WAY_SEGMENT = 121;
    protected static final int OVERLAPPING_HIGHWAY_LINEAR_WAY = 131;
    protected static final int OVERLAPPING_RAILWAY_LINEAR_WAY = 132;
    protected static final int OVERLAPPING_WATERWAY_LINEAR_WAY = 133;

    protected static final ListProperty IGNORED_KEYS = new ListProperty(
            "overlapping-ways.ignored-keys", Arrays.asList(
                    "barrier", "indoor", "building", "building:part", "historic:building", "demolished:building",
                    "removed:building", "disused:building", "abandoned:building", "proposed:building", "man_made"));
    protected static final Predicate<OsmPrimitive> IGNORED = primitive ->
            IGNORED_KEYS.get().stream().anyMatch(primitive::hasKey) || primitive.hasTag("tourism", "camp_site");

    /** Constructor */
    public OverlappingWays() {
        super(tr("Overlapping ways"),
                tr("This test checks that a connection between two nodes "
                        + "is not used by more than one way."));
    }

    @Override
    public void startTest(ProgressMonitor monitor) {
        super.startTest(monitor);
        nodePairs = new MultiMap<>(1000);
        includeOther = isBeforeUpload ? ValidatorPrefHelper.PREF_OTHER_UPLOAD.get() : ValidatorPrefHelper.PREF_OTHER.get();
        onlyKnownLinear = Config.getPref().getBoolean("overlapping-ways.only-known-linear", true);
        ignoreLayer = Config.getPref().getBoolean("overlapping-ways.ignore-layer", false);
    }

    private static boolean parentMultipolygonConcernsArea(OsmPrimitive p) {
        return p.referrers(Relation.class)
                .anyMatch(Relation::isMultipolygon);
    }

    @Override
    public void endTest() {
        Map<List<Way>, Set<WaySegment>> seenWays = new HashMap<>(500);

        for (Set<WaySegment> duplicated : nodePairs.values()) {
            if (duplicated.size() <= 1)
                continue;
            if (ignoreLayer) {
                analyseOverlaps(duplicated, seenWays);
            } else {
                // group by layer tag value (which is very likely null)
                Map<String, Set<WaySegment>> grouped = new HashMap<>();
                for (WaySegment ws : duplicated) {
                    // order in set is important
                    grouped.computeIfAbsent(OsmUtils.getLayer(ws.getWay()), k -> new LinkedHashSet<>()).add(ws);
                }
                grouped.values().forEach(group -> analyseOverlaps(group, seenWays));
            }
        }
        nodePairs = null;

        super.endTest();
    }

    private void analyseOverlaps(Set<WaySegment> duplicated, Map<List<Way>, Set<WaySegment>> seenWays) {
        int ways = duplicated.size();
        if (ways <= 1)
            return;

        List<Way> currentWays = duplicated.stream().map(ws -> ws.getWay()).collect(Collectors.toList());
        Collection<WaySegment> highlight;
        if ((highlight = seenWays.get(currentWays)) != null) {
            /* this combination of ways was seen before, just add highlighted segment */
            highlight.addAll(duplicated);
        } else {
            int countHighway = 0;
            int countRailway = 0;
            int countWaterway = 0;
            int countOther = 0;
            int numAreas = 0;
            for (WaySegment ws : duplicated) {
                boolean isArea = ws.getWay().concernsArea();
                if (ws.getWay().hasKey(HIGHWAY)) {
                    if (!isArea) {
                        countHighway++;
                    }
                } else if (ws.getWay().hasKey(RAILWAY)) {
                    if (!isArea) {
                        countRailway++;
                    }
                } else if (ws.getWay().hasKey(WATERWAY)) {
                    if (!isArea) {
                        countWaterway++;
                    }
                } else {
                    if (ws.getWay().getInterestingTags().isEmpty() && parentMultipolygonConcernsArea(ws.getWay()))
                        isArea = true;
                    if (!isArea && isOtherLinear(ws.getWay())) {
                        countOther++;
                    }
                }
                if (isArea) {
                    numAreas++;
                }
            }
            if (numAreas == ways) {
                // no linear object, we don't care when areas share segments
                return;
            }


            // If two or more of the overlapping ways are highways or railways mark a separate error
            String errortype;
            int type;
            int allKnownLinear = countHighway + countRailway + countWaterway + countOther;
            final Severity severity;
            if (countHighway > 1) {
                errortype = tr("Overlapping highways");
                type = OVERLAPPING_HIGHWAY;
                severity = Severity.ERROR;
            } else if (countRailway > 1) {
                errortype = tr("Overlapping railways");
                type = OVERLAPPING_RAILWAY;
                severity = Severity.ERROR;
            } else if (countWaterway > 1) {
                errortype = tr("Overlapping waterways");
                type = OVERLAPPING_WATERWAY;
                severity = Severity.ERROR;
            } else if (countHighway > 0 && countHighway < allKnownLinear) {
                errortype = tr("Highway shares segment with linear way");
                type = OVERLAPPING_HIGHWAY_LINEAR_WAY;
                severity = Severity.WARNING;
            } else if (countRailway > 0 && countRailway < allKnownLinear) {
                errortype = tr("Railway shares segment with linear way");
                type = OVERLAPPING_HIGHWAY_LINEAR_WAY;
                severity = Severity.WARNING;
            } else if (countWaterway > 0 && countWaterway < allKnownLinear) {
                errortype = tr("Waterway shares segment with linear way");
                type = OVERLAPPING_WATERWAY_LINEAR_WAY;
                severity = Severity.WARNING;
            } else if (!includeOther || onlyKnownLinear) {
                return;
            } else if (countHighway > 0) {
                errortype = tr("Highway shares segment with other way");
                type = OVERLAPPING_HIGHWAY_AREA;
                severity = Severity.OTHER;
            } else if (countRailway > 0) {
                errortype = tr("Railway shares segment with other way");
                type = OVERLAPPING_RAILWAY_AREA;
                severity = Severity.OTHER;
            } else if (countWaterway > 0) {
                errortype = tr("Waterway shares segment with other way");
                type = OVERLAPPING_WATERWAY_AREA;
                severity = Severity.OTHER;
            } else {
                errortype = tr("Ways share segment");
                type = OVERLAPPING_WAY;
                severity = Severity.OTHER;
            }

            List<OsmPrimitive> prims = new ArrayList<>(currentWays);
            errors.add(TestError.builder(this, severity, type)
                    .message(errortype)
                    .primitives(prims)
                    .highlightWaySegments(duplicated)
                    .build());
            seenWays.put(currentWays, duplicated);
        }
    }

    private static boolean isOtherLinear(Way way) {
        // it is assumed that area=* was evaluated before and is false
        return (way.hasKey("barrier", "addr:interpolation", "route", "ford")
                || way.hasTag("natural", "tree_row", "cliff", "ridge")
                || way.hasTag("power", "line", "minor_line", "cable", "portal")
                || way.hasTag("man_made", "pipeline"));
    }

    protected static Set<WaySegment> checkDuplicateWaySegment(Way w) {
        // test for ticket #4959
        Set<WaySegment> segments = new TreeSet<>((o1, o2) -> {
            final List<Node> n1 = Arrays.asList(o1.getFirstNode(), o1.getSecondNode());
            final List<Node> n2 = Arrays.asList(o2.getFirstNode(), o2.getSecondNode());
            Collections.sort(n1);
            Collections.sort(n2);
            final int first = n1.get(0).compareTo(n2.get(0));
            final int second = n1.get(1).compareTo(n2.get(1));
            return first != 0 ? first : second;
        });
        final Set<WaySegment> duplicateWaySegments = new HashSet<>();

        for (int i = 0; i < w.getNodesCount() - 1; i++) {
            final WaySegment segment = new WaySegment(w, i);
            final boolean wasInSet = !segments.add(segment);
            if (wasInSet) {
                duplicateWaySegments.add(segment);
            }
        }
        return duplicateWaySegments;
    }

    @Override
    public void visit(Way w) {

        final Set<WaySegment> duplicateWaySegment = checkDuplicateWaySegment(w);
        if (!duplicateWaySegment.isEmpty()) {
            errors.add(TestError.builder(this, Severity.ERROR, DUPLICATE_WAY_SEGMENT)
                    .message(tr("Way contains segment twice"))
                    .primitives(w)
                    .highlightWaySegments(duplicateWaySegment)
                    .build());
            return;
        }

        if (IGNORED.test(w))
            return;

        if (onlyKnownLinear && (w.concernsArea() || w.getInterestingTags().isEmpty()))
            return;

        Node lastN = null;
        int i = -2;
        for (Node n : w.getNodes()) {
            i++;
            if (lastN == null) {
                lastN = n;
                continue;
            }
            nodePairs.put(Pair.sort(new Pair<>(lastN, n)),
                    new WaySegment(w, i));
            lastN = n;
        }
    }
}
