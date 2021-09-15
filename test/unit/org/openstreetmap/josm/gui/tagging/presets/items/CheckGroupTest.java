// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.presets.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.swing.JPanel;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetItemGuiSupport;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

/**
 * Unit tests of {@link CheckGroup} class.
 */
@BasicPreferences
class CheckGroupTest {
    /**
     * Unit test for {@link CheckGroup#addToPanel}.
     */
    @Test
    void testAddToPanel() {
        CheckGroup cg = new CheckGroup();
        JPanel p = new JPanel();
        assertEquals(0, p.getComponentCount());
        assertFalse(cg.addToPanel(p, TaggingPresetItemGuiSupport.create(false)));
        assertTrue(p.getComponentCount() > 0);
    }
}
