//License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.RemoveNodesCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.tools.Shortcut;

public class UnJoinNodeWayAction extends JosmAction {
    public UnJoinNodeWayAction() {
        super(tr("Disconnect Node from Way"), "unjoinnodeway",
                tr("Disconnect nodes from a way they currently belong to"),
                Shortcut.registerShortcut("tools:unjoinnodeway",
                    tr("Tool: {0}", tr("Disconnect Node from Way")), KeyEvent.VK_J, Shortcut.ALT), true);
        putValue("help", ht("/Action/UnJoinNodeWay"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        Collection<OsmPrimitive> selection = getCurrentDataSet().getSelected();

        List<Node> selectedNodes = OsmPrimitive.getFilteredList(selection, Node.class);
        List<Way> selectedWays = OsmPrimitive.getFilteredList(selection, Way.class);
        List<Way> applicableWays = getApplicableWays(selectedWays, selectedNodes);

        if (applicableWays == null) {
            JOptionPane.showMessageDialog(
                    Main.parent,
                    tr("Select at least one node to be disconnected."),
                    tr("Warning"),
                    JOptionPane.WARNING_MESSAGE);
            return;
        } else if (applicableWays.isEmpty()) {
            JOptionPane.showMessageDialog(Main.parent,
                    trn("Selected node cannot be disconnected from anything.",
                        "Selected nodes cannot be disconnected from anything.",
                        selectedNodes.size()),
                    tr("Warning"),
                    JOptionPane.WARNING_MESSAGE);
            return;
        } else if (applicableWays.size() > 1) {
            JOptionPane.showMessageDialog(Main.parent,
                    trn("There is more than one way using the node you selected. Please select the way also.",
                        "There is more than one way using the nodes you selected. Please select the way also.",
                        selectedNodes.size()),
                    tr("Warning"),
                    JOptionPane.WARNING_MESSAGE);
            return;
        } else if (applicableWays.get(0).getRealNodesCount() < selectedNodes.size() + 2) {
            // there is only one affected way, but removing the selected nodes would only leave it
            // with less than 2 nodes
            JOptionPane.showMessageDialog(Main.parent,
                    trn("The affected way would disappear after disconnecting the selected node.",
                        "The affected way would disappear after disconnecting the selected nodes.",
                        selectedNodes.size()),
                    tr("Warning"),
                    JOptionPane.WARNING_MESSAGE);
            return;
        }


        // Finally, applicableWays contains only one perfect way
        Way selectedWay = applicableWays.get(0);

        // I'm sure there's a better way to handle this
        Main.main.undoRedo.add(new RemoveNodesCommand(selectedWay, selectedNodes));
        Main.map.repaint();
    }

    // Find ways to which the disconnect can be applied. This is the list of ways with more
    // than two nodes which pass through all the given nodes, intersected with the selected ways (if any)
    private List<Way> getApplicableWays(List<Way> selectedWays, List<Node> selectedNodes) {
        if (selectedNodes.isEmpty())
            return null;

        // List of ways shared by all nodes
        List<Way> result = new ArrayList<Way>(OsmPrimitive.getFilteredList(selectedNodes.get(0).getReferrers(), Way.class));
        for (int i=1; i<selectedNodes.size(); i++) {
            List<OsmPrimitive> ref = selectedNodes.get(i).getReferrers();
            for (Iterator<Way> it = result.iterator(); it.hasNext(); ) {
                if (!ref.contains(it.next())) {
                    it.remove();
                }
            }
        }

        // Remove broken ways
        for (Iterator<Way> it = result.iterator(); it.hasNext(); ) {
            if (it.next().getNodesCount() <= 2) {
                it.remove();
            }
        }

        if (selectedWays.isEmpty())
            return result;
        else {
            // Return only selected ways
            for (Iterator<Way> it = result.iterator(); it.hasNext(); ) {
                if (!selectedWays.contains(it.next())) {
                    it.remove();
                }
            }
            return result;
        }
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
        setEnabled(selection != null && !selection.isEmpty());
    }
}
