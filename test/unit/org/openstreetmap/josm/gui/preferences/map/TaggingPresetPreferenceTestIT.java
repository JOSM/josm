// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.preferences.sources.ExtendedSourceEntry;
import org.openstreetmap.josm.gui.preferences.AbstractExtendedSourceEntryTestCase;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPreset;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetItem;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetReader;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetsTest;
import org.openstreetmap.josm.gui.tagging.presets.items.Check;
import org.openstreetmap.josm.gui.tagging.presets.items.CheckGroup;
import org.openstreetmap.josm.gui.tagging.presets.items.Key;
import org.openstreetmap.josm.gui.tagging.presets.items.KeyedItem;
import org.openstreetmap.josm.gui.tagging.presets.items.Link;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.testutils.annotations.HTTPS;
import org.openstreetmap.josm.testutils.annotations.IntegrationTest;
import org.openstreetmap.josm.testutils.annotations.Territories;
import org.openstreetmap.josm.tools.HttpClient;
import org.openstreetmap.josm.tools.HttpClient.Response;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Utils;
import org.xml.sax.SAXException;

/**
 * Integration tests of {@link TaggingPresetPreference} class.
 */
@HTTPS
@IntegrationTest
@Territories
@Timeout(value = 20, unit = TimeUnit.MINUTES)
class TaggingPresetPreferenceTestIT extends AbstractExtendedSourceEntryTestCase {
    /**
     * Setup test
     * @throws IOException in case of I/O error
     */
    @BeforeAll
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
    public static List<Object[]> data() throws Exception {
        ImageProvider.clearCache();
        return getTestParameters(new TaggingPresetPreference.TaggingPresetSourceEditor().loadAndGetAvailableSources());
    }

    /**
     * Test that tagging presets are valid.
     * @param displayName displayed name
     * @param url URL
     * @param source source entry to test
     */
    @ParameterizedTest(name = "{0} - {1}")
    @MethodSource("data")
    void testPresetsValidity(String displayName, String url, ExtendedSourceEntry source) {
        assumeFalse(isIgnoredSubstring(source, source.url));
        List<String> ignoredErrors = new ArrayList<>();
        Set<String> errors = new HashSet<>();
        try {
            testPresets(errors, source, ignoredErrors);
        } catch (IOException e) {
            try {
                Logging.warn(e);
                // try again in case of temporary network error
                testPresets(errors, source, ignoredErrors);
            } catch (SAXException | IOException e1) {
                handleException(source, e1, errors, ignoredErrors);
            }
        } catch (SAXException | IllegalArgumentException e) {
            handleException(source, e, errors, ignoredErrors);
        }
        // #16567 - Shouldn't be necessary to print displayName if Ant worked properly
        // See https://josm.openstreetmap.de/ticket/16567#comment:53
        // See https://bz.apache.org/bugzilla/show_bug.cgi?id=64564
        // See https://github.com/apache/ant/pull/121
        assertTrue(errors.isEmpty(), displayName + " => " + errors);
        assumeTrue(ignoredErrors.isEmpty(), ignoredErrors.toString());
    }

