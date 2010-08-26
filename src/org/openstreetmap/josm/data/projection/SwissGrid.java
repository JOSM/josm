//License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Collection;
import java.util.Collections;
import javax.swing.Box;
import javax.swing.JPanel;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.gui.widgets.HtmlPanel;
import org.openstreetmap.josm.tools.GBC;

/**
 * Projection for the SwissGrid CH1903 / L03, see http://de.wikipedia.org/wiki/Swiss_Grid.
 *
 * Calculations are based on formula from
 * http://www.swisstopo.admin.ch/internet/swisstopo/en/home/topics/survey/sys/refsys/switzerland.parsysrelated1.37696.downloadList.12749.DownloadFile.tmp/ch1903wgs84en.pdf
 *
 * August 2010 update to this formula (rigorous formulas)
 * http://www.swisstopo.admin.ch/internet/swisstopo/en/home/topics/survey/sys/refsys/switzerland.parsysrelated1.37696.downloadList.97912.DownloadFile.tmp/swissprojectionen.pdf
 */
public class SwissGrid implements Projection, ProjectionSubPrefs {

    private static final double dX = 674.374;
    private static final double dY = 15.056;
    private static final double dZ = 405.346;

    private static final double phi0 = Math.toRadians(46.0 + 57.0 / 60 + 8.66 / 3600);
    private static final double lambda0 = Math.toRadians(7.0 + 26.0 / 60 + 22.50 / 3600);
    private static final double R = Ellipsoid.Bessel1841.a * Math.sqrt(1 - Ellipsoid.Bessel1841.e2) / (1 - (Ellipsoid.Bessel1841.e2 * Math.pow(Math.sin(phi0), 2)));
    private static final double alpha = Math.sqrt(1 + (Ellipsoid.Bessel1841.eb2 * Math.pow(Math.cos(phi0), 4)));
    private static final double b0 = Math.asin(Math.sin(phi0) / alpha);
    private static final double K = Math.log(Math.tan(Math.PI / 4 + b0 / 2)) - alpha
            * Math.log(Math.tan(Math.PI / 4 + phi0 / 2)) + alpha * Ellipsoid.Bessel1841.e / 2
            * Math.log((1 + Ellipsoid.Bessel1841.e * Math.sin(phi0)) / (1 - Ellipsoid.Bessel1841.e * Math.sin(phi0)));

    private static final double xTrans = 200000;
    private static final double yTrans = 600000;

    private static final double DELTA_PHI = 1e-11;

    private LatLon correctEllipoideGSR80toBressel1841(LatLon coord) {
        double[] XYZ = Ellipsoid.WGS84.latLon2Cart(coord);
        XYZ[0] -= dX;
        XYZ[1] -= dY;
        XYZ[2] -= dZ;
        return Ellipsoid.Bessel1841.cart2LatLon(XYZ);
    }

    private LatLon correctEllipoideBressel1841toGRS80(LatLon coord) {
        double[] XYZ = Ellipsoid.Bessel1841.latLon2Cart(coord);
        XYZ[0] += dX;
        XYZ[1] += dY;
        XYZ[2] += dZ;
        return Ellipsoid.WGS84.cart2LatLon(XYZ);
    }

    /**
     * @param wgs  WGS84 lat/lon (ellipsoid GRS80) (in degree)
     * @return eastnorth projection in Swiss National Grid (ellipsoid Bessel)
     */
    @Override
    public EastNorth latlon2eastNorth(LatLon wgs) {
        LatLon coord = correctEllipoideGSR80toBressel1841(wgs);
        double phi = Math.toRadians(coord.lat());
        double lambda = Math.toRadians(coord.lon());

        double S = alpha * Math.log(Math.tan(Math.PI / 4 + phi / 2)) - alpha * Ellipsoid.Bessel1841.e / 2
                * Math.log((1 + Ellipsoid.Bessel1841.e * Math.sin(phi)) / (1 - Ellipsoid.Bessel1841.e * Math.sin(phi))) + K;
        double b = 2 * (Math.atan(Math.exp(S)) - Math.PI / 4);
        double l = alpha * (lambda - lambda0);

        double lb = Math.atan2(Math.sin(l), Math.sin(b0) * Math.tan(b) + Math.cos(b0) * Math.cos(l));
        double bb = Math.asin(Math.cos(b0) * Math.sin(b) - Math.sin(b0) * Math.cos(b) * Math.cos(l));

        double y = R * lb;
        double x = R / 2 * Math.log((1 + Math.sin(bb)) / (1 - Math.sin(bb)));

        return new EastNorth(y + yTrans, x + xTrans);
    }

    /**
     * @param xy SwissGrid east/north (in meters)
     * @return LatLon WGS84 (in degree)
     */
    @Override
    public LatLon eastNorth2latlon(EastNorth xy) {
        double x = xy.north() - xTrans;
        double y = xy.east() - yTrans;

        double lb = y / R;
        double bb = 2 * (Math.atan(Math.exp(x / R)) - Math.PI / 4);

        double b = Math.asin(Math.cos(b0) * Math.sin(bb) + Math.sin(b0) * Math.cos(bb) * Math.cos(lb));
        double l = Math.atan2(Math.sin(lb), Math.cos(b0) * Math.cos(lb) - Math.sin(b0) * Math.tan(bb));

        double lambda = lambda0 + l / alpha;
        double phi = b;
        double S = 0;

        double prevPhi = -1000;
        int iteration = 0;
        // iteration to finds S and phi
        while (Math.abs(phi - prevPhi) > DELTA_PHI) {
            if (++iteration > 30) {
                throw new RuntimeException("Two many iterations");
            }
            prevPhi = phi;
            S = 1 / alpha * (Math.log(Math.tan(Math.PI / 4 + b / 2)) - K) + Ellipsoid.Bessel1841.e
                    * Math.log(Math.tan(Math.PI / 4 + Math.asin(Ellipsoid.Bessel1841.e * Math.sin(phi)) / 2));
            phi = 2 * Math.atan(Math.exp(S)) - Math.PI / 2;
        }

        LatLon coord = correctEllipoideBressel1841toGRS80(new LatLon(Math.toDegrees(phi), Math.toDegrees(lambda)));
        return coord;
    }

    @Override
    public String toString() {
        return tr("Swiss Grid (Switzerland)");
    }

    @Override
    public String toCode() {
        return "EPSG:21781";
    }

    @Override
    public int hashCode() {
        return getClass().getName().hashCode(); // we have no variables
    }

    @Override
    public String getCacheDirectoryName() {
        return "swissgrid";
    }

    @Override
    public Bounds getWorldBoundsLatLon() {
        return new Bounds(new LatLon(45.7, 5.7), new LatLon(47.9, 10.6));
    }

    @Override
    public double getDefaultZoomInPPD() {
        // This will set the scale bar to about 100 m
        return 1.01;
    }

    @Override
    public void setupPreferencePanel(JPanel p) {
        p.add(new HtmlPanel("<i>CH1903 / LV03 (without local corrections)</i>"), GBC.eol().fill(GBC.HORIZONTAL));
        p.add(Box.createVerticalGlue(), GBC.eol().fill(GBC.BOTH));
    }

    @Override
    public void setPreferences(Collection<String> args) {
    }

    @Override
    public Collection<String> getPreferences(JPanel p) {
        return Collections.singletonList("CH1903");
    }

    @Override
    public Collection<String> getPreferencesFromCode(String code) {
        if ("EPSG:21781".equals(code))
            return Collections.singletonList("CH1903");
        return null;
    }
}
