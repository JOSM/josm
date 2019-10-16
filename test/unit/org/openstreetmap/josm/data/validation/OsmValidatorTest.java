// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.trajano.commons.testing.UtilityClassTestUtil;

/**
 * Unit tests for class {@link OsmValidator}.
 */
public class OsmValidatorTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().projection();

    /**
     * Setup test.
     * @throws Exception if an error occurs
     */
    @Before
    public void setUp() throws Exception {
        OsmValidator.clearIgnoredErrors();
    }
    /**
     * Tests that {@code OsmValidator} satisfies utility class criterias.
     * @throws ReflectiveOperationException if an error occurs
     */
    @Test
    public void testUtilityClass() throws ReflectiveOperationException {
        UtilityClassTestUtil.assertUtilityClassWellDefined(OsmValidator.class);
    }

    /**
     * Test that {@link OsmValidator#cleanupIgnoredErrors()} really removes entry with element IDs when group is ignored
     */
    @Test
    public void testCleanupIgnoredErrors1() {
        OsmValidator.addIgnoredError("1351:n_2449148994:w_236955234", "Way end node near other way");
        OsmValidator.addIgnoredError("1351:n_6871910559:w_733713588", "Way end node near other way");
        OsmValidator.addIgnoredError("1351");
        OsmValidator.cleanupIgnoredErrors();
        assertTrue(OsmValidator.hasIgnoredError("1351"));
        assertFalse(OsmValidator.hasIgnoredError("1351:n_6871910559:w_733713588"));
        assertFalse(OsmValidator.hasIgnoredError("1351:n_2449148994:w_236955234"));
    }

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/17837">Bug #17837</a>.
     * {@link OsmValidator#cleanupIgnoredErrors()} must not remove entry 1201 when 120 is before it.
     */
    @Test
    public void testCleanupIgnoredErrorsTicket17837() {
        OsmValidator.addIgnoredError("120");
        OsmValidator.addIgnoredError("3000");
        OsmValidator.addIgnoredError("1201"); // starts with 120, but has different code
        OsmValidator.cleanupIgnoredErrors();
        assertTrue(OsmValidator.hasIgnoredError("1201"));
    }

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/18223">Bug #18223</a>.
     * {@link OsmValidator#cleanupIgnoredErrors()} must not combine primitives.
     */
    @Test
    public void testCleanupIgnoredErrorsTicket18223() {
        OsmValidator.addIgnoredError("1351:n_2449148994:w_236955234", "Way end node near other way");
        OsmValidator.addIgnoredError("1351:n_6871910559:w_733713588", "Way end node near other way");
        OsmValidator.cleanupIgnoredErrors();
        assertFalse(OsmValidator.hasIgnoredError("1351"));
        assertTrue(OsmValidator.hasIgnoredError("1351:n_2449148994:w_236955234"));
        assertTrue(OsmValidator.hasIgnoredError("1351:n_6871910559:w_733713588"));
    }

}
