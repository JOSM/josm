package org.openstreetmap.josm.actions.relation;

import java.awt.event.ActionEvent;
import static javax.swing.Action.NAME;
import static javax.swing.Action.SHORT_DESCRIPTION;
import static javax.swing.Action.SMALL_ICON;


import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.tools.ImageProvider;

import static org.openstreetmap.josm.tools.I18n.tr;

/**
 * The action for activating a relation in relation list dialog
 */
public class SelectInRelationListAction extends AbstractRelationAction {
    public SelectInRelationListAction() {
        putValue(NAME, tr("Select in relation list"));
        putValue(SHORT_DESCRIPTION, tr("Select relation in relation list."));
        putValue(SMALL_ICON, ImageProvider.get("dialogs", "selectionlist"));
    }

    public void actionPerformed(ActionEvent e) {
        if (!isEnabled() || relations.isEmpty()) return;
        if (Main.map.relationListDialog!=null)
            Main.map.relationListDialog.selectRelations(relations);
    }
}
