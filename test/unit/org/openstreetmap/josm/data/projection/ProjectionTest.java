// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Random;

import org.junit.Assert;
import org.junit.Test;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;

/**
 * Unit tests for class {@link Projection}.
 */
public class ProjectionTest {

    private static Random rand = new SecureRandom();

    boolean error;
    String text;

    /**
     * Tests that projections are numerically stable in their definition bounds (round trip error &lt; 1e-5)
     */
    @Test
    public void testProjections() {
        error = false;
        text = "";

        testProjection(Projections.getProjectionByCode("EPSG:4326")); // WGS 84
        testProjection(Projections.getProjectionByCode("EPSG:3857")); // Mercator
        testProjection(Projections.getProjectionByCode("EPSG:3301")); // Lambert EST

        for (int i = 0; i <= 3; ++i) {
            testProjection(Projections.getProjectionByCode("EPSG:"+Integer.toString(27561+i))); // Lambert 4 Zones France
        }

        for (int i = 0; i <= 4; ++i) {
            testProjection(Projections.getProjectionByCode("EPSG:"+Integer.toString(2176+i))); // PUWG Poland
        }

        testProjection(Projections.getProjectionByCode("EPSG:21781")); // Swiss grid

        for (int i = 0; i <= 60; ++i) {
            testProjection(Projections.getProjectionByCode("EPSG:"+Integer.toString(32601+i))); // UTM North
            testProjection(Projections.getProjectionByCode("EPSG:"+Integer.toString(32701+i))); // UTM South
        }

        for (String c : Arrays.asList("2969", "2970", "2972", "2973")) {
            testProjection(Projections.getProjectionByCode("EPSG:"+c)); // UTM France DOM
        }

        for (int i = 0; i <= 8; ++i) {
            testProjection(Projections.getProjectionByCode("EPSG:"+Integer.toString(3942+i))); // Lambert CC9 Zones France
        }

        for (int i = 0; i <= 17; ++i) {
            testProjection(Projections.getProjectionByCode("EPSG:"+Integer.toString(102421+i))); // WGS_1984_ARC_System Zones
        }

        testProjection(Projections.getProjectionByCode("EPSG:102016")); // North Pole
        testProjection(Projections.getProjectionByCode("EPSG:102019")); // South Pole

        if (error) {
            System.err.println(text);
            Assert.fail();
        }
    }

    private void testProjection(Projection p) {
        if (p != null) {
            double maxErrLat = 0, maxErrLon = 0;
            Bounds b = p.getWorldBoundsLatLon();

            text += String.format("*** %s %s%n", p.toString(), p.toCode());
            for (int num = 0; num < 1000; ++num) {

                LatLon ll0 = random(b);
                LatLon ll = ll0;

                for (int i = 0; i < 10; ++i) {
                    EastNorth en = p.latlon2eastNorth(ll);
                    ll = p.eastNorth2latlon(en);
                }
                maxErrLat = Math.max(maxErrLat, Math.abs(ll0.lat() - ll.lat()));
                maxErrLon = Math.max(maxErrLon, Math.abs(ll0.lon() - ll.lon()));
            }

            String mark = "";
            if (maxErrLat + maxErrLon > 1e-5) {
                mark = "--FAILED-- ";
                error = true;
            }
            text += String.format("%s errorLat: %s errorLon: %s%n", mark, maxErrLat, maxErrLon);
        }
    }

    private LatLon random(Bounds b) {
        for (int i = 0; i < 20; i++) {
            double lat = rand.nextDouble() * (b.getMax().lat() - b.getMin().lat()) + b.getMin().lat();
            double lon = rand.nextDouble() * (b.getMax().lon() - b.getMin().lon()) + b.getMin().lon();
            LatLon result = new LatLon(lat, lon);
            if (result.isValid()) return result;
        }
        throw new RuntimeException();
    }

    boolean error2;
    String text2;
    Collection<String> projIds;

    /**
     * Tests that projections are numerically stable in their definition bounds (round trip error &lt; epsilon)
     */
    @Test
    public void testProjs() {
        error2 = false;
        text2 = "";

        projIds = new HashSet<>(Projections.getAllBaseProjectionIds());

        final double EPS = 1e-6;
        testProj("lonlat", EPS, "");
        testProj("lcc", EPS, "+lat_0=34");
        testProj("lcc", EPS, "+lat_1=87 +lat_2=83.6 +lat_0=85.43");
        testProj("somerc", EPS, "+lat_0=47");
        testProj("tmerc", 1e-5, "+bounds=-2.5,-89,2.5,89");
        testProj("tmerc", 2e-3, "");
        testProj("sterea", EPS, "+lat_0=52");
        testProj("aea", EPS, "+lat_1=27.5 +lat_2=35 +lat_0=18");
        testProj("stere", 1e-5, "+lat_0=-90 +lat_ts=-70");
        testProj("stere", 1e-5, "+lat_0=90 +lat_ts=90");
        testProj("omerc", EPS, "+lat_0=4 +lonc=115 +alpha=53 +no_uoff +gamma=53.130 +bounds=112,4,116,7");
        testProj("cass", 1e-3, "+lat_0=11 +bounds=-1.0,-89,1.0,89");
        testProj("laea", 3e-3, "+lat_0=34");
        testProj("merc", 1e-5, "");
        testProj("sinu", 1e-4, "");
        testProj("aeqd", 1e-5, "+lon_0=0dE +lat_0=90dN");
        testProj("aeqd", 1e-5, "+lon_0=0dE +lat_0=90dS");
        testProj("eqc", 1e-5, "");

        if (error2) {
            System.err.println(text2);
            Assert.fail();
        }
        Assert.assertTrue("missing test: "+projIds, projIds.isEmpty());
    }

    private void testProj(String id, double eps, String prefAdd) {
        final int NUM_IT = 1000;
        projIds.remove(id);
        String pref = String.format("+proj=%s +ellps=WGS84 +nadgrids=null "+prefAdd, id);
        CustomProjection p = new CustomProjection();
        try {
            p.update(pref);
        } catch (ProjectionConfigurationException ex) {
            throw new RuntimeException(ex);
        }
        Bounds b = p.getWorldBoundsLatLon();
        double maxDist = 0;
        LatLon maxLatLon = null;
        for (int i = 0; i < NUM_IT; i++) {
            LatLon ll1 = random(b);
            EastNorth en = p.latlon2eastNorth(ll1);
            LatLon ll2 = p.eastNorth2latlon(en);
            Assert.assertTrue(p.toCode() + " at " + ll1 + " is " + ll2, ll2.isValid());
            double dist = ll1.greatCircleDistance(ll2);
            if (dist > eps) {
                error2 = true;
                if (dist > maxDist) {
                    maxDist = dist;
                    maxLatLon = ll1;
                }
            }
        }
        if (maxDist > 0) {
            text2 += id + ": dist " + maxDist + " at " + maxLatLon + "\n";
        }
    }
}
