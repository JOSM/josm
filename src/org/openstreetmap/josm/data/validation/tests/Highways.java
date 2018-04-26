// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.openstreetmap.josm.data.validation.tests.CrossingWays.HIGHWAY;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.Test;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.tools.Utils;

/**
 * Test that performs semantic checks on highways.
 * @since 5902
 */
public class Highways extends Test {

    protected static final int WRONG_ROUNDABOUT_HIGHWAY = 2701;
    protected static final int MISSING_PEDESTRIAN_CROSSING = 2702;
    protected static final int SOURCE_MAXSPEED_UNKNOWN_COUNTRY_CODE = 2703;
    protected static final int SOURCE_MAXSPEED_UNKNOWN_CONTEXT = 2704;
    protected static final int SOURCE_MAXSPEED_CONTEXT_MISMATCH_VS_MAXSPEED = 2705;
    protected static final int SOURCE_MAXSPEED_CONTEXT_MISMATCH_VS_HIGHWAY = 2706;
    protected static final int SOURCE_WRONG_LINK = 2707;

    protected static final String SOURCE_MAXSPEED = "source:maxspeed";

    /**
     * Classified highways in order of importance
     */
    // CHECKSTYLE.OFF: SingleSpaceSeparator
    private static final List<String> CLASSIFIED_HIGHWAYS = Arrays.asList(
            "motorway",  "motorway_link",
            "trunk",     "trunk_link",
            "primary",   "primary_link",
            "secondary", "secondary_link",
            "tertiary",  "tertiary_link",
            "unclassified",
            "residential",
            "living_street");
    // CHECKSTYLE.ON: SingleSpaceSeparator

    private static final Set<String> KNOWN_SOURCE_MAXSPEED_CONTEXTS = new HashSet<>(Arrays.asList(
            "urban", "rural", "zone", "zone20", "zone:20", "zone30", "zone:30",
            "nsl_single", "nsl_dual", "motorway", "trunk", "living_street", "bicycle_road"));

    private static final Set<String> ISO_COUNTRIES = new HashSet<>(Arrays.asList(Locale.getISOCountries()));

    private boolean leftByPedestrians;
    private boolean leftByCyclists;
    private boolean leftByCars;
    private int pedestrianWays;
    private int cyclistWays;
    private int carsWays;

    /**
     * Constructs a new {@code Highways} test.
     */
    public Highways() {
        super(tr("Highways"), tr("Performs semantic checks on highways."));
    }

    @Override
    public void visit(Node n) {
        if (n.isUsable()) {
            if (!n.hasTag("crossing", "no")
             && !(n.hasKey("crossing") && (n.hasTag(HIGHWAY, "crossing")
                                        || n.hasTag(HIGHWAY, "traffic_signals")))
             && n.isReferredByWays(2)) {
                testMissingPedestrianCrossing(n);
            }
            if (n.hasKey(SOURCE_MAXSPEED)) {
                // Check maxspeed but not context against highway for nodes
                // as maxspeed is not set on highways here but on signs, speed cameras, etc.
                testSourceMaxspeed(n, false);
            }
        }
    }

    @Override
    public void visit(Way w) {
        if (w.isUsable()) {
            if (w.isClosed() && w.hasTag(HIGHWAY, CLASSIFIED_HIGHWAYS) && w.hasTag("junction", "roundabout")
                    && IN_DOWNLOADED_AREA_STRICT.test(w)) {
                // TODO: find out how to handle splitted roundabouts (see #12841)
                testWrongRoundabout(w);
            }
            if (w.hasKey(SOURCE_MAXSPEED)) {
                // Check maxspeed, including context against highway
                testSourceMaxspeed(w, true);
            }
            testHighwayLink(w);
        }
    }

    private void testWrongRoundabout(Way w) {
        Map<String, List<Way>> map = new HashMap<>();
        // Count all highways (per type) connected to this roundabout, except correct links
        // As roundabouts are closed ways, take care of not processing the first/last node twice
        for (Node n : new HashSet<>(w.getNodes())) {
            for (Way h : Utils.filteredCollection(n.getReferrers(), Way.class)) {
                String value = h.get(HIGHWAY);
                if (h != w && value != null) {
                    boolean link = value.endsWith("_link");
                    boolean linkOk = isHighwayLinkOkay(h);
                    if (link && !linkOk) {
                        // "Autofix" bad link value to avoid false positive in roundabout check
                        value = value.replaceAll("_link$", "");
                    }
                    if (!link || !linkOk) {
                        List<Way> list = map.get(value);
                        if (list == null) {
                            list = new ArrayList<>();
                            map.put(value, list);
                        }
                        list.add(h);
                    }
                }
            }
        }
        // The roundabout should carry the highway tag of its two biggest highways
        for (String s : CLASSIFIED_HIGHWAYS) {
            List<Way> list = map.get(s);
            if (list != null && list.size() >= 2) {
                // Except when a single road is connected, but with two oneway segments
                Boolean oneway1 = OsmUtils.getOsmBoolean(list.get(0).get("oneway"));
                Boolean oneway2 = OsmUtils.getOsmBoolean(list.get(1).get("oneway"));
                if (list.size() > 2 || oneway1 == null || oneway2 == null || !oneway1 || !oneway2) {
                    // Error when the highway tags do not match
                    String value = w.get(HIGHWAY);
                    if (!value.equals(s)) {
                        errors.add(TestError.builder(this, Severity.WARNING, WRONG_ROUNDABOUT_HIGHWAY)
                                .message(tr("Incorrect roundabout (highway: {0} instead of {1})", value, s))
                                .primitives(w)
                                .fix(() -> new ChangePropertyCommand(w, HIGHWAY, s))
                                .build());
                    }
                    break;
                }
            }
        }
    }

