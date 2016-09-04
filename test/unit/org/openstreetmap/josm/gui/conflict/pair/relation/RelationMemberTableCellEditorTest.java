// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.pair.relation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.awt.Component;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link RelationMemberTableCellEditor} class.
 */
public class RelationMemberTableCellEditorTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences();

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
