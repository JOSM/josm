// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.correction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.data.correction.TagCorrection;

/**
 * Unit tests of {@link TagCorrectionTable} class.
 */
public class TagCorrectionTableTest {

    /**
     * Setup tests
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Test of {@link TagCorrectionTable#TagCorrectionTable}.
     */
    @Test
    public void testTagCorrectionTable() {
        TagCorrectionTable t = new TagCorrectionTable(Arrays.asList(new TagCorrection("foo", "bar", "foo", "baz")));
        assertNotNull(t.getCellRenderer(0, 0));
        assertNotNull(t.getCellRenderer(0, 1));
        assertNotNull(t.getCellRenderer(0, 2));
        assertNotNull(t.getCellRenderer(0, 3));
        TagCorrectionTableModel model = t.getCorrectionTableModel();
        assertEquals(1, model.getCorrections().size());
        assertEquals(1, model.getRowCount());
        assertEquals(4, model.getApplyColumn());
        assertTrue(model.getApply(0));
        assertEquals(String.class, model.getColumnClass(0));
        assertEquals(Boolean.class, model.getColumnClass(4));
        assertEquals("Old key", model.getColumnName(0));
        assertEquals("Old value", model.getColumnName(1));
        assertEquals("New key", model.getColumnName(2));
        assertEquals("New value", model.getColumnName(3));
        assertEquals("Apply?", model.getColumnName(4));
        assertNull(model.getColumnName(5));
        assertFalse(model.isCellEditable(0, 0));
        assertTrue(model.isCellEditable(0, 4));
        assertEquals("foo", model.getValueAt(0, 0));
        assertEquals("bar", model.getValueAt(0, 1));
        assertEquals("foo", model.getValueAt(0, 2));
        assertEquals("baz", model.getValueAt(0, 3));
        assertEquals(Boolean.TRUE, model.getValueAt(0, 4));
        assertNull(model.getValueAt(0, 5));
    }
}
