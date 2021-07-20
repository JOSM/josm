// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.projection;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.gui.preferences.PreferencesTestUtils;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

/**
 * Unit tests of {@link ProjectionPreference} class.
 */
@BasicPreferences
class ProjectionPreferenceTest {
    /**
     * Unit test of {@link ProjectionPreference#ProjectionPreference}.
     */
    @Test
    void testProjectionPreference() {
        assertNotNull(new ProjectionPreference.Factory().createPreferenceSetting());
    }

    /**
     * Unit test of {@link ProjectionPreference#addGui}.
     */
    @Test
    void testAddGui() {
        PreferencesTestUtils.doTestPreferenceSettingAddGui(new ProjectionPreference.Factory(), null);
    }
}
