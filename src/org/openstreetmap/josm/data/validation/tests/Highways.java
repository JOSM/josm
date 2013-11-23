// License: GPL. See LICENSE file for details.
package org.openstreetmap.josm.data.validation.tests;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.command.Command;
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
    
    /**
     * Classified highways in order of importance
     */
    protected static final List<String> CLASSIFIED_HIGHWAYS = Arrays.asList(
            "motorway",  "motorway_link", 
            "trunk",     "trunk_link", 
            "primary",   "primary_link", 
            "secondary", "secondary_link",
            "tertiary",  "tertiary_link",
            "living_street",
            "residential",
            "unclassified");

    boolean leftByPedestrians = false;
    boolean leftByCyclists = false;
    boolean leftByCars = false;
    int pedestrianWays = 0;
    int cyclistWays = 0;
    int carsWays = 0;
    
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
        if (n.isUsable() && !n.hasTag("highway", "crossing") && !n.hasTag("crossing", "no") && n.isReferredByWays(2)) {
            testMissingPedestrianCrossing(n);
        }
    }

    @Override
    public void visit(Way w) {
        if (w.isUsable() && w.hasKey("highway") && w.hasKey("junction") && w.get("junction").equals("roundabout")) {
            testWrongRoundabout(w);
        }
    }

    private void testWrongRoundabout(Way w) {
        Map<String, List<Way>> map = new HashMap<String, List<Way>>();
        // Count all highways (per type) connected to this roundabout
        // As roundabouts are closed ways, take care of not processing the first/last node twice
        for (Node n : new HashSet<Node>(w.getNodes())) {
            for (Way h : Utils.filteredCollection(n.getReferrers(), Way.class)) {
                if (h != w && h.hasKey("highway")) {
                    List<Way> list = map.get(h.get("highway"));
                    if (list == null) {
                        map.put(h.get("highway"), list = new ArrayList<Way>());
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
                if (highway.equals("footway") || highway.equals("path")) {
                    handlePedestrianWay(n, w);
                    if (w.hasTag("bicycle", "yes", "designated")) {
                        handleCyclistWay(n, w);
                    }
                } else if (highway.equals("cycleway")) {
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
                    errors.add(new TestError(this, Severity.WARNING, tr("Missing crossing information"), MISSING_PEDESTRIAN_CROSSING, n));
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
    
    @Override
    public boolean isFixable(TestError testError) {
        return testError instanceof WrongRoundaboutHighway;
    }

    @Override
    public Command fixError(TestError testError) {
        if (testError instanceof WrongRoundaboutHighway) {
            return new ChangePropertyCommand(testError.getPrimitives().iterator().next(), 
                    "highway", ((WrongRoundaboutHighway) testError).correctValue);
        }
        return null;
    }
}
