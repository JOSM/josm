// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

/**
 * Unit tests of {@link ShowStatusReportAction}.
 */
@BasicPreferences
class ShowStatusReportActionTest {
    /**
     * The API keys and tokens in the URLs of preference values, e.g. of custom imagery, are stripped from the status
     * report; other values are kept.
     */
    @Test
    void testValueCleanupStripsUrls() {
        assertEquals("https://tiles.example.org/{zoom}/{x}/{y}.png?apikey=...stripped...",
                ShowStatusReportAction.valueCleanup("https://tiles.example.org/{zoom}/{x}/{y}.png?apikey=a-secret-key"));

        final Map<String, String> imagery = new LinkedHashMap<>();
        imagery.put("name", "Custom");
        imagery.put("url", "https://tiles.example.org/{zoom}/{x}/{y}.png?access_token=a-secret-token");
        assertEquals("[{name=Custom, url=https://tiles.example.org/{zoom}/{x}/{y}.png?access_token=...stripped...}]",
                ShowStatusReportAction.valueCleanup(Collections.singletonList(imagery)));

        assertEquals("[[https://example.org/?key=...stripped..., other]]", ShowStatusReportAction.valueCleanup(
                Collections.singletonList(Arrays.asList("https://example.org/?key=a-secret-key", "other"))));
        assertEquals("[a, b]", ShowStatusReportAction.valueCleanup(Arrays.asList("a", "b")));
    }
}