    private void testPresets(Set<String> messages, ExtendedSourceEntry source, List<String> ignoredErrors)
            throws SAXException, IOException {
        Collection<TaggingPreset> presets = TaggingPresetReader.readAll(source.url, true);
        assertFalse(presets.isEmpty());
        TaggingPresetsTest.waitForIconLoading(presets);
        // check that links are correct and not redirections
        presets.parallelStream().flatMap(x -> x.data.stream().filter(Link.class::isInstance).map(i -> ((Link) i).getUrl())).forEach(u -> {
            try {
                Response cr = HttpClient.create(new URL(u), "HEAD").setMaxRedirects(-1).connect();
                final int code = cr.getResponseCode();
                if (HttpClient.isRedirect(code)) {
                    addOrIgnoreError(source, messages,
                            "Found HTTP redirection for " + u + " -> " + code + " -> " + cr.getHeaderField("Location"), ignoredErrors);
                } else if (code >= 400) {
                    addOrIgnoreError(source, messages, "Found HTTP error for " + u + " -> " + code, ignoredErrors);
                }
            } catch (IOException e) {
                Logging.error(e);
            }
        });
        presets.parallelStream().flatMap(TaggingPresetPreferenceTestIT::checkForDuplicates)
                .filter(Objects::nonNull)
                .forEach(message -> addOrIgnoreError(source, messages, message, ignoredErrors));
        Collection<String> errorsAndWarnings = Logging.getLastErrorAndWarnings();
        boolean error = false;
        for (String message : errorsAndWarnings) {
            if (message.contains(TaggingPreset.PRESET_ICON_ERROR_MSG_PREFIX)) {
                error = true;
                addOrIgnoreError(source, messages, message, ignoredErrors);
            }
        }
        if (error) {
            Logging.clearLastErrorAndWarnings();
        }
    }

    void addOrIgnoreError(ExtendedSourceEntry source, Set<String> messages, String message, List<String> ignoredErrors) {
        if (isIgnoredSubstring(source, message)) {
            ignoredErrors.add(message);
        } else {
            messages.add(message);
        }
    }

    /**
     * Look for duplicate key/value objects
     * @param preset to check
     * @return The messages to print to console for fixing
     */
    private static Stream<String> checkForDuplicates(TaggingPreset preset) {
        final HashMap<String, List<KeyedItem>> dupMap = preset.data.stream()
                .flatMap(TaggingPresetPreferenceTestIT::getKeyedItems)
                .collect(Collectors.groupingBy(i -> i.key, HashMap::new, Collectors.toCollection(ArrayList::new)));
        dupMap.values().forEach(TaggingPresetPreferenceTestIT::removeUnnecessaryDuplicates);
        dupMap.values().removeIf(l -> l.size() <= 1);
        if (!dupMap.isEmpty()) {
            final StringBuilder prefixBuilder = new StringBuilder();
            if (preset.group != null && preset.group.name != null) {
                prefixBuilder.append(preset.group.name).append('/');
            }
            if (preset.name != null) {
                prefixBuilder.append(preset.name).append('/');
            }
            final String prefix = prefixBuilder.toString();
            return dupMap.keySet().stream().map(k -> "Duplicate key: " + prefix + k);
        }
        return Stream.empty();
    }

    /**
     * Remove keys that are technically duplicates, but are otherwise OK due to working around limitations of the XML.
     * @param l The list of keyed items to look through
     */
    private static void removeUnnecessaryDuplicates(List<KeyedItem> l) {
        // Remove keys that are "truthy" when a check will be on or off. This seems to be used for setting defaults in chunks.
        // We might want to extend chunks to have child `<key>` elements which will set default values for the chunk.
        ArrayList<KeyedItem> toRemove = new ArrayList<>(Math.min(4, l.size() / 10));
        for (Key first : Utils.filteredCollection(l, Key.class)) {
            for (Check second : Utils.filteredCollection(l, Check.class)) {
                if (second.value_off.equals(first.value) || second.value_on.equals(first.value)) {
                    toRemove.add(first);
                }
            }
        }
        l.removeAll(toRemove);
    }

    /**
     * Convert an item to a collection of items (needed for {@link CheckGroup})
     * @param item The item to convert
     * @return The {@link KeyedItem}s to use
     */
    private static Stream<? extends KeyedItem> getKeyedItems(TaggingPresetItem item) {
        // We care about cases where a preset has two separate hardcoded values
        // Check should use default="on|off" and value_(on|off) to control the default.
        if (item instanceof Key || item instanceof Check) {
            return Stream.of((KeyedItem) item);
        } else if (item instanceof CheckGroup) {
            return ((CheckGroup) item).checks.stream();
        }
        return Stream.empty();
    }
}
