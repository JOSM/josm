// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.correction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.correction.RoleCorrection;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;

/**
 * Unit tests of {@link RoleCorrectionTable} class.
 */
class RoleCorrectionTableTest {
    /**
     * Test of {@link RoleCorrectionTable#RoleCorrectionTable}.
     */
    @Test
    void testRoleCorrectionTable() {
        Relation r = new Relation();
        RelationMember member = new RelationMember("foo", new Node());
        r.addMember(member);
        RoleCorrection rc = new RoleCorrection(r, 0, member, "bar");
        RoleCorrectionTable t = new RoleCorrectionTable(Collections.singletonList(rc));
        assertNotNull(t.getCellRenderer(0, 0));
        assertNotNull(t.getCellRenderer(0, 1));
        assertNotNull(t.getCellRenderer(0, 2));
        assertNotNull(t.getCellRenderer(0, 3));
        RoleCorrectionTableModel model = t.getCorrectionTableModel();
        assertEquals(1, model.getCorrections().size());
        assertEquals(1, model.getRowCount());
        assertEquals(3, model.getApplyColumn());
        assertTrue(model.getApply(0));
        assertEquals(String.class, model.getColumnClass(0));
        assertEquals(Boolean.class, model.getColumnClass(3));
        assertEquals("Relation", model.getColumnName(0));
        assertEquals("Old role", model.getColumnName(1));
        assertEquals("New role", model.getColumnName(2));
        assertEquals("Apply?", model.getColumnName(3));
        assertNull(model.getColumnName(4));
        assertFalse(model.isCellEditable(0, 0));
        assertTrue(model.isCellEditable(0, 3));
        assertEquals("relation (0, 1 member)", model.getValueAt(0, 0));
        assertEquals("foo", model.getValueAt(0, 1));
        assertEquals("bar", model.getValueAt(0, 2));
        assertTrue((Boolean) model.getValueAt(0, 3));
        assertNull(model.getValueAt(0, 4));
        model.setValueAt("", 0, 0);
        assertEquals("relation (0, 1 member)", model.getValueAt(0, 0));
        model.setValueAt("", 0, 3);
        assertTrue((Boolean) model.getValueAt(0, 3));
        model.setValueAt(Boolean.FALSE, 0, 3);
        assertFalse((Boolean) model.getValueAt(0, 3));
        RoleCorrection[] array = new RoleCorrection[15];
        Arrays.fill(array, rc);
        t = new RoleCorrectionTable(Arrays.asList(array));
        assertEquals(array.length, t.getCorrectionTableModel().getCorrections().size());
    }
}
