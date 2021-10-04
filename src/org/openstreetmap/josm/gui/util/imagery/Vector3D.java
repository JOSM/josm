// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.util.imagery;

/**
 * A basic 3D vector class (immutable)
 * @author Taylor Smock (documentation, spherical conversions)
 * @since 18246
 */
public final class Vector3D {
    /**
     * This determines how arguments are used in {@link Vector3D#Vector3D(VectorType, double, double, double)}.
     */
    public enum VectorType {
        /** Standard cartesian coordinates (x, y, z) */
        XYZ,
        /** Physics (radial distance, polar angle, azimuthal angle) */
        RPA,
        /** Mathematics (radial distance, azimuthal angle, polar angle) */
        RAP
    }

    /** A non-null default vector */
    public static final Vector3D DEFAULT_VECTOR_3D = new Vector3D(0, 0, 1);

    private final double x;
    private final double y;
    private final double z;
    /* The following are all lazily calculated, but should always be the same */
    /** The radius r */
    private volatile double radialDistance = Double.NaN;
    /** The polar angle theta (inclination) */
    private volatile double polarAngle = Double.NaN;
    /** Cosine of polar angle (angle from Z axis, AKA straight up) */
    private volatile double polarAngleCos = Double.NaN;
    /** Sine of polar angle (angle from Z axis, AKA straight up) */
    private volatile double polarAngleSin = Double.NaN;
    /** The azimuthal angle phi */
    private volatile double azimuthalAngle = Double.NaN;
    /** Cosine of azimuthal angle (angle from X axis) */
    private volatile double azimuthalAngleCos = Double.NaN;
    /** Sine of azimuthal angle (angle from X axis) */
    private volatile double azimuthalAngleSin = Double.NaN;

    /**
     * Create a new Vector3D object using the XYZ coordinate system
     *
     * @param x The x coordinate
     * @param y The y coordinate
     * @param z The z coordinate
     */
    public Vector3D(double x, double y, double z) {
        this(VectorType.XYZ, x, y, z);
    }

    /**
     * Create a new Vector3D object. See ordering in {@link VectorType}.
     *
     * @param first The first coordinate
     * @param second The second coordinate
     * @param third The third coordinate
     * @param vectorType The coordinate type (determines how the other variables are treated)
     */
    public Vector3D(VectorType vectorType, double first, double second, double third) {
        if (vectorType == VectorType.XYZ) {
            this.x = first;
            this.y = second;
            this.z = third;
        } else {
            this.radialDistance = first;
            if (vectorType == VectorType.RPA) {
                this.azimuthalAngle = third;
                this.polarAngle = second;
            } else {
                this.azimuthalAngle = second;
                this.polarAngle = third;
            }
            // Since we have to run the calculations anyway, ensure they are cached.
            this.x = this.radialDistance * this.getAzimuthalAngleCos() * this.getPolarAngleSin();
            this.y = this.radialDistance * this.getAzimuthalAngleSin() * this.getPolarAngleSin();
            this.z = this.radialDistance * this.getPolarAngleCos();
        }
    }

    /**
     * Get the x coordinate
     *
     * @return The x coordinate
     */
    public double getX() {
        return x;
    }

    /**
     * Get the y coordinate
     *
     * @return The y coordinate
     */
    public double getY() {
        return y;
    }

    /**
     * Get the z coordinate
     *
     * @return The z coordinate
     */
    public double getZ() {
        return z;
    }

    /**
     * Get the radius
     *
     * @return The radius
     */
    public double getRadialDistance() {
        if (Double.isNaN(this.radialDistance)) {
            this.radialDistance = Math.sqrt(Math.pow(this.x, 2) + Math.pow(this.y, 2) + Math.pow(this.z, 2));
        }
        return this.radialDistance;
    }

    /**
     * Get the polar angle (inclination)
     *
     * @return The polar angle
     */
    public double getPolarAngle() {
        if (Double.isNaN(this.polarAngle)) {
            // This was Math.atan(x, z) in the Mapillary plugin
            // This should be Math.atan(y, z)
            this.polarAngle = Math.atan2(this.x, this.z);
        }
        return this.polarAngle;
    }

    /**
     * Get the polar angle cossine (inclination)
     *
     * @return The polar angle cosine
     */
    public double getPolarAngleCos() {
        if (Double.isNaN(this.polarAngleCos)) {
            this.polarAngleCos = Math.cos(this.getPolarAngle());
        }
        return this.polarAngleCos;
    }

    /**
     * Get the polar angle sine (inclination)
     *
     * @return The polar angle sine
     */
    public double getPolarAngleSin() {
        if (Double.isNaN(this.polarAngleSin)) {
            this.polarAngleSin = Math.sin(this.getPolarAngle());
        }
        return this.polarAngleSin;
    }

    /**
     * Get the azimuthal angle
     *
     * @return The azimuthal angle
     */
    public double getAzimuthalAngle() {
        if (Double.isNaN(this.azimuthalAngle)) {
            if (Double.isNaN(this.radialDistance)) {
                // Force calculation
                this.getRadialDistance();
            }
            // Avoid issues where x, y, and z are 0
            if (this.radialDistance == 0) {
                this.azimuthalAngle = 0;
            } else {
                // This was Math.acos(y / radialDistance) in the Mapillary plugin
                // This should be Math.acos(z / radialDistance)
                this.azimuthalAngle = Math.acos(this.y / this.radialDistance);
            }
        }
        return this.azimuthalAngle;
    }

    /**
     * Get the azimuthal angle cosine
     *
     * @return The azimuthal angle cosine
     */
    public double getAzimuthalAngleCos() {
        if (Double.isNaN(this.azimuthalAngleCos)) {
            this.azimuthalAngleCos = Math.cos(this.getAzimuthalAngle());
        }
        return this.azimuthalAngleCos;
    }

    /**
     * Get the azimuthal angle sine
     *
     * @return The azimuthal angle sine
     */
    public double getAzimuthalAngleSin() {
        if (Double.isNaN(this.azimuthalAngleSin)) {
            this.azimuthalAngleSin = Math.sin(this.getAzimuthalAngle());
        }
        return this.azimuthalAngleSin;
    }

    /**
     * Normalize the vector
     *
     * @return A normalized vector
     */
    public Vector3D normalize() {
        final double length = Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2) + Math.pow(z, 2));
        final double newX;
        final double newY;
        final double newZ;
        if (length == 0 || Double.isNaN(length)) {
            newX = 0;
            newY = 0;
            newZ = 0;
        } else {
            newX = x / length;
            newY = y / length;
            newZ = z / length;
        }
        return new Vector3D(newX, newY, newZ);
    }

    @Override
    public int hashCode() {
        return Double.hashCode(this.x) + 31 * Double.hashCode(this.y) + 31 * 31 * Double.hashCode(this.z);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Vector3D) {
            Vector3D other = (Vector3D) o;
            return this.x == other.x && this.y == other.y && this.z == other.z;
        }
        return false;
    }

    @Override
    public String toString() {
        return "[x=" + this.x + ", y=" + this.y + ", z=" + this.z + ", r=" + this.radialDistance + ", inclination="
            + this.polarAngle + ", azimuthal=" + this.azimuthalAngle + "]";
    }
}
