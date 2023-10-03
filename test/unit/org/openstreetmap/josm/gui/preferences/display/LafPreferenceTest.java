// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.display;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.gui.preferences.PreferencesTestUtils;

/**
 * Unit tests of {@link LafPreference} class.
 */
class LafPreferenceTest {
    /**
     * Unit test of {@link LafPreference#LafPreference}.
     */
    @Test
    void testLafPreference() {
        assertNotNull(new LafPreference.Factory().createPreferenceSetting());
    }

    /**
     * Unit test of {@link LafPreference#addGui}.
     */
    @Test
    void testAddGui() {
        PreferencesTestUtils.doTestPreferenceSettingAddGui(new LafPreference.Factory(), null);
    }
}
