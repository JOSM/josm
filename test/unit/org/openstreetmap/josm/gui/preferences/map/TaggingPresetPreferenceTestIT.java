// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.preferences.sources.ExtendedSourceEntry;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPreset;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetReader;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.testutils.ParallelParameterized;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Logging;
import org.xml.sax.SAXException;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Integration tests of {@link TaggingPresetPreference} class.
 */
@RunWith(ParallelParameterized.class)
public class TaggingPresetPreferenceTestIT {

    /**
     * Setup rule
     */
    @ClassRule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public static JOSMTestRules test = new JOSMTestRules().https().timeout(10000*60).parameters();

    /** Entry to test */
    private final ExtendedSourceEntry source;
    private final List<String> ignoredErrors = new ArrayList<>();
    private static final List<String> errorsToIgnore = new ArrayList<>();

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
        return new TaggingPresetPreference.TaggingPresetSourceEditor().loadAndGetAvailableSources().stream()
                .map(x -> new Object[] {x.getDisplayName(), x.url, x}).collect(Collectors.toList());
    }

    /**
     * Constructs a new {@code TaggingPresetPreferenceTestIT}
     * @param displayName displayed name
     * @param url URL
     * @param source source entry to test
     */
    public TaggingPresetPreferenceTestIT(String displayName, String url, ExtendedSourceEntry source) {
        this.source = source;
    }

    /**
     * Test that tagging presets are valid.
     * @throws Exception in case of error
     */
    @Test
    public void testPresetsValidity() throws Exception {
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

    private void handleException(Exception e, Set<String> errors) {
        e.printStackTrace();
        String s = source.url + " => " + e.toString();
        if (isIgnoredSubstring(s)) {
            ignoredErrors.add(s);
        } else {
            errors.add(s);
        }
    }

    private void testPresets(Set<String> messages, ExtendedSourceEntry source) throws SAXException, IOException {
        Collection<TaggingPreset> presets = TaggingPresetReader.readAll(source.url, true);
        assertFalse(presets.isEmpty());
        // wait for asynchronous icon loading
        presets.stream().map(TaggingPreset::getIconLoadingTask).filter(Objects::nonNull).forEach(t -> {
            try {
                t.get(30, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                Logging.error(e);
            }
        });
        Collection<String> errorsAndWarnings = Logging.getLastErrorAndWarnings();
        boolean error = false;
        for (String message : errorsAndWarnings) {
            if (message.contains(TaggingPreset.PRESET_ICON_ERROR_MSG_PREFIX)) {
                error = true;
                if (isIgnoredSubstring(message)) {
                    ignoredErrors.add(message);
                } else {
                    messages.add(message);
                }
            }
        }
        if (error) {
            Logging.clearLastErrorAndWarnings();
        }
    }

    private static boolean isIgnoredSubstring(String substring) {
        return errorsToIgnore.parallelStream().anyMatch(x -> substring.contains(x));
    }
}
