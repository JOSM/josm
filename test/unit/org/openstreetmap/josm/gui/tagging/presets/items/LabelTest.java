// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.presets.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.swing.JPanel;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetItemGuiSupport;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

/**
 * Unit tests of {@link Label} class.
 */
@BasicPreferences
class LabelTest {
    /**
     * Unit test for {@link Label#addToPanel}.
     */
    @Test
    void testAddToPanel() {
        JPanel p = new JPanel();
        assertEquals(0, p.getComponentCount());
        assertTrue(new Label().addToPanel(p, TaggingPresetItemGuiSupport.create(false)));
        assertTrue(p.getComponentCount() > 0);
    }
}
