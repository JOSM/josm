// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import java.util.Locale;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;

/**
 * Determines how an icon is to be rotated depending on the primitive to be displayed.
 * @since  8199 (creation)
 * @since 10599 (functional interface)
 * @since 12756 (moved from {@code gui.util} package)
 */
@FunctionalInterface
public interface RotationAngle {

    /**
     * The rotation along a way.
     */
    final class WayDirectionRotationAngle implements RotationAngle {
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

        @Override
        public String toString() {
            return "way-direction";
        }

        @Override
        public int hashCode() {
            return 1;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            return obj != null && getClass() == obj.getClass();
        }
    }

    /**
     * A static rotation
     */
    final class StaticRotationAngle implements RotationAngle {
        private final double angle;

        private StaticRotationAngle(double angle) {
            this.angle = angle;
        }

        @Override
        public double getRotationAngle(OsmPrimitive p) {
            return angle;
        }

        @Override
        public String toString() {
            return angle + "rad";
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            long temp = Double.doubleToLongBits(angle);
            result = prime * result + (int) (temp ^ (temp >>> 32));
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            StaticRotationAngle other = (StaticRotationAngle) obj;
            return Double.doubleToLongBits(angle) == Double.doubleToLongBits(other.angle);
        }
    }

    /**
     * A no-rotation angle that always returns 0.
     * @since 11726
     */
    RotationAngle NO_ROTATION = new StaticRotationAngle(0);

    /**
     * Calculates the rotation angle depending on the primitive to be displayed.
     * @param p primitive
     * @return rotation angle
     */
    double getRotationAngle(OsmPrimitive p);

    /**
     * Always returns the fixed {@code angle}.
     * @param angle angle
     * @return rotation angle
     */
    static RotationAngle buildStaticRotation(final double angle) {
        return new StaticRotationAngle(angle);
    }

    /**
     * Parses the rotation angle from the specified {@code string}.
     * @param string angle as string
     * @return rotation angle
     */
    static RotationAngle buildStaticRotation(final String string) {
        try {
            return buildStaticRotation(parseCardinalRotation(string));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid string: " + string, e);
        }
    }

    /**
     * Converts an angle diven in cardinal directions to radians.
     * The following values are supported: {@code n}, {@code north}, {@code ne}, {@code northeast},
     * {@code e}, {@code east}, {@code se}, {@code southeast}, {@code s}, {@code south},
     * {@code sw}, {@code southwest}, {@code w}, {@code west}, {@code nw}, {@code northwest}.
     * @param cardinal the angle in cardinal directions
     * @return the angle in radians
     */
    static double parseCardinalRotation(final String cardinal) {
        switch (cardinal.toLowerCase(Locale.ENGLISH)) {
            case "n":
            case "north":
                return 0; // 0 degree => 0 radian
            case "ne":
            case "northeast":
                return Utils.toRadians(45);
            case "e":
            case "east":
                return Utils.toRadians(90);
            case "se":
            case "southeast":
                return Utils.toRadians(135);
            case "s":
            case "south":
                return Math.PI; // 180 degree
            case "sw":
            case "southwest":
                return Utils.toRadians(225);
            case "w":
            case "west":
                return Utils.toRadians(270);
            case "nw":
            case "northwest":
                return Utils.toRadians(315);
            default:
                throw new IllegalArgumentException("Unexpected cardinal direction " + cardinal);
        }
    }

    /**
     * Computes the angle depending on the referencing way segment, or {@code 0} if none exists.
     * @return rotation angle
     */
    static RotationAngle buildWayDirectionRotation() {
        return new WayDirectionRotationAngle();
    }
}
