//License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;

import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.DeleteCommand;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.TigerUtils;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.CollectBackReferencesVisitor;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.Pair;
import org.openstreetmap.josm.tools.Shortcut;


/**
 * Merge two or more nodes into one node.
 * (based on Combine ways)
 *
 * @author Matthew Newton
 *
 */
public class MergeNodesAction extends JosmAction {

    public MergeNodesAction() {
        super(tr("Merge Nodes"), "mergenodes", tr("Merge nodes into the oldest one."),
                Shortcut.registerShortcut("tools:mergenodes", tr("Tool: {0}", tr("Merge Nodes")), KeyEvent.VK_M, Shortcut.GROUP_EDIT), true);
    }

    public void actionPerformed(ActionEvent event) {
        if (!isEnabled())
            return;

        Collection<OsmPrimitive> selection = getCurrentDataSet().getSelected();
        LinkedList<Node> selectedNodes = new LinkedList<Node>();

        // the selection check should stop this procedure starting if
        // nothing but node are selected - otherwise we don't care
        // anyway as long as we have at least two nodes
        for (OsmPrimitive osm : selection)
            if (osm instanceof Node) {
                selectedNodes.add((Node)osm);
            }

        if (selectedNodes.size() < 2) {
            JOptionPane.showMessageDialog(
                    Main.parent,
                    tr("Please select at least two nodes to merge."),
                    tr("Warning"),
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        // Find which node to merge into (i.e. which one will be left)
        // - this should be combined from two things:
        //   1. It will be the first node in the list that has a
        //      positive ID number, OR the first node.
        //   2. It will be at the position of the first node in the
        //      list.
        //
        // *However* - there is the problem that the selection list is
        // _not_ in the order that the nodes were clicked on, meaning
        // that the user doesn't know which node will be chosen (so
        // (2) is not implemented yet.)  :-(
        Node useNode = null;
        for (Node n: selectedNodes) {
            if (n.id > 0) {
                useNode = n;
                break;
            }
        }
        if (useNode == null) {
            useNode = selectedNodes.iterator().next();
        }

        mergeNodes(selectedNodes, useNode);
    }

    /**
     * really do the merging - returns the node that is left
     */
    public Node mergeNodes(LinkedList<Node> allNodes, Node dest) {
        Node newNode = new Node(dest);

        // Check whether all ways have identical relationship membership. More
        // specifically: If one of the selected ways is a member of relation X
        // in role Y, then all selected ways must be members of X in role Y.

        // FIXME: In a later revision, we should display some sort of conflict
        // dialog like we do for tags, to let the user choose which relations
        // should be kept.

        // Step 1, iterate over all relations and figure out which of our
        // selected ways are members of a relation.
        HashMap<Pair<Relation,String>, HashSet<Node>> backlinks =
            new HashMap<Pair<Relation,String>, HashSet<Node>>();
        HashSet<Relation> relationsUsingNodes = new HashSet<Relation>();
        for (Relation r : getCurrentDataSet().relations) {
            if (r.deleted || r.incomplete) {
                continue;
            }
            for (RelationMember rm : r.getMembers()) {
                if (rm.isNode()) {
                    for (Node n : allNodes) {
                        if (rm.getMember() == n) {
                            Pair<Relation,String> pair = new Pair<Relation,String>(r, rm.getRole());
                            HashSet<Node> nodelinks = new HashSet<Node>();
                            if (backlinks.containsKey(pair)) {
                                nodelinks = backlinks.get(pair);
                            } else {
                                nodelinks = new HashSet<Node>();
                                backlinks.put(pair, nodelinks);
                            }
                            nodelinks.add(n);

                            // this is just a cache for later use
                            relationsUsingNodes.add(r);
                        }
                    }
                }
            }
        }

        // Complain to the user if the ways don't have equal memberships.
        for (HashSet<Node> nodelinks : backlinks.values()) {
            if (!nodelinks.containsAll(allNodes)) {
                int option = new ExtendedDialog(Main.parent,
                        tr("Merge nodes with different memberships?"),
                        tr("The selected nodes have differing relation memberships.  "
                                + "Do you still want to merge them?"),
                                new String[] {tr("Merge Anyway"), tr("Cancel")},
                                new String[] {"mergenodes.png", "cancel.png"}).getValue();
                if (option == 1) {
                    break;
                }
                return null;
            }
        }

        // collect properties for later conflict resolving
        Map<String, Set<String>> props = new TreeMap<String, Set<String>>();
        for (Node n : allNodes) {
            for (Entry<String,String> e : n.entrySet()) {
                if (!props.containsKey(e.getKey())) {
                    props.put(e.getKey(), new TreeSet<String>());
                }
                props.get(e.getKey()).add(e.getValue());
            }
        }

        // display conflict dialog
        Map<String, JComboBox> components = new HashMap<String, JComboBox>();
        JPanel p = new JPanel(new GridBagLayout());
        for (Entry<String, Set<String>> e : props.entrySet()) {
            if (TigerUtils.isTigerTag(e.getKey())) {
                String combined = TigerUtils.combineTags(e.getKey(), e.getValue());
                newNode.put(e.getKey(), combined);
            } else if (e.getValue().size() > 1) {
                JComboBox c = new JComboBox(e.getValue().toArray());
                c.setEditable(true);
                p.add(new JLabel(e.getKey()), GBC.std());
                p.add(Box.createHorizontalStrut(10), GBC.std());
                p.add(c, GBC.eol());
                components.put(e.getKey(), c);
            } else {
                newNode.put(e.getKey(), e.getValue().iterator().next());
            }
        }

        if (!components.isEmpty()) {
            int answer = new ExtendedDialog(Main.parent,
                    tr("Enter values for all conflicts."),
                    p,
                    new String[] {tr("Solve Conflicts"), tr("Cancel")},
                    new String[] {"dialogs/conflict.png", "cancel.png"}).getValue();
            if (answer != 1)
                return null;
            for (Entry<String, JComboBox> e : components.entrySet()) {
                newNode.put(e.getKey(), e.getValue().getEditor().getItem().toString());
            }
        }

        LinkedList<Command> cmds = new LinkedList<Command>();
        cmds.add(new ChangeCommand(dest, newNode));

        Collection<OsmPrimitive> del = new HashSet<OsmPrimitive>();

        for (Way w : getCurrentDataSet().ways) {
            if (w.deleted || w.incomplete || w.getNodesCount() < 1) {
                continue;
            }
            boolean modify = false;
            for (Node sn : allNodes) {
                if (sn == dest) {
                    continue;
                }
                if (w.containsNode(sn)) {
                    modify = true;
                }
            }
            if (!modify) {
                continue;
            }
            // OK - this way contains one or more nodes to change
            ArrayList<Node> nn = new ArrayList<Node>();
            Node lastNode = null;
            for (Node pushNode: w.getNodes()) {
                if (allNodes.contains(pushNode)) {
                    pushNode = dest;
                }
                if (pushNode != lastNode) {
                    nn.add(pushNode);
                }
                lastNode = pushNode;
            }
            if (nn.size() < 2) {
                CollectBackReferencesVisitor backRefs =
                    new CollectBackReferencesVisitor(getCurrentDataSet(), false);
                w.visit(backRefs);
                if (!backRefs.data.isEmpty()) {
                    JOptionPane.showMessageDialog(
                            Main.parent,
                            tr("Cannot merge nodes: " +
                            "Would have to delete a way that is still used."),
                            tr("Warning"),
                            JOptionPane.WARNING_MESSAGE
                    );
                    return null;
                }
                del.add(w);
            } else {
                Way newWay = new Way(w);
                newWay.setNodes(nn);
                cmds.add(new ChangeCommand(w, newWay));
            }
        }

        // delete any merged nodes
        del.addAll(allNodes);
        del.remove(dest);
        if (!del.isEmpty()) {
            cmds.add(new DeleteCommand(del));
        }

        // modify all relations containing the now-deleted nodes
        for (Relation r : relationsUsingNodes) {
            List<RelationMember> newMembers = new ArrayList<RelationMember>();
            HashSet<String> rolesToReAdd = new HashSet<String>();
            for (RelationMember rm : r.getMembers()) {
                // Don't copy the member if it points to one of our nodes,
                // just keep a note to re-add it later on.
                if (allNodes.contains(rm.getMember())) {
                    rolesToReAdd.add(rm.getRole());
                } else {
                    newMembers.add(rm);
                }
            }
            for (String role : rolesToReAdd) {
                newMembers.add(new RelationMember(role, dest));
            }
            Relation newRel = new Relation(r);
            newRel.setMembers(newMembers);
            cmds.add(new ChangeCommand(r, newRel));
        }

        Main.main.undoRedo.add(new SequenceCommand(tr("Merge {0} nodes", allNodes.size()), cmds));
        getCurrentDataSet().setSelected(dest);

        return dest;
    }


    /**
     * Enable the "Merge Nodes" menu option if more then one node is selected
     */
    @Override
    public void updateEnabledState() {
        if (getCurrentDataSet() == null || getCurrentDataSet().getSelected().isEmpty()) {
            setEnabled(false);
            return;
        }
        boolean ok = true;
        if (getCurrentDataSet().getSelected().size() < 2) {
            setEnabled(false);
            return;
        }
        for (OsmPrimitive osm : getCurrentDataSet().getSelected()) {
            if (!(osm instanceof Node)) {
                ok = false;
                break;
            }
        }
        setEnabled(ok);
    }
}
