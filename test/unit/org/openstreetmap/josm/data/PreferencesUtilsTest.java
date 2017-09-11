// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.trajano.commons.testing.UtilityClassTestUtil;

/**
 * Unit tests for class {@link PreferencesUtils}.
 */
public class PreferencesUtilsTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences();

    /**
     * Setup test.
     */
    @Before
    public void setUp() {
        PreferencesUtils.resetLog();
    }

    /**
     * Test method for {@link PreferencesUtils#log}.
     */
    @Test
    public void testLog() {
        assertEquals("", PreferencesUtils.getLog());
        PreferencesUtils.log("test");
        assertEquals("test\n", PreferencesUtils.getLog());
        PreferencesUtils.log("%d\n", 100);
        assertEquals("test\n100\n", PreferencesUtils.getLog());
        PreferencesUtils.log("test");
        assertEquals("test\n100\ntest\n", PreferencesUtils.getLog());
    }

    /**
     * Tests that {@code PreferencesUtils} satisfies utility class criterias.
     * @throws ReflectiveOperationException if an error occurs
     */
    @Test
    public void testUtilityClass() throws ReflectiveOperationException {
        UtilityClassTestUtil.assertUtilityClassWellDefined(PreferencesUtils.class);
    }
}
