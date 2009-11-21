// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.data.projection;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagLayout;
import java.util.Collection;
import java.util.Collections;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.tools.GBC;

/**
 * Directly use latitude / longitude values as x/y.
 *
 * @author Dirk Stöcker
 * code based on JavaScript from Chuck Taylor
 */
public class UTM implements Projection, ProjectionSubPrefs {

    public static final int DEFAULT_ZONE = 30;
    private int zone = DEFAULT_ZONE;

    final private double UTMScaleFactor = 0.9996;

    /* Ellipsoid model constants (WGS84) - TODO Use Elliposid class here too */
    final private double sm_EccSquared = 6.69437999013e-03;

    /*
    * ArcLengthOfMeridian
    *
    * Computes the ellipsoidal distance from the equator to a point at a
    * given latitude.
    *
    * Reference: Hoffmann-Wellenhof, B., Lichtenegger, H., and Collins, J.,
    * GPS: Theory and Practice, 3rd ed.  New York: Springer-Verlag Wien, 1994.
    *
    * Inputs:
    *     phi - Latitude of the point, in radians.
    *
    * Globals:
    *     Ellipsoid.GRS80.a - Ellipsoid model major axis.
    *     Ellipsoid.GRS80.b - Ellipsoid model minor axis.
    *
    * Returns:
    *     The ellipsoidal distance of the point from the equator, in meters.
    *
    */
    private double ArcLengthOfMeridian(double phi)
    {
        /* Precalculate n */
        double n = (Ellipsoid.GRS80.a - Ellipsoid.GRS80.b) / (Ellipsoid.GRS80.a + Ellipsoid.GRS80.b);

        /* Precalculate alpha */
        double alpha = ((Ellipsoid.GRS80.a + Ellipsoid.GRS80.b) / 2.0)
           * (1.0 + (Math.pow (n, 2.0) / 4.0) + (Math.pow (n, 4.0) / 64.0));

        /* Precalculate beta */
        double beta = (-3.0 * n / 2.0) + (9.0 * Math.pow (n, 3.0) / 16.0)
           + (-3.0 * Math.pow (n, 5.0) / 32.0);

        /* Precalculate gamma */
        double gamma = (15.0 * Math.pow (n, 2.0) / 16.0)
            + (-15.0 * Math.pow (n, 4.0) / 32.0);

        /* Precalculate delta */
        double delta = (-35.0 * Math.pow (n, 3.0) / 48.0)
            + (105.0 * Math.pow (n, 5.0) / 256.0);

        /* Precalculate epsilon */
        double epsilon = (315.0 * Math.pow (n, 4.0) / 512.0);

        /* Now calculate the sum of the series and return */
        return alpha
        * (phi + (beta * Math.sin (2.0 * phi))
            + (gamma * Math.sin (4.0 * phi))
            + (delta * Math.sin (6.0 * phi))
            + (epsilon * Math.sin (8.0 * phi)));
    }

    /*
    * UTMCentralMeridian
    *
    * Determines the central meridian for the given UTM zone.
    *
    * Inputs:
    *     zone - An integer value designating the UTM zone, range [1,60].
    *
    * Returns:
    *   The central meridian for the given UTM zone, in radians, or zero
    *   if the UTM zone parameter is outside the range [1,60].
    *   Range of the central meridian is the radian equivalent of [-177,+177].
    *
    */
    private double UTMCentralMeridian(int zone)
    {
        return Math.toRadians(-183.0 + (zone * 6.0));
    }
    private double UTMCentralMeridianDeg(int zone)
    {
        return -183.0 + (zone * 6.0);
    }

