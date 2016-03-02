// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import static org.junit.Assert.assertEquals;

import javax.swing.JList;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.data.osm.Changeset;

/**
 * Unit tests of {@link ChangesetCellRenderer} class.
 */
public class ChangesetCellRendererTest {
    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init(false);
    }

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
