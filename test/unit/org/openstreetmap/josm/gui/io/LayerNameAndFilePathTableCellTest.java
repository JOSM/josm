// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import javax.swing.JTable;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.layer.AbstractModifiableLayer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

/**
 * Unit tests of {@link LayerNameAndFilePathTableCell} class.
 */
public class LayerNameAndFilePathTableCellTest {
    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init(false);
    }

    /**
     * Test of {@link LayerNameAndFilePathTableCell} class.
     */
    @Test
    public void testLayerNameAndFilePathTableCell() {
        JTable table = new JTable();
        File file = new File("test");
        String name = "layername";
        AbstractModifiableLayer layer = new OsmDataLayer(new DataSet(), name, file);
        SaveLayerInfo value = new SaveLayerInfo(layer);
        LayerNameAndFilePathTableCell c = new LayerNameAndFilePathTableCell();
        assertEquals(c, c.getTableCellEditorComponent(table, value, false, 0, 0));
        assertEquals(c, c.getTableCellRendererComponent(table, value, false, false, 0, 0));
        assertTrue(c.isCellEditable(null));
        assertTrue(c.shouldSelectCell(null));
        assertNull(c.getCellEditorValue());
        assertTrue(c.stopCellEditing());
        assertEquals(file, c.getCellEditorValue());
    }
}
