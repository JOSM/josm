// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.presets.items;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Collections;

import javax.swing.JPanel;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.data.osm.OsmPrimitive;

/**
 * Unit tests of {@link Key} class.
 */
public class KeyTest {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUp() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Unit test for {@link Key#addToPanel}.
     */
    @Test
    public void testAddToPanel() {
        JPanel p = new JPanel();
        assertEquals(0, p.getComponentCount());
        assertFalse(new Key().addToPanel(p, Collections.<OsmPrimitive>emptyList(), false));
        assertEquals(0, p.getComponentCount());
    }
}
