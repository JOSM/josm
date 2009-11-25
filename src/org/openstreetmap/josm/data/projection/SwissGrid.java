//License: GPL. For details, see LICENSE file.

package org.openstreetmap.josm.data.projection;

import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;

/**
 * Projection for the SwissGrid, see http://de.wikipedia.org/wiki/Swiss_Grid.
 *
 * Calculations are based on formula from
 * http://www.swisstopo.admin.ch/internet/swisstopo/en/home/topics/survey/sys/refsys/switzerland.parsysrelated1.37696.downloadList.12749.DownloadFile.tmp/ch1903wgs84en.pdf
 *
 */
public class SwissGrid implements Projection {
    /**
     * @param wgs  WGS84 lat/lon (ellipsoid GRS80) (in degree)
     * @return eastnorth projection in Swiss National Grid (ellipsoid Bessel)
     */
    public EastNorth latlon2eastNorth(LatLon wgs) {
            double phi = 3600d * wgs.lat();
            double lambda = 3600d * wgs.lon();

            double phiprime = (phi - 169028.66d) / 10000d;
            double lambdaprime = (lambda - 26782.5d) / 10000d;

            // precompute squares for lambdaprime and phiprime
            //
            double lambdaprime_2 = Math.pow(lambdaprime,2);
            double phiprime_2 = Math.pow(phiprime,2);

            double north =
                  200147.07d
                + 308807.95d                           * phiprime
                +   3745.25d    * lambdaprime_2
                +     76.63d                           * phiprime_2
                -    194.56d    * lambdaprime_2        * phiprime
                +    119.79d                           * Math.pow(phiprime,3);

            double east =
                  600072.37d
                + 211455.93d  * lambdaprime
                - 10938.51d   * lambdaprime             * phiprime
                - 0.36d       * lambdaprime             * phiprime_2
                - 44.54d      * Math.pow(lambdaprime,3);

            return new EastNorth(east, north);
    }

    /**
     * @param xy SwissGrid east/north (in meters)
     * @return LatLon WGS84 (in degree)
     */

    public LatLon eastNorth2latlon(EastNorth xy) {
        double yp = (xy.east() - 600000d) / 1000000d;
        double xp = (xy.north() - 200000d) / 1000000d;

        // precompute squares of xp and yp
        //
        double xp_2 = Math.pow(xp,2);
        double yp_2 = Math.pow(yp,2);

        // lambda = latitude, phi = longitude
        double lmbp  =    2.6779094d
                        + 4.728982d      * yp
                        + 0.791484d      * yp               * xp
                        + 0.1306d        * yp               * xp_2
                        - 0.0436d        * Math.pow(yp,3);

        double phip =     16.9023892d
                        +  3.238272d                        * xp
                        -  0.270978d    * yp_2
                        -  0.002528d                        * xp_2
                        -  0.0447d      * yp_2              * xp
                        -  0.0140d                          * Math.pow(xp,3);

        double lmb = lmbp * 100.0d / 36.0d;
        double phi = phip * 100.0d / 36.0d;

        return new LatLon(phi,lmb);
    }

    @Override public String toString() {
        return tr("Swiss Grid (Switzerland)");
    }

    public String toCode() {
        return "EPSG:21781";
    }

    @Override
    public int hashCode() {
        return getClass().getName().hashCode(); // we have no variables
    }

    public String getCacheDirectoryName() {
        return "swissgrid";
    }

    public Bounds getWorldBoundsLatLon()
    {
        return new Bounds(
                new LatLon(45.7, 5.7),
                new LatLon(47.9, 10.6));
    }

    public double getDefaultZoomInPPD() {
        // This will set the scale bar to about 100 m
        return 1.01;
    }
}
