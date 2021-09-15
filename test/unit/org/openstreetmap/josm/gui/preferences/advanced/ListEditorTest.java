// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.advanced;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.gui.preferences.advanced.ListEditor.ListSettingTableModel;

/**
 * Unit tests of {@link ListEditor} class.
 */
class ListEditorTest {
    /**
     * Unit test of {@link ListSettingTableModel} class.
     */
    @Test
    void testListSettingTableModel() {
        ListSettingTableModel model = new ListSettingTableModel(null);
        assertNotNull(model.getData());
        model = new ListSettingTableModel(Arrays.asList("foo"));
        assertTrue(model.getData().contains("foo"));
        assertEquals(2, model.getRowCount());
        assertEquals(1, model.getColumnCount());
        assertEquals("foo", model.getValueAt(0, 0));
        assertEquals("", model.getValueAt(1, 0));
        assertTrue(model.isCellEditable(0, 0));
        model.setValueAt("bar", 0, 0);
        assertEquals("bar", model.getValueAt(0, 0));
        model.setValueAt("test", 1, 0);
        assertEquals("test", model.getValueAt(1, 0));
        assertEquals(3, model.getRowCount());
    }
}
