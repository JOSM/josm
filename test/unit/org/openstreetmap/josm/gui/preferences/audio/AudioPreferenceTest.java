// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.audio;

import static org.junit.Assert.assertNotNull;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.preferences.PreferencesTestUtils;

/**
 * Unit tests of {@link AudioPreference} class.
 */
public class AudioPreferenceTest {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Unit test of {@link AudioPreference#AudioPreference}.
     */
    @Test
    public void testAudioPreference() {
        assertNotNull(new AudioPreference.Factory().createPreferenceSetting());
    }

    /**
     * Unit test of {@link AudioPreference#addGui}.
     */
    @Test
    public void testAddGui() {
        Main.pref.putBoolean("audio.menuinvisible", true);
        PreferencesTestUtils.doTestPreferenceSettingAddGui(new AudioPreference.Factory(), null);
        Main.pref.putBoolean("audio.menuinvisible", false);
        PreferencesTestUtils.doTestPreferenceSettingAddGui(new AudioPreference.Factory(), null);
    }
}
