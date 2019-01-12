// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation;

import java.util.Arrays;
import java.util.Collection;

import javax.swing.Action;

import org.openstreetmap.josm.actions.relation.DeleteRelationsAction;
import org.openstreetmap.josm.actions.relation.DownloadMembersAction;
import org.openstreetmap.josm.actions.relation.DownloadSelectedIncompleteMembersAction;
import org.openstreetmap.josm.actions.relation.EditRelationAction;
import org.openstreetmap.josm.actions.relation.SelectInRelationListAction;
import org.openstreetmap.josm.actions.relation.SelectMembersAction;
import org.openstreetmap.josm.actions.relation.SelectRelationAction;
import org.openstreetmap.josm.gui.PopupMenuHandler;

/**
 * Utility class to setup a {@link PopupMenuHandler} with a consistent set of relation actions.
 * @since 14685
 */
public final class RelationPopupMenus {
    private RelationPopupMenus() {
        // Hide default constructor for utils classes
    }

    /**
     * Adds relation actions to the given {@link PopupMenuHandler}
     * @param menu handler to add actions to
     * @param excludeActions 0 or more action classes to exclude, i.e., not add
     * @return {@code menu}
     */
    @SafeVarargs
    public static PopupMenuHandler setupHandler(PopupMenuHandler menu, Class<? extends Action>... excludeActions) {
        final Collection<Class<? extends Action>> exclude = Arrays.asList(excludeActions);
        if (!exclude.contains(EditRelationAction.class)) {
            menu.addAction(new EditRelationAction());
        }

        if (!exclude.contains(DeleteRelationsAction.class)) {
            menu.addAction(new DeleteRelationsAction());
            menu.addSeparator();
        }

        if (!exclude.contains(SelectInRelationListAction.class)) {
            menu.addAction(new SelectInRelationListAction());
        }

        menu.addAction(new SelectRelationAction(false));
        menu.addAction(new SelectRelationAction(true));
        menu.addAction(new SelectMembersAction(false));
        menu.addAction(new SelectMembersAction(true));
        menu.addSeparator();

        menu.addAction(new DownloadMembersAction());
        menu.addAction(new DownloadSelectedIncompleteMembersAction());

        return menu;
    }
}