    /*
    * FootpointLatitude
    *
    * Computes the footpoint latitude for use in converting transverse
    * Mercator coordinates to ellipsoidal coordinates.
    *
    * Reference: Hoffmann-Wellenhof, B., Lichtenegger, H., and Collins, J.,
    *   GPS: Theory and Practice, 3rd ed.  New York: Springer-Verlag Wien, 1994.
    *
    * Inputs:
    *   y - The UTM northing coordinate, in meters.
    *
    * Returns:
    *   The footpoint latitude, in radians.
    *
    */
    private double FootpointLatitude(double y)
    {
        /* Precalculate n (Eq. 10.18) */
        double n = (Ellipsoid.GRS80.a - Ellipsoid.GRS80.b) / (Ellipsoid.GRS80.a + Ellipsoid.GRS80.b);

        /* Precalculate alpha_ (Eq. 10.22) */
        /* (Same as alpha in Eq. 10.17) */
        double alpha_ = ((Ellipsoid.GRS80.a + Ellipsoid.GRS80.b) / 2.0)
            * (1 + (Math.pow (n, 2.0) / 4) + (Math.pow (n, 4.0) / 64));

        /* Precalculate y_ (Eq. 10.23) */
        double y_ = y / alpha_;

        /* Precalculate beta_ (Eq. 10.22) */
        double beta_ = (3.0 * n / 2.0) + (-27.0 * Math.pow (n, 3.0) / 32.0)
            + (269.0 * Math.pow (n, 5.0) / 512.0);

        /* Precalculate gamma_ (Eq. 10.22) */
        double gamma_ = (21.0 * Math.pow (n, 2.0) / 16.0)
            + (-55.0 * Math.pow (n, 4.0) / 32.0);

        /* Precalculate delta_ (Eq. 10.22) */
        double delta_ = (151.0 * Math.pow (n, 3.0) / 96.0)
            + (-417.0 * Math.pow (n, 5.0) / 128.0);

        /* Precalculate epsilon_ (Eq. 10.22) */
        double epsilon_ = (1097.0 * Math.pow (n, 4.0) / 512.0);

        /* Now calculate the sum of the series (Eq. 10.21) */
        return y_ + (beta_ * Math.sin (2.0 * y_))
            + (gamma_ * Math.sin (4.0 * y_))
            + (delta_ * Math.sin (6.0 * y_))
            + (epsilon_ * Math.sin (8.0 * y_));
    }

    /*
    * MapLatLonToXY
    *
    * Converts a latitude/longitude pair to x and y coordinates in the
    * Transverse Mercator projection.  Note that Transverse Mercator is not
    * the same as UTM; a scale factor is required to convert between them.
    *
    * Reference: Hoffmann-Wellenhof, B., Lichtenegger, H., and Collins, J.,
    * GPS: Theory and Practice, 3rd ed.  New York: Springer-Verlag Wien, 1994.
    *
    * Inputs:
    *    phi - Latitude of the point, in radians.
    *    lambda - Longitude of the point, in radians.
    *    lambda0 - Longitude of the central meridian to be used, in radians.
    *
    * Outputs:
    *    xy - A 2-element array containing the x and y coordinates
    *         of the computed point.
    *
    * Returns:
    *    The function does not return a value.
    *
    */
    public EastNorth MapLatLonToXY(double phi, double lambda, double lambda0)
    {
        /* Precalculate ep2 */
        double ep2 = (Math.pow (Ellipsoid.GRS80.a, 2.0) - Math.pow (Ellipsoid.GRS80.b, 2.0)) / Math.pow (Ellipsoid.GRS80.b, 2.0);

        /* Precalculate nu2 */
        double nu2 = ep2 * Math.pow (Math.cos (phi), 2.0);

        /* Precalculate N */
        double N = Math.pow (Ellipsoid.GRS80.a, 2.0) / (Ellipsoid.GRS80.b * Math.sqrt (1 + nu2));

        /* Precalculate t */
        double t = Math.tan (phi);
        double t2 = t * t;
        double tmp = (t2 * t2 * t2) - Math.pow (t, 6.0);

        /* Precalculate l */
        double l = lambda - lambda0;

        /* Precalculate coefficients for l**n in the equations below
           so a normal human being can read the expressions for easting
           and northing
           -- l**1 and l**2 have coefficients of 1.0 */
        double l3coef = 1.0 - t2 + nu2;

        double l4coef = 5.0 - t2 + 9 * nu2 + 4.0 * (nu2 * nu2);

        double l5coef = 5.0 - 18.0 * t2 + (t2 * t2) + 14.0 * nu2
            - 58.0 * t2 * nu2;

        double l6coef = 61.0 - 58.0 * t2 + (t2 * t2) + 270.0 * nu2
            - 330.0 * t2 * nu2;

        double l7coef = 61.0 - 479.0 * t2 + 179.0 * (t2 * t2) - (t2 * t2 * t2);

        double l8coef = 1385.0 - 3111.0 * t2 + 543.0 * (t2 * t2) - (t2 * t2 * t2);

        return new EastNorth(
        /* Calculate easting (x) */
        N * Math.cos (phi) * l
            + (N / 6.0 * Math.pow (Math.cos (phi), 3.0) * l3coef * Math.pow (l, 3.0))
            + (N / 120.0 * Math.pow (Math.cos (phi), 5.0) * l5coef * Math.pow (l, 5.0))
            + (N / 5040.0 * Math.pow (Math.cos (phi), 7.0) * l7coef * Math.pow (l, 7.0)),
        /* Calculate northing (y) */
        ArcLengthOfMeridian (phi)
            + (t / 2.0 * N * Math.pow (Math.cos (phi), 2.0) * Math.pow (l, 2.0))
            + (t / 24.0 * N * Math.pow (Math.cos (phi), 4.0) * l4coef * Math.pow (l, 4.0))
            + (t / 720.0 * N * Math.pow (Math.cos (phi), 6.0) * l6coef * Math.pow (l, 6.0))
            + (t / 40320.0 * N * Math.pow (Math.cos (phi), 8.0) * l8coef * Math.pow (l, 8.0)));
    }

