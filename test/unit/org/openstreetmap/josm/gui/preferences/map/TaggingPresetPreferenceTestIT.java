// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.preferences.sources.ExtendedSourceEntry;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPreset;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetReader;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Logging;
import org.xml.sax.SAXException;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Integration tests of {@link TaggingPresetPreference} class.
 */
public class TaggingPresetPreferenceTestIT {

    /**
     * Setup rule
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().https().timeout(10000*60);

    /**
     * Test that available tagging presets are valid.
     * @throws Exception in case of error
     */
    @Test
    public void testValidityOfAvailablePresets() throws Exception {
        ImageProvider.clearCache();
        Collection<ExtendedSourceEntry> sources = new TaggingPresetPreference.TaggingPresetSourceEditor()
                .loadAndGetAvailableSources();
        assertFalse(sources.isEmpty());
        // Double traditional timeouts to avoid random problems
        Config.getPref().putInt("socket.timeout.connect", 30);
        Config.getPref().putInt("socket.timeout.read", 60);
        Map<Object, Throwable> allErrors = new HashMap<>();
        Set<String> allMessages = new HashSet<>();
        for (ExtendedSourceEntry source : sources) {
            System.out.println(source.url);
            try {
                testPresets(allMessages, source);
            } catch (IOException e) {
                try {
                    Logging.warn(e);
                    // try again in case of temporary network error
                    testPresets(allMessages, source);
                } catch (SAXException | IOException e1) {
                    e.printStackTrace();
                    // ignore frequent network errors with www.freietonne.de causing too much Jenkins failures
                    if (!source.url.contains("www.freietonne.de")) {
                        allErrors.put(source.url, e1);
                    }
                    System.out.println(" => KO");
                }
            } catch (SAXException | IllegalArgumentException e) {
                e.printStackTrace();
                if (!source.url.contains("yopaseopor/")) {
                    // ignore https://raw.githubusercontent.com/yopaseopor/traffic_signs_preset_JOSM cause too much errors
                    allErrors.put(source.url, e);
                }
                System.out.println(" => KO");
            }
        }
        ImageProvider.clearCache();
        assertTrue(allErrors.toString(), allErrors.isEmpty());
        assertTrue(allMessages.toString(), allMessages.isEmpty());
    }

    private static void testPresets(Set<String> allMessages, ExtendedSourceEntry source) throws SAXException, IOException {
        Collection<TaggingPreset> presets = TaggingPresetReader.readAll(source.url, true);
        assertFalse(presets.isEmpty());
        Collection<String> errorsAndWarnings = Logging.getLastErrorAndWarnings();
        boolean error = false;
        for (String message : errorsAndWarnings) {
            if (message.contains(TaggingPreset.PRESET_ICON_ERROR_MSG_PREFIX)) {
                error = true;
                // ignore https://github.com/yopaseopor/traffic_signs_preset_JOSM because of far too frequent missing icons errors
                if (!source.url.contains("yopaseopor/traffic_signs")) {
                    allMessages.add(message);
                }
            }
        }
        System.out.println(error ? " => KO" : " => OK");
        if (error) {
            Logging.clearLastErrorAndWarnings();
        }
    }
}
