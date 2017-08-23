// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.relation;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;

import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapFrame;
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
        MapFrame map = MainApplication.getMap();
        if (!isEnabled() || relations.isEmpty() || map == null || map.relationListDialog == null) return;
        map.relationListDialog.unfurlDialog();
        map.relationListDialog.selectRelations(relations);
    }
}
