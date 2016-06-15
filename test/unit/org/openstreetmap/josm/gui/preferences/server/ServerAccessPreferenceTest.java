// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.server;

import static org.junit.Assert.assertNotNull;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.gui.preferences.PreferencesTestUtils;

/**
 * Unit tests of {@link ServerAccessPreference} class.
 */
public class ServerAccessPreferenceTest {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Unit test of {@link ServerAccessPreference#ServerAccessPreference}.
     */
    @Test
    public void testServerAccessPreference() {
        assertNotNull(new ServerAccessPreference.Factory().createPreferenceSetting());
    }

    /**
     * Unit test of {@link ServerAccessPreference#addGui}.
     */
    @Test
    public void testAddGui() {
        PreferencesTestUtils.doTestPreferenceSettingAddGui(new ServerAccessPreference.Factory(), null);
    }
}
