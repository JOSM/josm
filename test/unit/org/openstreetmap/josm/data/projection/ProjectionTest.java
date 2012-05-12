// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection;

import java.util.Random;

import org.junit.Assert;
import org.junit.Test;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;

public class ProjectionTest {

    private static final boolean debug = false;
    private static Random rand = new Random(System.currentTimeMillis());

    boolean error;
    String text;

    @Test
    public void proj() {
        error = false;
        text = "";

        testProj(new Epsg4326());
        testProj(new Mercator());
        if (!"yes".equals(System.getProperty("suppressPermanentFailure"))) {
            testProj(new LambertEST());
        }

        for (int i=0; i<=3; ++i) {
            testProj(new Lambert(i));
        }

        for (int i=0; i<=4; ++i) {
            testProj(new Puwg(i));
        }

        testProj(new SwissGrid());

        for (int i=0; i<=6; ++i) {
            int zone;
            if (i==0) {
                zone = 1;
            } else if (i==6) {
                zone = 60;
            } else {
                zone = rand.nextInt(60) + 1;
            }
            UTM.Hemisphere hem = rand.nextBoolean() ? UTM.Hemisphere.North : UTM.Hemisphere.South;
            testProj(new UTM(zone, hem));
        }

        if (!"yes".equals(System.getProperty("suppressPermanentFailure"))) {
            for (int i=0; i<=4; ++i) {
                testProj(new UTM_France_DOM(i));
            }
        }

        for (int i=0; i<=8; ++i) {
            testProj(new LambertCC9Zones(i));
        }

        if (error) {
            System.err.println(text);
            Assert.fail();
        }
    }

    private void testProj(Projection p) {
        double maxErrLat = 0, maxErrLon = 0;

        Bounds b = p.getWorldBoundsLatLon();

        text += String.format("*** %s %s\n", p.toString(), p.toCode());
        for (int num=0; num < 1000; ++num) {

            double lat = rand.nextDouble() * (b.getMax().lat() - b.getMin().lat()) + b.getMin().lat();
            double lon = rand.nextDouble() * (b.getMax().lon() - b.getMin().lon()) + b.getMin().lon();

            LatLon ll = new LatLon(lat, lon);

            for (int i=0; i<10; ++i) {
                EastNorth en = p.latlon2eastNorth(ll);
                ll = p.eastNorth2latlon(en);
            }
            maxErrLat = Math.max(maxErrLat, Math.abs(lat - ll.lat()));
            maxErrLon = Math.max(maxErrLon, Math.abs(lon - ll.lon()));
        }

        String mark = "";
        if (maxErrLat + maxErrLon > 1e-5) {
            mark = "--FAILED-- ";
            error = true;
        }
        text += String.format("%s errorLat: %s errorLon: %s\n", mark, maxErrLat, maxErrLon);
    }
}
