// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests for the Swiss projection grid.
 */
public class SwissGridTest {
    private static final String SWISS_EPSG_CODE = "EPSG:21781";
    private boolean debug = false;

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().projectionNadGrids();

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUp() {
        ProjectionRegistry.setProjection(Projections.getProjectionByCode(SWISS_EPSG_CODE)); // Swiss grid
    }

    // CHECKSTYLE.OFF: LineLength
    // CHECKSTYLE.OFF: SingleSpaceSeparator

    /**
     * source: https://www.swisstopo.admin.ch/content/swisstopo-internet/en/topics/survey/reference-systems/projections/_jcr_content/contentPar/tabs/items/dokumente_publikatio/tabPar/downloadlist/downloadItems/463_1459341821844.download/refsyse.pdf
     */
    ProjData[] data = {
            new ProjData("Zimmerwald",     d(7, 27, 54.983506), d(46, 52, 37.540562), 947.149, 602030.680, 191775.030, 897.915),
            new ProjData("Chrischona",     d(7, 40, 6.983077), d(47, 34, 1.385301), 504.935,  617306.300, 268507.300, 456.064),
            new ProjData("Pfaender",       d(9, 47, 3.697723), d(47, 30, 55.172797), 1089.372, 776668.105, 265372.681, 1042.624),
            new ProjData("La Givrine",     d(6, 6, 7.326361), d(46, 27, 14.690021), 1258.274,  497313.292, 145625.438, 1207.434),
            new ProjData("Monte Generoso", d(9, 1, 16.389053), d(45, 55, 45.438020), 1685.027, 722758.810, 87649.670, 1636.600) };

    // CHECKSTYLE.ON: SingleSpaceSeparator
    // CHECKSTYLE.ON: LineLength

    private double d(double deg, double min, double sec) {
        return deg + min / 60. + sec / 3600.;
    }

    private static class ProjData {
        public String name;
        public LatLon ll;
        public EastNorth en;

        ProjData(String name, double lon, double lat, double h1, double x, double y, double h2) {
            this.name = name;
            ll = new LatLon(lat, lon);
            en = new EastNorth(x, y);
        }
    }

    private static final double EPSILON_ACCURATE = 0.05;

    private void projReferenceTest(final double epsilon) {
        Projection swiss = Projections.getProjectionByCode("EPSG:21781"); // Swiss grid
        StringBuilder errs = new StringBuilder();
        for (ProjData pd : data) {
            EastNorth en2 = swiss.latlon2eastNorth(pd.ll);
            if (Math.abs(pd.en.east() - en2.east()) > epsilon || Math.abs(pd.en.north() - en2.north()) > epsilon) {
                errs.append(String.format("%s should be: %s but is: %s%n", pd.name, pd.en, en2));
            }
        }
        assertSame(errs.toString(), errs.length(), 0);
    }

    /**
     * Test projection accuracy.
     */
    @Test
    public void testProjReferenceTestAccurate() {
        projReferenceTest(EPSILON_ACCURATE);
    }

    /**
     * Unit test A: lat/lon => east/north
     */
    @Test
    public void testAlatlon2eastNorth() {
        LatLon ll = new LatLon(46.518, 6.567);
        EastNorth en = ProjectionRegistry.getProjection().latlon2eastNorth(ll);
        if (debug) {
            System.out.println(en);
        }
        assertTrue("Lausanne", Math.abs(en.east() - 533112.13) < 0.1);
        assertTrue("Lausanne", Math.abs(en.north() - 152227.35) < 0.1);

        ll = new LatLon(47.78, 8.58);
        en = ProjectionRegistry.getProjection().latlon2eastNorth(ll);
        if (debug) {
            System.out.println(en);
        }
        assertTrue("Schafouse", Math.abs(en.east() - 685542.97) < 0.1);
        assertTrue("Schafouse", Math.abs(en.north() - 292783.21) < 0.1);

        ll = new LatLon(46.58, 10.48);
        en = ProjectionRegistry.getProjection().latlon2eastNorth(ll);
        if (debug) {
            System.out.println(en);
        }
        assertTrue("Grinson", Math.abs(en.east() - 833066.95) < 0.1);
        assertTrue("Grinson", Math.abs(en.north() - 163265.32) < 0.1);

        ll = new LatLon(46.0 + 57.0 / 60 + 3.89813884505 / 3600, 7.0 + 26.0 / 60 + 19.076595154147 / 3600);
        en = ProjectionRegistry.getProjection().latlon2eastNorth(ll);
        if (debug) {
            System.out.println(en);
        }
        assertTrue("Berne", Math.abs(en.east() - 600000.0) < 0.1);
        assertTrue("Berne", Math.abs(en.north() - 200000.0) < 0.1);

        // http://geodesy.geo.admin.ch/reframe/lv03towgs84?easting=700000&northing=100000
        ll = new LatLon(46.04412093223244, 8.730497366167727);
        en = ProjectionRegistry.getProjection().latlon2eastNorth(ll);
        if (debug) {
            System.out.println(en);
        }
        assertTrue("Ref", Math.abs(en.east() - 700000.0) < 0.1);
        assertTrue("Ref", Math.abs(en.north() - 100000.0) < 0.1);
    }

