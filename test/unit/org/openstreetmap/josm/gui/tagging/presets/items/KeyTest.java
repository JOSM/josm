// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.presets.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import javax.swing.JPanel;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetItemGuiSupport;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetItemTest;

/**
 * Unit tests of {@link Key} class.
 */
class KeyTest implements RegionSpecificTest, TaggingPresetItemTest {
    @Override
    public Key getInstance() {
        final Key key = new Key();
        key.key = "highway";
        key.value = "residential";
        return key;
    }

    /**
     * Unit test for {@link Key#addToPanel}.
     */
    @Test
    @Override
    public void testAddToPanel() {
        JPanel p = new JPanel();
        assertEquals(0, p.getComponentCount());
        assertFalse(new Key().addToPanel(p, TaggingPresetItemGuiSupport.create(false)));
        assertEquals(0, p.getComponentCount());
    }
}
