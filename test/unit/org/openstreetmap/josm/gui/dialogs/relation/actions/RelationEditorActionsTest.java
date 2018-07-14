// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation.actions;

import org.junit.Test;

/**
 * Unit tests for relation editor actions.
 */
public class RelationEditorActionsTest extends AbstractRelationEditorActionTest {

    /**
     * Check that all actions do not crash.
     */
    @Test
    public void testAllActions() {
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
        new DeleteCurrentRelationAction(relationEditorAccess).actionPerformed(null);

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

        new SetRoleAction(relationEditorAccess).actionPerformed(null);
    }
}
