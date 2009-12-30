// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.AddCommand;
import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.PrimitiveId;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.AbstractVisitor;
import org.openstreetmap.josm.data.osm.visitor.Visitor;
import org.openstreetmap.josm.gui.DefaultNameFormatter;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Splits a way into multiple ways (all identical except for their node list).
 *
 * Ways are just split at the selected nodes.  The nodes remain in their
 * original order.  Selected nodes at the end of a way are ignored.
 */

public class SplitWayAction extends JosmAction {

    private Way selectedWay;
    private List<Node> selectedNodes;

    public static class SplitWayResult {
        private final Command command;
        private final List<? extends PrimitiveId> newSelection;

        public SplitWayResult(Command command, List<? extends PrimitiveId> newSelection) {
            this.command = command;
            this.newSelection = newSelection;
        }

        public Command getCommand() {
            return command;
        }

        public List<? extends PrimitiveId> getNewSelection() {
            return newSelection;
        }
    }

    /**
     * Create a new SplitWayAction.
     */
    public SplitWayAction() {
        super(tr("Split Way"), "splitway", tr("Split a way at the selected node."),
                Shortcut.registerShortcut("tools:splitway", tr("Tool: {0}", tr("Split Way")), KeyEvent.VK_P, Shortcut.GROUP_EDIT), true);
        putValue("help", ht("/Action/SplitWay"));
    }

