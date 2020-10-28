// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.remotecontrol;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.gui.preferences.PreferencesTestUtils;

/**
 * Unit tests of {@link RemoteControlPreference} class.
 */
class RemoteControlPreferenceTest {

    /**
     * Setup test.
     */
    @BeforeAll
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init();
    }

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
