// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

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
import org.openstreetmap.josm.gui.DefaultNameFormatter;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Splits a way into multiple ways (all identical except for their node list).
 *
 * Ways are just split at the selected nodes.  The nodes remain in their
 * original order.  Selected nodes at the end of a way are ignored.
 */

public class SplitWayAction extends JosmAction {


    public static class SplitWayResult {
        private final Command command;
        private final List<? extends PrimitiveId> newSelection;
        private Way originalWay;
        private List<Way> newWays;

        public SplitWayResult(Command command, List<? extends PrimitiveId> newSelection, Way originalWay, List<Way> newWays) {
            this.command = command;
            this.newSelection = newSelection;
            this.originalWay = originalWay;
            this.newWays = newWays;
        }

        public Command getCommand() {
            return command;
        }

        public List<? extends PrimitiveId> getNewSelection() {
            return newSelection;
        }

        public Way getOriginalWay() {
            return originalWay;
        }

        public List<Way> getNewWays() {
            return newWays;
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

        List<Node> selectedNodes = OsmPrimitive.getFilteredList(selection, Node.class);
        List<Way> selectedWays = OsmPrimitive.getFilteredList(selection, Way.class);
        List<Relation> selectedRelations = OsmPrimitive.getFilteredList(selection, Relation.class);
        List<Way> applicableWays = getApplicableWays(selectedWays, selectedNodes);

        if (applicableWays == null) {
            JOptionPane.showMessageDialog(
                    Main.parent,
                    tr("The current selection cannot be used for splitting - no node is selected."),
                    tr("Warning"),
                    JOptionPane.WARNING_MESSAGE);
            return;
        } else if (applicableWays.isEmpty()) {
            JOptionPane.showMessageDialog(Main.parent,
                    tr("The selected nodes do not share the same way."),
                    tr("Warning"),
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        { // Remove ways that doesn't have selected node in the middle
            Iterator<Way> it = applicableWays.iterator();
            WAY_LOOP:
                while (it.hasNext()) {
                    Way w = it.next();
                    assert w.isUsable(); // Way is referrer of selected node(s) so it must be usable
                    int last = w.getNodesCount() - 1;
                    boolean circular = w.isClosed();

                    for (Node n : selectedNodes) {
                        int i = w.getNodes().indexOf(n);
                        if (!(circular || (i > 0 && i < last))) {
                            it.remove();
                            continue WAY_LOOP;
                        }
                    }
                }
        }

        if (applicableWays.isEmpty()) {
            JOptionPane.showMessageDialog(Main.parent,
                    trn("The selected node is not in the middle of any way.",
                            "The selected nodes are not in the middle of any way.",
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
        }

        // Finally, applicableWays contains only one perfect way
        Way selectedWay = applicableWays.get(0);

        List<List<Node>> wayChunks = buildSplitChunks(selectedWay, selectedNodes);
        if (wayChunks != null) {
            List<OsmPrimitive> sel = new ArrayList<OsmPrimitive>(selectedWays.size() + selectedRelations.size());
            sel.addAll(selectedWays);
            sel.addAll(selectedRelations);
            SplitWayResult result = splitWay(getEditLayer(),selectedWay, wayChunks, sel);
            Main.main.undoRedo.add(result.getCommand());
            getCurrentDataSet().setSelected(result.getNewSelection());
        }
    }

    private List<Way> getApplicableWays(List<Way> selectedWays, List<Node> selectedNodes) {
        if (selectedNodes.isEmpty())
            return null;

        // List of ways shared by all nodes
        List<Way> result = new ArrayList<Way>(OsmPrimitive.getFilteredList(selectedNodes.get(0).getReferrers(), Way.class));
        for (int i=1; i<selectedNodes.size(); i++) {
            Iterator<Way> it = result.iterator();
            List<OsmPrimitive> ref = selectedNodes.get(i).getReferrers();
            while (it.hasNext()) {
                if (!ref.contains(it.next())) {
                    it.remove();
                }
            }
        }

        { // Remove broken ways
            Iterator<Way> it = result.iterator();
            while (it.hasNext()) {
                if (it.next().getNodesCount() <= 2) {
                    it.remove();
                }
            }
        }

        if (selectedWays.isEmpty())
            return result;
        else {
            // Return only selected ways
            Iterator<Way> it = result.iterator();
            while (it.hasNext()) {
                if (!selectedWays.contains(it.next())) {
                    it.remove();
                }
            }
            return result;
        }

    }

    /**
     * Splits the nodes of {@code wayToSplit} into a list of node sequences
     * which are separated at the nodes in {@code splitPoints}.
     *
     * This method displays warning messages if {@code wayToSplit} and/or
     * {@code splitPoints} aren't consistent.
     *
     * Returns null, if building the split chunks fails.
     *
     * @param wayToSplit the way to split. Must not be null.
     * @param splitPoints the nodes where the way is split. Must not be null.
     * @return the list of chunks
     */
    static public List<List<Node>> buildSplitChunks(Way wayToSplit, List<Node> splitPoints){
        CheckParameterUtil.ensureParameterNotNull(wayToSplit, "wayToSplit");
        CheckParameterUtil.ensureParameterNotNull(splitPoints, "splitPoints");

        Set<Node> nodeSet = new HashSet<Node>(splitPoints);
        List<List<Node>> wayChunks = new LinkedList<List<Node>>();
        List<Node> currentWayChunk = new ArrayList<Node>();
        wayChunks.add(currentWayChunk);

        Iterator<Node> it = wayToSplit.getNodes().iterator();
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
                return null;
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
            return null;
        }
        return wayChunks;
    }

    /**
     * Splits a way
     * @param layer
     * @param way
     * @param wayChunks
     * @return
     */
    public static SplitWayResult splitWay(OsmDataLayer layer, Way way, List<List<Node>> wayChunks, Collection<? extends OsmPrimitive> selection) {
        // build a list of commands, and also a new selection list
        Collection<Command> commandList = new ArrayList<Command>(wayChunks.size());
        List<OsmPrimitive> newSelection = new ArrayList<OsmPrimitive>(selection.size() + wayChunks.size());
        newSelection.addAll(selection);

        Iterator<List<Node>> chunkIt = wayChunks.iterator();

        // First, change the original way
        Way changedWay = new Way(way);
        changedWay.setNodes(chunkIt.next());
        commandList.add(new ChangeCommand(way, changedWay));
        if (!newSelection.contains(way)) {
            newSelection.add(way);
        }

        List<Way> newWays = new ArrayList<Way>();
        // Second, create new ways
        while (chunkIt.hasNext()) {
            Way wayToAdd = new Way();
            wayToAdd.setKeys(way.getKeys());
            newWays.add(wayToAdd);
            wayToAdd.setNodes(chunkIt.next());
            commandList.add(new AddCommand(layer,wayToAdd));
            newSelection.add(wayToAdd);

        }
        boolean warnmerole = false;
        boolean warnme = false;
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

            int i_c = 0, i_r = 0;
            List<RelationMember> relationMembers = r.getMembers();
            for (RelationMember rm: relationMembers) {
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
                            } else {
                                insert = false;
                            }
                        }
                        else if(!"via".equals(role)) {
                            warnme = true;
                        }
                    }
                    else if (!("route".equals(type)) && !("multipolygon".equals(type))) {
                        warnme = true;
                    }
                    if (c == null) {
                        c = new Relation(r);
                    }

                    if(insert)
                    {
                        if (rm.hasRole() && !("multipolygon".equals(type))) {
                            warnmerole = true;
                        }

                        Boolean backwards = null;
                        int k = 1;
                        while (i_r - k >= 0 || i_r + k < relationMembers.size()) {
                            if ((i_r - k >= 0) && relationMembers.get(i_r - k).isWay()){
                                Way w = relationMembers.get(i_r - k).getWay();
                                if ((w.lastNode() == way.firstNode()) || w.firstNode() == way.firstNode()) {
                                    backwards = false;
                                } else if ((w.firstNode() == way.lastNode()) || w.lastNode() == way.lastNode()) {
                                    backwards = true;
                                }
                                break;
                            }
                            if ((i_r + k < relationMembers.size()) && relationMembers.get(i_r + k).isWay()){
                                Way w = relationMembers.get(i_r + k).getWay();
                                if ((w.lastNode() == way.firstNode()) || w.firstNode() == way.firstNode()) {
                                    backwards = true;
                                } else if ((w.firstNode() == way.lastNode()) || w.lastNode() == way.lastNode()) {
                                    backwards = false;
                                }
                                break;
                            }
                            k++;
                        }

                        int j = i_c;
                        for (Way wayToAdd : newWays) {
                            RelationMember em = new RelationMember(rm.getRole(), wayToAdd);
                            j++;
                            if ((backwards != null) && backwards) {
                                c.addMember(i_c, em);
                            } else {
                                c.addMember(j, em);
                            }
                        }
                        i_c = j;
                    }
                }
                i_c++; i_r++;
            }

