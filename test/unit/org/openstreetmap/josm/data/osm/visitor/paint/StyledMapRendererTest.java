// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.visitor.paint;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.osm.visitor.paint.StyledMapRenderer.StyleRecord;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

/**
 * Test the {@link StyledMapRenderer}
 * @author Michael Zangl
 * @since 12078
 */
public class StyledMapRendererTest {

    /**
     * Tests the floatToFixed function.
     */
    @Test
    public void testFloatToFixed() {
        long inf = floatToFixedCheckBits(Float.POSITIVE_INFINITY, 24);
        long big = floatToFixedCheckBits(Float.MAX_VALUE, 24);
        long two = floatToFixedCheckBits(2, 24);
        // We use 15 bit for the significand. This should give us at least 3 decimal places
        long x = floatToFixedCheckBits(1.001f, 24);
        long one = floatToFixedCheckBits(1, 24);
        long delta = floatToFixedCheckBits(Float.MIN_VALUE * 500, 24);
        long epsilon = floatToFixedCheckBits(Float.MIN_VALUE, 24);
        long zero = floatToFixedCheckBits(0, 24);
        long negzero = floatToFixedCheckBits(-0, 24);
        long negepsilon = floatToFixedCheckBits(-Float.MIN_VALUE, 24);
        long negdelta = floatToFixedCheckBits(-Float.MIN_VALUE * 500, 24);
        long negone = floatToFixedCheckBits(-1, 24);
        long negx = floatToFixedCheckBits(-1.001f, 24);
        long negtwo = floatToFixedCheckBits(-2, 24);
        long negbig = floatToFixedCheckBits(-Float.MAX_VALUE, 24);
        long neginf = floatToFixedCheckBits(Float.NEGATIVE_INFINITY, 24);

        System.out.println(Integer.toHexString(Float.floatToIntBits(-Float.MAX_VALUE)));
        // Positive
        assertTrue(inf > big);
        assertTrue(big > two);
        assertTrue(two > x);
        assertTrue(x > one);
        assertTrue(one > delta);

        // Close to zero - we don't care which way the epsilon round, but delta should not equal zero.
        assertTrue(delta > zero);
        assertTrue(negzero > negdelta);

        assertTrue(delta >= epsilon);
        assertTrue(epsilon >= zero);
        assertTrue(zero >= negzero);
        assertTrue(negzero >= negepsilon);
        assertTrue(negepsilon >= negdelta);

        // Negative
        assertTrue(negdelta > negone);
        assertTrue(negone > negx);
        assertTrue(negx > negtwo);
        assertTrue(negtwo > negbig);
        assertTrue(negbig > neginf);

        // Check the bit count
        floatToFixedCheckBits((float) Math.PI, 32);
        floatToFixedCheckBits((float) Math.PI, 20);
        floatToFixedCheckBits((float) Math.PI, 25);
        floatToFixedCheckBits(Float.NaN, 24);
    }

    private static long floatToFixedCheckBits(float number, int totalBits) {
        long result = StyleRecord.floatToFixed(number, totalBits);
        long shouldBeZero = result >> totalBits;
        assertEquals(0, shouldBeZero);
        return result;
    }

    /**
     * Unit test of methods {@link StyleRecord#equals} and {@link StyleRecord#hashCode}.
     */
    @Test
    public void testEqualsContract() {
        TestUtils.assumeWorkingEqualsVerifier();
        EqualsVerifier.forClass(StyleRecord.class).usingGetClass()
            .suppress(Warning.NONFINAL_FIELDS)
            .verify();
    }
}