    /*
    * MapXYToLatLon
    *
    * Converts x and y coordinates in the Transverse Mercator projection to
    * a latitude/longitude pair.  Note that Transverse Mercator is not
    * the same as UTM; a scale factor is required to convert between them.
    *
    * Reference: Hoffmann-Wellenhof, B., Lichtenegger, H., and Collins, J.,
    *   GPS: Theory and Practice, 3rd ed.  New York: Springer-Verlag Wien, 1994.
    *
    * Inputs:
    *   x - The easting of the point, in meters.
    *   y - The northing of the point, in meters.
    *   lambda0 - Longitude of the central meridian to be used, in radians.
    *
    * Outputs:
    *   philambda - A 2-element containing the latitude and longitude
    *               in radians.
    *
    * Returns:
    *   The function does not return a value.
    *
    * Remarks:
    *   The local variables Nf, nuf2, tf, and tf2 serve the same purpose as
    *   N, nu2, t, and t2 in MapLatLonToXY, but they are computed with respect
    *   to the footpoint latitude phif.
    *
    *   x1frac, x2frac, x2poly, x3poly, etc. are to enhance readability and
    *   to optimize computations.
    *
    */
    public LatLon MapXYToLatLon(double x, double y, double lambda0)
    {
        /* Get the value of phif, the footpoint latitude. */
        double phif = FootpointLatitude (y);

        /* Precalculate ep2 */
        double ep2 = (Math.pow (Ellipsoid.GRS80.a, 2.0) - Math.pow (Ellipsoid.GRS80.b, 2.0))
              / Math.pow (Ellipsoid.GRS80.b, 2.0);

        /* Precalculate cos (phif) */
        double cf = Math.cos (phif);

        /* Precalculate nuf2 */
        double nuf2 = ep2 * Math.pow (cf, 2.0);

        /* Precalculate Nf and initialize Nfpow */
        double Nf = Math.pow (Ellipsoid.GRS80.a, 2.0) / (Ellipsoid.GRS80.b * Math.sqrt (1 + nuf2));
        double Nfpow = Nf;

        /* Precalculate tf */
        double tf = Math.tan (phif);
        double tf2 = tf * tf;
        double tf4 = tf2 * tf2;

        /* Precalculate fractional coefficients for x**n in the equations
           below to simplify the expressions for latitude and longitude. */
        double x1frac = 1.0 / (Nfpow * cf);

        Nfpow *= Nf;   /* now equals Nf**2) */
        double x2frac = tf / (2.0 * Nfpow);

        Nfpow *= Nf;   /* now equals Nf**3) */
        double x3frac = 1.0 / (6.0 * Nfpow * cf);

        Nfpow *= Nf;   /* now equals Nf**4) */
        double x4frac = tf / (24.0 * Nfpow);

        Nfpow *= Nf;   /* now equals Nf**5) */
        double x5frac = 1.0 / (120.0 * Nfpow * cf);

        Nfpow *= Nf;   /* now equals Nf**6) */
        double x6frac = tf / (720.0 * Nfpow);

        Nfpow *= Nf;   /* now equals Nf**7) */
        double x7frac = 1.0 / (5040.0 * Nfpow * cf);

        Nfpow *= Nf;   /* now equals Nf**8) */
        double x8frac = tf / (40320.0 * Nfpow);

        /* Precalculate polynomial coefficients for x**n.
           -- x**1 does not have a polynomial coefficient. */
        double x2poly = -1.0 - nuf2;
        double x3poly = -1.0 - 2 * tf2 - nuf2;
        double x4poly = 5.0 + 3.0 * tf2 + 6.0 * nuf2 - 6.0 * tf2 * nuf2 - 3.0 * (nuf2 *nuf2) - 9.0 * tf2 * (nuf2 * nuf2);
        double x5poly = 5.0 + 28.0 * tf2 + 24.0 * tf4 + 6.0 * nuf2 + 8.0 * tf2 * nuf2;
        double x6poly = -61.0 - 90.0 * tf2 - 45.0 * tf4 - 107.0 * nuf2 + 162.0 * tf2 * nuf2;
        double x7poly = -61.0 - 662.0 * tf2 - 1320.0 * tf4 - 720.0 * (tf4 * tf2);
        double x8poly = 1385.0 + 3633.0 * tf2 + 4095.0 * tf4 + 1575 * (tf4 * tf2);

        return new LatLon(
        /* Calculate latitude */
        Math.toDegrees(
        phif + x2frac * x2poly * (x * x)
        + x4frac * x4poly * Math.pow (x, 4.0)
        + x6frac * x6poly * Math.pow (x, 6.0)
        + x8frac * x8poly * Math.pow (x, 8.0)),
        Math.toDegrees(
        /* Calculate longitude */
        lambda0 + x1frac * x
        + x3frac * x3poly * Math.pow (x, 3.0)
        + x5frac * x5poly * Math.pow (x, 5.0)
        + x7frac * x7poly * Math.pow (x, 7.0)));
    }

