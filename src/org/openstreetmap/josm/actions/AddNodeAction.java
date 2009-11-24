// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.gui.help.HelpUtil.ht;

import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.AddCommand;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * This action displays a dialog where the user can enter a latitude and longitude,
 * and when ok is pressed, a new node is created at the specified position.
 */
public final class AddNodeAction extends JosmAction {

    public AddNodeAction() {
        super(tr("Add Node..."), "addnode", tr("Add a node by entering latitude and longitude."),
                Shortcut.registerShortcut("addnode", tr("Edit: {0}", tr("Add Node...")), KeyEvent.VK_D, Shortcut.GROUP_EDIT,
                        Shortcut.SHIFT_DEFAULT), true);
        putValue("help", ht("/Action/AddNode"));
    }

    public void actionPerformed(ActionEvent e) {
        // we abort if we are not in the context of an OsmDataLayer
        //
        if (!isEnabled())
            return;

        JPanel p = new JPanel(new GridBagLayout());
        p.add(new JLabel("<html>"+
                tr("Enter the coordinates for the new node.") +
                "<br>" + tr("Use decimal degrees.") +
                "<br>" + tr("Negative values denote Western/Southern hemisphere.")),
                GBC.eol());

        p.add(new JLabel(tr("Latitude")), GBC.std().insets(0,10,5,0));
        final JTextField lat = new JTextField(12);
        p.add(lat, GBC.eol().insets(0,10,0,0));
        p.add(new JLabel(tr("Longitude")), GBC.std().insets(0,0,5,10));
        final JTextField lon = new JTextField(12);
        p.add(lon, GBC.eol().insets(0,0,0,10));

        Node nnew = null;

        while(nnew == null) {
            JOptionPane pane = new JOptionPane(p, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
            pane.createDialog(Main.parent, tr("Add Node...")).setVisible(true);
            if (!Integer.valueOf(JOptionPane.OK_OPTION).equals(pane.getValue()))
                return;
            try {
                LatLon ll = new LatLon(Double.parseDouble(lat.getText()), Double.parseDouble(lon.getText()));
                if (!ll.isOutSideWorld()) {
                    nnew = new Node(ll);
                }
            } catch (Exception ex) { }
        }

        /* Now execute the commands to add the dupicated contents of the paste buffer to the map */
        Main.main.undoRedo.add(new AddCommand(nnew));
        getCurrentDataSet().setSelected(nnew);
        Main.map.mapView.repaint();
    }

    @Override
    protected void updateEnabledState() {
        setEnabled(getEditLayer() != null);
    }

}
