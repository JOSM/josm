// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.audio;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.gui.preferences.PreferencesTestUtils;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.testutils.annotations.JosmHome;

/**
 * Unit tests of {@link AudioPreference} class.
 */
@JosmHome
@BasicPreferences
class AudioPreferenceTest {
    /**
     * Unit test of {@link AudioPreference#AudioPreference}.
     */
    @Test
    void testAudioPreference() {
        assertNotNull(new AudioPreference.Factory().createPreferenceSetting());
    }

    /**
     * Unit test of {@link AudioPreference#addGui}.
     */
    @Test
    void testAddGui() {
        Config.getPref().putBoolean("audio.menuinvisible", true);
        PreferencesTestUtils.doTestPreferenceSettingAddGui(new AudioPreference.Factory(), null);
        Config.getPref().putBoolean("audio.menuinvisible", false);
        PreferencesTestUtils.doTestPreferenceSettingAddGui(new AudioPreference.Factory(), null);
    }
}
