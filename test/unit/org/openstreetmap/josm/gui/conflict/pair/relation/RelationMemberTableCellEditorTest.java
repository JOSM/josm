// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.pair.relation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.awt.Component;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.RelationMember;

/**
 * Unit tests of {@link RelationMemberTableCellEditor} class.
 */
public class RelationMemberTableCellEditorTest {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Unit test of {@link RelationMemberTableCellEditor#RelationMemberTableCellEditor}.
     */
    @Test
    public void testRelationMemberTableCellEditor() {
        RelationMemberTableCellEditor editor = new RelationMemberTableCellEditor();
        assertNull(editor.getTableCellEditorComponent(null, null, false, 0, 0));
        Component component = editor.getTableCellEditorComponent(null, new RelationMember("foo", new Node()), false, 0, 0);
        assertNotNull(component);
        assertEquals("foo", editor.getCellEditorValue());
    }
}
