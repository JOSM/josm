// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.remotecontrol;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.gui.preferences.PreferencesTestUtils;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

/**
 * Unit tests of {@link RemoteControlPreference} class.
 */
@BasicPreferences
class RemoteControlPreferenceTest {
    /**
     * Unit test of {@link RemoteControlPreference#RemoteControlPreference}.
     */
    @Test
    void testRemoteControlPreference() {
        assertNotNull(new RemoteControlPreference.Factory().createPreferenceSetting());
    }

    /**
     * Unit test of {@link RemoteControlPreference#addGui}.
     */
    @Test
    void testAddGui() {
        PreferencesTestUtils.doTestPreferenceSettingAddGui(new RemoteControlPreference.Factory(), null);
    }
}
