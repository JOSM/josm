// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection.proj;

import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.projection.ProjectionConfigurationException;
import org.openstreetmap.josm.tools.JosmRuntimeException;

/**
 * Azimuthal Equidistant projection.
 * <p>
 * This implementation does not include the Guam or Micronesia variants.
 *
 * @author Gerald Evenden (original PROJ.4 implementation in C)
 * @author Ben Caradoc-Davies (Transient Software Limited)
 * @see <a href="https://pubs.er.usgs.gov/publication/pp1395"><em>Map Projections: A Working Manual</em>, Snyder (1987), pages 191-202</a>
 * @see <a href="http://geotiff.maptools.org/proj_list/azimuthal_equidistant.html">PROJ.4 notes on parameters</a>
 * @see <a href="https://github.com/OSGeo/proj.4/blob/master/src/PJ_aeqd.c">PROJ.4 implemention in C</a>
 * @see <a href="https://en.wikipedia.org/wiki/Azimuthal_equidistant_projection">Wikipedia</a>
 * @see <a href="http://mathworld.wolfram.com/AzimuthalEquidistantProjection.html">Wolfram Alpha</a>
 * @since 13598
 */
public class AzimuthalEquidistant extends AbstractProj {

    /**
     * Less strict tolerance.
     */
    public static final double EPS10 = 1.e-10;

    /**
     * Stricter tolerance.
     */
    public static final double TOL = 1.e-14;

    /**
     * Half of π.
     */
    public static final double HALF_PI = Math.PI / 2;

    /**
     * The four possible modes or aspects of the projection.
     */
    public enum Mode {
        /** North pole */
        NORTH_POLAR,
        /** South pole */
        SOUTH_POLAR,
        /** Equator */
        EQUATORIAL,
        /** Oblique */
        OBLIQUE;
    }

    /**
     * Length of semi-major axis, in metres. This is named '<var>a</var>' or '<var>R</var>'
     * (Radius in spherical cases) in Snyder.
     */
    protected double semiMajor;

    /**
     * Length of semi-minor axis, in metres. This is named '<var>b</var>' in Snyder.
     */
    protected double semiMinor;

    /**
     * Central longitude in <u>radians</u>. Default value is 0, the Greenwich meridian.
     * This is called '<var>lambda0</var>' in Snyder.
     */
    protected double centralMeridian;

    /**
     * Latitude of origin in <u>radians</u>. Default value is 0, the equator.
     * This is called '<var>phi0</var>' in Snyder.
     */
    protected double latitudeOfOrigin;

    /**
     * The mode or aspect of the projection.
     */
    protected Mode mode;

    /**
     * Geodesic calculator used for this projection. Not used and set to null for polar projections.
     */
    //protected Geodesic geodesic; // See https://josm.openstreetmap.de/ticket/16129#comment:21

    /**
     * The sine of the central latitude of the projection.
     */
    protected double sinph0;

    /**
     * The cosine of the central latitude of the projection.
     */
    protected double cosph0;

    /**
     * Meridian distance from the equator to the pole. Not used and set to NaN for non-polar projections.
     */
    protected double mp;

    @Override
    public String getName() {
        return tr("Azimuthal Equidistant");
    }

    @Override
    public String getProj4Id() {
        return "aeqd";
    }

    @Override
    public void initialize(ProjParameters params) throws ProjectionConfigurationException {
        super.initialize(params);
        if (params.lon0 == null)
            throw new ProjectionConfigurationException(tr("Parameter ''{0}'' required.", "lon_0"));
        if (params.lat0 == null)
            throw new ProjectionConfigurationException(tr("Parameter ''{0}'' required.", "lat_0"));
        centralMeridian = Math.toRadians(params.lon0);
        latitudeOfOrigin = Math.toRadians(params.lat0);
        semiMajor = params.ellps.a;
        semiMinor = params.ellps.b;
        if (Math.abs(latitudeOfOrigin - HALF_PI) < EPS10) {
            mode = Mode.NORTH_POLAR;
            mp = mlfn(HALF_PI, 1, 0);
            sinph0 = 1;
            cosph0 = 0;
        } else if (Math.abs(latitudeOfOrigin + HALF_PI) < EPS10) {
            mode = Mode.SOUTH_POLAR;
            mp = mlfn(-HALF_PI, -1, 0);
            sinph0 = -1;
            cosph0 = 0;
        } else if (Math.abs(latitudeOfOrigin) < EPS10) {
            mode = Mode.EQUATORIAL;
            mp = Double.NaN;
            sinph0 = 0;
            cosph0 = 1;
            //geodesic = new Geodesic(semiMajor, (semiMajor - semiMinor) / semiMajor);
            throw new ProjectionConfigurationException("Equatorial AzimuthalEquidistant not yet supported");
        } else {
            mode = Mode.OBLIQUE;
            mp = Double.NaN;
            sinph0 = Math.sin(latitudeOfOrigin);
            cosph0 = Math.cos(latitudeOfOrigin);
            //geodesic = new Geodesic(semiMajor, (semiMajor - semiMinor) / semiMajor);
            throw new ProjectionConfigurationException("Oblique AzimuthalEquidistant not yet supported");
        }
    }

    @Override
    public Bounds getAlgorithmBounds() {
        return new Bounds(-89, -180, 89, 180, false);
    }

    @Override
    public double[] project(double latRad, double lonRad) {
        return spherical ? projectSpherical(latRad, lonRad) : projectEllipsoidal(latRad, lonRad);
    }

