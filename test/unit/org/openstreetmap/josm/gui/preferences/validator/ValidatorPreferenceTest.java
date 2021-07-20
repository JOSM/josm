// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.validator;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.gui.preferences.PreferencesTestUtils;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

/**
 * Unit tests of {@link ValidatorPreference} class.
 */
@BasicPreferences
class ValidatorPreferenceTest {
    /**
     * Unit test of {@link ValidatorPreference#ValidatorPreference}.
     */
    @Test
    void testValidatorPreference() {
        assertNotNull(new ValidatorPreference.Factory().createPreferenceSetting());
    }

    /**
     * Unit test of {@link ValidatorPreference#addGui}.
     */
    @Test
    void testAddGui() {
        PreferencesTestUtils.doTestPreferenceSettingAddGui(new ValidatorPreference.Factory(), null);
    }
}
