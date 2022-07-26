// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.correction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.correction.TagCorrection;

/**
 * Unit tests of {@link TagCorrectionTable} class.
 */
class TagCorrectionTableTest {
    /**
     * Test of {@link TagCorrectionTable#TagCorrectionTable}.
     */
    @Test
    void testTagCorrectionTable() {
        TagCorrection tc1 = new TagCorrection("foo", "bar", "foo", "baz");
        TagCorrection tc2 = new TagCorrection("bar", "foo", "baz", "foo");
        TagCorrectionTable t = new TagCorrectionTable(Arrays.asList(tc1, tc2));
        assertNotNull(t.getCellRenderer(0, 0));
        assertNotNull(t.getCellRenderer(0, 1));
        assertNotNull(t.getCellRenderer(0, 2));
        assertNotNull(t.getCellRenderer(0, 3));
        assertNotNull(t.getCellRenderer(0, 4));
        assertNotNull(t.getCellRenderer(1, 0));
        assertNotNull(t.getCellRenderer(1, 1));
        assertNotNull(t.getCellRenderer(1, 2));
        assertNotNull(t.getCellRenderer(1, 3));
        assertNotNull(t.getCellRenderer(1, 4));
        TagCorrectionTableModel model = t.getCorrectionTableModel();
        assertEquals(2, model.getCorrections().size());
        assertEquals(2, model.getRowCount());
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
        assertFalse(model.isCellEditable(1, 0));
        assertTrue(model.isCellEditable(0, 4));
        assertTrue(model.isCellEditable(1, 4));
        assertEquals("foo", model.getValueAt(0, 0));
        assertEquals("bar", model.getValueAt(0, 1));
        assertEquals("foo", model.getValueAt(0, 2));
        assertEquals("baz", model.getValueAt(0, 3));
        assertTrue((Boolean) model.getValueAt(0, 4));
        assertNull(model.getValueAt(0, 5));
        assertEquals("bar", model.getValueAt(1, 0));
        assertEquals("foo", model.getValueAt(1, 1));
        assertEquals("baz", model.getValueAt(1, 2));
        assertEquals("foo", model.getValueAt(1, 3));
        assertTrue((Boolean) model.getValueAt(1, 4));
        assertNull(model.getValueAt(1, 5));
        model.setValueAt("", 0, 0);
        assertEquals("foo", model.getValueAt(0, 0));
        model.setValueAt("", 0, 4);
        assertTrue((Boolean) model.getValueAt(0, 4));
        model.setValueAt(Boolean.FALSE, 0, 4);
        assertFalse((Boolean) model.getValueAt(0, 4));
        TagCorrection[] array = new TagCorrection[15];
        Arrays.fill(array, tc1);
        t = new TagCorrectionTable(Arrays.asList(array));
        assertEquals(array.length, t.getCorrectionTableModel().getCorrections().size());
    }
}
