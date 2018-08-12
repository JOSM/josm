// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.server;

import static org.junit.Assert.assertNotNull;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.gui.preferences.PreferencesTestUtils;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link AuthenticationPreference} class.
 */
public class AuthenticationPreferenceTest {

    /**
     * Setup tests
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences();

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
