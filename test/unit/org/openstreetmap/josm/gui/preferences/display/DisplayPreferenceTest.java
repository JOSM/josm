// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.display;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.gui.preferences.PreferencesTestUtils;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

/**
 * Unit tests of {@link DisplayPreference} class.
 */
@BasicPreferences
class DisplayPreferenceTest {
    /**
     * Unit test of {@link DisplayPreference#DisplayPreference}.
     */
    @Test
    void testDisplayPreference() {
        assertNotNull(new DisplayPreference.Factory().createPreferenceSetting());
    }

    /**
     * Unit test of {@link DisplayPreference#addGui}.
     */
    @Test
    void testAddGui() {
        PreferencesTestUtils.doTestPreferenceSettingAddGui(new DisplayPreference.Factory(), null);
    }
}
