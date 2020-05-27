// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Collections;

import javax.swing.SwingUtilities;

import org.openstreetmap.josm.command.AddCommand;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.dialogs.LatLonDialog;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * This action displays a dialog where the user can enter a latitude and longitude,
 * and when ok is pressed, a new node is created at the specified position.
 */
public final class AddNodeAction extends JosmAction {
    // remember input from last time
    private String textLatLon, textEastNorth;

    /**
     * Constructs a new {@code AddNodeAction}.
     */
    public AddNodeAction() {
        super(tr("Add Node..."), "addnode", tr("Add a node by entering latitude / longitude or easting / northing."),
                Shortcut.registerShortcut("addnode", tr("Edit: {0}", tr("Add Node...")),
                        KeyEvent.VK_D, Shortcut.SHIFT), true);
        setHelpId(ht("/Action/AddNode"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!isEnabled())
            return;

        // #17682 - Run the action later in EDT to make sure the KeyEvent triggering it is consumed before the dialog is shown
        SwingUtilities.invokeLater(() -> {
            LatLonDialog dialog = new LatLonDialog(MainApplication.getMainFrame(), tr("Add Node..."), ht("/Action/AddNode"));

            if (textLatLon != null) {
                dialog.setLatLonText(textLatLon);
            }
            if (textEastNorth != null) {
                dialog.setEastNorthText(textEastNorth);
            }

            dialog.showDialog();

            if (dialog.getValue() != 1)
                return;

            LatLon coordinates = dialog.getCoordinates();
            if (coordinates == null)
                return;

            textLatLon = dialog.getLatLonText();
            textEastNorth = dialog.getEastNorthText();

            Node nnew = new Node(coordinates);

            // add the node
            DataSet ds = getLayerManager().getEditDataSet();
            UndoRedoHandler.getInstance().add(new AddCommand(ds, nnew));
            ds.setSelected(nnew);
            MapView mapView = MainApplication.getMap().mapView;
            if (mapView != null && !mapView.getRealBounds().contains(nnew.getCoor())) {
                AutoScaleAction.zoomTo(Collections.<OsmPrimitive>singleton(nnew));
            }
        });
    }

    @Override
    protected boolean listenToSelectionChange() {
        return false;
    }

    @Override
    protected void updateEnabledState() {
        setEnabled(getLayerManager().getEditLayer() != null);
    }
}
