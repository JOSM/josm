// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.TreeSet;

import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.WaySegment;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.Test;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.tools.Geometry;
import org.openstreetmap.josm.tools.bugreport.BugReport;

/**
 * Find highways that have sharp angles
 * @author Taylor Smock
 * @since 15406
 */
public class SharpAngles extends Test {
    private static final int SHARPANGLESCODE = 3800;
    /** The code for a sharp angle */
    private static final int SHARP_ANGLES = SHARPANGLESCODE + 0;
    /** The maximum angle for sharp angles */
    private double maxAngle = 45.0; // degrees
    /** The length that at least one way segment must be shorter than */
    private double maxLength = 10.0; // meters
    /** The stepping points for severity */
    private Map<Double, Severity> severityBreakPoints = new LinkedHashMap<>();
    /** Specific highway types to ignore */
    private Collection<String> ignoreHighways = new TreeSet<>(
            Arrays.asList("platform", "rest_area", "services", "via_ferrata"));

    /**
     * Construct a new {@code IntersectionIssues} object
     */
    public SharpAngles() {
        super(tr("Sharp angles"), tr("Check for sharp angles on roads"));
        setBreakPoints();
    }

    @Override
    public void visit(Way way) {
        if (!way.isUsable()) return;
        if (way.hasKey("highway") && !way.hasTag("area", "yes") &&
                    !ignoreHighways.contains(way.get("highway"))) {
            try {
                checkWayForSharpAngles(way);
            } catch (RuntimeException e) {
                throw BugReport.intercept(e).put("way", way);
            }
        }
    }

    /**
     * Check nodes in a way for sharp angles
     * @param way A way to check for sharp angles
     */
    public void checkWayForSharpAngles(Way way) {
        Node node1 = null;
        Node node2 = null;
        Node node3 = null;
        int i = -2;
        for (Node node : way.getNodes()) {
            node1 = node2;
            node2 = node3;
            node3 = node;
            checkAngle(node1, node2, node3, i, way, false);
            i++;
        }
        if (way.isClosed() && way.getNodesCount() > 2) {
            node1 = node2;
            node2 = node3;
            // Get the second node, not the first node, since a closed way has first node == last node
            node3 = way.getNode(1);
            checkAngle(node1, node2, node3, i, way, true);
        }
    }

    private void checkAngle(Node node1, Node node2, Node node3, int i, Way way, boolean last) {
        if (node1 == null || node2 == null || node3 == null) return;
        EastNorth n1 = node1.getEastNorth();
        EastNorth n2 = node2.getEastNorth();
        EastNorth n3 = node3.getEastNorth();
        double angle = Math.toDegrees(Math.abs(Geometry.getCornerAngle(n1, n2, n3)));
        if (angle < maxAngle) {
            processSharpAngleForErrorCreation(angle, i, way, last, node2);
        }
    }

    private void processSharpAngleForErrorCreation(double angle, int i, Way way, boolean last, Node pointNode) {
        WaySegment ws1 = new WaySegment(way, i);
        WaySegment ws2 = new WaySegment(way, last ? 0 : i + 1);
        double shorterLen = Math.min(ws1.toWay().getLength(), ws2.toWay().getLength());
        if (shorterLen < maxLength) {
            createNearlyOverlappingError(angle, way, pointNode);
        }
    }

    private void createNearlyOverlappingError(double angle, Way way, OsmPrimitive primitive) {
        TestError.Builder testError = TestError.builder(this, getSeverity(angle), SHARP_ANGLES)
                .primitives(way)
                .highlight(primitive)
                .message(tr("Sharp angle"));
        errors.add(testError.build());
    }

    private Severity getSeverity(double angle) {
        Severity rSeverity = Severity.OTHER;
        for (Entry<Double, Severity> entry : severityBreakPoints.entrySet()) {
            if (angle < entry.getKey()) {
                rSeverity = entry.getValue();
            }
        }
        return rSeverity;
    }

    /**
     * Set the maximum length for the shortest segment
     * @param length The max length in meters
     */
    public void setMaxLength(double length) {
        maxLength = length;
    }

    /**
     * Add a highway to ignore
     * @param highway The highway type to ignore (e.g., if you want to ignore residential roads, use "residential")
     */
    public void addIgnoredHighway(String highway) {
        ignoreHighways.add(highway);
    }

    /**
     * Set the maximum angle
     * @param angle The maximum angle in degrees.
     */
    public void setMaxAngle(double angle) {
        maxAngle = angle;
        setBreakPoints();
    }

    /**
     * Set the breakpoints for the test
     */
    private void setBreakPoints() {
        severityBreakPoints.clear();
        severityBreakPoints.put(maxAngle, Severity.OTHER);
        severityBreakPoints.put(maxAngle * 2 / 3, Severity.WARNING);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hash(ignoreHighways, maxAngle, maxLength);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj) || getClass() != obj.getClass())
            return false;
        SharpAngles other = (SharpAngles) obj;
        return Objects.equals(ignoreHighways, other.ignoreHighways)
                && Double.doubleToLongBits(maxAngle) == Double.doubleToLongBits(other.maxAngle)
                && Double.doubleToLongBits(maxLength) == Double.doubleToLongBits(other.maxLength);
    }
}
