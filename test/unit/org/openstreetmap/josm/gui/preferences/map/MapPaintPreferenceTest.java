// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.map;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.gui.preferences.PreferencesTestUtils;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

/**
 * Unit tests of {@link MapPaintPreference} class.
 */
@BasicPreferences
class MapPaintPreferenceTest {
    /**
     * Unit test of {@link MapPaintPreference#MapPaintPreference}.
     */
    @Test
    void testMapPaintPreference() {
        assertNotNull(new MapPaintPreference.Factory().createPreferenceSetting());
    }

    /**
     * Unit test of {@link MapPaintPreference#addGui}.
     */
    @Test
    void testAddGui() {
        PreferencesTestUtils.doTestPreferenceSettingAddGui(new MapPaintPreference.Factory(), null);
    }
}