            if (c != null) {
                commandList.add(new ChangeCommand(layer,r, c));
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

        return new SplitWayResult(
                new SequenceCommand(
                        tr("Split way {0} into {1} parts", way.getDisplayName(DefaultNameFormatter.getInstance()),wayChunks.size()),
                        commandList
                ),
                newSelection,
                way,
                newWays
        );
    }

    /**
     * Splits the way {@code way} at the nodes in {@code atNodes} and replies
     * the result of this process in an instance of {@see SplitWayResult}.
     *
     * Note that changes are not applied to the data yet. You have to
     * submit the command in {@see SplitWayResult#getCommand()} first,
     * i.e. {@code Main.main.undoredo.add(result.getCommand())}.
     *
     * Replies null if the way couldn't be split at the given nodes.
     *
     * @param layer the layer which the way belongs to. Must not be null.
     * @param way the way to split. Must not be null.
     * @param atNodes the list of nodes where the way is split. Must not be null.
     * @return the result from the split operation
     */
    static public SplitWayResult split(OsmDataLayer layer, Way way, List<Node> atNodes, Collection<? extends OsmPrimitive> selection){
        List<List<Node>> chunks = buildSplitChunks(way, atNodes);
        if (chunks == null) return null;
        return splitWay(layer,way, chunks, selection);
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
        for (OsmPrimitive primitive: selection) {
            if (primitive instanceof Node) {
                setEnabled(true); // Selection still can be wrong, but let SplitWayAction process and tell user what's wrong
                return;
            }
        }
        setEnabled(false);
    }
}
