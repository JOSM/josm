// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.imagery;

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.util.Arrays;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.gui.preferences.PreferencesTestUtils;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link ImageryPreference} class.
 */
public class ImageryPreferenceTest {

    /**
     * Setup tests
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().platform().main();

    /**
     * Unit test of {@link ImageryPreference#ImageryPreference}.
     */
    @Test
    public void testImageryPreference() {
        assertNotNull(new ImageryPreference.Factory().createPreferenceSetting());
    }

    /**
     * Unit test of {@link ImageryPreference#addGui}.
     */
    @Test
    public void testAddGui() {
        String fileUrl = new File(TestUtils.getTestDataRoot()+"__files/imagery/maps.xml").toURI().toString();
        Config.getPref().putList("imagery.layers.sites", Arrays.asList(fileUrl));
        PreferencesTestUtils.doTestPreferenceSettingAddGui(new ImageryPreference.Factory(), null);
    }
}
