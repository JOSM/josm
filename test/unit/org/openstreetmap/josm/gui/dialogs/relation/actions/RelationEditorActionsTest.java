// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation.actions;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.gui.dialogs.relation.MemberTable;
import org.openstreetmap.josm.gui.dialogs.relation.MemberTableModel;
import org.openstreetmap.josm.gui.dialogs.relation.RelationAware;
import org.openstreetmap.josm.gui.dialogs.relation.SelectionTableModel;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.tagging.TagEditorModel;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompletingTextField;

/**
 * Unit tests for relation editor actions.
 */
public class RelationEditorActionsTest {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init(true);
    }

    /**
     * Test all actions with minimal data.
     */
    @Test
    public void testAllActions() {
        final DataSet ds = new DataSet();
        final Relation orig = new Relation(1);
        ds.addPrimitive(orig);
        OsmDataLayer layer = new OsmDataLayer(new DataSet(), "test", null);
        MemberTableModel memberTableModel = new MemberTableModel(orig, layer, null);
        SelectionTableModel selectionTableModel = new SelectionTableModel(layer);

        RelationAware editor = new RelationAware() {
            private Relation r = orig;

            @Override
            public void setRelation(Relation relation) {
                r = relation;
            }

            @Override
            public boolean isDirtyRelation() {
                return false;
            }

            @Override
            public Relation getRelationSnapshot() {
                return r;
            }

            @Override
            public Relation getRelation() {
                return r;
            }
        };

        MemberTable memberTable = new MemberTable(layer, editor.getRelation(), memberTableModel);
        TagEditorModel tagModel = new TagEditorModel();
        AutoCompletingTextField tfRole = new AutoCompletingTextField();

        new AddSelectedAfterSelection(memberTableModel, selectionTableModel, editor).actionPerformed(null);
        new AddSelectedBeforeSelection(memberTableModel, selectionTableModel, editor).actionPerformed(null);
        new AddSelectedAtStartAction(memberTableModel, selectionTableModel, editor).actionPerformed(null);
        new AddSelectedAtEndAction(memberTableModel, selectionTableModel, editor).actionPerformed(null);

        new ApplyAction(memberTable, memberTableModel, tagModel, layer, editor).actionPerformed(null);
        new RefreshAction(memberTable, memberTableModel, tagModel, layer, editor).actionPerformed(null);
        new OKAction(memberTable, memberTableModel, tagModel, layer, editor, tfRole).actionPerformed(null);
        new CancelAction(memberTable, memberTableModel, tagModel, layer, editor, tfRole).actionPerformed(null);

        new CopyMembersAction(memberTableModel, layer, editor).actionPerformed(null);
        new PasteMembersAction(memberTableModel, layer, editor).actionPerformed(null);

        new DeleteCurrentRelationAction(layer, editor).actionPerformed(null);

        new DownloadIncompleteMembersAction(memberTable, memberTableModel, "downloadincomplete", layer, editor).actionPerformed(null);
        new DownloadSelectedIncompleteMembersAction(memberTable, memberTableModel, null, layer, editor).actionPerformed(null);

        new DuplicateRelationAction(memberTableModel, tagModel, layer).actionPerformed(null);
        new EditAction(memberTable, memberTableModel, layer).actionPerformed(null);

        new MoveDownAction(memberTable, memberTableModel, "movedown").actionPerformed(null);
        new MoveUpAction(memberTable, memberTableModel, "moveup").actionPerformed(null);
        new RemoveAction(memberTable, memberTableModel, "remove").actionPerformed(null);

        new RemoveSelectedAction(memberTableModel, selectionTableModel, layer).actionPerformed(null);
        new SelectedMembersForSelectionAction(memberTableModel, selectionTableModel, layer).actionPerformed(null);

        new SelectPrimitivesForSelectedMembersAction(memberTable, memberTableModel, layer).actionPerformed(null);

        new SortAction(memberTable, memberTableModel).actionPerformed(null);
        new SortBelowAction(memberTable, memberTableModel).actionPerformed(null);
        new ReverseAction(memberTable, memberTableModel).actionPerformed(null);

        new SetRoleAction(memberTable, memberTableModel, tfRole).actionPerformed(null);
    }
}
