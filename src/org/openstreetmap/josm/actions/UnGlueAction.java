// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.AddCommand;
import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Duplicate nodes that are used by multiple ways.
 *
 * Resulting nodes are identical, up to their position.
 *
 * This is the opposite of the MergeNodesAction.
 *
 * If a single node is selected, it will copy that node and remove all tags from the old one
 */
public class UnGlueAction extends JosmAction {

    private Node selectedNode;
    private Way selectedWay;
    private Set<Node> selectedNodes;

    /**
     * Create a new UnGlueAction.
     */
    public UnGlueAction() {
        super(tr("UnGlue Ways"), "unglueways", tr("Duplicate nodes that are used by multiple ways."),
                Shortcut.registerShortcut("tools:unglue", tr("Tool: {0}", tr("UnGlue Ways")), KeyEvent.VK_G, Shortcut.DIRECT), true);
        putValue("help", ht("/Action/UnGlue"));
    }

    /**
     * Called when the action is executed.
     *
     * This method does some checking on the selection and calls the matching unGlueWay method.
     */
    @Override
    public void actionPerformed(ActionEvent e) {

        Collection<OsmPrimitive> selection = getCurrentDataSet().getSelected();

        String errMsg = null;
        int errorTime = Notification.TIME_DEFAULT;
        if (checkSelection(selection)) {
            if (!checkAndConfirmOutlyingUnglue()) {
                return;
            }
            int count = 0;
            for (Way w : OsmPrimitive.getFilteredList(selectedNode.getReferrers(), Way.class)) {
                if (!w.isUsable() || w.getNodesCount() < 1) {
                    continue;
                }
                count++;
            }
            if (count < 2) {
                // If there aren't enough ways, maybe the user wanted to unglue the nodes
                // (= copy tags to a new node)
                if (checkForUnglueNode(selection)) {
                    unglueNode(e);
                } else {
                    errorTime = Notification.TIME_SHORT;
                    errMsg = tr("This node is not glued to anything else.");
                }
            } else {
                // and then do the work.
                unglueWays();
            }
        } else if (checkSelection2(selection)) {
            if (!checkAndConfirmOutlyingUnglue()) {
                return;
            }
            Set<Node> tmpNodes = new HashSet<Node>();
            for (Node n : selectedNodes) {
                int count = 0;
                for (Way w : OsmPrimitive.getFilteredList(n.getReferrers(), Way.class)) {
                    if (!w.isUsable()) {
                        continue;
                    }
                    count++;
                }
                if (count >= 2) {
                    tmpNodes.add(n);
                }
            }
            if (tmpNodes.size() < 1) {
                if (selection.size() > 1) {
                    errMsg =  tr("None of these nodes are glued to anything else.");
                } else {
                    errMsg = tr("None of this way''s nodes are glued to anything else.");
                }
            } else {
                // and then do the work.
                selectedNodes = tmpNodes;
                unglueWays2();
            }
        } else {
            errorTime = Notification.TIME_VERY_LONG;
            errMsg =
                tr("The current selection cannot be used for unglueing.")+"\n"+
                "\n"+
                tr("Select either:")+"\n"+
                tr("* One tagged node, or")+"\n"+
                tr("* One node that is used by more than one way, or")+"\n"+
                tr("* One node that is used by more than one way and one of those ways, or")+"\n"+
                tr("* One way that has one or more nodes that are used by more than one way, or")+"\n"+
                tr("* One way and one or more of its nodes that are used by more than one way.")+"\n"+
                "\n"+
                tr("Note: If a way is selected, this way will get fresh copies of the unglued\n"+
                        "nodes and the new nodes will be selected. Otherwise, all ways will get their\n"+
                "own copy and all nodes will be selected.");
        }

        if(errMsg != null) {
            new Notification(
                    errMsg)
                    .setIcon(JOptionPane.ERROR_MESSAGE)
                    .setDuration(errorTime)
                    .show();
        }

        selectedNode = null;
        selectedWay = null;
        selectedNodes = null;
    }

