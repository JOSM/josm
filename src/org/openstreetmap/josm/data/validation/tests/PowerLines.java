// License: GPL. See LICENSE file for details.
package org.openstreetmap.josm.data.validation.tests;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.paint.relations.Multipolygon;
import org.openstreetmap.josm.data.osm.visitor.paint.relations.Multipolygon.JoinedWay;
import org.openstreetmap.josm.data.osm.visitor.paint.relations.MultipolygonCache;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.Test;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.tools.Geometry;

/**
 * Checks for nodes in power lines/minor_lines that do not have a power=tower/pole tag.<br>
 * See #7812 for discussions about this test.
 */
public class PowerLines extends Test {

    protected static final int POWER_LINES = 2501;

    /** Values for {@code power} key interpreted as power lines */
    public static final Collection<String> POWER_LINE_TAGS = Arrays.asList("line", "minor_line");
    /** Values for {@code power} key interpreted as power towers */
    public static final Collection<String> POWER_TOWER_TAGS = Arrays.asList("tower", "pole");
    /** Values for {@code power} key interpreted as power stations */
    public static final Collection<String> POWER_STATION_TAGS = Arrays.asList("station", "sub_station", "substation", "plant", "generator");
    /** Values for {@code power} key interpreted as allowed power items */
    public static final Collection<String> POWER_ALLOWED_TAGS = Arrays.asList("switch", "transformer", "busbar", "generator");

    private final Map<Way, String> towerPoleTagMap = new HashMap<Way, String>();

    private final List<PowerLineError> potentialErrors = new ArrayList<PowerLineError>();

    private final List<OsmPrimitive> powerStations = new ArrayList<OsmPrimitive>();

    /**
     * Constructs a new {@code PowerLines} test.
     */
    public PowerLines() {
        super(tr("Power lines"), tr("Checks for nodes in power lines that do not have a power=tower/pole tag."));
    }

    @Override
    public void visit(Way w) {
        if (w.isUsable()) {
            if (isPowerLine(w)) {
                String fixValue = null;
                boolean erroneous = false;
                boolean canFix = false;
                for (Node n : w.getNodes()) {
                    if (!isPowerTower(n)) {
                        if (!isPowerAllowed(n)) {
                            potentialErrors.add(new PowerLineError(n, w));
                            erroneous = true;
                        }
                    } else if (fixValue == null) {
                        // First tower/pole tag found, remember it
                        fixValue = n.get("power");
                        canFix = true;
                    } else if (!fixValue.equals(n.get("power"))) {
                        // The power line contains both "tower" and "pole" -> cannot fix this error
                        canFix = false;
                    }
                }
                if (erroneous && canFix) {
                    towerPoleTagMap.put(w, fixValue);
                }
            } else if (w.isClosed() && isPowerStation(w)) {
                powerStations.add(w);
            }
        }
    }

    @Override
    public void visit(Relation r) {
        if (r.isMultipolygon() && isPowerStation(r)) {
            powerStations.add(r);
        }
    }

    @Override
    public void startTest(ProgressMonitor progressMonitor) {
        super.startTest(progressMonitor);
        towerPoleTagMap.clear();
        powerStations.clear();
        potentialErrors.clear();
    }

    @Override
    public void endTest() {
        for (PowerLineError e : potentialErrors) {
            Node n = e.getNode();
            if (n != null && !isInPowerStation(n)) {
                errors.add(e);
            }
        }
        potentialErrors.clear();
        super.endTest();
    }

    protected final boolean isInPowerStation(Node n) {
        for (OsmPrimitive station : powerStations) {
            List<List<Node>> nodesLists = new ArrayList<List<Node>>();
            if (station instanceof Way) {
                nodesLists.add(((Way)station).getNodes());
            } else if (station instanceof Relation) {
                Multipolygon polygon = MultipolygonCache.getInstance().get(Main.map.mapView, (Relation) station);
                if (polygon != null) {
                    for (JoinedWay outer : Multipolygon.joinWays(polygon.getOuterWays())) {
                        nodesLists.add(outer.getNodes());
                    }
                }
            }
            for (List<Node> nodes : nodesLists) {
                if (Geometry.nodeInsidePolygon(n, nodes)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public Command fixError(TestError testError) {
        if (testError instanceof PowerLineError && isFixable(testError)) {
            // primitives list can be empty if all primitives have been purged
            Iterator<? extends OsmPrimitive> it = testError.getPrimitives().iterator();
            if (it.hasNext()) {
                return new ChangePropertyCommand(it.next(),
                        "power", towerPoleTagMap.get(((PowerLineError)testError).line));
            }
        }
        return null;
    }

    @Override
    public boolean isFixable(TestError testError) {
        return testError instanceof PowerLineError && towerPoleTagMap.containsKey(((PowerLineError)testError).line);
    }

    /**
     * Determines if the specified way denotes a power line.
     * @param w The way to be tested
     * @return {@code true} if power key is set and equal to line/minor_line
     */
    protected static final boolean isPowerLine(Way w) {
        return isPowerIn(w, POWER_LINE_TAGS);
    }

    /**
     * Determines if the specified primitive denotes a power station.
     * @param p The primitive to be tested
     * @return {@code true} if power key is set and equal to station/sub_station/plant
     */
    protected static final boolean isPowerStation(OsmPrimitive p) {
        return isPowerIn(p, POWER_STATION_TAGS);
    }

    /**
     * Determines if the specified node denotes a power tower/pole.
     * @param n The node to be tested
     * @return {@code true} if power key is set and equal to tower/pole
     */
    protected static final boolean isPowerTower(Node n) {
        return isPowerIn(n, POWER_TOWER_TAGS);
    }

    /**
     * Determines if the specified node denotes a power infrastructure allowed on a power line.
     * @param n The node to be tested
     * @return True if power key is set and equal to switch/tranformer/busbar/generator
     */
    protected static final boolean isPowerAllowed(Node n) {
        return isPowerIn(n, POWER_ALLOWED_TAGS);
    }

    /**
     * Helper function to check if power tags is a certain value.
     * @param p The primitive to be tested
     * @param values List of possible values
     * @return {@code true} if power key is set and equal to possible values
     */
    private static final boolean isPowerIn(OsmPrimitive p, Collection<String> values) {
        String v = p.get("power");
        return v != null && values != null && values.contains(v);
    }

    protected class PowerLineError extends TestError {
        private final Way line;
        public PowerLineError(Node n, Way line) {
            super(PowerLines.this, Severity.WARNING,
                    tr("Missing power tower/pole within power line"), POWER_LINES, n);
            this.line = line;
        }
        public final Node getNode() {
            // primitives list can be empty if all primitives have been purged
            Iterator<? extends OsmPrimitive> it = getPrimitives().iterator();
            return it.hasNext() ? (Node) it.next() : null;
        }
    }
}
