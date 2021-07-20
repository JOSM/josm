// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.display;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.gui.preferences.PreferencesTestUtils;
import org.openstreetmap.josm.testutils.annotations.FullPreferences;
import org.openstreetmap.josm.testutils.annotations.I18n;

/**
 * Unit tests of {@link DrawingPreference} class.
 */
@FullPreferences
@I18n
class DrawingPreferenceTest {

    /**
     * Unit test of {@link DrawingPreference#DrawingPreference}.
     */
    @Test
    void testDrawingPreference() {
        assertNotNull(new DrawingPreference.Factory().createPreferenceSetting());
    }

    /**
     * Unit test of {@link DrawingPreference#addGui}.
     */
    @Test
    void testAddGui() {
        PreferencesTestUtils.doTestPreferenceSettingAddGui(new DrawingPreference.Factory(), null);
    }
}