    /**
     * Unit test B: east/north => lat/lon
     */
    @Test
    public void testBeastNorth2latlon() {
        EastNorth en = new EastNorth(533112.13, 152227.35);
        LatLon ll = ProjectionRegistry.getProjection().eastNorth2latlon(en);
        if (debug) {
            System.out.println(ll);
        }
        assertTrue("Lausanne", Math.abs(ll.lat() - 46.518) < 0.00001);
        assertTrue("Lausanne", Math.abs(ll.lon() - 6.567) < 0.00001);

        en = new EastNorth(685542.97, 292783.21);
        ll = ProjectionRegistry.getProjection().eastNorth2latlon(en);
        if (debug) {
            System.out.println(ll);
        }
        assertTrue("Schafouse", Math.abs(ll.lat() - 47.78) < 0.00001);
        assertTrue("Schafouse", Math.abs(ll.lon() - 8.58) < 0.00001);

        en = new EastNorth(833066.95, 163265.32);
        ll = ProjectionRegistry.getProjection().eastNorth2latlon(en);
        if (debug) {
            System.out.println(ll);
        }
        assertTrue("Grinson", Math.abs(ll.lat() - 46.58) < 0.00001);
        assertTrue("Grinson", Math.abs(ll.lon() - 10.48) < 0.00001);

        en = new EastNorth(600000.0, 200000.0);
        ll = ProjectionRegistry.getProjection().eastNorth2latlon(en);
        if (debug) {
            System.out.println(ll);
        }
        assertTrue("Berne", Math.abs(ll.lat() - (46.0 + 57.0 / 60 + 3.89813884505 / 3600)) < 0.00001);
        assertTrue("Berne", Math.abs(ll.lon() - (7.0 + 26.0 / 60 + 19.076595154147 / 3600)) < 0.00001);

        // http://geodesy.geo.admin.ch/reframe/lv03towgs84?easting=700000&northing=100000
        en = new EastNorth(700000.0, 100000.0);
        ll = ProjectionRegistry.getProjection().eastNorth2latlon(en);
        if (debug) {
            System.out.println(ll);
        }
        assertTrue("Ref", Math.abs(ll.lat() - 46.04412093223244) < 0.00001);
        assertTrue("Ref", Math.abs(ll.lon() - 8.730497366167727) < 0.00001);
    }

    /**
     * Send and return should have less than 2mm of difference.
     */
    @Test
    public void testCsendandreturn() {
        EastNorth en = new EastNorth(533111.69, 152227.85);
        LatLon ll = ProjectionRegistry.getProjection().eastNorth2latlon(en);
        EastNorth en2 = ProjectionRegistry.getProjection().latlon2eastNorth(ll);
        if (debug) {
            System.out.println(en.east() - en2.east());
        }
        if (debug) {
            System.out.println(en.north() - en2.north());
        }
        assertTrue("Lausanne", Math.abs(en.east() - en2.east()) < 0.002);
        assertTrue("Lausanne", Math.abs(en.north() - en2.north()) < 0.002);

        en = new EastNorth(685544.16, 292782.91);
        ll = ProjectionRegistry.getProjection().eastNorth2latlon(en);
        en2 = ProjectionRegistry.getProjection().latlon2eastNorth(ll);
        if (debug) {
            System.out.println(en.east() - en2.east());
        }
        if (debug) {
            System.out.println(en.north() - en2.north());
        }
        assertTrue("Schafouse", Math.abs(en.east() - en2.east()) < 0.002);
        assertTrue("Schafouse", Math.abs(en.north() - en2.north()) < 0.002);

        en = new EastNorth(833068.04, 163265.39);
        ll = ProjectionRegistry.getProjection().eastNorth2latlon(en);
        en2 = ProjectionRegistry.getProjection().latlon2eastNorth(ll);
        if (debug) {
            System.out.println(en.east() - en2.east());
        }
        if (debug) {
            System.out.println(en.north() - en2.north());
        }
        assertTrue("Grinson", Math.abs(en.east() - en2.east()) < 0.002);
        assertTrue("Grinson", Math.abs(en.north() - en2.north()) < 0.002);

        en = new EastNorth(600000.0, 200000.0);
        ll = ProjectionRegistry.getProjection().eastNorth2latlon(en);
        en2 = ProjectionRegistry.getProjection().latlon2eastNorth(ll);
        if (debug) {
            System.out.println(en.east() - en2.east());
        }
        if (debug) {
            System.out.println(en.north() - en2.north());
        }
        assertTrue("Berne", Math.abs(en.east() - en2.east()) < 0.002);
        assertTrue("Berne", Math.abs(en.north() - en2.north()) < 0.002);

        en = new EastNorth(700000.0, 100000.0);
        ll = ProjectionRegistry.getProjection().eastNorth2latlon(en);
        en2 = ProjectionRegistry.getProjection().latlon2eastNorth(ll);
        if (debug) {
            System.out.println(en.east() - en2.east());
        }
        if (debug) {
            System.out.println(en.north() - en2.north());
        }
        assertTrue("Ref", Math.abs(en.east() - en2.east()) < 0.002);
        assertTrue("Ref", Math.abs(en.north() - en2.north()) < 0.002);
    }
}
