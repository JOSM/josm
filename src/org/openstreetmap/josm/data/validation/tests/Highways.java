// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.Test;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.tools.Predicate;
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

    /**
     * Classified highways in order of importance
     */
    private static final List<String> CLASSIFIED_HIGHWAYS = Arrays.asList(
            "motorway",  "motorway_link",
            "trunk",     "trunk_link",
            "primary",   "primary_link",
            "secondary", "secondary_link",
            "tertiary",  "tertiary_link",
            "unclassified",
            "residential",
            "living_street");

    private static final Set<String> KNOWN_SOURCE_MAXSPEED_CONTEXTS = new HashSet<>(Arrays.asList(
            "urban", "rural", "zone", "zone30", "zone:30", "nsl_single", "nsl_dual", "motorway", "trunk", "living_street", "bicycle_road"));

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

    protected class WrongRoundaboutHighway extends TestError {

        public final String correctValue;

        public WrongRoundaboutHighway(Way w, String key) {
            super(Highways.this, Severity.WARNING,
                    tr("Incorrect roundabout (highway: {0} instead of {1})", w.get("highway"), key),
                    WRONG_ROUNDABOUT_HIGHWAY, w);
            this.correctValue = key;
        }
    }

    @Override
    public void visit(Node n) {
        if (n.isUsable()) {
            if (!n.hasTag("crossing", "no")
             && !(n.hasKey("crossing") && (n.hasTag("highway", "crossing") || n.hasTag("highway", "traffic_signals")))
             && n.isReferredByWays(2)) {
                testMissingPedestrianCrossing(n);
            }
            if (n.hasKey("source:maxspeed")) {
                // Check maxspeed but not context against highway for nodes
                // as maxspeed is not set on highways here but on signs, speed cameras, etc.
                testSourceMaxspeed(n, false);
            }
        }
    }

    @Override
    public void visit(Way w) {
        if (w.isUsable()) {
            if (w.hasKey("highway") && CLASSIFIED_HIGHWAYS.contains(w.get("highway"))
                    && w.hasKey("junction") && "roundabout".equals(w.get("junction"))) {
                testWrongRoundabout(w);
            }
            if (w.hasKey("source:maxspeed")) {
                // Check maxspeed, including context against highway
                testSourceMaxspeed(w, true);
            }
            testHighwayLink(w);
        }
    }

    private void testWrongRoundabout(Way w) {
        Map<String, List<Way>> map = new HashMap<>();
        // Count all highways (per type) connected to this roundabout, except links
        // As roundabouts are closed ways, take care of not processing the first/last node twice
        for (Node n : new HashSet<>(w.getNodes())) {
            for (Way h : Utils.filteredCollection(n.getReferrers(), Way.class)) {
                String value = h.get("highway");
                if (h != w && value != null && !value.endsWith("_link")) {
                    List<Way> list = map.get(value);
                    if (list == null) {
                        map.put(value, list = new ArrayList<>());
                    }
                    list.add(h);
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
                    if (!w.get("highway").equals(s)) {
                        errors.add(new WrongRoundaboutHighway(w, s));
                    }
                    break;
                }
            }
        }
    }

    public static boolean isHighwayLinkOkay(final Way way) {
        final String highway = way.get("highway");
        if (highway == null || !highway.endsWith("_link")) {
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

        return Utils.exists(Utils.filteredCollection(referrers, Way.class), new Predicate<Way>() {
            @Override
            public boolean evaluate(final Way otherWay) {
                return !way.equals(otherWay) && otherWay.hasTag("highway", highway, highway.replaceAll("_link$", ""));
            }
        });
    }

    private void testHighwayLink(final Way way) {
        if (!isHighwayLinkOkay(way)) {
            errors.add(new TestError(this, Severity.WARNING,
                    tr("Highway link is not linked to adequate highway/link"), SOURCE_WRONG_LINK, way));
        }
    }

    private void testMissingPedestrianCrossing(Node n) {
        leftByPedestrians = false;
        leftByCyclists = false;
        leftByCars = false;
        pedestrianWays = 0;
        cyclistWays = 0;
        carsWays = 0;

        for (Way w : OsmPrimitive.getFilteredList(n.getReferrers(), Way.class)) {
            String highway = w.get("highway");
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
                    errors.add(new TestError(this, Severity.OTHER, tr("Missing pedestrian crossing information"),
                            MISSING_PEDESTRIAN_CROSSING, n));
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
        String value = p.get("source:maxspeed");
        if (value.matches("[A-Z]{2}:.+")) {
            int index = value.indexOf(':');
            // Check country
            String country = value.substring(0, index);
            if (!ISO_COUNTRIES.contains(country)) {
                errors.add(new TestError(this, Severity.WARNING,
                        tr("Unknown country code: {0}", country), SOURCE_MAXSPEED_UNKNOWN_COUNTRY_CODE, p));
            }
            // Check context
            String context = value.substring(index+1);
            if (!KNOWN_SOURCE_MAXSPEED_CONTEXTS.contains(context)) {
                errors.add(new TestError(this, Severity.WARNING,
                        tr("Unknown source:maxspeed context: {0}", context), SOURCE_MAXSPEED_UNKNOWN_CONTEXT, p));
            }
            // TODO: Check coherence of context against maxspeed
            // TODO: Check coherence of context against highway
        }
    }

    @Override
    public boolean isFixable(TestError testError) {
        return testError instanceof WrongRoundaboutHighway;
    }

    @Override
    public Command fixError(TestError testError) {
        if (testError instanceof WrongRoundaboutHighway) {
            // primitives list can be empty if all primitives have been purged
            Iterator<? extends OsmPrimitive> it = testError.getPrimitives().iterator();
            if (it.hasNext()) {
                return new ChangePropertyCommand(it.next(),
                        "highway", ((WrongRoundaboutHighway) testError).correctValue);
            }
        }
        return null;
    }
}
