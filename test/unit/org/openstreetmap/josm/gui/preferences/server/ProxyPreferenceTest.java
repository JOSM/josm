// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.server;

import static org.junit.Assert.assertNotNull;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.gui.preferences.PreferencesTestUtils;

/**
 * Unit tests of {@link ProxyPreference} class.
 */
public class ProxyPreferenceTest {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Unit test of {@link ProxyPreference#ProxyPreference}.
     */
    @Test
    public void testProxyPreference() {
        assertNotNull(new ProxyPreference.Factory().createPreferenceSetting());
    }

    /**
     * Unit test of {@link ProxyPreference#addGui}.
     */
    @Test
    public void testAddGui() {
        PreferencesTestUtils.doTestPreferenceSettingAddGui(new ProxyPreference.Factory(), ServerAccessPreference.class);
    }
}
