// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.presets.items;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;

import javax.swing.JPanel;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link Combo} class.
 */
public class ComboTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().main();

    /**
     * Unit test for {@link Combo#addToPanel}.
     */
    @Test
    public void testAddToPanel() {
        JPanel p = new JPanel();
        assertEquals(0, p.getComponentCount());
        assertTrue(new Combo().addToPanel(p, Collections.<OsmPrimitive>emptyList(), false));
        assertTrue(p.getComponentCount() > 0);
    }
}
