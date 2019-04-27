// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Collection;

import org.openstreetmap.josm.command.MoveCommand;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.dialogs.LatLonDialog;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * This action displays a dialog with the coordinates of a node where the user can change them,
 * and when ok is pressed, the node is relocated to the specified position.
 */
public final class MoveNodeAction extends JosmAction {

    /**
     * Constructs a new {@code MoveNodeAction}.
     */
    public MoveNodeAction() {
        super(tr("Move Node..."), "movenode", tr("Edit latitude and longitude of a node."),
                Shortcut.registerShortcut("movenode", tr("Edit: {0}", tr("Move Node...")),
                        KeyEvent.VK_M, Shortcut.NONE), true);
        setHelpId(ht("/Action/MoveNode"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Collection<Node> selNodes = getLayerManager().getEditDataSet().getSelectedNodes();
        if (!isEnabled() || selNodes.size() != 1)
            return;

        LatLonDialog dialog = new LatLonDialog(MainApplication.getMainFrame(), tr("Move Node..."), ht("/Action/MoveNode"));
        Node n = (Node) selNodes.toArray()[0];
        dialog.setCoordinates(n.getCoor());
        dialog.showDialog();
        if (dialog.getValue() != 1)
            return;

        LatLon coordinates = dialog.getCoordinates();
        if (coordinates == null)
            return;

        // move the node
        UndoRedoHandler.getInstance().add(new MoveCommand(n, coordinates));
        if (n.getCoor().distance(coordinates) > 1) {
            // see #13538: Avoid rounding error near 180 longitude which moves nodes too far
            if (coordinates.lon() >= 180.0) {
                coordinates = new LatLon(coordinates.lat(), Math.nextDown(180.0));
            } else if (coordinates.lon() <= -180.0) {
                coordinates = new LatLon(coordinates.lat(), Math.nextUp(-180.0));
            }
            UndoRedoHandler.getInstance().undo(1);
            UndoRedoHandler.getInstance().add(new MoveCommand(n, coordinates));
        }
        MainApplication.getMap().mapView.repaint();
    }

    @Override
    protected void updateEnabledState() {
        updateEnabledStateOnCurrentSelection();
    }

    @Override
    protected void updateEnabledState(Collection<? extends OsmPrimitive> selection) {
        setEnabled(OsmUtils.isOsmCollectionEditable(selection) && selection.size() == 1 && selection.toArray()[0] instanceof Node);
    }
}
