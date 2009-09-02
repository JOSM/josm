// License: GPL. Copyright 2007 by Immanuel Scholz and others
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
import java.util.ListIterator;
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
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.Pair;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Combines multiple ways into one.
 *
 * @author Imi
 */
public class CombineWayAction extends JosmAction {

    public CombineWayAction() {
        super(tr("Combine Way"), "combineway", tr("Combine several ways into one."),
                Shortcut.registerShortcut("tools:combineway", tr("Tool: {0}", tr("Combine Way")), KeyEvent.VK_C, Shortcut.GROUP_EDIT), true);
    }

    @SuppressWarnings("unchecked")
    public void actionPerformed(ActionEvent event) {
        if (getCurrentDataSet() == null)
            return;
        Collection<OsmPrimitive> selection = getCurrentDataSet().getSelected();
        LinkedList<Way> selectedWays = new LinkedList<Way>();

        for (OsmPrimitive osm : selection)
            if (osm instanceof Way) {
                selectedWays.add((Way)osm);
            }

        if (selectedWays.size() < 2) {
            JOptionPane.showMessageDialog(
                    Main.parent,
                    tr("Please select at least two ways to combine."),
                    tr("Information"),
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        // Check whether all ways have identical relationship membership. More
        // specifically: If one of the selected ways is a member of relation X
        // in role Y, then all selected ways must be members of X in role Y.

        // FIXME: In a later revision, we should display some sort of conflict
        // dialog like we do for tags, to let the user choose which relations
        // should be kept.

        // Step 1, iterate over all relations and figure out which of our
        // selected ways are members of a relation.
        HashMap<Pair<Relation,String>, HashSet<Way>> backlinks =
            new HashMap<Pair<Relation,String>, HashSet<Way>>();
        HashSet<Relation> relationsUsingWays = new HashSet<Relation>();
        for (Relation r : getCurrentDataSet().relations) {
            if (r.isDeleted() || r.incomplete) {
                continue;
            }
            for (RelationMember rm : r.getMembers()) {
                if (rm.isWay()) {
                    for(Way w : selectedWays) {
                        if (rm.getMember() == w) {
                            Pair<Relation,String> pair = new Pair<Relation,String>(r, rm.getRole());
                            HashSet<Way> waylinks = new HashSet<Way>();
                            if (backlinks.containsKey(pair)) {
                                waylinks = backlinks.get(pair);
                            } else {
                                waylinks = new HashSet<Way>();
                                backlinks.put(pair, waylinks);
                            }
                            waylinks.add(w);

                            // this is just a cache for later use
                            relationsUsingWays.add(r);
                        }
                    }
                }
            }
        }

        // Complain to the user if the ways don't have equal memberships.
        for (HashSet<Way> waylinks : backlinks.values()) {
            if (!waylinks.containsAll(selectedWays)) {
                int option = new ExtendedDialog(Main.parent,
                        tr("Combine ways with different memberships?"),
                        tr("The selected ways have differing relation memberships.  "
                                + "Do you still want to combine them?"),
                                new String[] {tr("Combine Anyway"), tr("Cancel")},
                                new String[] {"combineway.png", "cancel.png"}).getValue();
                if (option == 1) {
                    break;
                }

                return;
            }
        }

        // collect properties for later conflict resolving
        Map<String, Set<String>> props = new TreeMap<String, Set<String>>();
        for (Way w : selectedWays) {
            for (Entry<String,String> e : w.entrySet()) {
                if (!props.containsKey(e.getKey())) {
                    props.put(e.getKey(), new TreeSet<String>());
                }
                props.get(e.getKey()).add(e.getValue());
            }
        }

        List<Node> nodeList = null;
        Object firstTry = actuallyCombineWays(selectedWays, false);
        if (firstTry instanceof List<?>) {
            nodeList = (List<Node>) firstTry;
        } else {
            Object secondTry = actuallyCombineWays(selectedWays, true);
            if (secondTry instanceof List<?>) {
                int option = new ExtendedDialog(Main.parent,
                        tr("Change directions?"),
                        tr("The ways can not be combined in their current directions.  "
                                + "Do you want to reverse some of them?"),
                                new String[] {tr("Reverse and Combine"), tr("Cancel")},
                                new String[] {"wayflip.png", "cancel.png"}).getValue();
                if (option != 1) return;
                nodeList = (List<Node>) secondTry;
            } else {
                JOptionPane.showMessageDialog(
                        Main.parent,
                        secondTry, // FIXME: not sure whether this fits in a dialog
                        tr("Information"),
                        JOptionPane.INFORMATION_MESSAGE
                );
                return;
            }
        }

        // Find the most appropriate way to modify.

        // Eventually this might want to be the way with the longest
        // history or the longest selected way but for now just attempt
        // to reuse an existing id.
        Way modifyWay = selectedWays.peek();
        for (Way w : selectedWays) {
            modifyWay = w;
            if (w.getId() != 0) {
                break;
            }
        }
        Way newWay = new Way(modifyWay);

        newWay.setNodes(nodeList);

        // display conflict dialog
        Map<String, JComboBox> components = new HashMap<String, JComboBox>();
        JPanel p = new JPanel(new GridBagLayout());
        for (Entry<String, Set<String>> e : props.entrySet()) {
            if (TigerUtils.isTigerTag(e.getKey())) {
                String combined = TigerUtils.combineTags(e.getKey(), e.getValue());
                newWay.put(e.getKey(), combined);
            } else if (e.getValue().size() > 1) {
                JComboBox c = new JComboBox(e.getValue().toArray());
                c.setEditable(true);
                p.add(new JLabel(e.getKey()), GBC.std());
                p.add(Box.createHorizontalStrut(10), GBC.std());
                p.add(c, GBC.eol());
                components.put(e.getKey(), c);
            } else {
                newWay.put(e.getKey(), e.getValue().iterator().next());
            }
        }

        if (!components.isEmpty()) {
            int answer = new ExtendedDialog(Main.parent,
                    tr("Enter values for all conflicts."),
                    p,
                    new String[] {tr("Solve Conflicts"), tr("Cancel")},
                    new String[] {"dialogs/conflict.png", "cancel.png"}).getValue();
            if (answer != 1) return;

            for (Entry<String, JComboBox> e : components.entrySet()) {
                newWay.put(e.getKey(), e.getValue().getEditor().getItem().toString());
            }
        }

        LinkedList<Command> cmds = new LinkedList<Command>();
        LinkedList<Way> deletedWays = new LinkedList<Way>(selectedWays);
        deletedWays.remove(modifyWay);
        cmds.add(new DeleteCommand(deletedWays));
        cmds.add(new ChangeCommand(modifyWay, newWay));

        // modify all relations containing the now-deleted ways
        for (Relation r : relationsUsingWays) {
            List<RelationMember> newMembers = new ArrayList<RelationMember>();
            HashSet<String> rolesToReAdd = new HashSet<String>();
            for (RelationMember rm : r.getMembers()) {
                // Don't copy the member if it to one of our ways, just keep a
                // note to re-add it later on.
                if (selectedWays.contains(rm.getMember())) {
                    rolesToReAdd.add(rm.getRole());
                } else {
                    newMembers.add(rm);
                }
            }
            for (String role : rolesToReAdd) {
                newMembers.add(new RelationMember(role, modifyWay));
            }
            Relation newRel = new Relation(r);
            newRel.setMembers(newMembers);
            cmds.add(new ChangeCommand(r, newRel));
        }
        Main.main.undoRedo.add(new SequenceCommand(tr("Combine {0} ways", selectedWays.size()), cmds));
        getCurrentDataSet().setSelected(modifyWay);
    }

    /**
     * @return a message if combining failed, else a list of nodes.
     */
    private Object actuallyCombineWays(List<Way> ways, boolean ignoreDirection) {
        // Battle plan:
        //  1. Split the ways into small chunks of 2 nodes and weed out
        //     duplicates.
        //  2. Take a chunk and see if others could be appended or prepended,
        //     if so, do it and remove it from the list of remaining chunks.
        //     Rather, rinse, repeat.
        //  3. If this algorithm does not produce a single way,
        //     complain to the user.
        //  4. Profit!

        HashSet<Pair<Node,Node>> chunkSet = new HashSet<Pair<Node,Node>>();
        for (Way w : ways) {
            chunkSet.addAll(w.getNodePairs(ignoreDirection));
        }

        LinkedList<Pair<Node,Node>> chunks = new LinkedList<Pair<Node,Node>>(chunkSet);

        if (chunks.isEmpty())
            return tr("All the ways were empty");

        List<Node> nodeList = Pair.toArrayList(chunks.poll());
        while (!chunks.isEmpty()) {
            ListIterator<Pair<Node,Node>> it = chunks.listIterator();
            boolean foundChunk = false;
            while (it.hasNext()) {
                Pair<Node,Node> curChunk = it.next();
                if (curChunk.a == nodeList.get(nodeList.size() - 1)) { // append
                    nodeList.add(curChunk.b);
                } else if (curChunk.b == nodeList.get(0)) { // prepend
                    nodeList.add(0, curChunk.a);
                } else if (ignoreDirection && curChunk.b == nodeList.get(nodeList.size() - 1)) { // append
                    nodeList.add(curChunk.a);
                } else if (ignoreDirection && curChunk.a == nodeList.get(0)) { // prepend
                    nodeList.add(0, curChunk.b);
                } else {
                    continue;
                }

                foundChunk = true;
                it.remove();
                break;
            }
            if (!foundChunk) {
                break;
            }
        }

        if (!chunks.isEmpty())
            return tr("Could not combine ways "
                    + "(They could not be merged into a single string of nodes)");

        return nodeList;
    }

    @Override
    protected void updateEnabledState() {
        if (getCurrentDataSet() == null) {
            setEnabled(false);
            return;
        }
        Collection<OsmPrimitive> selection = getCurrentDataSet().getSelected();
        int numWays = 0;

        for (OsmPrimitive osm : selection)
            if (osm instanceof Way) {
                numWays++;
            }
        setEnabled(numWays >= 2);
    }
}
