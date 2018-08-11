// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.presets.items;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;

import javax.swing.JPanel;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.spi.preferences.Config;

/**
 * Unit tests of {@link Link} class.
 */
public class LinkTest {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUp() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Unit test for {@link Link#addToPanel}.
     */
    @Test
    public void testAddToPanel() {
        Link l = new Link();
        JPanel p = new JPanel();
        assertEquals(0, p.getComponentCount());
        assertFalse(l.addToPanel(p, Collections.<OsmPrimitive>emptyList(), false));
        assertEquals(0, p.getComponentCount());

        l.href = Config.getUrls().getJOSMWebsite();
        assertFalse(l.addToPanel(p, Collections.<OsmPrimitive>emptyList(), false));
        assertTrue(p.getComponentCount() > 0);

        l.locale_href = Config.getUrls().getJOSMWebsite();
        assertFalse(l.addToPanel(p, Collections.<OsmPrimitive>emptyList(), false));
        assertTrue(p.getComponentCount() > 0);
    }
}
