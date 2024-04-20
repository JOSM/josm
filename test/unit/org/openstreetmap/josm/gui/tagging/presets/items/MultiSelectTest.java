// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.presets.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.swing.JPanel;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetItemGuiSupport;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetItemTest;
import org.openstreetmap.josm.testutils.annotations.Main;

/**
 * Unit tests of {@link MultiSelect} class.
 */
@Main
class MultiSelectTest implements TaggingPresetItemTest {
    @Override
    public MultiSelect getInstance() {
        return new MultiSelect();
    }

    /**
     * Unit test for {@link Check#addToPanel}.
     */
    @Override
    @Test
    public void testAddToPanel() {
        JPanel p = new JPanel();
        assertEquals(0, p.getComponentCount());
        assertTrue(getInstance().addToPanel(p, TaggingPresetItemGuiSupport.create(false)));
        assertTrue(p.getComponentCount() > 0);
    }
}
