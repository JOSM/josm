// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.advanced;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.gui.preferences.PreferencesTestUtils;
import org.openstreetmap.josm.testutils.annotations.FullPreferences;

/**
 * Unit tests of {@link AdvancedPreference} class.
 */
@FullPreferences
class AdvancedPreferenceTest {
    /**
     * Unit test of {@link AdvancedPreference#AdvancedPreference}.
     */
    @Test
    void testAdvancedPreference() {
        assertNotNull(new AdvancedPreference.Factory().createPreferenceSetting());
    }

    /**
     * Unit test of {@link AdvancedPreference#addGui}.
     */
    @Test
    void testAddGui() {
        assertDoesNotThrow(() -> PreferencesTestUtils.doTestPreferenceSettingAddGui(new AdvancedPreference.Factory(), null));
    }
}
