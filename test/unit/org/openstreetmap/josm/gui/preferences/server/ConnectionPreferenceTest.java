// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.server;

import static org.junit.Assert.assertNotNull;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.gui.preferences.PreferencesTestUtils;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link ConnectionPreference} class.
 */
public class ConnectionPreferenceTest {

    /**
     * Setup tests
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences();

    /**
     * Unit test of {@link ConnectionPreference#AuthenticationPreference}.
     */
    @Test
    public void testAuthenticationPreference() {
        assertNotNull(new ConnectionPreference.Factory().createPreferenceSetting());
    }

    /**
     * Unit test of {@link ConnectionPreference#addGui}.
     */
    @Test
    public void testAddGui() {
        PreferencesTestUtils.doTestPreferenceSettingAddGui(new ConnectionPreference.Factory(), ServerAccessPreference.class);
    }
}
