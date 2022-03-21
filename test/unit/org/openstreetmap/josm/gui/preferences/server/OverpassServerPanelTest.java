// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.server;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openstreetmap.josm.gui.widgets.HistoryComboBox;
import org.openstreetmap.josm.io.OverpassDownloadReader;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

/**
 * Test class for {@link OverpassServerPanel}
 * @author Taylor Smock
 * @since 18403
 */
@BasicPreferences
class OverpassServerPanelTest {
    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/21953">#21953</a>
     */
    @ParameterizedTest
    @ValueSource(strings = {"https://something.example.com/api", "https://something.example.com/api/"})
    void testOverpassApiServerSaved(final String someRandomUrl) {
        // We expect the API to have a trailing /
        final String expected = someRandomUrl.endsWith("/") ? someRandomUrl : someRandomUrl + '/';
        final OverpassServerPanel panel = new OverpassServerPanel();
        panel.initFromPreferences();
        final HistoryComboBox historyComboBox = Stream.of(panel.getComponents()).filter(HistoryComboBox.class::isInstance)
                .map(HistoryComboBox.class::cast).findFirst().orElseGet(() -> fail("No HistoryComboBox found"));
        assertEquals(OverpassDownloadReader.OVERPASS_SERVER.get(), historyComboBox.getText());
        assertAll(OverpassDownloadReader.OVERPASS_SERVER_HISTORY.get().stream()
                .map(server -> () -> assertNotNull(historyComboBox.getModel().find(server), "Server " + server + " not found")));
        historyComboBox.setText(someRandomUrl);
        panel.saveToPreferences();
        panel.initFromPreferences();
        assertEquals(OverpassDownloadReader.OVERPASS_SERVER.get(), historyComboBox.getText());
        assertAll(OverpassDownloadReader.OVERPASS_SERVER_HISTORY.get().stream()
                .map(server -> () -> assertNotNull(historyComboBox.getModel().find(server), "Server " + server + " not found")));
        assertEquals(expected, OverpassDownloadReader.OVERPASS_SERVER.get());
        assertTrue(OverpassDownloadReader.OVERPASS_SERVER_HISTORY.get().contains(expected),
                String.join(";", OverpassDownloadReader.OVERPASS_SERVER_HISTORY.get()));
    }
}
