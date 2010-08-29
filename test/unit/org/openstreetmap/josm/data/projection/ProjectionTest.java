// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection;

import java.util.Collections;
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
        if (!"yes".equals(System.getProperty("supressPermanentFailure"))) {
            testProj(new LambertEST());
        }

        Lambert lam = new Lambert();
        for (int zone=1; zone<=4; ++zone) {
            lam.setPreferences(Collections.singletonList(Integer.toString(zone)));
            testProj(lam);
        }

        Puwg puwg = new Puwg();
        for (PuwgData pd : Puwg.Zones) {
            puwg.setPreferences(Collections.singletonList(pd.toCode()));
            testProj(puwg);
        }

        testProj(new SwissGrid());

        UTM utm = new UTM();
        for (int i=0; i<=6; ++i) {
            int zone;
            if (i==0) {
                zone = 0;
            } else if (i==6) {
                zone = 59;
            } else {
                zone = rand.nextInt(60);
            }
            utm.setPreferences(Collections.singletonList(Integer.toString(zone)));
            testProj(utm);

        }
        UTM_France_DOM utmFr = new UTM_France_DOM();
        for (int zone=1; zone<=5; ++zone) {
            utmFr.setPreferences(Collections.singletonList(Integer.toString(zone)));
            testProj(utmFr);
        }

        LambertCC9Zones lamCC9 = new LambertCC9Zones();
        for (int i=1; i<=9; ++i) {
            lamCC9.setPreferences(Collections.singletonList(Integer.toString(i)));
            testProj(lamCC9);
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
        for (int num=0; num < 1; ++num) {

            double lat = rand.nextDouble() * (b.getMax().lat() - b.getMin().lat()) + b.getMin().lat();
            double lon = rand.nextDouble() * (b.getMax().lon() - b.getMin().lon()) + b.getMin().lon();

            LatLon ll = new LatLon(lat, lon);

            for (int i=0; i<1; ++i) {
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
