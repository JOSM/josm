// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation.actions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Set;

import org.junit.Test;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.gui.datatransfer.ClipboardUtils;
import org.openstreetmap.josm.gui.datatransfer.PrimitiveTransferable;
import org.openstreetmap.josm.gui.datatransfer.RelationMemberTransferable;
import org.openstreetmap.josm.gui.datatransfer.data.PrimitiveTransferData;
import org.openstreetmap.josm.gui.util.GuiHelper;

/**
 * Test for {@link PasteMembersAction}
 * @author Michael Zangl
 */
public class PasteMembersActionTest extends AbstractRelationEditorActionTest {
    /**
     * Test {@link PasteMembersAction#isEnabled()}
     */
    @Test
    public void testEnabledState() {
        copyString();

        PasteMembersAction action = new PasteMembersAction(relationEditorAccess);
        ClipboardUtils.getClipboard().addFlavorListener(action);

        try {
            assertFalse(action.isEnabled());

            Node node = new Node();
            copyNode(node);
            syncListener();
            assertTrue(action.isEnabled());

            copyMember(node);
            syncListener();
            assertTrue(action.isEnabled());

            copyString();
            syncListener();
            assertFalse(action.isEnabled());
        } finally {
            ClipboardUtils.getClipboard().removeFlavorListener(action);
        }
    }

    private void syncListener() {
        GuiHelper.runInEDTAndWait(() -> {
            // nop
        });
    }

    /**
     * Test that pasting produces the result required
     */
    @Test
    public void testActionWrongClipboard() {
        copyString();
        PasteMembersAction action = new PasteMembersAction(relationEditorAccess);
        action.actionPerformed(null);

        Relation relation = new Relation(1);
        relationEditorAccess.getMemberTableModel().applyToRelation(relation);
        assertEquals(0, relation.getMembersCount());
    }

    /**
     * Test that pasting produces the result required
     */
    @Test
    public void testActionForMembers() {
        Node testNode = new Node(10);
        layer.data.addPrimitive(testNode);
        copyMember(testNode);
        PasteMembersAction action = new PasteMembersAction(relationEditorAccess);
        action.actionPerformed(null);

        Relation relation = new Relation(1);
        relationEditorAccess.getMemberTableModel().applyToRelation(relation);
        assertEquals(1, relation.getMembersCount());
        assertEquals("test", relation.getMember(0).getRole());
        assertSame(testNode, relation.getMember(0).getMember());
    }

    /**
     * Test that pasting primitvies produces the result required
     */
    @Test
    public void testActionForPrimitives() {
        Node testNode = new Node(10);
        layer.data.addPrimitive(testNode);
        copyNode(testNode);
        PasteMembersAction action = new PasteMembersAction(relationEditorAccess);
        action.actionPerformed(null);

        Relation relation = new Relation(1);
        relationEditorAccess.getMemberTableModel().applyToRelation(relation);
        assertEquals(1, relation.getMembersCount());
        assertEquals("", relation.getMember(0).getRole());
        assertSame(testNode, relation.getMember(0).getMember());
    }

    private void copyNode(Node node) {
        PrimitiveTransferData data = PrimitiveTransferData.getData(Collections.singleton(node));
        ClipboardUtils.copy(new PrimitiveTransferable(data));
    }

    private void copyMember(Node node) {
        Set<RelationMember> members = Collections.singleton(new RelationMember("test", node));
        ClipboardUtils.copy(new RelationMemberTransferable(members));
    }

    private void copyString() {
        ClipboardUtils.copyString("");
    }
}
