// License: GPL. See LICENSE file for details.
package org.openstreetmap.josm.data.validation.tests;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.WaySegment;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.Test;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.tools.MultiMap;
import org.openstreetmap.josm.tools.Pair;

/**
 * Tests if there are overlapping ways
 *
 * @author frsantos
 */
public class OverlappingWays extends Test {
    
    /** Bag of all way segments */
    MultiMap<Pair<Node,Node>, WaySegment> nodePairs;

    protected static int OVERLAPPING_HIGHWAY = 101;
    protected static int OVERLAPPING_RAILWAY = 102;
    protected static int OVERLAPPING_WAY = 103;
    protected static int OVERLAPPING_HIGHWAY_AREA = 111;
    protected static int OVERLAPPING_RAILWAY_AREA = 112;
    protected static int OVERLAPPING_WAY_AREA = 113;
    protected static int OVERLAPPING_AREA = 120;

    /** Constructor */
    public OverlappingWays() {
        super(tr("Overlapping ways."),
              tr("This test checks that a connection between two nodes "
                + "is not used by more than one way."));
    }

    @Override
    public void startTest(ProgressMonitor monitor)  {
        super.startTest(monitor);
        nodePairs = new MultiMap<Pair<Node,Node>, WaySegment>(1000);
    }

    @Override
    public void endTest() {
        Map<List<Way>, LinkedHashSet<WaySegment>> ways_seen = new HashMap<List<Way>, LinkedHashSet<WaySegment>>(500);

        for (LinkedHashSet<WaySegment> duplicated : nodePairs.values()) {
            int ways = duplicated.size();

            if (ways > 1) {
                List<OsmPrimitive> prims = new ArrayList<OsmPrimitive>();
                List<Way> current_ways = new ArrayList<Way>();
                Collection<WaySegment> highlight;
                int highway = 0;
                int railway = 0;
                int area = 0;

                for (WaySegment ws : duplicated) {
                    if (ws.way.get("highway") != null) {
                        highway++;
                    } else if (ws.way.get("railway") != null) {
                        railway++;
                    }
                    Boolean ar = OsmUtils.getOsmBoolean(ws.way.get("area"));
                    if (ar != null && ar) {
                        area++;
                    }
                    if (ws.way.get("landuse") != null
                            || ws.way.get("natural") != null
                            || ws.way.get("amenity") != null
                            || ws.way.get("leisure") != null
                            || ws.way.get("building") != null) {
                        area++;
                        ways--;
                    }

                    prims.add(ws.way);
                    current_ways.add(ws.way);
                }
                /* These ways not seen before
                 * If two or more of the overlapping ways are
                 * highways or railways mark a separate error
                 */
                if ((highlight = ways_seen.get(current_ways)) == null) {
                    String errortype;
                    int type;

                    if (area > 0) {
                        if (ways == 0 || duplicated.size() == area) {
                            errortype = tr("Overlapping areas");
                            type = OVERLAPPING_AREA;
                        } else if (highway == ways) {
                            errortype = tr("Overlapping highways (with area)");
                            type = OVERLAPPING_HIGHWAY_AREA;
                        } else if (railway == ways) {
                            errortype = tr("Overlapping railways (with area)");
                            type = OVERLAPPING_RAILWAY_AREA;
                        } else {
                            errortype = tr("Overlapping ways (with area)");
                            type = OVERLAPPING_WAY_AREA;
                        }
                    }
                    else if (highway == ways) {
                        errortype = tr("Overlapping highways");
                        type = OVERLAPPING_HIGHWAY;
                    } else if (railway == ways) {
                        errortype = tr("Overlapping railways");
                        type = OVERLAPPING_RAILWAY;
                    } else {
                        errortype = tr("Overlapping ways");
                        type = OVERLAPPING_WAY;
                    }

                    errors.add(new TestError(this, 
                            type < OVERLAPPING_HIGHWAY_AREA ? Severity.WARNING : Severity.OTHER,
                            tr(errortype), type, prims, duplicated));
                    ways_seen.put(current_ways, duplicated);
                } else { /* way seen, mark highlight layer only */
                    for (WaySegment ws : duplicated) {
                        highlight.add(ws);
                    }
                }
            }
        }
        super.endTest();
        nodePairs = null;
    }

    @Override
    public void visit(Way w) {
        Node lastN = null;
        int i = -2;
        for (Node n : w.getNodes()) {
            i++;
            if (lastN == null) {
                lastN = n;
                continue;
            }
            nodePairs.put(Pair.sort(new Pair<Node,Node>(lastN, n)),
                new WaySegment(w, i));
            lastN = n;
        }
    }
}