    public EastNorth latlon2eastNorth(LatLon p) {
        EastNorth a = MapLatLonToXY(Math.toRadians(p.lat()), Math.toRadians(p.lon()), UTMCentralMeridian(getzone()));
        return new EastNorth(a.east() * UTMScaleFactor + 3500000.0, a.north() * UTMScaleFactor);
    }

    public LatLon eastNorth2latlon(EastNorth p) {
        return MapXYToLatLon((p.east()-3500000.0)/UTMScaleFactor, p.north()/UTMScaleFactor, UTMCentralMeridian(getzone()));
    }

    @Override public String toString() {
        return tr("UTM");
    }

    public int getzone()
    {
        return zone;
    }

    public String toCode() {
        return "EPSG:"+ (325800 + getzone());
    }

    public String getCacheDirectoryName() {
        return "epsg"+ (325800 + getzone());
    }

    public Bounds getWorldBoundsLatLon()
    {
        return new Bounds(
                new LatLon(-85.0, UTMCentralMeridianDeg(getzone())-5.0),
                new LatLon(85.0, UTMCentralMeridianDeg(getzone())+5.0));
    }

    public double getDefaultZoomInPPD() {
        // this will set the map scaler to about 1000 m
        return 10;
    }

    private JPanel prefpanel = null;
    private JComboBox prefcb = null;
    public JPanel getPreferencePanel() {
        if(prefpanel != null)
            return prefpanel;

        prefcb = new JComboBox();
        for(int i = 1; i <= 60; i++) {
            prefcb.addItem(i);
        }

        prefcb.setSelectedIndex(zone - 1);
        prefpanel = new JPanel(new GridBagLayout());
        prefpanel.add(new JLabel(tr("UTM Zone")), GBC.std().insets(5,5,0,5));
        prefpanel.add(GBC.glue(1, 0), GBC.std().fill(GBC.HORIZONTAL));
        prefpanel.add(prefcb, GBC.eop().fill(GBC.HORIZONTAL));
        prefpanel.add(GBC.glue(1, 1), GBC.eol().fill(GBC.BOTH));
        return prefpanel;
    }

    public Collection<String> getPreferences() {
        if(prefcb == null)
            return null;
        int zone = prefcb.getSelectedIndex() + 1;
        return Collections.singleton(Integer.toString(zone));
    }

    public void destroyCachedPanel() {
        prefpanel = null;
        prefcb = null;
    }

    public void setPreferences(Collection<String> args)
    {
        zone = DEFAULT_ZONE;
        try {
            for(String s : args)
            {
                zone = Integer.parseInt(s);
                if(zone <= 0 || zone > 60)
                    zone = DEFAULT_ZONE;
                break;
            }
        } catch(NumberFormatException e) {}
    }

    public Collection<String> getPreferencesFromCode(String code)
    {
        if(code.startsWith("EPSG:3258"))
        {
            try {
                String zonestring = code.substring(9);
                int zoneval = Integer.parseInt(zonestring);
                if(zoneval > 0 && zone <= 60)
                {
                    return Collections.singleton(zonestring);
                }
            } catch(NumberFormatException e) {}
        }
        return null;
    }
}
