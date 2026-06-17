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
import org.openstreetmap.josm.tools.Utils;

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

    /**
     * Check and filter a list of primitives before adding them as relation members.
     * Prompt users for confirmation when duplicates are detected and prevent relation loops.
     *
     * @param primitives The primitives to be checked and filtered
     * @return The primitives to add to the relation. Never {@code null}, but may be an empty list.
     * @throws AddAbortException when a relation loop is detected
     */
    protected List<OsmPrimitive> filterConfirmedPrimitives(List<OsmPrimitive> primitives) throws AddAbortException {
        return filterConfirmedPrimitives(primitives, false);
    }

    /**
     * Check and filter a list of primitives before adding them as relation members.
     * Prompt users for confirmation when duplicates are detected and prevent relation loops.
     *
     * @param primitives The primitives to be checked and filtered
     * @param abortOnSkip If the user decides to not add a primitive or adding a primitive would 
     *                    cause a relation loop, abort (throw {@code AddAbortException})
     * @return The primitives to add to the relation. Never {@code null}, but may be an empty list.
     * @throws AddAbortException when a relation loop is detected or {@code abortOnSkip} is 
     *                           {@code true} <i>and</i> the user decides to not add a primitive.
     * @since xxx
     */
    protected List<OsmPrimitive> filterConfirmedPrimitives(List<OsmPrimitive> primitives, boolean abortOnSkip) throws AddAbortException {
        if (Utils.isEmpty(primitives))
            return primitives;
        List<OsmPrimitive> ret = new ArrayList<>();
        ConditionalOptionPaneUtil.startBulkOperation("add_primitive_to_relation");
        try {
            for (OsmPrimitive primitive : primitives) {
                if (primitive instanceof Relation) {
                    List<Relation> loop = RelationChecker.checkAddMember(editorAccess.getEditor().getRelation(), (Relation) primitive);
                    if (!loop.isEmpty() && loop.get(0).equals(loop.get(loop.size() - 1))) {
                        GenericRelationEditor.warnOfCircularReferences(primitive, loop);
                        if (abortOnSkip) {
                            throw new AddAbortException();
                        }
                        continue;
                    }
                }
                if (isPotentialDuplicate(primitive)) {
                    if (GenericRelationEditor.confirmAddingPrimitive(primitive)) {
                        ret.add(primitive);
                    } else if (abortOnSkip) {
                        throw new AddAbortException();
                    }
                } else {
                    ret.add(primitive);
                }
            }
        } finally {
            ConditionalOptionPaneUtil.endBulkOperation("add_primitive_to_relation");
        }

        return ret;
    }
}
