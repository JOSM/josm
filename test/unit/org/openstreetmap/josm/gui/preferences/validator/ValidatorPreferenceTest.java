// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.validator;

import static org.junit.Assert.assertNotNull;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.gui.preferences.PreferencesTestUtils;

/**
 * Unit tests of {@link ValidatorPreference} class.
 */
public class ValidatorPreferenceTest {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Unit test of {@link ValidatorPreference#ValidatorPreference}.
     */
    @Test
    public void testValidatorPreference() {
        assertNotNull(new ValidatorPreference.Factory().createPreferenceSetting());
    }

    /**
     * Unit test of {@link ValidatorPreference#addGui}.
     */
    @Test
    public void testAddGui() {
        PreferencesTestUtils.doTestPreferenceSettingAddGui(new ValidatorPreference.Factory(), null);
    }
}
