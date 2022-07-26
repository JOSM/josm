// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.shortcut;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.gui.preferences.PreferencesTestUtils;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

/**
 * Unit tests of {@link ShortcutPreference} class.
 */
@BasicPreferences
class ShortcutPreferenceTest {
    /**
     * Unit test of {@link ShortcutPreference#ShortcutPreference}.
     */
    @Test
    void testShortcutPreference() {
        assertNotNull(new ShortcutPreference.Factory().createPreferenceSetting());
    }

    /**
     * Unit test of {@link ShortcutPreference#addGui}.
     */
    @Test
    void testAddGui() {
        PreferencesTestUtils.doTestPreferenceSettingAddGui(new ShortcutPreference.Factory(), null);
    }
}
