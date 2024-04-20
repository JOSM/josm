// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.presets.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.swing.JPanel;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetItemGuiSupport;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetItemTest;
import org.openstreetmap.josm.spi.preferences.Config;

/**
 * Unit tests of {@link Link} class.
 */
class LinkTest implements TaggingPresetItemTest {
    @Override
    public Link getInstance() {
        return new Link();
    }

    /**
     * Unit test for {@link Link#addToPanel}.
     */
    @Override
    @Test
    public void testAddToPanel() {
        Link l = getInstance();
        JPanel p = new JPanel();
        assertEquals(0, p.getComponentCount());
        assertFalse(l.addToPanel(p, TaggingPresetItemGuiSupport.create(false)));
        assertEquals(0, p.getComponentCount());

        l.href = Config.getUrls().getJOSMWebsite();
        assertFalse(l.addToPanel(p, TaggingPresetItemGuiSupport.create(false)));
        assertTrue(p.getComponentCount() > 0);

        l.locale_href = Config.getUrls().getJOSMWebsite();
        assertFalse(l.addToPanel(p, TaggingPresetItemGuiSupport.create(false)));
        assertTrue(p.getComponentCount() > 0);
    }
}