    /**
     * Assumes there is one tagged Node stored in selectedNode that it will try to unglue.
     * (i.e. copy node and remove all tags from the old one. Relations will not be removed)
     */
    private void unglueNode(ActionEvent e) {
        LinkedList<Command> cmds = new LinkedList<Command>();

        Node c = new Node(selectedNode);
        c.removeAll();
        getCurrentDataSet().clearSelection(c);
        cmds.add(new ChangeCommand(selectedNode, c));

        Node n = new Node(selectedNode, true);

        // If this wasn't called from menu, place it where the cursor is/was
        if(e.getSource() instanceof JPanel) {
            MapView mv = Main.map.mapView;
            n.setCoor(mv.getLatLon(mv.lastMEvent.getX(), mv.lastMEvent.getY()));
        }

        cmds.add(new AddCommand(n));

        fixRelations(selectedNode, cmds, Collections.singletonList(n));

        Main.main.undoRedo.add(new SequenceCommand(tr("Unglued Node"), cmds));
        getCurrentDataSet().setSelected(n);
        Main.map.mapView.repaint();
    }

    /**
     * Checks if selection is suitable for ungluing. This is the case when there's a single,
     * tagged node selected that's part of at least one way (ungluing an unconnected node does
     * not make sense. Due to the call order in actionPerformed, this is only called when the
     * node is only part of one or less ways.
     *
     * @param selection The selection to check against
     * @return {@code true} if selection is suitable
     */
    private boolean checkForUnglueNode(Collection<? extends OsmPrimitive> selection) {
        if (selection.size() != 1)
            return false;
        OsmPrimitive n = (OsmPrimitive) selection.toArray()[0];
        if (!(n instanceof Node))
            return false;
        if (OsmPrimitive.getFilteredList(n.getReferrers(), Way.class).isEmpty())
            return false;

        selectedNode = (Node) n;
        return selectedNode.isTagged();
    }

    /**
     * Checks if the selection consists of something we can work with.
     * Checks only if the number and type of items selected looks good.
     *
     * If this method returns "true", selectedNode and selectedWay will
     * be set.
     *
     * Returns true if either one node is selected or one node and one
     * way are selected and the node is part of the way.
     *
     * The way will be put into the object variable "selectedWay", the
     * node into "selectedNode".
     */
    private boolean checkSelection(Collection<? extends OsmPrimitive> selection) {

        int size = selection.size();
        if (size < 1 || size > 2)
            return false;

        selectedNode = null;
        selectedWay = null;

        for (OsmPrimitive p : selection) {
            if (p instanceof Node) {
                selectedNode = (Node) p;
                if (size == 1 || selectedWay != null)
                    return size == 1 || selectedWay.containsNode(selectedNode);
            } else if (p instanceof Way) {
                selectedWay = (Way) p;
                if (size == 2 && selectedNode != null)
                    return selectedWay.containsNode(selectedNode);
            }
        }

        return false;
    }

    /**
     * Checks if the selection consists of something we can work with.
     * Checks only if the number and type of items selected looks good.
     *
     * Returns true if one way and any number of nodes that are part of
     * that way are selected. Note: "any" can be none, then all nodes of
     * the way are used.
     *
     * The way will be put into the object variable "selectedWay", the
     * nodes into "selectedNodes".
     */
    private boolean checkSelection2(Collection<? extends OsmPrimitive> selection) {
        if (selection.size() < 1)
            return false;

        selectedWay = null;
        for (OsmPrimitive p : selection) {
            if (p instanceof Way) {
                if (selectedWay != null)
                    return false;
                selectedWay = (Way) p;
            }
        }
        if (selectedWay == null)
            return false;

        selectedNodes = new HashSet<Node>();
        for (OsmPrimitive p : selection) {
            if (p instanceof Node) {
                Node n = (Node) p;
                if (!selectedWay.containsNode(n))
                    return false;
                selectedNodes.add(n);
            }
        }

        if (selectedNodes.size() < 1) {
            selectedNodes.addAll(selectedWay.getNodes());
        }

        return true;
    }

    /**
     * dupe the given node of the given way
     *
     * assume that OrginalNode is in the way
     * <ul>
     * <li>the new node will be put into the parameter newNodes.</li>
     * <li>the add-node command will be put into the parameter cmds.</li>
     * <li>the changed way will be returned and must be put into cmds by the caller!</li>
     * </ul>
     */
    private Way modifyWay(Node originalNode, Way w, List<Command> cmds, List<Node> newNodes) {
        // clone the node for the way
        Node newNode = new Node(originalNode, true /* clear OSM ID */);
        newNodes.add(newNode);
        cmds.add(new AddCommand(newNode));

        List<Node> nn = new ArrayList<Node>();
        for (Node pushNode : w.getNodes()) {
            if (originalNode == pushNode) {
                pushNode = newNode;
            }
            nn.add(pushNode);
        }
        Way newWay = new Way(w);
        newWay.setNodes(nn);

        return newWay;
    }