    /**
     * Determines if the given link road is correct, see https://wiki.openstreetmap.org/wiki/Highway_link.
     * @param way link road
     * @return {@code true} if the link road is correct or if the check cannot be performed due to missing data
     */
    public static boolean isHighwayLinkOkay(final Way way) {
        final String highway = way.get(HIGHWAY);
        if (highway == null || !highway.endsWith("_link")
                || !IN_DOWNLOADED_AREA.test(way.getNode(0)) || !IN_DOWNLOADED_AREA.test(way.getNode(way.getNodesCount()-1))) {
            return true;
        }

        final Set<OsmPrimitive> referrers = new HashSet<>();

        if (way.isClosed()) {
            // for closed way we need to check all adjacent ways
            for (Node n: way.getNodes()) {
                referrers.addAll(n.getReferrers());
            }
        } else {
            referrers.addAll(way.firstNode().getReferrers());
            referrers.addAll(way.lastNode().getReferrers());
        }

        // Find ways of same class (exact class of class_link)
        List<Way> sameClass = Utils.filteredCollection(referrers, Way.class).stream().filter(
                otherWay -> !way.equals(otherWay) && otherWay.hasTag(HIGHWAY, highway, highway.replaceAll("_link$", "")))
                .collect(Collectors.toList());
        if (sameClass.size() > 1) {
            // It is possible to have a class_link between 2 segments of same class
            // in roundabout designs that physically separate a specific turn from the main roundabout
            // But if we have more than a single adjacent class, and one of them is a roundabout, that's an error
            for (Way w : sameClass) {
                if (w.hasTag("junction", "roundabout")) {
                    return false;
                }
            }
        }
        // Link roads should always at least one adjacent segment of same class
        return !sameClass.isEmpty();
    }

    private void testHighwayLink(final Way way) {
        if (!isHighwayLinkOkay(way)) {
            errors.add(TestError.builder(this, Severity.WARNING, SOURCE_WRONG_LINK)
                    .message(tr("Highway link is not linked to adequate highway/link"))
                    .primitives(way)
                    .build());
        }
    }

    private void testMissingPedestrianCrossing(Node n) {
        leftByPedestrians = false;
        leftByCyclists = false;
        leftByCars = false;
        pedestrianWays = 0;
        cyclistWays = 0;
        carsWays = 0;

        for (Way w : n.getParentWays()) {
            String highway = w.get(HIGHWAY);
            if (highway != null) {
                if ("footway".equals(highway) || "path".equals(highway)) {
                    handlePedestrianWay(n, w);
                    if (w.hasTag("bicycle", "yes", "designated")) {
                        handleCyclistWay(n, w);
                    }
                } else if ("cycleway".equals(highway)) {
                    handleCyclistWay(n, w);
                    if (w.hasTag("foot", "yes", "designated")) {
                        handlePedestrianWay(n, w);
                    }
                } else if (CLASSIFIED_HIGHWAYS.contains(highway)) {
                    // Only look at classified highways for now:
                    // - service highways support is TBD (see #9141 comments)
                    // - roads should be determined first. Another warning is raised anyway
                    handleCarWay(n, w);
                }
                if ((leftByPedestrians || leftByCyclists) && leftByCars) {
                    errors.add(TestError.builder(this, Severity.OTHER, MISSING_PEDESTRIAN_CROSSING)
                            .message(tr("Incomplete pedestrian crossing tagging. Required tags are {0} and {1}.",
                            "highway=crossing|traffic_signals", "crossing=*"))
                            .primitives(n)
                            .build());
                    return;
                }
            }
        }
    }

    private void handleCarWay(Node n, Way w) {
        carsWays++;
        if (!w.isFirstLastNode(n) || carsWays > 1) {
            leftByCars = true;
        }
    }

    private void handleCyclistWay(Node n, Way w) {
        cyclistWays++;
        if (!w.isFirstLastNode(n) || cyclistWays > 1) {
            leftByCyclists = true;
        }
    }

    private void handlePedestrianWay(Node n, Way w) {
        pedestrianWays++;
        if (!w.isFirstLastNode(n) || pedestrianWays > 1) {
            leftByPedestrians = true;
        }
    }

    private void testSourceMaxspeed(OsmPrimitive p, boolean testContextHighway) {
        String value = p.get(SOURCE_MAXSPEED);
        if (value.matches("[A-Z]{2}:.+")) {
            int index = value.indexOf(':');
            // Check country
            String country = value.substring(0, index);
            if (!ISO_COUNTRIES.contains(country)) {
                final TestError.Builder error = TestError.builder(this, Severity.WARNING, SOURCE_MAXSPEED_UNKNOWN_COUNTRY_CODE)
                        .message(tr("Unknown country code: {0}", country))
                        .primitives(p);
                if ("UK".equals(country)) {
                    errors.add(error.fix(() -> new ChangePropertyCommand(p, SOURCE_MAXSPEED, value.replace("UK:", "GB:"))).build());
                } else {
                    errors.add(error.build());
                }
            }
            // Check context
            String context = value.substring(index+1);
            if (!KNOWN_SOURCE_MAXSPEED_CONTEXTS.contains(context)) {
                errors.add(TestError.builder(this, Severity.WARNING, SOURCE_MAXSPEED_UNKNOWN_CONTEXT)
                        .message(tr("Unknown source:maxspeed context: {0}", context))
                        .primitives(p)
                        .build());
            }
            if (testContextHighway) {
                // TODO: Check coherence of context against maxspeed
                // TODO: Check coherence of context against highway
            }
        }
    }
}