    @Override
    public double[] invproject(double east, double north) {
        return spherical ? invprojectSpherical(east, north) : invprojectEllipsoidal(east, north);
    }

    double[] projectSpherical(double latRad, double lonRad) {
        double x = 0;
        double y = 0;
        double sinphi = Math.sin(latRad);
        double cosphi = Math.cos(latRad);
        double coslam = Math.cos(lonRad);
        switch (mode) {
        case EQUATORIAL:
        case OBLIQUE:
            if (mode == Mode.EQUATORIAL) {
                y = cosphi * coslam;
            } else { // Oblique
                y = sinph0 * sinphi + cosph0 * cosphi * coslam;
            }
            if (Math.abs(Math.abs(y) - 1) < TOL) {
                if (y < 0) {
                    throw new JosmRuntimeException("TOLERANCE_ERROR");
                } else {
                    x = 0;
                    y = 0;
                }
            } else {
                y = Math.acos(y);
                y /= Math.sin(y);
                x = y * cosphi * Math.sin(lonRad);
                y *= (mode == Mode.EQUATORIAL) ? sinphi
                        : (cosph0 * sinphi - sinph0 * cosphi * coslam);
            }
            break;
        case NORTH_POLAR:
            latRad = -latRad;
            coslam = -coslam;
            // fall through
        case SOUTH_POLAR:
            if (Math.abs(latRad - HALF_PI) < EPS10) {
                throw new JosmRuntimeException("TOLERANCE_ERROR");
            }
            y = HALF_PI + latRad;
            x = y * Math.sin(lonRad);
            y *= coslam;
            break;
        }
        return new double[] {x, y};
    }

    double[] invprojectSpherical(double east, double north) {
        double x = east;
        double y = north;
        double lambda = 0;
        double phi = 0;
        double c_rh = Math.hypot(x, y);
        if (c_rh > Math.PI) {
            if (c_rh - EPS10 > Math.PI) {
                throw new JosmRuntimeException("TOLERANCE_ERROR");
            }
        } else if (c_rh < EPS10) {
            phi = latitudeOfOrigin;
            lambda = 0.;
        } else {
            if (mode == Mode.OBLIQUE || mode == Mode.EQUATORIAL) {
                double sinc = Math.sin(c_rh);
                double cosc = Math.cos(c_rh);
                if (mode == Mode.EQUATORIAL) {
                    phi = aasin(y * sinc / c_rh);
                    x *= sinc;
                    y = cosc * c_rh;
                } else { // Oblique
                    phi = aasin(cosc * sinph0 + y * sinc * cosph0 / c_rh);
                    y = (cosc - sinph0 * Math.sin(phi)) * c_rh;
                    x *= sinc * cosph0;
                }
                lambda = (y == 0) ? 0 : Math.atan2(x, y);
            } else if (mode == Mode.NORTH_POLAR) {
                phi = HALF_PI - c_rh;
                lambda = Math.atan2(x, -y);
            } else { // South Polar
                phi = c_rh - HALF_PI;
                lambda = Math.atan2(x, y);
            }
        }
        return new double[] {phi, lambda};
    }

    double[] projectEllipsoidal(double latRad, double lonRad) {
        double x = 0;
        double y = 0;
        double coslam = Math.cos(lonRad);
        double cosphi = Math.cos(latRad);
        double sinphi = Math.sin(latRad);
        switch (mode) {
        case NORTH_POLAR:
            coslam = -coslam;
            // fall through
        case SOUTH_POLAR:
            double rho = Math.abs(mp - mlfn(latRad, sinphi, cosphi));
            x = rho * Math.sin(lonRad);
            y = rho * coslam;
            break;
        case EQUATORIAL:
        case OBLIQUE:
            if (Math.abs(lonRad) < EPS10 && Math.abs(latRad - latitudeOfOrigin) < EPS10) {
                x = 0;
                y = 0;
                break;
            }
            /*GeodesicData g = geodesic.Inverse(Math.toDegrees(latitudeOfOrigin),
                    Math.toDegrees(centralMeridian), Math.toDegrees(latRad),
                    Math.toDegrees(lonRad + centralMeridian));
            double azi1 = Math.toRadians(g.azi1);
            x = g.s12 * Math.sin(azi1) / semiMajor;
            y = g.s12 * Math.cos(azi1) / semiMajor;*/
            break;
        }
        return new double[] {x, y};
    }

    double[] invprojectEllipsoidal(double east, double north) {
        double x = east;
        double y = north;
        double lambda = 0;
        double phi = 0;
        double c = Math.hypot(x, y);
        if (c < EPS10) {
            phi = latitudeOfOrigin;
            lambda = 0;
        } else {
            if (mode == Mode.OBLIQUE || mode == Mode.EQUATORIAL) {
                /*double x2 = x * semiMajor;
                double y2 = y * semiMajor;
                double azi1 = Math.atan2(x2, y2);
                double s12 = Math.sqrt(x2 * x2 + y2 * y2);
                GeodesicData g = geodesic.Direct(Math.toDegrees(latitudeOfOrigin),
                        Math.toDegrees(centralMeridian), Math.toDegrees(azi1), s12);
                phi = Math.toRadians(g.lat2);
                lambda = Math.toRadians(g.lon2);*/
                lambda -= centralMeridian;
            } else { // Polar
                phi = invMlfn((mode == Mode.NORTH_POLAR) ? (mp - c) : (mp + c));
                lambda = Math.atan2(x, (mode == Mode.NORTH_POLAR) ? -y : y);
            }
        }
        return new double[] {phi, lambda};
    }
}
