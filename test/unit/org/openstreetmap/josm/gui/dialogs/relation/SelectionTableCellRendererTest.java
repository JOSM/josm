// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation;

import static org.junit.Assert.assertEquals;

import javax.swing.JTable;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.data.osm.Node;

/**
 * Unit tests of {@link SelectionTableCellRenderer} class.
 */
public class SelectionTableCellRendererTest {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Unit test of {@link SelectionTableCellRenderer#SelectionTableCellRenderer}.
     */
    @Test
    public void testSelectionTableCellRenderer() {
        MemberTableModel model = new MemberTableModel(null, null, null);
        SelectionTableCellRenderer r = new SelectionTableCellRenderer(model);
        assertEquals(r, r.getTableCellRendererComponent(null, null, false, false, 0, 0));
        assertEquals(r, r.getTableCellRendererComponent(new JTable(model), new Node(), false, false, 0, 0));
    }
}
