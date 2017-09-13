// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.openstreetmap.josm.data.validation.tests.CrossingWays.HIGHWAY;
import static org.openstreetmap.josm.data.validation.tests.CrossingWays.RAILWAY;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.WaySegment;
import org.openstreetmap.josm.data.preferences.ListProperty;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.Test;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.tools.MultiMap;
import org.openstreetmap.josm.tools.Pair;

/**
 * Tests if there are overlapping ways.
 *
 * @author frsantos
 */
public class OverlappingWays extends Test {

    /** Bag of all way segments */
    private MultiMap<Pair<Node, Node>, WaySegment> nodePairs;

    protected static final int OVERLAPPING_HIGHWAY = 101;
    protected static final int OVERLAPPING_RAILWAY = 102;
    protected static final int OVERLAPPING_WAY = 103;
    protected static final int OVERLAPPING_HIGHWAY_AREA = 111;
    protected static final int OVERLAPPING_RAILWAY_AREA = 112;
    protected static final int OVERLAPPING_WAY_AREA = 113;
    protected static final int OVERLAPPING_AREA = 120;
    protected static final int DUPLICATE_WAY_SEGMENT = 121;

    protected static final ListProperty IGNORED_KEYS = new ListProperty(
            "overlapping-ways.ignored-keys", Arrays.asList("barrier", "building", "historic:building", "man_made"));

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
    }

    private static boolean parentMultipolygonConcernsArea(OsmPrimitive p) {
        for (Relation r : OsmPrimitive.getFilteredList(p.getReferrers(), Relation.class)) {
            if (r.concernsArea()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void endTest() {
        Map<List<Way>, Set<WaySegment>> seenWays = new HashMap<>(500);

        Collection<TestError> preliminaryErrors = new ArrayList<>();
        for (Set<WaySegment> duplicated : nodePairs.values()) {
            int ways = duplicated.size();

            if (ways > 1) {
                List<OsmPrimitive> prims = new ArrayList<>();
                List<Way> currentWays = new ArrayList<>();
                Collection<WaySegment> highlight;
                int highway = 0;
                int railway = 0;
                int area = 0;

                for (WaySegment ws : duplicated) {
                    if (ws.way.hasKey(HIGHWAY)) {
                        highway++;
                    } else if (ws.way.hasKey(RAILWAY)) {
                        railway++;
                    }
                    Boolean ar = OsmUtils.getOsmBoolean(ws.way.get("area"));
                    if (ar != null && ar) {
                        area++;
                    }
                    if (ws.way.concernsArea() || parentMultipolygonConcernsArea(ws.way)) {
                        area++;
                        ways--;
                    }

                    prims.add(ws.way);
                    currentWays.add(ws.way);
                }
                // These ways not seen before
                // If two or more of the overlapping ways are highways or railways mark a separate error
                if ((highlight = seenWays.get(currentWays)) == null) {
                    String errortype;
                    int type;

                    if (area > 0) {
                        if (ways == 0 || duplicated.size() == area) {
                            errortype = tr("Areas share segment");
                            type = OVERLAPPING_AREA;
                        } else if (highway == ways) {
                            errortype = tr("Highways share segment with area");
                            type = OVERLAPPING_HIGHWAY_AREA;
                        } else if (railway == ways) {
                            errortype = tr("Railways share segment with area");
                            type = OVERLAPPING_RAILWAY_AREA;
                        } else {
                            errortype = tr("Ways share segment with area");
                            type = OVERLAPPING_WAY_AREA;
                        }
                    } else if (highway == ways) {
                        errortype = tr("Overlapping highways");
                        type = OVERLAPPING_HIGHWAY;
                    } else if (railway == ways) {
                        errortype = tr("Overlapping railways");
                        type = OVERLAPPING_RAILWAY;
                    } else {
                        errortype = tr("Overlapping ways");
                        type = OVERLAPPING_WAY;
                    }

                    Severity severity = type < OVERLAPPING_HIGHWAY_AREA ? Severity.WARNING : Severity.OTHER;
                    preliminaryErrors.add(TestError.builder(this, severity, type)
                            .message(errortype)
                            .primitives(prims)
                            .highlightWaySegments(duplicated)
                            .build());
                    seenWays.put(currentWays, duplicated);
                } else { /* way seen, mark highlight layer only */
                    highlight.addAll(duplicated);
                }
            }
        }

        // see ticket #9598 - only report if at least 3 segments are shared, except for overlapping ways, i.e warnings (see #9820)
        for (TestError error : preliminaryErrors) {
            if (error.getSeverity().equals(Severity.WARNING) || error.getHighlighted().size() / error.getPrimitives().size() >= 3) {
                boolean ignore = false;
                for (String ignoredKey : IGNORED_KEYS.get()) {
                    if (error.getPrimitives().stream().anyMatch(p -> p.hasKey(ignoredKey))) {
                        ignore = true;
                        break;
                    }
                }
                if (!ignore) {
                    errors.add(error);
                }
            }
        }

        super.endTest();
        nodePairs = null;
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
        if (duplicateWaySegments.size() > 1) {
            return duplicateWaySegments;
        } else {
            return null;
        }
    }

    @Override
    public void visit(Way w) {

        final Set<WaySegment> duplicateWaySegment = checkDuplicateWaySegment(w);
        if (duplicateWaySegment != null) {
            errors.add(TestError.builder(this, Severity.ERROR, DUPLICATE_WAY_SEGMENT)
                    .message(tr("Way contains segment twice"))
                    .primitives(w)
                    .highlightWaySegments(duplicateWaySegment)
                    .build());
            return;
        }

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
