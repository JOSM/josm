// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.trajano.commons.testing.UtilityClassTestUtil;

/**
 * Unit tests of {@link Territories} class.
 */
public class TerritoriesTest {
    /**
     * Test rules.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules rules = new JOSMTestRules().platform().projection().territories();

    /**
     * Tests that {@code Territories} satisfies utility class criterias.
     * @throws ReflectiveOperationException if an error occurs
     */
    @Test
    public void testUtilityClass() throws ReflectiveOperationException {
        UtilityClassTestUtil.assertUtilityClassWellDefined(Territories.class);
    }

    /**
     * Test of {@link Territories#isIso3166Code} method.
     */
    @Test
    public void testIsIso3166Code() {
        check("Paris", new LatLon(48.8567, 2.3508), "EU", "FR", "FX");
    }

    private static void check(String name, LatLon ll, String... expectedCodes) {
        for (String e : expectedCodes) {
            assertTrue(name + " " + e, Territories.isIso3166Code(e, ll));
        }
    }
}
