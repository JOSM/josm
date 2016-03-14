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
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresets;

/**
 * Unit tests of {@link PresetLink} class.
 */
public class PresetLinkTest {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUp() {
        JOSMFixture.createUnitTestFixture().init();
        TaggingPresets.readFromPreferences();
    }

    /**
     * Unit test for {@link PresetLink#addToPanel}.
     */
    @Test
    public void testAddToPanel() {
        PresetLink l = new PresetLink();
        l.preset_name = "River";
        JPanel p = new JPanel();
        assertEquals(0, p.getComponentCount());
        assertFalse(l.addToPanel(p, Collections.<OsmPrimitive>emptyList(), false));
        assertTrue(p.getComponentCount() > 0);
    }
}
