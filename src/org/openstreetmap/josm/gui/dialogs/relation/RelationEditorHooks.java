// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.openstreetmap.josm.gui.dialogs.relation.actions.IRelationEditorActionGroup;

/**
 * This class allows to hook into the relation editor. It can be used to overwrite specific behavior.
 *
 * @author Michael Zangl
 * @since 14027
 */
public final class RelationEditorHooks {

    private static final CopyOnWriteArrayList<IRelationEditorActionGroup> memberActions = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<IRelationEditorActionGroup> selectionActions = new CopyOnWriteArrayList<>();

    private RelationEditorHooks() {
        // only static methods.
    }

    /**
     * Adds actions to the members action toolbar
     * @param group The group to add
     */
    public static void addActionsToMembers(IRelationEditorActionGroup group) {
        memberActions.add(group);
    }

    /**
     * Adds actions to the selection action toolbar
     * @param group The group to add
     */
    public static void addActionsToSelectio(IRelationEditorActionGroup group) {
        selectionActions.add(group);
    }

    /* package */ static List<IRelationEditorActionGroup> getMemberActions() {
        return Collections.unmodifiableList(memberActions);
    }

    /* package */ static List<IRelationEditorActionGroup> getSelectActions() {
        return Collections.unmodifiableList(selectionActions);
    }

}
