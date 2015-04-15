package org.openstreetmap.josm.gui.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.tools.Geometry;
import org.openstreetmap.josm.tools.SubclassFilteredCollection;
import org.openstreetmap.josm.tools.Utils;

/**
 * Determines how an icon is to be rotated depending on the primitive to displayed.
 */
public abstract class RotationAngle {
    /**
     * Calculates the rotation angle depending on the primitive to displayed.
     */
    public abstract double getRotationAngle(OsmPrimitive p);

    /**
     * Always returns the fixed {@code angle}.
     */
    public static RotationAngle buildStaticRotation(final double angle) {
        return new RotationAngle() {
            @Override
            public double getRotationAngle(OsmPrimitive p) {
                return angle;
            }
        };
    }

    /**
     * Parses the rotation angle from the specified {@code string}.
     */
    public static RotationAngle buildStaticRotation(final String string) {
        final Pattern radiansPattern = Pattern.compile("(\\d+\\.?\\d*)\\s*(rad)?");
        final Pattern degreesPattern = Pattern.compile("(\\d+\\.?\\d*)\\s*(deg|°)");
        final double value;
        Matcher matcher;
        if ((matcher = radiansPattern.matcher(string)).matches()) {
            value = Double.parseDouble(matcher.group(1));
        } else if ((matcher = degreesPattern.matcher(string)).matches()) {
            value = Math.toRadians(Double.parseDouble(matcher.group(1)));
        } else {
            try {
                value = parseCardinalRotation(string);
            } catch (IllegalArgumentException ignore) {
                throw new IllegalArgumentException("Invalid string: " + string);
            }
        }
        return buildStaticRotation(value);
    }

    /**
     * Converts an angle diven in cardinal directions to radians.
     * The following values are supported: {@code n}, {@code north}, {@code ne}, {@code northeast},
     * {@code e}, {@code east}, {@code se}, {@code southeast}, {@code s}, {@code south},
     * {@code sw}, {@code southwest}, {@code w}, {@code west}, {@code nw}, {@code northwest}.
     * @param cardinal the angle in cardinal directions
     * @return the angle in radians
     */
    public static double parseCardinalRotation(final String cardinal) {
        switch (cardinal.toLowerCase()) {
            case "n":
            case "north":
                return Math.toRadians(0);
            case "ne":
            case "northeast":
                return Math.toRadians(45);
            case "e":
            case "east":
                return Math.toRadians(90);
            case "se":
            case "southeast":
                return Math.toRadians(135);
            case "s":
            case "south":
                return Math.toRadians(180);
            case "sw":
            case "southwest":
                return Math.toRadians(225);
            case "w":
            case "west":
                return Math.toRadians(270);
            case "nw":
            case "northwest":
                return Math.toRadians(315);
            default:
                throw new IllegalArgumentException("Unexpected cardinal direction " + cardinal);
        }
    }

    /**
     * Computes the angle depending on the referencing way segment, or {@code 0} if none exists.
     */
    public static RotationAngle buildWayDirectionRotation() {
        return new RotationAngle() {
            @Override
            public double getRotationAngle(OsmPrimitive p) {
                if (!(p instanceof Node)) {
                    return 0;
                }
                final Node n = (Node) p;
                final SubclassFilteredCollection<OsmPrimitive, Way> ways = Utils.filteredCollection(n.getReferrers(), Way.class);
                if (ways.isEmpty()) {
                    return 0;
                }
                final Way w = ways.iterator().next();
                final int idx = w.getNodes().indexOf(n);
                if (idx == 0) {
                    return -Geometry.getSegmentAngle(n.getEastNorth(), w.getNode(idx + 1).getEastNorth());
                } else {
                    return -Geometry.getSegmentAngle(w.getNode(idx - 1).getEastNorth(), n.getEastNorth());
                }
            }
        };
    }
}
