// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.validator;

import static org.junit.Assert.assertNotNull;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.gui.preferences.PreferencesTestUtils;

/**
 * Unit tests of {@link ValidatorTestsPreference} class.
 */
public class ValidatorTestsPreferenceTest {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Unit test of {@link ValidatorTestsPreference#ValidatorTestsPreference}.
     */
    @Test
    public void testValidatorTestsPreference() {
        assertNotNull(new ValidatorTestsPreference.Factory().createPreferenceSetting());
    }

    /**
     * Unit test of {@link ValidatorTestsPreference#addGui}.
     */
    @Test
    public void testAddGui() {
        PreferencesTestUtils.doTestPreferenceSettingAddGui(new ValidatorTestsPreference.Factory(), ValidatorPreference.class);
    }
}
