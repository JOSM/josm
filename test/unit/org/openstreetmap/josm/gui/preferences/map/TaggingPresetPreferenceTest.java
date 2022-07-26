// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.map;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.gui.preferences.PreferencesTestUtils;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

/**
 * Unit tests of {@link TaggingPresetPreference} class.
 */
@BasicPreferences
class TaggingPresetPreferenceTest {
    /**
     * Unit test of {@link TaggingPresetPreference#TaggingPresetPreference}.
     */
    @Test
    void testTaggingPresetPreference() {
        assertNotNull(new TaggingPresetPreference.Factory().createPreferenceSetting());
    }

    /**
     * Unit test of {@link TaggingPresetPreference#addGui}.
     */
    @Test
    void testAddGui() {
        PreferencesTestUtils.doTestPreferenceSettingAddGui(new TaggingPresetPreference.Factory(), null);
    }
}
