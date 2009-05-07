//License: GPL. For details, see LICENSE file.

package org.openstreetmap.josm.data.projection;

import static org.openstreetmap.josm.tools.I18n.tr;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
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
    private boolean doAlertOnCoordinatesOufOfRange = true;

    /**
     * replies true if if wgs is in or reasonably close to Switzerland. False otherwise.
     *
     * @param wgs  lat/lon in WGS89
     * @return
     */
    protected boolean latlonInAcceptableRange(LatLon wgs) {
        // coordinate transformation is invoked for boundary values regardless
        // of current data set.
        //
        if (Math.abs(wgs.lon()) == Projection.MAX_LON && Math.abs(wgs.lat()) == Projection.MAX_LAT) {
            return true;
        }
        return   wgs.lon() >= 5.7 && wgs.lon() <= 10.6
               && wgs.lat() >= 45.7 && wgs.lat() <= 47.9;
    }

    /**
     * displays an alert if lat/lon are not reasonably close to Switzerland.
     */
    protected void alertCoordinatesOutOfRange() {
        JOptionPane.showMessageDialog(Main.parent,
                tr("The projection \"{0}\" is designed for\n"
                + "latitudes between 45.7\u00b0 and 47.9\u00b0\n"
                + "and longitutes between 5.7\u00b0 and 10.6\u00b0 only.\n"
                + "Use another projection system if you are not working\n"
                + "on a data set of Switzerland or Liechtenstein.\n"
                + "Do not upload any data after this message.", this.toString()),
                "Current projection not suitable",
                JOptionPane.WARNING_MESSAGE
        );
        doAlertOnCoordinatesOufOfRange = false;
    }

    /**
     * @param wgs  WGS84 lat/lon (ellipsoid GRS80) (in degree)
     * @return eastnorth projection in Swiss National Grid (ellipsoid Bessel)
     */
    public EastNorth latlon2eastNorth(LatLon wgs) {
            if (!latlonInAcceptableRange(wgs)) {
                if (doAlertOnCoordinatesOufOfRange) {
                    alertCoordinatesOutOfRange();
                }
            }

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

    public double scaleFactor() {
        return 1.0;
    }

    @Override public String toString() {
        return tr("Swiss Grid (Switzerland)");
    }

    public String toCode() {
        return "EPSG:21781";
    }

    public String getCacheDirectoryName() {
        return "swissgrid";
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof SwissGrid;
    }

    @Override
    public int hashCode() {
        return SwissGrid.class.hashCode();
    }
}
