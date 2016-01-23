// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.plugin;

import static org.junit.Assert.assertNotNull;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;

/**
 * Unit tests of {@link PluginPreference} class.
 */
public class PluginPreferenceTest {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Unit test of {@link PluginPreference#PluginPreference}.
     */
    @Test
    public void testPluginPreference()  {
        assertNotNull(new PluginPreference.Factory().createPreferenceSetting());
    }
}
