// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation.actions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.validation.tests.RelationChecker;
import org.openstreetmap.josm.gui.ConditionalOptionPaneUtil;
import org.openstreetmap.josm.gui.dialogs.relation.GenericRelationEditor;
import org.openstreetmap.josm.gui.dialogs.relation.GenericRelationEditor.AddAbortException;

/**
 * Abstract superclass of "Add from selection" actions.
 * @since 9496
 */
abstract class AddFromSelectionAction extends AbstractRelationEditorAction {
    private static final long serialVersionUID = 1L;

    protected AddFromSelectionAction(IRelationEditorActionAccess editorAccess,
            IRelationEditorUpdateOn... updateOn) {
        super(editorAccess, updateOn);
    }

    protected boolean isPotentialDuplicate(OsmPrimitive primitive) {
        return editorAccess.getMemberTableModel().hasMembersReferringTo(Collections.singleton(primitive));
    }

    protected List<OsmPrimitive> filterConfirmedPrimitives(List<OsmPrimitive> primitives) throws AddAbortException {
        if (primitives == null || primitives.isEmpty())
            return primitives;
        List<OsmPrimitive> ret = new ArrayList<>();
        ConditionalOptionPaneUtil.startBulkOperation("add_primitive_to_relation");
        for (OsmPrimitive primitive : primitives) {
            if (primitive instanceof Relation) {
                List<Relation> loop = RelationChecker.checkAddMember(editorAccess.getEditor().getRelation(), (Relation) primitive);
                if (!loop.isEmpty() && loop.get(0).equals(loop.get(loop.size() - 1))) {
                    GenericRelationEditor.warnOfCircularReferences(primitive, loop);
                    continue;
                }
            }
            if (isPotentialDuplicate(primitive)) {
                if (GenericRelationEditor.confirmAddingPrimitive(primitive)) {
                    ret.add(primitive);
                }
            } else {
                ret.add(primitive);
            }
        }
        ConditionalOptionPaneUtil.endBulkOperation("add_primitive_to_relation");
        return ret;
    }
}
