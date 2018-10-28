// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation.actions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.awt.Component;
import java.awt.Container;
import javax.swing.Icon;
import javax.swing.JOptionPane;
import javax.swing.text.JTextComponent;

import org.openstreetmap.josm.testutils.mockers.JOptionPaneSimpleMocker;
import org.openstreetmap.josm.gui.ConditionalOptionPaneUtil;

import com.google.common.collect.ImmutableMap;

import mockit.Mock;
import mockit.MockUp;

import org.junit.Test;

/**
 * Unit tests for relation editor actions.
 */
public class RelationEditorActionsTest extends AbstractRelationEditorActionTest {

    /**
     * Check that all dialog-less actions do not crash.
     */
    @Test
    public void testNoDialogActions() {
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
    public void testDeleteCurrentRelationAction() {
        final JOptionPaneSimpleMocker jopsMocker = new JOptionPaneSimpleMocker(
            ImmutableMap.<String, Object>of(
                "<html>\n  <head>\n    \n  </head>\n  <body>\n    You are about to delete 1 "
                + "relation:\n\n    "
                + "<ul>\n      <li>\n        incomplete\n      </li>\n    </ul>\n    <br>\n    "
                + "This step is rarely necessary and cannot be undone easily after being \n    "
                + "uploaded to the server.<br>Do you really want to delete?\n  </body>\n</html>\n", JOptionPane.YES_OPTION,
                "<html>\n  <head>\n    \n  </head>\n  <body>\n    You are about to delete incomplete "
                + "objects.<br>This will cause problems \n    because you don\'t see the real object.<br>"
                + "Do you really want to delete?\n  </body>\n</html>\n",
                JOptionPane.YES_OPTION
            )
        ) {
            public String getStringFromOriginalMessage(Object originalMessage) {
                return ((JTextComponent) ((Container) originalMessage).getComponent(0)).getText();
            }
        };

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
    public void testSetRoleAction() {
        final JOptionPaneSimpleMocker.MessagePanelMocker mpMocker = new JOptionPaneSimpleMocker.MessagePanelMocker();
        // JOptionPaneSimpleMocker doesn't handle showOptionDialog calls because of their potential
        // complexity, but this is quite a simple use of showOptionDialog which we can mock from scratch.
        final boolean[] jopMockerCalled = new boolean[] {false};
        final MockUp<JOptionPane> jopMocker = new MockUp<JOptionPane>() {
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
}
