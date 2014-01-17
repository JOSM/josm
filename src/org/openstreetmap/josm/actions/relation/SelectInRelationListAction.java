// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.relation;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * The action for activating a relation in relation list dialog
 * @since 5793
 */
public class SelectInRelationListAction extends AbstractRelationAction {

    /**
     * Constructs a new <code>SelectInRelationListAction</code>.
     */
    public SelectInRelationListAction() {
        putValue(NAME, tr("Select in relation list"));
        putValue(SHORT_DESCRIPTION, tr("Select relation in relation list."));
        putValue(SMALL_ICON, ImageProvider.get("dialogs", "selectionlist"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!isEnabled() || relations.isEmpty() || Main.map==null || Main.map.relationListDialog==null) return;
        Main.map.relationListDialog.unfurlDialog();
        Main.map.relationListDialog.selectRelations(relations);
    }
}
