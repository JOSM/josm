// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.command.RemoveNodesCommand;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Disconnect nodes from a way they currently belong to.
 * @since 6253
 */
public class UnJoinNodeWayAction extends JosmAction {

    /**
     * Constructs a new {@code UnJoinNodeWayAction}.
     */
    public UnJoinNodeWayAction() {
        super(tr("Disconnect Node from Way"), "unjoinnodeway",
                tr("Disconnect nodes from a way they currently belong to"),
                Shortcut.registerShortcut("tools:unjoinnodeway",
                    tr("Tool: {0}", tr("Disconnect Node from Way")), KeyEvent.VK_J, Shortcut.ALT), true);
        setHelpId(ht("/Action/UnJoinNodeWay"));
    }

    /**
     * Called when the action is executed.
     */
    @Override
    public void actionPerformed(ActionEvent e) {

        final DataSet dataSet = getLayerManager().getEditDataSet();
        List<Node> selectedNodes = new ArrayList<>(dataSet.getSelectedNodes());
        List<Way> selectedWays = new ArrayList<>(dataSet.getSelectedWays());

        selectedNodes = cleanSelectedNodes(selectedWays, selectedNodes);

        List<Way> applicableWays = getApplicableWays(selectedWays, selectedNodes);

        if (applicableWays == null) {
            notify(tr("Select at least one node to be disconnected."),
                   JOptionPane.WARNING_MESSAGE);
            return;
        } else if (applicableWays.isEmpty()) {
            notify(trn("Selected node cannot be disconnected from anything.",
                       "Selected nodes cannot be disconnected from anything.",
                       selectedNodes.size()),
                   JOptionPane.WARNING_MESSAGE);
            return;
        } else if (applicableWays.size() > 1) {
            notify(trn("There is more than one way using the node you selected. "
                       + "Please select the way also.",
                       "There is more than one way using the nodes you selected. "
                       + "Please select the way also.",
                       selectedNodes.size()),
                   JOptionPane.WARNING_MESSAGE);
            return;
        } else if (applicableWays.get(0).getRealNodesCount() < selectedNodes.size() + 2) {
            // there is only one affected way, but removing the selected nodes would only leave it
            // with less than 2 nodes
            notify(trn("The affected way would disappear after disconnecting the "
                       + "selected node.",
                       "The affected way would disappear after disconnecting the "
                       + "selected nodes.",
                       selectedNodes.size()),
                   JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Finally, applicableWays contains only one perfect way
        Way selectedWay = applicableWays.get(0);

        // I'm sure there's a better way to handle this
        UndoRedoHandler.getInstance().add(
                new RemoveNodesCommand(selectedWay, new HashSet<>(selectedNodes)));
    }

    /**
     * Send a notification message.
     * @param msg Message to be sent.
     * @param messageType Nature of the message.
     */
    public void notify(String msg, int messageType) {
        new Notification(msg).setIcon(messageType).show();
    }

    /**
     * Removes irrelevant nodes from user selection.
     *
     * The action can be performed reliably even if we remove :
     *   * Nodes not referenced by any ways
     *   * When only one way is selected, nodes not part of this way (#10396).
     *
     * @param selectedWays  List of user selected way.
     * @param selectedNodes List of user selected nodes.
     * @return New list of nodes cleaned of irrelevant nodes.
     */
    private List<Node> cleanSelectedNodes(List<Way> selectedWays,
                                          List<Node> selectedNodes) {
        List<Node> resultingNodes = new LinkedList<>();

        // List of node referenced by a route
        for (Node n: selectedNodes) {
            if (n.isReferredByWays(1)) {
                resultingNodes.add(n);
            }
        }
        // If exactly one selected way, remove node not referencing par this way.
        if (selectedWays.size() == 1) {
            Way w = selectedWays.get(0);
            for (Node n: new ArrayList<>(resultingNodes)) {
                if (!w.containsNode(n)) {
                    resultingNodes.remove(n);
                }
            }
        }
        // Warn if nodes were removed
        if (resultingNodes.size() != selectedNodes.size()) {
            notify(tr("Some irrelevant nodes have been removed from the selection"),
                   JOptionPane.INFORMATION_MESSAGE);
        }
        return resultingNodes;
    }

    /**
     * Find ways to which the disconnect can be applied. This is the list of ways
     * with more than two nodes which pass through all the given nodes, intersected
     * with the selected ways (if any)
     * @param selectedWays List of user selected ways.
     * @param selectedNodes List of user selected nodes.
     * @return List of relevant ways
     */
    static List<Way> getApplicableWays(List<Way> selectedWays, List<Node> selectedNodes) {
        if (selectedNodes.isEmpty())
            return null;

        // List of ways shared by all nodes
        List<Way> result = new ArrayList<>(selectedNodes.get(0).getParentWays());
        for (int i = 1; i < selectedNodes.size(); i++) {
            List<Way> ref = selectedNodes.get(i).getParentWays();
            result.removeIf(way -> !ref.contains(way));
        }

        // Remove broken ways
        result.removeIf(way -> way.getNodesCount() <= 2);

        if (selectedWays.isEmpty())
            return result;
        else {
            // Return only selected ways
            result.removeIf(way -> !selectedWays.contains(way));
            return result;
        }
    }

    @Override
    protected void updateEnabledState() {
        updateEnabledStateOnCurrentSelection();
    }

    @Override
    protected void updateEnabledState(Collection<? extends OsmPrimitive> selection) {
        updateEnabledStateOnModifiableSelection(selection);
    }
}
