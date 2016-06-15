// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.server;

import static org.junit.Assert.assertNotNull;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.gui.preferences.PreferencesTestUtils;

/**
 * Unit tests of {@link AuthenticationPreference} class.
 */
public class AuthenticationPreferenceTest {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Unit test of {@link AuthenticationPreference#AuthenticationPreference}.
     */
    @Test
    public void testAuthenticationPreference() {
        assertNotNull(new AuthenticationPreference.Factory().createPreferenceSetting());
    }

    /**
     * Unit test of {@link AuthenticationPreference#addGui}.
     */
    @Test
    public void testAddGui() {
        PreferencesTestUtils.doTestPreferenceSettingAddGui(new AuthenticationPreference.Factory(), ServerAccessPreference.class);
    }
}
