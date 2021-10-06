// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.presets.items;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.JOSMFixture;

/**
 * Unit tests of {@link PresetListEntry} class.
 */
class PresetListEntryTest {

    /**
     * Setup test.
     */
    @BeforeAll
    public static void setUp() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Non-regression test for ticket <a href="https://josm.openstreetmap.de/ticket/12416">#12416</a>.
     */
    @Test
    void testTicket12416() {
        assertTrue(new PresetListEntry("", null).getListDisplay(200).contains(" "));
    }
}
