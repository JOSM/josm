// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.display;

import static org.junit.Assert.assertNotNull;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.gui.preferences.PreferencesTestUtils;

/**
 * Unit tests of {@link LanguagePreference} class.
 */
public class LanguagePreferenceTest {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Unit test of {@link LanguagePreference#LanguagePreference}.
     */
    @Test
    public void testLanguagePreference() {
        assertNotNull(new LanguagePreference.Factory().createPreferenceSetting());
    }

    /**
     * Unit test of {@link LanguagePreference#addGui}.
     */
    @Test
    public void testAddGui() {
        PreferencesTestUtils.doTestPreferenceSettingAddGui(new LanguagePreference.Factory(), null);
    }
}
