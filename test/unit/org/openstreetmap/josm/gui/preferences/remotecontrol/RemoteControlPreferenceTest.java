// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.remotecontrol;

import static org.junit.Assert.assertNotNull;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.gui.preferences.PreferencesTestUtils;

/**
 * Unit tests of {@link RemoteControlPreference} class.
 */
public class RemoteControlPreferenceTest {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Unit test of {@link RemoteControlPreference#RemoteControlPreference}.
     */
    @Test
    public void testRemoteControlPreference() {
        assertNotNull(new RemoteControlPreference.Factory().createPreferenceSetting());
    }

    /**
     * Unit test of {@link RemoteControlPreference#addGui}.
     */
    @Test
    public void testAddGui() {
        PreferencesTestUtils.doTestPreferenceSettingAddGui(new RemoteControlPreference.Factory(), null);
    }
}
