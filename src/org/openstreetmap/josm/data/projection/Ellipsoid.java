/*
 * Import from fr.geo.convert package, a geographic coordinates converter.
 * (http://www.i3s.unice.fr/~johan/gps/)
 * License: GPL. For details, see LICENSE file.
 * Copyright (C) 2002 Johan Montagnat (johan@creatis.insa-lyon.fr)
 */

package org.openstreetmap.josm.data.projection;

/**
 * the reference ellipsoids
 */
public class Ellipsoid {
    /**
     * Clarke's ellipsoid (NTF system)
     */
    public static final Ellipsoid clarke = new Ellipsoid(6378249.2, 6356515.0);
    /**
     * Hayford's ellipsoid (ED50 system)
     */
    public static final Ellipsoid hayford =
        new Ellipsoid(6378388.0, 6356911.9461);
    /**
     * GRS80 ellipsoid
     */
    public static final Ellipsoid GRS80 = new Ellipsoid(6378137.0, 6356752.3141);

    /**
     * WGS84 ellipsoid
     */
    public static final Ellipsoid WGS84 = new Ellipsoid(6378137.0, 6356752.3142);

    /**
     * half long axis
     */
    public final double a;
    /**
     * half short axis
     */
    public final double b;
    /**
     * first eccentricity
     */
    public final double e;
    /**
     * first eccentricity squared
     */
    public final double e2;

    /**
     * square of the second eccentricity
     */
    public final double eb2;

    /**
     * create a new ellipsoid and precompute its parameters
     *
     * @param a ellipsoid long axis (in meters)
     * @param b ellipsoid short axis (in meters)
     */
    public Ellipsoid(double a, double b) {
        this.a = a;
        this.b = b;
        e2 = (a*a - b*b) / (a*a);
        e = Math.sqrt(e2);
        eb2 = e2 / (1.0 - e2);
    }

    /**
     * Returns the <i>radius of curvature in the prime vertical</i>
     * for this reference ellipsoid at the specified latitude.
     *
     * @param phi The local latitude (radians).
     * @return The radius of curvature in the prime vertical (meters).
     */
    public double verticalRadiusOfCurvature(final double phi) {
        return a / Math.sqrt(1.0 - (e2 * sqr(Math.sin(phi))));
    }

    private static double sqr(final double x) {
        return x * x;
    }

    /**
     *  Returns the meridional arc, the true meridional distance on the
     * ellipsoid from the equator to the specified latitude, in meters.
     *
     * @param phi   The local latitude (in radians).
     * @return  The meridional arc (in meters).
     */
    public double meridionalArc(final double phi) {
        final double sin2Phi = Math.sin(2.0 * phi);
        final double sin4Phi = Math.sin(4.0 * phi);
        final double sin6Phi = Math.sin(6.0 * phi);
        final double sin8Phi = Math.sin(8.0 * phi);
        // TODO . calculate 'f'
        //double f = 1.0 / 298.257222101; // GRS80
        double f = 1.0 / 298.257223563; // WGS84
        final double n = f / (2.0 - f);
        final double n2 = n * n;
        final double n3 = n2 * n;
        final double n4 = n3 * n;
        final double n5 = n4 * n;
        final double n1n2 = n - n2;
        final double n2n3 = n2 - n3;
        final double n3n4 = n3 - n4;
        final double n4n5 = n4 - n5;
        final double ap = a * (1.0 - n + (5.0 / 4.0) * (n2n3) + (81.0 / 64.0) * (n4n5));
        final double bp = (3.0 / 2.0) * a * (n1n2 + (7.0 / 8.0) * (n3n4) + (55.0 / 64.0) * n5);
        final double cp = (15.0 / 16.0) * a * (n2n3 + (3.0 / 4.0) * (n4n5));
        final double dp = (35.0 / 48.0) * a * (n3n4 + (11.0 / 16.0) * n5);
        final double ep = (315.0 / 512.0) * a * (n4n5);
        return ap * phi - bp * sin2Phi + cp * sin4Phi - dp * sin6Phi + ep * sin8Phi;
    }

    /**
     *  Returns the <i>radius of curvature in the meridian<i>
     *  for this reference ellipsoid at the specified latitude.
     *
     * @param phi The local latitude (in radians).
     * @return  The radius of curvature in the meridian (in meters).
     */
    public double meridionalRadiusOfCurvature(final double phi) {
        return verticalRadiusOfCurvature(phi)
        / (1.0 + eb2 * sqr(Math.cos(phi)));
    }

    /**
     * Returns isometric latitude of phi on given first eccentricity (e)
     * @param phi The local latitude (radians).
     * @return isometric latitude of phi on first eccentricity (e)
     */
    public double latitudeIsometric(double phi, double e) {
        double v1 = 1-e*Math.sin(phi);
        double v2 = 1+e*Math.sin(phi);
        return Math.log(Math.tan(Math.PI/4+phi/2)*Math.pow(v1/v2,e/2));
    }

    /**
     * Returns isometric latitude of phi on first eccentricity (e)
     * @param phi The local latitude (radians).
     * @return isometric latitude of phi on first eccentricity (e)
     */
    public double latitudeIsometric(double phi) {
        double v1 = 1-e*Math.sin(phi);
        double v2 = 1+e*Math.sin(phi);
        return Math.log(Math.tan(Math.PI/4+phi/2)*Math.pow(v1/v2,e/2));
    }

    /*
     * Returns geographic latitude of isometric latitude of first eccentricity (e)
     * and epsilon precision
     */
    public double latitude(double latIso, double e, double epsilon) {
        double lat0 = 2*Math.atan(Math.exp(latIso))-Math.PI/2;
        double lati = lat0;
        double lati1 = 1.0; // random value to start the iterative processus
        while(Math.abs(lati1-lati)>=epsilon) {
            lati = lati1;
            double v1 = 1+e*Math.sin(lati);
            double v2 = 1-e*Math.sin(lati);
            lati1 = 2*Math.atan(Math.pow(v1/v2,e/2)*Math.exp(latIso))-Math.PI/2;
        }
        return lati1;
    }
}
