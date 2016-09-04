// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import static org.junit.Assert.assertEquals;

import javax.swing.JList;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link ChangesetCellRenderer} class.
 */
public class ChangesetCellRendererTest {
    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences();

    /**
     * Test of {@link ChangesetCellRenderer} class.
     */
    @Test
    public void testChangesetCellRenderer() {
        JList<Changeset> list = new JList<>();
        Changeset cs = new Changeset();
        ChangesetCellRenderer c = new ChangesetCellRenderer();
        assertEquals(c, c.getListCellRendererComponent(list, cs, 0, false, false));
    }
}
