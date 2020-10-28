// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.coor.LatLonTest;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Checks that rounding of coordinates is not too slow.
 */
class RoundingPerformanceTest {

    private static double oldRoundToOsmPrecision(double value) {
        // Old method, causes rounding errors, but efficient
        return Math.round(value / LatLon.MAX_SERVER_PRECISION) * LatLon.MAX_SERVER_PRECISION;
    }

    /**
     * Checks that rounding of coordinates is not too slow.
     */
    @Test
    @SuppressFBWarnings(value = "RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT")
    void testRounding() {
        final int n = 1000000;
        long start = System.nanoTime();
        for (int i = 0; i < n; i++) {
            for (double value : LatLonTest.SAMPLE_VALUES) {
                oldRoundToOsmPrecision(value);
            }
        }
        long end = System.nanoTime();
        long oldTime = end-start;
        System.out.println("Old time: "+oldTime/1000000.0 + " ms");

        start = System.nanoTime();
        for (int i = 0; i < n; i++) {
            for (double value : LatLonTest.SAMPLE_VALUES) {
                LatLon.roundToOsmPrecision(value);
            }
        }
        end = System.nanoTime();
        long newTime = end-start;
        System.out.println("New time: "+newTime/1000000.0 + " ms");

        assertTrue(newTime <= oldTime*12);
    }
}
