// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection.proj;

import static org.junit.Assert.assertFalse;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Tests for {@link LonLat}.
 */
public class LonLatTest {
    /**
     * Setup rule.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    /**
     * Test {@link LonLat#lonIsLinearToEast}
     */
    @Test
    public void testLonIsLinearToEast() {
        assertFalse(new LonLat().lonIsLinearToEast());
    }
}
