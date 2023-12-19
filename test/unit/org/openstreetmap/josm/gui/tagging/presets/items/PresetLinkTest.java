// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.presets.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.swing.JPanel;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetItemGuiSupport;
import org.openstreetmap.josm.testutils.annotations.TaggingPresets;

/**
 * Unit tests of {@link PresetLink} class.
 */
@TaggingPresets
class PresetLinkTest {
    /**
     * Unit test for {@link PresetLink#addToPanel}.
     */
    @Test
    void testAddToPanel() {
        PresetLink l = new PresetLink();
        l.preset_name = "River";
        JPanel p = new JPanel();
        assertEquals(0, p.getComponentCount());
        assertFalse(l.addToPanel(p, TaggingPresetItemGuiSupport.create(false)));
        assertTrue(p.getComponentCount() > 0);
    }
}
