// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.coor;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * @author Vincent
 *
 */
public class LatLonTest {

    protected static final double[] sampleValues = new double[]{
            -180.0, -179.9, -179.6, -179.5, -179.4, -179.1, -179.0, -100.0, -99.9, -10.0, -9.9, -1.0, -0.1,
            180.0,  179.9,  179.6,  179.5,  179.4,  179.1,  179.0,  100.0,  99.9,  10.0,  9.9,  1.0,  0.1,
            0.12, 0.123, 0.1234, 0.12345, 0.123456, 0.1234567,
            1.12, 1.123, 1.1234, 1.12345, 1.123456, 1.1234567,
            10.12, 10.123, 10.1234, 10.12345, 10.123456, 10.1234567,
            100.12, 100.123, 100.1234, 100.12345, 100.123456, 100.1234567
           };
    
    /**
     * Test of {@link LatLon#roundToOsmPrecisionStrict}
     */
    @Test
    public void testRoundToOsmPrecisionStrict() {
        
        for (double value : sampleValues) {
            assertEquals(LatLon.roundToOsmPrecisionStrict(value), value, 0);
        }
        
        assertEquals(LatLon.roundToOsmPrecisionStrict(0.0), 0.0, 0);
        assertEquals(LatLon.roundToOsmPrecisionStrict(-0.0), 0.0, 0);
        
        assertEquals(LatLon.roundToOsmPrecisionStrict(0.12345678),  0.1234568, 0);
        assertEquals(LatLon.roundToOsmPrecisionStrict(0.123456789), 0.1234568, 0);

        assertEquals(LatLon.roundToOsmPrecisionStrict(1.12345678),  1.1234568, 0);
        assertEquals(LatLon.roundToOsmPrecisionStrict(1.123456789), 1.1234568, 0);

        assertEquals(LatLon.roundToOsmPrecisionStrict(10.12345678),  10.1234568, 0);
        assertEquals(LatLon.roundToOsmPrecisionStrict(10.123456789), 10.1234568, 0);

        assertEquals(LatLon.roundToOsmPrecisionStrict(100.12345678),  100.1234568, 0);
        assertEquals(LatLon.roundToOsmPrecisionStrict(100.123456789), 100.1234568, 0);

        assertEquals(LatLon.roundToOsmPrecisionStrict(100.00000001),  100.0000000, 0);
        assertEquals(LatLon.roundToOsmPrecisionStrict(100.000000001),  100.0000000, 0);
        assertEquals(LatLon.roundToOsmPrecisionStrict(100.0000000001),  100.0000000, 0);
        assertEquals(LatLon.roundToOsmPrecisionStrict(100.00000000001),  100.0000000, 0);
        assertEquals(LatLon.roundToOsmPrecisionStrict(100.000000000001),  100.0000000, 0);
        assertEquals(LatLon.roundToOsmPrecisionStrict(100.0000000000001),  100.0000000, 0);
        assertEquals(LatLon.roundToOsmPrecisionStrict(100.00000000000001),  100.0000000, 0);
        assertEquals(LatLon.roundToOsmPrecisionStrict(100.000000000000001),  100.0000000, 0);
        assertEquals(LatLon.roundToOsmPrecisionStrict(100.0000000000000001),  100.0000000, 0);
        assertEquals(LatLon.roundToOsmPrecisionStrict(100.00000000000000001),  100.0000000, 0);
        assertEquals(LatLon.roundToOsmPrecisionStrict(100.000000000000000001),  100.0000000, 0);
        assertEquals(LatLon.roundToOsmPrecisionStrict(100.0000000000000000001),  100.0000000, 0);
        assertEquals(LatLon.roundToOsmPrecisionStrict(100.00000000000000000001),  100.0000000, 0);

        assertEquals(LatLon.roundToOsmPrecisionStrict(99.999999999999999999999),  100.0000000, 0);
        assertEquals(LatLon.roundToOsmPrecisionStrict(99.99999999999999999999),  100.0000000, 0);
        assertEquals(LatLon.roundToOsmPrecisionStrict(99.9999999999999999999),  100.0000000, 0);
        assertEquals(LatLon.roundToOsmPrecisionStrict(99.999999999999999999),  100.0000000, 0);
        assertEquals(LatLon.roundToOsmPrecisionStrict(99.99999999999999999),  100.0000000, 0);
        assertEquals(LatLon.roundToOsmPrecisionStrict(99.9999999999999999),  100.0000000, 0);
        assertEquals(LatLon.roundToOsmPrecisionStrict(99.999999999999999),  100.0000000, 0);
        assertEquals(LatLon.roundToOsmPrecisionStrict(99.99999999999999),  100.0000000, 0);
        assertEquals(LatLon.roundToOsmPrecisionStrict(99.9999999999999),  100.0000000, 0);
        assertEquals(LatLon.roundToOsmPrecisionStrict(99.999999999999),  100.0000000, 0);
        assertEquals(LatLon.roundToOsmPrecisionStrict(99.99999999999),  100.0000000, 0);
        assertEquals(LatLon.roundToOsmPrecisionStrict(99.9999999999),  100.0000000, 0);
        assertEquals(LatLon.roundToOsmPrecisionStrict(99.999999999),  100.0000000, 0);
        assertEquals(LatLon.roundToOsmPrecisionStrict(99.99999999),  100.0000000, 0);
        assertEquals(LatLon.roundToOsmPrecisionStrict(99.9999999),  99.9999999, 0);
    }
    
    /**
     * Test of {@link LatLon#toIntervalLon}
     */
    @Test
    public void testToIntervalLon() {
        assertEquals(-180.0, LatLon.toIntervalLon(-180.0), 0);
        assertEquals(0.0, LatLon.toIntervalLon(0.0), 0);
        assertEquals(180.0, LatLon.toIntervalLon(180.0), 0);

        assertEquals(179.0, LatLon.toIntervalLon(-181.0), 0);
        assertEquals(-179.0, LatLon.toIntervalLon(181.0), 0);

        assertEquals(-1.0, LatLon.toIntervalLon(359.0), 0);
        assertEquals(1.0, LatLon.toIntervalLon(-359.0), 0);

        assertEquals(1.0, LatLon.toIntervalLon(361.0), 0);
        assertEquals(-1.0, LatLon.toIntervalLon(-361.0), 0);

        assertEquals(179.0, LatLon.toIntervalLon(539.0), 0);
        assertEquals(-179.0, LatLon.toIntervalLon(-539.0), 0);

        assertEquals(-179.0, LatLon.toIntervalLon(541.0), 0);
        assertEquals(179.0, LatLon.toIntervalLon(-541.0), 0);
    }

    /**
     * Test of {@link LatLon#equals}
     */
    @Test
    public void testEquals() {
        for (int i = 1; i < sampleValues.length; i++) {
            LatLon a = new LatLon(sampleValues[i-1], sampleValues[i]);
            LatLon b = new LatLon(sampleValues[i-1], sampleValues[i]);
            assertEquals(a, b);
        }
    }

    /**
     * Test of {@link LatLon#hashCode}
     */
    @Test
    public void testHashCode() {
        for (int i = 1; i < sampleValues.length; i++) {
            LatLon a = new LatLon(sampleValues[i-1], sampleValues[i]);
            LatLon b = new LatLon(sampleValues[i-1], sampleValues[i]);
            assertEquals(a.hashCode(), b.hashCode());
        }
    }
}
