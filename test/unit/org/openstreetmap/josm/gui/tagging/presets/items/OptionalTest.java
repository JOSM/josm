// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.presets.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.swing.JPanel;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetItemGuiSupport;

/**
 * Unit tests of {@link Optional} class.
 */
class OptionalTest {

    /**
     * Setup test.
     */
    @BeforeAll
    public static void setUp() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Unit test for {@link Optional#addToPanel}.
     */
    @Test
    void testAddToPanel() {
        JPanel p = new JPanel();
        assertEquals(0, p.getComponentCount());
        assertFalse(new Optional().addToPanel(p, TaggingPresetItemGuiSupport.create(false)));
        assertTrue(p.getComponentCount() > 0);
    }
}
