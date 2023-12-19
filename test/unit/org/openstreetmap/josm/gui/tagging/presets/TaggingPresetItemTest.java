// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.presets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import javax.swing.JPanel;

import org.junit.jupiter.api.Test;

/**
 * Test class for {@link TaggingPresetItem}
 */
public interface TaggingPresetItemTest {
    /**
     * Get the instance to test
     *
     * @return The item to test
     */
    TaggingPresetItem getInstance();

    /**
     * Test method for {@link TaggingPresetItem#addToPanel(JPanel, TaggingPresetItemGuiSupport)}
     */
    @Test
    default void testAddToPanel() {
        TaggingPresetItem item = getInstance();
        JPanel p = new JPanel();
        assertEquals(0, p.getComponentCount());
        assertFalse(item.addToPanel(p, TaggingPresetItemGuiSupport.create(false)));
        assertNotEquals(0, p.getComponentCount());
    }
}
