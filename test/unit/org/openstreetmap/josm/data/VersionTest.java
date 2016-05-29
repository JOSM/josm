// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data;

import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;

/**
 * Unit tests for class {@link Version}.
 */
public class VersionTest {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Unit test of {@link Version#getAgentString}
     */
    @Test
    public void testGetAgentString() {
        String v = Version.getInstance().getAgentString(false);
        assertTrue(v.startsWith("JOSM/1.5 ("));
        assertTrue(v.endsWith(" en)"));
        v = Version.getInstance().getAgentString(true);
        assertTrue(v.startsWith("JOSM/1.5 ("));
        assertTrue(v.contains(" en) "));
    }
}
