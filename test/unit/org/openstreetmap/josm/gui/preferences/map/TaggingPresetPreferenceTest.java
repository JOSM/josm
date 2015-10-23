// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.preferences.SourceEditor.ExtendedSourceEntry;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPreset;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetReader;
import org.xml.sax.SAXException;

/**
 * Unit tests of {@link TaggingPresetPreference} class.
 */
public class TaggingPresetPreferenceTest {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Test that available tagging presets are valid.
     */
    @Test
    public void testValidityOfAvailablePresets() {
        Collection<ExtendedSourceEntry> sources = new TaggingPresetPreference.TaggingPresetSourceEditor()
                .loadAndGetAvailableSources();
        assertFalse(sources.isEmpty());
        Collection<Throwable> allErrors = new ArrayList<>();
        Set<String> allMessages = new HashSet<>();
        for (ExtendedSourceEntry source : sources) {
            System.out.print(source.url);
            try {
                Collection<TaggingPreset> presets = TaggingPresetReader.readAll(source.url, true);
                assertFalse(presets.isEmpty());
                System.out.println(" => OK");
                allMessages.addAll(Main.getLastErrorAndWarnings());
            } catch (SAXException | IOException e) {
                e.printStackTrace();
                allErrors.add(e);
                System.out.println(" => KO");
            }
        }
        assertTrue(allErrors.isEmpty());
        for (String message : allMessages) {
            if (message.contains(TaggingPreset.PRESET_ICON_ERROR_MSG_PREFIX)) {
                fail(message);
            }
        }
    }
}
