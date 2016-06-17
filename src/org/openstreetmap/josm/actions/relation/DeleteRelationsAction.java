// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.relation;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.util.Collection;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.mapmode.DeleteAction;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Action that delete relations
 * @since 5799
 */
public class DeleteRelationsAction extends AbstractRelationAction {

    /**
     * Constructs a new <code>DeleteRelationsAction</code>.
     */
    public DeleteRelationsAction() {
        putValue(SHORT_DESCRIPTION, tr("Delete the selected relation"));
        putValue(NAME, tr("Delete"));
        putValue(SMALL_ICON, ImageProvider.get("dialogs", "delete"));
    }

    protected void deleteRelation(Collection<Relation> toDelete) {
        if (toDelete == null)
            return;
        DeleteAction.deleteRelations(Main.getLayerManager().getEditLayer(), toDelete);
        // clear selection after deletion
        if (Main.map.relationListDialog != null)
                Main.map.relationListDialog.selectRelations(null);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!isEnabled() || !Main.main.hasEditLayer())
            return;
        deleteRelation(relations);
    }
}
