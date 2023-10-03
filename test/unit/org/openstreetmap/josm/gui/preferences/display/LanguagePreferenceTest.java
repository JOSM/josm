// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.display;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.gui.preferences.PreferencesTestUtils;

/**
 * Unit tests of {@link LanguagePreference} class.
 */
class LanguagePreferenceTest {
    /**
     * Unit test of {@link LanguagePreference#LanguagePreference}.
     */
    @Test
    void testLanguagePreference() {
        assertNotNull(new LanguagePreference.Factory().createPreferenceSetting());
    }

    /**
     * Unit test of {@link LanguagePreference#addGui}.
     */
    @Test
    void testAddGui() {
        PreferencesTestUtils.doTestPreferenceSettingAddGui(new LanguagePreference.Factory(), null);
    }
}
