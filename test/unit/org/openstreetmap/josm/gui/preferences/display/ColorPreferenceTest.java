// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.display;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.gui.preferences.PreferencesTestUtils;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

/**
 * Unit tests of {@link ColorPreference} class.
 */
@BasicPreferences
class ColorPreferenceTest {
    /**
     * Unit test of {@link ColorPreference#ColorPreference}.
     */
    @Test
    void testColorPreference() {
        assertNotNull(new ColorPreference.Factory().createPreferenceSetting());
    }

    /**
     * Unit test of {@link ColorPreference#addGui}.
     */
    @Test
    void testAddGui() {
        PreferencesTestUtils.doTestPreferenceSettingAddGui(new ColorPreference.Factory(), null);
    }
}
