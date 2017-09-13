// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation.actions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.gui.ConditionalOptionPaneUtil;
import org.openstreetmap.josm.gui.dialogs.relation.GenericRelationEditor;
import org.openstreetmap.josm.gui.dialogs.relation.GenericRelationEditor.AddAbortException;
import org.openstreetmap.josm.gui.dialogs.relation.IRelationEditor;
import org.openstreetmap.josm.gui.dialogs.relation.MemberTable;
import org.openstreetmap.josm.gui.dialogs.relation.MemberTableModel;
import org.openstreetmap.josm.gui.dialogs.relation.SelectionTable;
import org.openstreetmap.josm.gui.dialogs.relation.SelectionTableModel;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

/**
 * Abstract superclass of "Add from selection" actions.
 * @since 9496
 */
abstract class AddFromSelectionAction extends AbstractRelationEditorAction {

    protected final SelectionTable selectionTable;
    protected final SelectionTableModel selectionTableModel;

    protected AddFromSelectionAction(MemberTable memberTable, MemberTableModel memberTableModel, SelectionTable selectionTable,
            SelectionTableModel selectionTableModel, String actionMapKey, OsmDataLayer layer, IRelationEditor editor) {
        super(memberTable, memberTableModel, actionMapKey, layer, editor);
        this.selectionTable = selectionTable;
        this.selectionTableModel = selectionTableModel;
    }

    protected boolean isPotentialDuplicate(OsmPrimitive primitive) {
        return memberTableModel.hasMembersReferringTo(Collections.singleton(primitive));
    }

    protected List<OsmPrimitive> filterConfirmedPrimitives(List<OsmPrimitive> primitives) throws AddAbortException {
        if (primitives == null || primitives.isEmpty())
            return primitives;
        List<OsmPrimitive> ret = new ArrayList<>();
        ConditionalOptionPaneUtil.startBulkOperation("add_primitive_to_relation");
        for (OsmPrimitive primitive : primitives) {
            if (primitive instanceof Relation && editor.getRelation() != null && editor.getRelation().equals(primitive)) {
                GenericRelationEditor.warnOfCircularReferences(primitive);
                continue;
            }
            if (isPotentialDuplicate(primitive)) {
                if (GenericRelationEditor.confirmAddingPrimitive(primitive)) {
                    ret.add(primitive);
                }
                continue;
            } else {
                ret.add(primitive);
            }
        }
        ConditionalOptionPaneUtil.endBulkOperation("add_primitive_to_relation");
        return ret;
    }
}
