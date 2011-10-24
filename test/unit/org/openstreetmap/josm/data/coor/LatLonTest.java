// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.coor;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * @author Vincent
 *
 */
public class LatLonTest {

    @Test
    public void roundingTests() {
        
        for (double value : new double[]{
                -180.0, -179.9, -179.6, -179.5, -179.4, -179.1, -179.0, -100.0, -99.9, -10.0, -9.9, -1.0, -0.1,
                 180.0,  179.9,  179.6,  179.5,  179.4,  179.1,  179.0,  100.0,  99.9,  10.0,  9.9,  1.0,  0.1,
                 0.12, 0.123, 0.1234, 0.12345, 0.123456, 0.1234567,
                 1.12, 1.123, 1.1234, 1.12345, 1.123456, 1.1234567,
                 10.12, 10.123, 10.1234, 10.12345, 10.123456, 10.1234567,
                 100.12, 100.123, 100.1234, 100.12345, 100.123456, 100.1234567
                }) {
            assertEquals(LatLon.roundToOsmPrecision(value), value, 0);
        }
        
        assertEquals(LatLon.roundToOsmPrecision(0.0), 0.0, 0);
        assertEquals(LatLon.roundToOsmPrecision(-0.0), 0.0, 0);
        
        assertEquals(LatLon.roundToOsmPrecision(0.12345678),  0.1234568, 0);
        assertEquals(LatLon.roundToOsmPrecision(0.123456789), 0.1234568, 0);

        assertEquals(LatLon.roundToOsmPrecision(1.12345678),  1.1234568, 0);
        assertEquals(LatLon.roundToOsmPrecision(1.123456789), 1.1234568, 0);

        assertEquals(LatLon.roundToOsmPrecision(10.12345678),  10.1234568, 0);
        assertEquals(LatLon.roundToOsmPrecision(10.123456789), 10.1234568, 0);

        assertEquals(LatLon.roundToOsmPrecision(100.12345678),  100.1234568, 0);
        assertEquals(LatLon.roundToOsmPrecision(100.123456789), 100.1234568, 0);
    }
}
