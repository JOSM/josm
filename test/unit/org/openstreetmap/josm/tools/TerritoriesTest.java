// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.testutils.JOSMTestRules;

/**
 * Unit tests of {@link Territories} class.
 */
public class TerritoriesTest {
    /**
     * Test rules.
     */
    @Rule
    public JOSMTestRules rules = new JOSMTestRules().platform().projection().commands();

    /**
     * Test of {@link Territories#getIso3166Codes} method.
     */
    @Test
    public void testGetIso3166Codes() {
        check("Paris", new LatLon(48.8567, 2.3508), "EU", "FR", "FX");
    }

    private static void check(String name, LatLon ll, String ... expectedCodes) {
        Set<String> codes = Territories.getIso3166Codes(ll);
        assertEquals(name + " -> " + codes, expectedCodes.length, codes.size());
        for (String e : expectedCodes) {
            assertTrue(e, codes.contains(e));
        }
    }
}
