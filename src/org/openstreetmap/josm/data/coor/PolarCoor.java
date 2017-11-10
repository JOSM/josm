// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.coor;

import java.io.Serializable;
import java.util.Objects;

/**
 * Polar coordinate.
 * @since 13107 (extracted from {@code AlignInCircleAction})
 */
public class PolarCoor implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Radial coordinate (distance from the pole).
     */
    public final double radius;

    /**
     * Angular coordinate in radians.
     */
    public final double angle;

    /**
     * Reference point (analogous to the origin of a Cartesian coordinate system).
     */
    public final EastNorth pole;

    /**
     * Constructs a new {@code PolarCoor}, using {@code (0,0)} as pole.
     * @param radius radial coordinate (distance from the pole)
     * @param angle angular coordinate in radians
     */
    public PolarCoor(double radius, double angle) {
        this(radius, angle, new EastNorth(0, 0));
    }

    /**
     * Constructs a new {@code PolarCoor}.
     * @param radius radial coordinate (distance from the pole)
     * @param angle angular coordinate in radians
     * @param pole reference point (analogous to the origin of a Cartesian coordinate system)
     */
    public PolarCoor(double radius, double angle, EastNorth pole) {
        this.radius = radius;
        this.angle = angle;
        this.pole = pole;
    }

    /**
     * Constructs a new {@code PolarCoor} from an {@link EastNorth}, using {@code (0,0)} as pole.
     * @param en east/north coordinates
     */
    public PolarCoor(EastNorth en) {
        this(en, new EastNorth(0, 0));
    }

    /**
     * Constructs a new {@code PolarCoor}.
     * @param en east/north coordinates
     * @param pole reference point (analogous to the origin of a Cartesian coordinate system)
     */
    public PolarCoor(EastNorth en, EastNorth pole) {
        this(en.distance(pole), computeAngle(en, pole), pole);
    }

    /**
     * Compute polar angle between an east/north and the pole.
     * @param en east/north coordinates
     * @param pole reference point (analogous to the origin of a Cartesian coordinate system)
     * @return polar angle in radians
     */
    public static double computeAngle(EastNorth en, EastNorth pole) {
        return Math.atan2(en.north() - pole.north(), en.east() - pole.east());
    }

    /**
     * Converts this {@code PolarCoor} to an {@link EastNorth} instance.
     * @return a new {@code EastNorth} instance
     */
    public EastNorth toEastNorth() {
        return new EastNorth(
                radius * Math.cos(angle) + pole.east(),
                radius * Math.sin(angle) + pole.north());
    }

    @Override
    public int hashCode() {
        return Objects.hash(radius, angle, pole);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        PolarCoor that = (PolarCoor) obj;
        return Double.compare(that.radius, radius) == 0 &&
               Double.compare(that.angle, angle) == 0 &&
               Objects.equals(that.pole, pole);
    }

    @Override
    public String toString() {
        return "PolarCoor [radius=" + radius + ", angle=" + angle + ", pole=" + pole + ']';
    }
}
