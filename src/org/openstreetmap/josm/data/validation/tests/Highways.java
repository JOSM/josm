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
    
    protected static final List<String> CLASSIFIED_HIGHWAYS = Arrays.asList(
            "motorway", "trunk", "primary", "secondary", "tertiary", "living_street", "residential", "unclassified");

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
    public void visit(Way w) {
        if (w.isUsable()) {
            if (w.hasKey("highway") && w.hasKey("junction") && w.get("junction").equals("roundabout")) {
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
                                errors.add(new TestError(this, Severity.WARNING, 
                                        tr("Incorrect roundabout (highway: {0} instead of {1})", w.get("highway"), s), 
                                        WRONG_ROUNDABOUT_HIGHWAY, w));
                            }
                            break;
                        }
                    }
                }
            }
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
