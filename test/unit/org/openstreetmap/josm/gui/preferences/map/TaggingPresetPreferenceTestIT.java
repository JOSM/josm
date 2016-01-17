// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.preferences.SourceEditor.ExtendedSourceEntry;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPreset;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetReader;
import org.xml.sax.SAXException;

/**
 * Integration tests of {@link TaggingPresetPreference} class.
 */
public class TaggingPresetPreferenceTestIT {

    /**
     * Global timeout applied to all test methods.
     */
    @Rule
    public Timeout globalTimeout = Timeout.seconds(10*60);

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
        // Double traditional timeouts to avoid random problems
        Main.pref.putInteger("socket.timeout.connect", 30);
        Main.pref.putInteger("socket.timeout.read", 60);
        Collection<Throwable> allErrors = new ArrayList<>();
        Set<String> allMessages = new HashSet<>();
        for (ExtendedSourceEntry source : sources) {
            System.out.println(source.url);
            try {
                testPresets(allMessages, source);
            } catch (IOException e) {
                try {
                    Main.warn(e);
                    // try again in case of temporary network error
                    testPresets(allMessages, source);
                } catch (SAXException | IOException e1) {
                    e.printStackTrace();
                    allErrors.add(e1);
                    System.out.println(" => KO");
                }
            } catch (SAXException e) {
                e.printStackTrace();
                allErrors.add(e);
                System.out.println(" => KO");
            }
        }
        assertTrue(allErrors.isEmpty());
        assertTrue(allMessages.isEmpty());
    }

    private static void testPresets(Set<String> allMessages, ExtendedSourceEntry source) throws SAXException, IOException {
        Collection<TaggingPreset> presets = TaggingPresetReader.readAll(source.url, true);
        assertFalse(presets.isEmpty());
        Collection<String> errorsAndWarnings = Main.getLastErrorAndWarnings();
        boolean error = false;
        for (String message : errorsAndWarnings) {
            if (message.contains(TaggingPreset.PRESET_ICON_ERROR_MSG_PREFIX)) {
                error = true;
                allMessages.add(message);
            }
        }
        System.out.println(error ? " => KO" : " => OK");
        if (error) {
            Main.clearLastErrorAndWarnings();
        }
    }
}
