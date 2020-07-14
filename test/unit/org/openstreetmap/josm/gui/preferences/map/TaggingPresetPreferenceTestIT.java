// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.preferences.sources.ExtendedSourceEntry;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPreset;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetReader;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetsTest;
import org.openstreetmap.josm.gui.tagging.presets.items.Link;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.tools.HttpClient;
import org.openstreetmap.josm.tools.HttpClient.Response;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Logging;
import org.xml.sax.SAXException;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Integration tests of {@link TaggingPresetPreference} class.
 */
@RunWith(Parameterized.class)
public class TaggingPresetPreferenceTestIT extends AbstractExtendedSourceEntryTestCase {

    /**
     * Setup rule
     */
    @ClassRule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public static JOSMTestRules test = new JOSMTestRules().https().timeout(10000*120).parameters();

    /**
     * Setup test
     * @throws IOException in case of I/O error
     */
    @BeforeClass
    public static void beforeClass() throws IOException {
        errorsToIgnore.addAll(TestUtils.getIgnoredErrorMessages(TaggingPresetPreferenceTestIT.class));
        // Double traditional timeouts to avoid random problems
        Config.getPref().putInt("socket.timeout.connect", 30);
        Config.getPref().putInt("socket.timeout.read", 60);
        // Make sure error messages are in english
        Locale.setDefault(Locale.ENGLISH);
    }

    /**
     * Returns list of tagging presets to test.
     * @return list of tagging presets to test
     * @throws Exception if an error occurs
     */
    @Parameters(name = "{0} - {1}")
    public static List<Object[]> data() throws Exception {
        ImageProvider.clearCache();
        return getTestParameters(new TaggingPresetPreference.TaggingPresetSourceEditor().loadAndGetAvailableSources());
    }

    /**
     * Constructs a new {@code TaggingPresetPreferenceTestIT}
     * @param displayName displayed name
     * @param url URL
     * @param source source entry to test
     */
    public TaggingPresetPreferenceTestIT(String displayName, String url, ExtendedSourceEntry source) {
        super(source);
    }

    /**
     * Test that tagging presets are valid.
     * @throws Exception in case of error
     */
    @Test
    public void testPresetsValidity() throws Exception {
        assumeFalse(isIgnoredSubstring(source.url));
        Set<String> errors = new HashSet<>();
        try {
            testPresets(errors, source);
        } catch (IOException e) {
            try {
                Logging.warn(e);
                // try again in case of temporary network error
                testPresets(errors, source);
            } catch (SAXException | IOException e1) {
                handleException(e1, errors);
            }
        } catch (SAXException | IllegalArgumentException e) {
            handleException(e, errors);
        }
        assertTrue(errors.toString(), errors.isEmpty());
        assumeTrue(ignoredErrors.toString(), ignoredErrors.isEmpty());
    }

    private void testPresets(Set<String> messages, ExtendedSourceEntry source) throws SAXException, IOException {
        Collection<TaggingPreset> presets = TaggingPresetReader.readAll(source.url, true);
        assertFalse(presets.isEmpty());
        TaggingPresetsTest.waitForIconLoading(presets);
        // check that links are correct and not redirections
        presets.parallelStream().flatMap(x -> x.data.stream().filter(i -> i instanceof Link).map(i -> ((Link) i).getUrl())).forEach(u -> {
            try {
                Response cr = HttpClient.create(new URL(u)).setMaxRedirects(-1).connect();
                final int code = cr.getResponseCode();
                if (HttpClient.isRedirect(code)) {
                    addOrIgnoreError(messages, "Found HTTP redirection for " + u + " -> " + code + " -> " + cr.getHeaderField("Location"));
                } else if (code >= 400) {
                    addOrIgnoreError(messages, "Found HTTP error for " + u + " -> " + code);
                }
            } catch (IOException e) {
                Logging.error(e);
            }
        });
        Collection<String> errorsAndWarnings = Logging.getLastErrorAndWarnings();
        boolean error = false;
        for (String message : errorsAndWarnings) {
            if (message.contains(TaggingPreset.PRESET_ICON_ERROR_MSG_PREFIX)) {
                error = true;
                addOrIgnoreError(messages, message);
            }
        }
        if (error) {
            Logging.clearLastErrorAndWarnings();
        }
    }

    void addOrIgnoreError(Set<String> messages, String message) {
        if (isIgnoredSubstring(message)) {
            ignoredErrors.add(message);
        } else {
            messages.add(message);
        }
    }
}
