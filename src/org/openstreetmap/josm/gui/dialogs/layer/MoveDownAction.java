// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.layer;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.openstreetmap.josm.gui.dialogs.IEnabledStateUpdating;
import org.openstreetmap.josm.gui.dialogs.LayerListDialog.LayerListModel;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * The action to move down the currently selected entries in the list.
 */
public class MoveDownAction extends AbstractAction implements IEnabledStateUpdating {
    private final LayerListModel model;

    /**
     * Constructs a new {@code MoveDownAction}.
     * @param model layer list model
     */
    public MoveDownAction(LayerListModel model) {
        this.model = model;
        putValue(NAME, tr("Move down"));
        new ImageProvider("dialogs", "down").getResource().attachImageIcon(this, true);
        putValue(SHORT_DESCRIPTION, tr("Move the selected layer one row down."));
        updateEnabledState();
    }

    @Override
    public void updateEnabledState() {
        setEnabled(model.canMoveDown());
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        model.moveDown();
    }
}
