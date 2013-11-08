// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.util.Collection;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.MoveCommand;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.dialogs.LatLonDialog;

/**
 * This action displays a dialog with the coordinates of a node where the user can change them,
 * and when ok is pressed, the node is relocated to the specified position.
 */
public final class MoveNodeAction extends JosmAction {

    public MoveNodeAction() {
        super(tr("Move Node..."), "movenode", tr("Edit latitude and longitude of a node."),
                null, /* no shortcut */
                true);
        putValue("help", ht("/Action/MoveNode"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!isEnabled() || (getCurrentDataSet().getSelectedNodes().size() != 1))
            return;

        LatLonDialog dialog = new LatLonDialog(Main.parent, tr("Move Node..."), ht("/Action/MoveNode"));
        Node n = (Node) getCurrentDataSet().getSelectedNodes().toArray()[0];
        dialog.setCoordinates(n.getCoor());
        dialog.showDialog();
        if (dialog.getValue() != 1)
            return;

        LatLon coordinates = dialog.getCoordinates();
        if (coordinates == null)
            return;

        // move the node
        Main.main.undoRedo.add(new MoveCommand(n, coordinates));
        Main.map.mapView.repaint();
    }

    @Override
    protected void updateEnabledState() {
        if (getCurrentDataSet() == null) {
            setEnabled(false);
        } else {
            updateEnabledState(getCurrentDataSet().getSelected());
        }
    }

    @Override
    protected void updateEnabledState(Collection<? extends OsmPrimitive> selection) {
        if (selection == null || selection.isEmpty()) {
            setEnabled(false);
            return;
        }
        if ((selection.size()) == 1 && (selection.toArray()[0] instanceof Node) ) {
            setEnabled(true);
        } else {
            setEnabled(false);
        }
    }
}