    /**
     * Called when the action is executed.
     *
     * This method performs an expensive check whether the selection clearly defines one
     * of the split actions outlined above, and if yes, calls the splitWay method.
     */
    public void actionPerformed(ActionEvent e) {

        Collection<OsmPrimitive> selection = getCurrentDataSet().getSelected();

        if (!checkSelection(selection)) {
            JOptionPane.showMessageDialog(
                    Main.parent,
                    tr("The current selection cannot be used for splitting."),
                    tr("Warning"),
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        selectedWay = null;
        selectedNodes = null;

        Visitor splitVisitor = new AbstractVisitor() {
            public void visit(Node n) {
                if (selectedNodes == null) {
                    selectedNodes = new LinkedList<Node>();
                }
                selectedNodes.add(n);
            }
            public void visit(Way w) {
                selectedWay = w;
            }
            public void visit(Relation e) {
                // enties are not considered
            }
        };

        for (OsmPrimitive p : selection) {
            p.visit(splitVisitor);
        }

        // If only nodes are selected, try to guess which way to split. This works if there
        // is exactly one way that all nodes are part of.
        if (selectedWay == null && selectedNodes != null) {
            Map<Way, Integer> wayOccurenceCounter = new HashMap<Way, Integer>();
            for (Node n : selectedNodes) {
                for (Way w : OsmPrimitive.getFilteredList(n.getReferrers(), Way.class)) {
                    if (!w.isUsable()) {
                        continue;
                    }
                    int last = w.getNodesCount() - 1;
                    if (last <= 0) {
                        continue; // zero or one node ways
                    }
                    boolean circular = w.isClosed();
                    int i = 0;
                    for (Node wn : w.getNodes()) {
                        if ((circular || (i > 0 && i < last)) && n.equals(wn)) {
                            Integer old = wayOccurenceCounter.get(w);
                            wayOccurenceCounter.put(w, (old == null) ? 1 : old + 1);
                            break;
                        }
                        i++;
                    }
                }
            }
            if (wayOccurenceCounter.isEmpty()) {
                JOptionPane.showMessageDialog(Main.parent,
                        trn("The selected node is not in the middle of any way.",
                                "The selected nodes are not in the middle of any way.",
                                selectedNodes.size()),
                                tr("Warning"),
                                JOptionPane.WARNING_MESSAGE);
                return;
            }

            for (Entry<Way, Integer> entry : wayOccurenceCounter.entrySet()) {
                if (entry.getValue().equals(selectedNodes.size())) {
                    if (selectedWay != null) {
                        JOptionPane.showMessageDialog(Main.parent,
                                trn("There is more than one way using the node you selected. Please select the way also.",
                                        "There is more than one way using the nodes you selected. Please select the way also.",
                                        selectedNodes.size()),
                                        tr("Warning"),
                                        JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    selectedWay = entry.getKey();
                }
            }

            if (selectedWay == null) {
                JOptionPane.showMessageDialog(Main.parent,
                        tr("The selected nodes do not share the same way."),
                        tr("Warning"),
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            // If a way and nodes are selected, verify that the nodes are part of the way.
        } else if (selectedWay != null && selectedNodes != null) {

            HashSet<Node> nds = new HashSet<Node>(selectedNodes);
            for (Node n : selectedWay.getNodes()) {
                nds.remove(n);
            }
            if (!nds.isEmpty()) {
                JOptionPane.showMessageDialog(Main.parent,
                        trn("The selected way does not contain the selected node.",
                                "The selected way does not contain all the selected nodes.",
                                selectedNodes.size()),
                                tr("Warning"),
                                JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        // and then do the work.
        splitWay();
    }

    /**
     * Checks if the selection consists of something we can work with.
     * Checks only if the number and type of items selected looks good;
     * does not check whether the selected items are really a valid
     * input for splitting (this would be too expensive to be carried
     * out from the selectionChanged listener).
     */
    private boolean checkSelection(Collection<? extends OsmPrimitive> selection) {
        boolean way = false;
        boolean node = false;
        for (OsmPrimitive p : selection) {
            if (p instanceof Way && !way) {
                way = true;
            } else if (p instanceof Node) {
                node = true;
            } else
                return false;
        }
        return node;
    }

    /**
     * Split a way into two or more parts, starting at a selected node.
     */
    private void splitWay() {
        // We take our way's list of nodes and copy them to a way chunk (a
        // list of nodes).  Whenever we stumble upon a selected node, we start
        // a new way chunk.

        Set<Node> nodeSet = new HashSet<Node>(selectedNodes);
        List<List<Node>> wayChunks = new LinkedList<List<Node>>();
        List<Node> currentWayChunk = new ArrayList<Node>();
        wayChunks.add(currentWayChunk);

        Iterator<Node> it = selectedWay.getNodes().iterator();
        while (it.hasNext()) {
            Node currentNode = it.next();
            boolean atEndOfWay = currentWayChunk.isEmpty() || !it.hasNext();
            currentWayChunk.add(currentNode);
            if (nodeSet.contains(currentNode) && !atEndOfWay) {
                currentWayChunk = new ArrayList<Node>();
                currentWayChunk.add(currentNode);
                wayChunks.add(currentWayChunk);
            }
        }

        // Handle circular ways specially.
        // If you split at a circular way at two nodes, you just want to split
        // it at these points, not also at the former endpoint.
        // So if the last node is the same first node, join the last and the
        // first way chunk.
        List<Node> lastWayChunk = wayChunks.get(wayChunks.size() - 1);
        if (wayChunks.size() >= 2
                && wayChunks.get(0).get(0) == lastWayChunk.get(lastWayChunk.size() - 1)
                && !nodeSet.contains(wayChunks.get(0).get(0))) {
            if (wayChunks.size() == 2) {
                JOptionPane.showMessageDialog(
                        Main.parent,
                        tr("You must select two or more nodes to split a circular way."),
                        tr("Warning"),
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            lastWayChunk.remove(lastWayChunk.size() - 1);
            lastWayChunk.addAll(wayChunks.get(0));
            wayChunks.remove(wayChunks.size() - 1);
            wayChunks.set(0, lastWayChunk);
        }

        if (wayChunks.size() < 2) {
            if (wayChunks.get(0).get(0) == wayChunks.get(0).get(wayChunks.get(0).size() - 1)) {
                JOptionPane.showMessageDialog(
                        Main.parent,
                        tr("You must select two or more nodes to split a circular way."),
                        tr("Warning"),
                        JOptionPane.WARNING_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(
                        Main.parent,
                        tr("The way cannot be split at the selected nodes. (Hint: Select nodes in the middle of the way.)"),
                        tr("Warning"),
                        JOptionPane.WARNING_MESSAGE);
            }
            return;
        }
        //Main.debug("wayChunks.size(): " + wayChunks.size());
        //Main.debug("way id: " + selectedWay.id);

        SplitWayResult result = splitWay(selectedWay, wayChunks);
        Main.main.undoRedo.add(result.getCommand());
        getCurrentDataSet().setSelected(result.getNewSelection());
    }

    public static SplitWayResult splitWay(Way way, List<List<Node>> wayChunks) {
        // build a list of commands, and also a new selection list
        Collection<Command> commandList = new ArrayList<Command>(wayChunks.size());
        List<Way> newSelection = new ArrayList<Way>(wayChunks.size());

        Iterator<List<Node>> chunkIt = wayChunks.iterator();

        // First, change the original way
        Way changedWay = new Way(way);
        changedWay.setNodes(chunkIt.next());
        commandList.add(new ChangeCommand(way, changedWay));
        newSelection.add(way);

        Collection<Way> newWays = new ArrayList<Way>();
        // Second, create new ways
        while (chunkIt.hasNext()) {
            Way wayToAdd = new Way();
            wayToAdd.setKeys(way.getKeys());
            newWays.add(wayToAdd);
            wayToAdd.setNodes(chunkIt.next());
            commandList.add(new AddCommand(wayToAdd));
            //Main.debug("wayToAdd: " + wayToAdd);
            newSelection.add(wayToAdd);

        }
        Boolean warnmerole = false;
        Boolean warnme = false;
        // now copy all relations to new way also

        for (Relation r : OsmPrimitive.getFilteredList(way.getReferrers(), Relation.class)) {
            if (!r.isUsable()) {
                continue;
            }
            Relation c = null;
            String type = r.get("type");
            if (type == null) {
                type = "";
            }
            int i = 0;

            for (RelationMember rm : r.getMembers()) {
                if (rm.isWay() && rm.getMember() == way) {
                    boolean insert = true;
                    if ("restriction".equals(type))
                    {
                        /* this code assumes the restriction is correct. No real error checking done */
                        String role = rm.getRole();
                        if("from".equals(role) || "to".equals(role))
                        {
                            OsmPrimitive via = null;
                            for (RelationMember rmv : r.getMembers()) {
                                if("via".equals(rmv.getRole())){
                                    via = rmv.getMember();
                                }
                            }
                            List<Node> nodes = new ArrayList<Node>();
                            if(via != null) {
                                if(via instanceof Node) {
                                    nodes.add((Node)via);
                                } else if(via instanceof Way) {
                                    nodes.add(((Way)via).lastNode());
                                    nodes.add(((Way)via).firstNode());
                                }
                            }
                            Way res = null;
                            for(Node n : nodes) {
                                if(changedWay.isFirstLastNode(n)) {
                                    res = way;
                                }
                            }
                            if(res == null)
                            {
                                for (Way wayToAdd : newWays) {
                                    for(Node n : nodes) {
                                        if(wayToAdd.isFirstLastNode(n)) {
                                            res = wayToAdd;
                                        }
                                    }
                                }
                                if(res != null)
                                {
                                    if (c == null) {
                                        c = new Relation(r);
                                    }
                                    c.addMember(new RelationMember(role, res));
                                    c.removeMembersFor(way);
                                    insert = false;
                                }
                            }
                            else
                                insert = false;
                        }
                        else if(!"via".equals(role))
                            warnme = true;
                    }
                    else if (!("route".equals(type)) && !("multipolygon".equals(type))) {
                        warnme = true;
                    }
                    if (c == null) {
                        c = new Relation(r);
                    }

                    if(insert)
                    {
                        int j = i;
                        boolean backwards = "backward".equals(rm.getRole());
                        for (Way wayToAdd : newWays) {
                            RelationMember em = new RelationMember(rm.getRole(), wayToAdd);
                            if (em.hasRole() && !("multipolygon".equals(type))) {
                                warnmerole = true;
                            }

                            j++;
                            if (backwards) {
                                c.addMember(i, em);
                            } else {
                                c.addMember(j, em);
                            }
                        }
                        i = j;
                    }
                }
                i++;
            }

            if (c != null) {
                commandList.add(new ChangeCommand(r, c));
            }
        }
        if (warnmerole) {
            JOptionPane.showMessageDialog(
                    Main.parent,
                    tr("<html>A role based relation membership was copied to all new ways.<br>You should verify this and correct it when necessary.</html>"),
                    tr("Warning"),
                    JOptionPane.WARNING_MESSAGE);
        } else if (warnme) {
            JOptionPane.showMessageDialog(
                    Main.parent,
                    tr("<html>A relation membership was copied to all new ways.<br>You should verify this and correct it when necessary.</html>"),
                    tr("Warning"),
                    JOptionPane.WARNING_MESSAGE);
        }

        return new SplitWayResult(new SequenceCommand(tr("Split way {0} into {1} parts",
                way.getDisplayName(DefaultNameFormatter.getInstance()),
                wayChunks.size()),
                commandList), newSelection);
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
        if (selection == null) {
            setEnabled(false);
            return;
        }
        setEnabled(checkSelection(selection));
    }
}
