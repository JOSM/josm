// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation.actions;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;

import javax.swing.Icon;
import javax.swing.JOptionPane;
import javax.swing.text.JTextComponent;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.gui.ConditionalOptionPaneUtil;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.testutils.annotations.DeleteCommandCallback;
import org.openstreetmap.josm.testutils.annotations.I18n;
import org.openstreetmap.josm.testutils.mockers.JOptionPaneSimpleMocker;

import mockit.Mock;
import mockit.MockUp;

/**
 * Unit tests for relation editor actions.
 */
@I18n
@DeleteCommandCallback
class RelationEditorActionsTest extends AbstractRelationEditorActionTest {

    /**
     * Check that all dialog-less actions do not crash.
     */
    @Test
    void testNoDialogActions() {
        new AddSelectedAfterSelection(relationEditorAccess).actionPerformed(null);
        new AddSelectedBeforeSelection(relationEditorAccess).actionPerformed(null);
        new AddSelectedAtStartAction(relationEditorAccess).actionPerformed(null);
        new AddSelectedAtEndAction(relationEditorAccess).actionPerformed(null);

        new ApplyAction(relationEditorAccess).actionPerformed(null);
        new RefreshAction(relationEditorAccess).actionPerformed(null);
        new OKAction(relationEditorAccess).actionPerformed(null);
        new CancelAction(relationEditorAccess).actionPerformed(null);

        new CopyMembersAction(relationEditorAccess).actionPerformed(null);
        new PasteMembersAction(relationEditorAccess).actionPerformed(null);

        new SelectAction(relationEditorAccess).actionPerformed(null);

        new DownloadIncompleteMembersAction(relationEditorAccess, "downloadincomplete").actionPerformed(null);
        new DownloadSelectedIncompleteMembersAction(relationEditorAccess).actionPerformed(null);

        new DuplicateRelationAction(relationEditorAccess).actionPerformed(null);
        new EditAction(relationEditorAccess).actionPerformed(null);

        new MoveDownAction(relationEditorAccess, "movedown").actionPerformed(null);
        new MoveUpAction(relationEditorAccess, "moveup").actionPerformed(null);
        new RemoveAction(relationEditorAccess, "remove").actionPerformed(null);

        new RemoveSelectedAction(relationEditorAccess).actionPerformed(null);
        new SelectedMembersForSelectionAction(relationEditorAccess).actionPerformed(null);

        new SelectPrimitivesForSelectedMembersAction(relationEditorAccess).actionPerformed(null);

        new SortAction(relationEditorAccess).actionPerformed(null);
        new SortBelowAction(relationEditorAccess).actionPerformed(null);
        new ReverseAction(relationEditorAccess).actionPerformed(null);
    }

    /**
     * Test DeleteCurrentRelationAction
     */
    @Test
    void testDeleteCurrentRelationAction() {
        TestUtils.assumeWorkingJMockit();
        final JOptionPaneSimpleMocker jopsMocker = new JOptionPaneSimpleMocker() {
            @Override
            public String getStringFromOriginalMessage(Object originalMessage) {
                return ((JTextComponent) ((Container) originalMessage).getComponent(0)).getText();
            }
        };
        jopsMocker.getMockResultMap().put(
                "<html>\n  <head>\n    \n  </head>\n  <body>\n    You are about to delete 1 "
                + "relation:\n\n    "
                + "<ul>\n      <li>\n        incomplete\n      </li>\n    </ul>\n    <br>\n    "
                + "This step is rarely necessary and cannot be undone easily after being \n    "
                + "uploaded to the server.<br>Do you really want to delete?\n  </body>\n</html>\n", JOptionPane.YES_OPTION);
        jopsMocker.getMockResultMap().put(
                "<html>\n  <head>\n    \n  </head>\n  <body>\n    You are about to delete incomplete "
                + "objects.<br>This will cause problems \n    because you don\'t see the real object.<br>"
                + "Do you really want to delete?\n  </body>\n</html>\n",
                JOptionPane.YES_OPTION);

        new DeleteCurrentRelationAction(relationEditorAccess).actionPerformed(null);

        assertEquals(2, jopsMocker.getInvocationLog().size());

        Object[] invocationLogEntry = jopsMocker.getInvocationLog().get(0);
        assertEquals(JOptionPane.YES_OPTION, (int) invocationLogEntry[0]);
        assertEquals("Delete relation?", invocationLogEntry[2]);

        invocationLogEntry = jopsMocker.getInvocationLog().get(1);
        assertEquals(JOptionPane.YES_OPTION, (int) invocationLogEntry[0]);
        assertEquals("Delete confirmation", invocationLogEntry[2]);
    }

    /**
     * Test SetRoleAction
     */
    @Test
    void testSetRoleAction() {
        TestUtils.assumeWorkingJMockit();
        final JOptionPaneSimpleMocker.MessagePanelMocker mpMocker = new JOptionPaneSimpleMocker.MessagePanelMocker();
        // JOptionPaneSimpleMocker doesn't handle showOptionDialog calls because of their potential
        // complexity, but this is quite a simple use of showOptionDialog which we can mock from scratch.
        final boolean[] jopMockerCalled = new boolean[] {false};
        new MockUp<JOptionPane>() {
            @Mock
            public int showOptionDialog(
                Component parentComponent,
                Object message,
                String title,
                int optionType,
                int messageType,
                Icon icon,
                Object[] options,
                Object initialValue
            ) {
                assertEquals(
                    "<html>You are setting an empty role on 0 objects.<br>This is equal to deleting the "
                    + "roles of these objects.<br>Do you really want to apply the new role?</html>",
                    mpMocker.getOriginalMessage((ConditionalOptionPaneUtil.MessagePanel) message).toString()
                );
                assertEquals(
                    "Confirm empty role",
                    title
                );
                jopMockerCalled[0] = true;
                return JOptionPane.YES_OPTION;
            }
        };

        new SetRoleAction(relationEditorAccess).actionPerformed(null);

        assertTrue(jopMockerCalled[0]);
    }

    /**
     * Non-regression test for JOSM #22024.
     * This is due to a race condition between uploading and refreshing the relation in the editor.
     */
    @Test
    void testNonRegression22024() {
        final DataSet ds = new DataSet();
        final Node node = new Node(LatLon.ZERO);
        Relation relation = TestUtils.newRelation("type=restriction", new RelationMember("", node));
        ds.addPrimitive(node);
        ds.addPrimitive(relation);
        MainApplication.getLayerManager().prepareLayerForUpload(new OsmDataLayer(ds, "testNonRegression22024", null));
        // Sanity check that behavior hasn't changed
        assertTrue(ds.isLocked(), "The dataset should be locked when it is being uploaded.");
        relationEditorAccess.getEditor().setRelation(relation);
        relationEditorAccess.getMemberTableModel().populate(relation);
        relationEditorAccess.getTagModel().initFromPrimitive(relation);
        relationEditorAccess.getEditor().reloadDataFromRelation();
        assertDoesNotThrow(relationEditorAccess::getChangedRelation);
    }
}
