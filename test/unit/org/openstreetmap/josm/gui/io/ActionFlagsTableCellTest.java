// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import javax.swing.JTable;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.layer.AbstractModifiableLayer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link ActionFlagsTableCell} class.
 */
class ActionFlagsTableCellTest {
    /**
     * Setup test.
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    /**
     * Test of {@link ActionFlagsTableCell} class.
     */
    @Test
    void testActionFlagsTableCell() {
        JTable table = new JTable();
        File file = new File("test");
        String name = "layername";
        AbstractModifiableLayer layer = new OsmDataLayer(new DataSet(), name, file);
        SaveLayerInfo value = new SaveLayerInfo(layer);
        ActionFlagsTableCell c = new ActionFlagsTableCell();
        assertEquals(c, c.getTableCellEditorComponent(table, value, false, 0, 0));
        assertEquals(c, c.getTableCellRendererComponent(table, value, false, false, 0, 0));
        assertTrue(c.isCellEditable(null));
        assertTrue(c.shouldSelectCell(null));
        assertNotNull(c.getCellEditorValue());
        assertTrue(c.stopCellEditing());
    }
}
