// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.validator;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.gui.preferences.PreferencesTestUtils;

/**
 * Unit tests of {@link ValidatorTestsPreference} class.
 */
class ValidatorTestsPreferenceTest {
    /**
     * Unit test of {@link ValidatorTestsPreference#ValidatorTestsPreference}.
     */
    @Test
    void testValidatorTestsPreference() {
        assertNotNull(new ValidatorTestsPreference.Factory().createPreferenceSetting());
    }

    /**
     * Unit test of {@link ValidatorTestsPreference#addGui}.
     */
    @Test
    void testAddGui() {
        PreferencesTestUtils.doTestPreferenceSettingAddGui(new ValidatorTestsPreference.Factory(), ValidatorPreference.class);
    }
}
