// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.presets.items;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Unit tests of {@link PresetListEntry} class.
 */
class PresetListEntryTest {
    /**
     * Non-regression test for ticket <a href="https://josm.openstreetmap.de/ticket/12416">#12416</a>.
     */
    @Test
    void testTicket12416() {
        assertTrue(new PresetListEntry("", null).getListDisplay(200).contains(" "));
    }

    /**
     * Non-regression test for ticket <a href="https://josm.openstreetmap.de/ticket/21550">#21550</a>
     */
    @Test
    void testTicket21550() {
        final PresetListEntry entry = new PresetListEntry("", new Combo());
        assertDoesNotThrow(entry::getCount);
    }
}
