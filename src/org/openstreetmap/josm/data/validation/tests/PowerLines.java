// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
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

    /** Test identifier */
    protected static final int POWER_LINES = 2501;
    protected static final int POWER_CONNECTION = 2502;

    /** Values for {@code power} key interpreted as power lines */
    static final Collection<String> POWER_LINE_TAGS = Arrays.asList("line", "minor_line");
    /** Values for {@code power} key interpreted as power towers */
    static final Collection<String> POWER_TOWER_TAGS = Arrays.asList("tower", "pole");
    /** Values for {@code power} key interpreted as power stations */
    static final Collection<String> POWER_STATION_TAGS = Arrays.asList("station", "sub_station", "substation", "plant", "generator");
    /** Values for {@code building} key interpreted as power stations */
    static final Collection<String> BUILDING_STATION_TAGS = Arrays.asList("transformer_tower");
    /** Values for {@code power} key interpreted as allowed power items */
    static final Collection<String> POWER_ALLOWED_TAGS = Arrays.asList("switch", "transformer", "busbar", "generator", "switchgear",
            "portal", "terminal", "insulator");

    private final Set<Node> badConnections = new LinkedHashSet<>();
    private final Set<Node> missingTowerOrPole = new LinkedHashSet<>();

    private final List<OsmPrimitive> powerStations = new ArrayList<>();

    /**
     * Constructs a new {@code PowerLines} test.
     */
    public PowerLines() {
        super(tr("Power lines"), tr("Checks for nodes in power lines that do not have a power=tower/pole tag."));
    }

    @Override
    public void visit(Way w) {
        if (w.isUsable()) {
            if (isPowerLine(w) && !w.hasTag("location", "underground")) {
                for (Node n : w.getNodes()) {
                    if (!isPowerTower(n) && !isPowerAllowed(n) && IN_DOWNLOADED_AREA.test(n)
                        && (!w.isFirstLastNode(n) || !isPowerStation(n))) {
                        missingTowerOrPole.add(n);
                    }
                }
            } else if (w.isClosed() && isPowerStation(w)) {
                powerStations.add(w);
            }
        }
    }

    @Override
    public void visit(Node n) {
        boolean nodeInLineOrCable = false;
        boolean connectedToUnrelated = false;
        for (Way parent : n.getParentWays()) {
            if (parent.hasTag("power", "line", "minor_line", "cable"))
                nodeInLineOrCable = true;
            else if (!isRelatedToPower(parent)) {
                connectedToUnrelated = true;
            }
        }
        if (nodeInLineOrCable && connectedToUnrelated)
            badConnections.add(n);
    }

    private static boolean isRelatedToPower(Way way) {
        if (way.hasTag("power") || way.hasTag("building"))
            return true;
        for (OsmPrimitive ref : way.getReferrers()) {
            if (ref instanceof Relation && ref.isMultipolygon() && (ref.hasTag("power") || ref.hasTag("building"))) {
                for (RelationMember rm : ((Relation) ref).getMembers()) {
                    if (way == rm.getMember())
                        return true;
                }
            }
        }
        return false;
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
        clearCollections();
    }

    @Override
    public void endTest() {
        for (Node n : missingTowerOrPole) {
            if (!isInPowerStation(n)) {
                errors.add(TestError.builder(this, Severity.WARNING, POWER_LINES)
                        .message(tr("Missing power tower/pole within power line"))
                        .primitives(n)
                        .build());
            }
        }

        for (Node n : badConnections) {
            errors.add(TestError.builder(this, Severity.WARNING, POWER_CONNECTION)
                    .message(tr("Node connects a power line or cable with an object "
                            + "which is not related to the power infrastructure."))
                    .primitives(n).build());
        }
        clearCollections();
        super.endTest();
    }

    protected final boolean isInPowerStation(Node n) {
        for (OsmPrimitive station : powerStations) {
            List<List<Node>> nodesLists = new ArrayList<>();
            if (station instanceof Way) {
                nodesLists.add(((Way) station).getNodes());
            } else if (station instanceof Relation) {
                Multipolygon polygon = MultipolygonCache.getInstance().get((Relation) station);
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
        return isPowerIn(p, POWER_STATION_TAGS) || isBuildingIn(p, BUILDING_STATION_TAGS);
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
     * Helper function to check if power tag is a certain value.
     * @param p The primitive to be tested
     * @param values List of possible values
     * @return {@code true} if power key is set and equal to possible values
     */
    private static boolean isPowerIn(OsmPrimitive p, Collection<String> values) {
        return p.hasTag("power", values);
    }

    /**
     * Helper function to check if building tag is a certain value.
     * @param p The primitive to be tested
     * @param values List of possible values
     * @return {@code true} if power key is set and equal to possible values
     */
    private static boolean isBuildingIn(OsmPrimitive p, Collection<String> values) {
        return p.hasTag("building", values);
    }

    private void clearCollections() {
        powerStations.clear();
        badConnections.clear();
        missingTowerOrPole.clear();
    }
}
