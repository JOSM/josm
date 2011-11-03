package org.openstreetmap.josm.data.osm;

import org.junit.Test;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.coor.LatLonTest;

import static org.junit.Assert.assertTrue;

public class RoundingPerformanceTest extends LatLonTest {
    
    private static double oldRoundToOsmPrecision(double value) {
        return Math.round(value / LatLon.MAX_SERVER_PRECISION) * LatLon.MAX_SERVER_PRECISION; // Old method, causes rounding errors, but efficient
    }

    @Test
    public void test() {
        final int n = 1000000;
        long start = System.nanoTime();
        for (int i = 0; i < n; i++) {
            for (double value : sampleValues) {
                oldRoundToOsmPrecision(value);
            }
        }
        long end = System.nanoTime();
        long oldTime = end-start;
        System.out.println("Old time: "+oldTime/1000000.0 + " ms");
        
        start = System.nanoTime();
        for (int i = 0; i < n; i++) {
            for (double value : sampleValues) {
                LatLon.roundToOsmPrecision(value);
            }
        }
        end = System.nanoTime();
        long newTime = end-start;
        System.out.println("New time: "+newTime/1000000.0 + " ms");
        
        assertTrue(newTime <= oldTime*10);
    }
}