    /**
     * put all newNodes into the same relation(s) that originalNode is in
     */
    private void fixRelations(Node originalNode, List<Command> cmds, List<Node> newNodes) {
        // modify all relations containing the node
        Relation newRel = null;
        HashSet<String> rolesToReAdd = null;
        for (Relation r : OsmPrimitive.getFilteredList(originalNode.getReferrers(), Relation.class)) {
            if (r.isDeleted()) {
                continue;
            }
            newRel = null;
            rolesToReAdd = null;
            for (RelationMember rm : r.getMembers()) {
                if (rm.isNode()) {
                    if (rm.getMember() == originalNode) {
                        if (newRel == null) {
                            newRel = new Relation(r);
                            rolesToReAdd = new HashSet<String>();
                        }
                        rolesToReAdd.add(rm.getRole());
                    }
                }
            }
            if (newRel != null) {
                for (Node n : newNodes) {
                    for (String role : rolesToReAdd) {
                        newRel.addMember(new RelationMember(role, n));
                    }
                }
                cmds.add(new ChangeCommand(r, newRel));
            }
        }
    }

    /**
     * dupe a single node into as many nodes as there are ways using it, OR
     *
     * dupe a single node once, and put the copy on the selected way
     */
    private void unglueWays() {
        LinkedList<Command> cmds = new LinkedList<Command>();
        LinkedList<Node> newNodes = new LinkedList<Node>();

        if (selectedWay == null) {
            Way wayWithSelectedNode = null;
            LinkedList<Way> parentWays = new LinkedList<Way>();
            for (OsmPrimitive osm : selectedNode.getReferrers()) {
                if (osm.isUsable() && osm instanceof Way) {
                    Way w = (Way) osm;
                    if (wayWithSelectedNode == null && !w.isFirstLastNode(selectedNode)) {
                        wayWithSelectedNode = w;
                    } else {
                        parentWays.add(w);
                    }
                }
            }
            if (wayWithSelectedNode == null) {
                parentWays.removeFirst();
            }
            for (Way w : parentWays) {
                cmds.add(new ChangeCommand(w, modifyWay(selectedNode, w, cmds, newNodes)));
            }
        } else {
            cmds.add(new ChangeCommand(selectedWay, modifyWay(selectedNode, selectedWay, cmds, newNodes)));
        }

        fixRelations(selectedNode, cmds, newNodes);

        Main.main.undoRedo.add(new SequenceCommand(/* for correct i18n of plural forms - see #9110 */
                trn("Dupe into {0} node", "Dupe into {0} nodes", newNodes.size() + 1, newNodes.size() + 1), cmds));
        // select one of the new nodes
        getCurrentDataSet().setSelected(newNodes.getFirst());
    }

    /**
     * dupe all nodes that are selected, and put the copies on the selected way
     *
     */
    private void unglueWays2() {
        LinkedList<Command> cmds = new LinkedList<Command>();
        List<Node> allNewNodes = new LinkedList<Node>();
        Way tmpWay = selectedWay;

        for (Node n : selectedNodes) {
            List<Node> newNodes = new LinkedList<Node>();
            tmpWay = modifyWay(n, tmpWay, cmds, newNodes);
            fixRelations(n, cmds, newNodes);
            allNewNodes.addAll(newNodes);
        }
        cmds.add(new ChangeCommand(selectedWay, tmpWay)); // only one changeCommand for a way, else garbage will happen

        Main.main.undoRedo.add(new SequenceCommand(
                trn("Dupe {0} node into {1} nodes", "Dupe {0} nodes into {1} nodes", selectedNodes.size(), selectedNodes.size(), selectedNodes.size()+allNewNodes.size()), cmds));
        getCurrentDataSet().setSelected(allNewNodes);
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

    protected boolean checkAndConfirmOutlyingUnglue() {
        List<OsmPrimitive> primitives = new ArrayList<OsmPrimitive>(2 + (selectedNodes == null ? 0 : selectedNodes.size()));
        if (selectedNodes != null)
            primitives.addAll(selectedNodes);
        if (selectedNode != null)
            primitives.add(selectedNode);
        return Command.checkAndConfirmOutlyingOperation("unglue",
                tr("Unglue confirmation"),
                tr("You are about to unglue nodes outside of the area you have downloaded."
                        + "<br>"
                        + "This can cause problems because other objects (that you do not see) might use them."
                        + "<br>"
                        + "Do you really want to unglue?"),
                tr("You are about to unglue incomplete objects."
                        + "<br>"
                        + "This will cause problems because you don''t see the real object."
                        + "<br>" + "Do you really want to unglue?"),
                primitives, null);
    }
}
