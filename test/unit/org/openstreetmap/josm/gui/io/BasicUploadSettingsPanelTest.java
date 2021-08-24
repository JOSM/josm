// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Arrays;
import java.util.List;

import org.openstreetmap.josm.spi.preferences.Config;

import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

import org.junit.jupiter.api.Test;

/**
 * Unit tests of {@link BasicUploadSettingsPanel} class.
 */
@BasicPreferences
class BasicUploadSettingsPanelTest {
    /**
     * Test of {@link BasicUploadSettingsPanel#BasicUploadSettingsPanel}.
     */
    @Test
    void testBasicUploadSettingsPanel() {
        assertNotNull(new BasicUploadSettingsPanel(new UploadDialogModel()));
    }

    private static void doTestGetLastChangesetTagFromHistory(String historyKey, List<String> def) {
        Config.getPref().putList(historyKey, null);
        Config.getPref().putInt(BasicUploadSettingsPanel.COMMENT_LAST_USED_KEY, 0);
        Config.getPref().putInt(BasicUploadSettingsPanel.COMMENT_MAX_AGE_KEY, 30);
        assertNull(BasicUploadSettingsPanel.getLastChangesetTagFromHistory(historyKey, def));          // age NOK (history empty)

        Config.getPref().putList(historyKey, Arrays.asList("foo", "bar"));
        assertNull(BasicUploadSettingsPanel.getLastChangesetTagFromHistory(historyKey, def));          // age NOK (history not empty)

        Config.getPref().putLong(BasicUploadSettingsPanel.COMMENT_LAST_USED_KEY, System.currentTimeMillis() / 1000);
        assertEquals("foo", BasicUploadSettingsPanel.getLastChangesetTagFromHistory(historyKey, def)); // age OK, history not empty

        Config.getPref().putList(historyKey, null);
        assertEquals(def.get(0), BasicUploadSettingsPanel.getLastChangesetTagFromHistory(historyKey, def));   // age OK, history empty
    }

    /**
     * Test of {@link BasicUploadSettingsPanel#getLastChangesetTagFromHistory} method.
     */
    @Test
    void testGetLastChangesetCommentFromHistory() {
        doTestGetLastChangesetTagFromHistory(
                BasicUploadSettingsPanel.COMMENT_HISTORY_KEY,
                Arrays.asList("baz", "quux"));
    }

    /**
     * Test of {@link BasicUploadSettingsPanel#getLastChangesetTagFromHistory} method.
     */
    @Test
    void testGetLastChangesetSourceFromHistory() {
        doTestGetLastChangesetTagFromHistory(
                BasicUploadSettingsPanel.SOURCE_HISTORY_KEY,
                BasicUploadSettingsPanel.getDefaultSources());
    }
}
