// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.validation.tests.Addresses;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

import net.trajano.commons.testing.UtilityClassTestUtil;

/**
 * Unit tests for class {@link OsmValidator}.
 */
@BasicPreferences
class OsmValidatorTest {
    /**
     * Setup test.
     * @throws Exception if an error occurs
     */
    @BeforeEach
    public void setUp() throws Exception {
        OsmValidator.clearIgnoredErrors();
    }

    /**
     * Tests that {@code OsmValidator} satisfies utility class criteria.
     * @throws ReflectiveOperationException if an error occurs
     */
    @Test
    void testUtilityClass() throws ReflectiveOperationException {
        UtilityClassTestUtil.assertUtilityClassWellDefined(OsmValidator.class);
    }

    /**
     * Test that {@link OsmValidator#cleanupIgnoredErrors()} really removes entry with element IDs when group is ignored
     */
    @Test
    void testCleanupIgnoredErrors1() {
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
    void testCleanupIgnoredErrorsTicket17837() {
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
    void testCleanupIgnoredErrorsTicket18223() {
        OsmValidator.addIgnoredError("1351:n_2449148994:w_236955234", "Way end node near other way");
        OsmValidator.addIgnoredError("1351:n_6871910559:w_733713588", "Way end node near other way");
        OsmValidator.cleanupIgnoredErrors();
        assertFalse(OsmValidator.hasIgnoredError("1351"));
        assertTrue(OsmValidator.hasIgnoredError("1351:n_2449148994:w_236955234"));
        assertTrue(OsmValidator.hasIgnoredError("1351:n_6871910559:w_733713588"));
    }

    /**
     * Test that tests are really removed, and that core tests cannot be removed
     */
    @Test
    void testRemoveTests() {
        org.openstreetmap.josm.data.validation.Test test = new org.openstreetmap.josm.data.validation.Test("test") {
        };
        assertNotEquals(org.openstreetmap.josm.data.validation.Test.class, test.getClass());
        OsmValidator.addTest(test.getClass());
        assertTrue(OsmValidator.getAllAvailableTestClasses().contains(test.getClass()));
        assertTrue(OsmValidator.removeTest(test.getClass()));
        assertFalse(OsmValidator.removeTest(test.getClass()));
        assertFalse(OsmValidator.getAllAvailableTestClasses().contains(test.getClass()));

        assertTrue(OsmValidator.getAllAvailableTestClasses().contains(Addresses.class));
        assertFalse(OsmValidator.removeTest(Addresses.class));
        assertTrue(OsmValidator.getAllAvailableTestClasses().contains(Addresses.class));
    }

}
