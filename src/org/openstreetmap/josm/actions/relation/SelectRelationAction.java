// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.relation;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Sets the current selection to specified list of relations
 * @since 5793
 */
public class SelectRelationAction extends AbstractRelationAction {

    private final boolean add;

    /**
     * Constructs a new <code>SelectRelationAction</code>.
     * @param add if <code>true</code>, the relation will be added to current selection. If <code>false</code>, the relation will replace the current selection.
     */
    public SelectRelationAction(boolean add) {
        putValue(SHORT_DESCRIPTION, add ? tr("Add the selected relations to the current selection") : tr("Set the current selection to the list of selected relations"));
        putValue(SMALL_ICON, ImageProvider.get("dialogs", "select"));
        putValue(NAME, add ? tr("Select relation (add)") : tr("Select relation"));
        this.add = add;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!isEnabled() || relations.isEmpty()) return;
        OsmDataLayer editLayer = Main.main.getEditLayer();
        if (editLayer==null || editLayer.data==null) return;
        if (add) {
            editLayer.data.addSelected(relations);
        } else {
            editLayer.data.setSelected(relations);
        }
    }
}
