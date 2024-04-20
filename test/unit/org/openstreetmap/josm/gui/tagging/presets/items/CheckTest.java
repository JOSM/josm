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
 * Unit tests of {@link Check} class.
 */
@Main
class CheckTest implements RegionSpecificTest, TaggingPresetItemTest {
    @Override
    public Check getInstance() {
        final Check check = new Check();
        check.key = "crossing:island";
        return check;
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
