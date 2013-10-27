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

        testProj(Projections.getProjectionByCode("EPSG:4326")); // WGS 84
        testProj(Projections.getProjectionByCode("EPSG:3857")); // Mercator
        if (!"yes".equals(System.getProperty("suppressPermanentFailure"))) {
            testProj(Projections.getProjectionByCode("EPSG:3301")); // Lambert EST
        }

        for (int i=0; i<=3; ++i) {
            testProj(Projections.getProjectionByCode("EPSG:"+Integer.toString(27561+i))); // Lambert 4 Zones France
        }

        for (int i=0; i<=4; ++i) {
            testProj(Projections.getProjectionByCode("EPSG:"+Integer.toString(2176+i))); // PUWG Poland
        }

        testProj(Projections.getProjectionByCode("EPSG:21781")); // Swiss grid

        for (int i=0; i<=60; ++i) {
            testProj(Projections.getProjectionByCode("EPSG:"+Integer.toString(32601+i))); // UTM North
            testProj(Projections.getProjectionByCode("EPSG:"+Integer.toString(32701+i))); // UTM South
        }

        if (!"yes".equals(System.getProperty("suppressPermanentFailure"))) {
            for (int i=0; i<=4; ++i) {
                testProj(Projections.getProjectionByCode("EPSG:"+Integer.toString(2969+i))); // UTM France DOM
            }
        }

        for (int i=0; i<=8; ++i) {
            testProj(Projections.getProjectionByCode("EPSG:"+Integer.toString(3942+i))); // Lambert CC9 Zones France
        }

        if (error) {
            System.err.println(text);
            Assert.fail();
        }
    }

    private void testProj(Projection p) {
        if (p != null) {
            double maxErrLat = 0, maxErrLon = 0;
            Bounds b = p.getWorldBoundsLatLon();
    
            text += String.format("*** %s %s%n", p.toString(), p.toCode());
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
            text += String.format("%s errorLat: %s errorLon: %s%n", mark, maxErrLat, maxErrLon);
        }
    }
}
