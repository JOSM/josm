// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link Territories} class.
 */
public class TerritoriesTest {
    /**
     * Test rules.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules rules = new JOSMTestRules().platform().projection().commands();

    /**
     * Test of {@link Territories#getIso3166Codes} method.
     */
    @Test
    public void testGetIso3166Codes() {
        check("Paris", new LatLon(48.8567, 2.3508), "EU", "FR", "FX");
    }

    private static void check(String name, LatLon ll, String ... expectedCodes) {
        for (String e : expectedCodes) {
            assertTrue(name + " " + e, Territories.isIso3166Code(e, ll));
        }
    }
}
