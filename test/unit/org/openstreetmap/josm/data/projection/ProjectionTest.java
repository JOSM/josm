// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Random;

import org.junit.Assert;
import org.junit.Test;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;

public class ProjectionTest {

    private static Random rand = new Random(System.currentTimeMillis());

    boolean error;
    String text;

    @Test
    public void projections() {
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
        for (int i=0; i<20; i++) {
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

    @Test
    public void projs() {
        error2 = false;
        text2 = "";

        projIds = new HashSet<>(Projections.getAllBaseProjectionIds());

        final double EPS = 1e-6;
        testProj("lonlat", EPS, "");
        testProj("josm:smerc", EPS, "");
        testProj("lcc", EPS, "+lat_0=34");
        testProj("lcc", EPS, "+lat_1=87 +lat_2=83.6 +lat_0=85.43");
        testProj("somerc", EPS, "+lat_0=47");
        testProj("tmerc", 2e-3, "");
        testProj("sterea", EPS, "+lat_0=52");

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
        for (int i=0; i<NUM_IT; i++) {
            LatLon ll1 = random(b);
            EastNorth en = p.latlon2eastNorth(ll1);
            LatLon ll2 = p.eastNorth2latlon(en);
            Assert.assertTrue(p.toCode() + " at " + ll1 + " is " + ll2, ll2.isValid());
            double dist = ll1.greatCircleDistance(ll2);
            if (dist > eps) {
                error2 = true;
                text2 += id + ": dist " + dist + " at " + ll1 + "\n";
                return;
            }
        }
    }
}
