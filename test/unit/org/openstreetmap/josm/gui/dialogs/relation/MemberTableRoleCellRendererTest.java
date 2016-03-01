// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation;

import static org.junit.Assert.assertEquals;

import javax.swing.JTable;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

/**
 * Unit tests of {@link MemberTableRoleCellRenderer} class.
 */
public class MemberTableRoleCellRendererTest {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Unit test of {@link MemberTableRoleCellRenderer#MemberTableRoleCellRenderer}.
     */
    @Test
    public void testMemberTableRoleCellRenderer() {
        MemberTableRoleCellRenderer r = new MemberTableRoleCellRenderer();
        assertEquals(r, r.getTableCellRendererComponent(null, null, false, false, 0, 0));
        assertEquals(r, r.getTableCellRendererComponent(
                new JTable(new MemberTableModel(null, new OsmDataLayer(new DataSet(), "", null), null)),
                "foo", false, false, 0, 0));
    }
}
