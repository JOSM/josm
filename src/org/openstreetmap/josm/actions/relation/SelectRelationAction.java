package org.openstreetmap.josm.actions.relation;

import java.awt.event.ActionEvent;
import static javax.swing.Action.NAME;
import static javax.swing.Action.SHORT_DESCRIPTION;
import static javax.swing.Action.SMALL_ICON;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.tools.ImageProvider;

import static org.openstreetmap.josm.tools.I18n.tr;

/**
 * Sets the current selection to specified list of relations 
 */
public class SelectRelationAction extends AbstractRelationAction {
    boolean add;

    public SelectRelationAction(boolean add) {
        putValue(SHORT_DESCRIPTION, add ? tr("Add the selected relations to the current selection") : tr("Set the current selection to the list of selected relations"));
        putValue(SMALL_ICON, ImageProvider.get("dialogs", "select"));
        putValue(NAME, add ? tr("Select relation (add)") : tr("Select relation"));
        this.add = add;
    }

    public void actionPerformed(ActionEvent e) {
        if (!isEnabled() || relations.isEmpty()) return;
        OsmDataLayer editLayer = Main.map.mapView.getEditLayer();
        if (editLayer==null || editLayer.data==null) return;
        if (add) {
            editLayer.data.addSelected(relations);
        } else {
            editLayer.data.setSelected(relations);
        }
    }
}
