// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.display;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.gui.layer.MapGridPaintable;
import org.openstreetmap.josm.gui.preferences.PreferencesTestUtils;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.testutils.annotations.Main;

/**
 * Unit tests of {@link GridPreference} class.
 */
@BasicPreferences
@Main
class GridPreferenceTest {
    /**
     * Unit test of {@link GridPreference#GridPreference}.
     */
    @Test
    void testGridPreference() {
        assertNotNull(new GridPreference.Factory().createPreferenceSetting());
    }

    /**
     * Unit test of {@link GridPreference#addGui}: the settings survive a round trip through the panel.
     */
    @Test
    void testAddGui() {
        MapGridPaintable.SPACING_X.put(0.25);
        MapGridPaintable.ROTATION.put(-12.5);
        try {
            PreferencesTestUtils.doTestPreferenceSettingAddGui(new GridPreference.Factory(), null);
            assertEquals(0.25, MapGridPaintable.SPACING_X.get(), 1e-12);
            assertEquals(-12.5, MapGridPaintable.ROTATION.get(), 1e-12);
        } finally {
            MapGridPaintable.SPACING_X.remove();
            MapGridPaintable.ROTATION.remove();
        }
    }
}
