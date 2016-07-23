// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.layer;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.openstreetmap.josm.gui.dialogs.IEnabledStateUpdating;
import org.openstreetmap.josm.gui.dialogs.LayerListDialog.LayerListModel;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * The action to move up the currently selected entries in the list.
 */
public class MoveUpAction extends AbstractAction implements IEnabledStateUpdating {
    private final LayerListModel model;

    /**
     * Constructs a new {@code MoveUpAction}.
     * @param model layer list model
     */
    public MoveUpAction(LayerListModel model) {
        this.model = model;
        putValue(NAME, tr("Move up"));
        new ImageProvider("dialogs", "up").getResource().attachImageIcon(this, true);
        putValue(SHORT_DESCRIPTION, tr("Move the selected layer one row up."));
        updateEnabledState();
    }

    @Override
    public void updateEnabledState() {
        setEnabled(model.canMoveUp());
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        model.moveUp();
    }
}
