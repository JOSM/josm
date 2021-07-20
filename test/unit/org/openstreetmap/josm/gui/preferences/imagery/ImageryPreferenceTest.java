// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.imagery;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.gui.preferences.PreferencesTestUtils;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.testutils.annotations.FullPreferences;
import org.openstreetmap.josm.testutils.annotations.Main;

/**
 * Unit tests of {@link ImageryPreference} class.
 */
@FullPreferences
@Main
class ImageryPreferenceTest {
    /**
     * Unit test of {@link ImageryPreference#ImageryPreference}.
     */
    @Test
    void testImageryPreference() {
        assertNotNull(new ImageryPreference.Factory().createPreferenceSetting());
    }

    /**
     * Unit test of {@link ImageryPreference#addGui}.
     */
    @Test
    void testAddGui() {
        String fileUrl = new File(TestUtils.getTestDataRoot()+"__files/imagery/maps.xml").toURI().toString();
        Config.getPref().putList("imagery.layers.sites", Arrays.asList(fileUrl));
        PreferencesTestUtils.doTestPreferenceSettingAddGui(new ImageryPreference.Factory(), null);
    }
}
