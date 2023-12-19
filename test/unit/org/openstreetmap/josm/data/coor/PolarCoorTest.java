// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.coor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.text.DecimalFormat;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.testutils.annotations.Projection;

import nl.jqno.equalsverifier.EqualsVerifier;

/**
 * Test the {@link PolarCoor} class.
 */
@Projection
class PolarCoorTest {
    /**
     * Test {@link PolarCoor#PolarCoor}
     */
    @Test
    void testPolarCoor() {
        EastNorth en = new EastNorth(1000, 500);
        PolarCoor pc = new PolarCoor(en);
        assertEquals(1118.033988749, pc.radius, 1e-7);
        assertEquals(0.463647609, pc.angle, 1e-7);
        assertEquals(new EastNorth(0, 0), pc.pole);
        assertTrue(en.equalsEpsilon(pc.toEastNorth(), 1e-7));

        pc = new PolarCoor(1118.033988749, 0.463647609);
        assertEquals(1118.033988749, pc.radius, 1e-7);
        assertEquals(0.463647609, pc.angle, 1e-7);
        assertEquals(new EastNorth(0, 0), pc.pole);
        assertTrue(en.equalsEpsilon(pc.toEastNorth(), 1e-7));
    }

    /**
     * Unit test of methods {@link PolarCoor#equals} and {@link PolarCoor#hashCode}.
     */
    @Test
    void testEqualsContract() {
        TestUtils.assumeWorkingEqualsVerifier();
        EqualsVerifier.forClass(PolarCoor.class).usingGetClass()
            .withPrefabValues(DecimalFormat.class, new DecimalFormat("00.0"), new DecimalFormat("00.000"))
            .verify();
    }

    /**
     * Unit test of method {@link PolarCoor#toString}.
     */
    @Test
    void testToString() {
        assertEquals("PolarCoor [radius=1118.033988749, angle=0.463647609, pole=EastNorth[e=0.0, n=0.0]]",
                new PolarCoor(1118.033988749, 0.463647609).toString());
    }
}
