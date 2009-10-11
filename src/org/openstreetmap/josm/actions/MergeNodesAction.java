//License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.conflict.tags.TagConflictResolutionUtil.combineTigerTags;
import static org.openstreetmap.josm.gui.conflict.tags.TagConflictResolutionUtil.completeTagCollectionForEditing;
import static org.openstreetmap.josm.gui.conflict.tags.TagConflictResolutionUtil.normalizeTagCollectionBeforeEditing;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.DeleteCommand;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.osm.BackreferencedDataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.TagCollection;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.BackreferencedDataSet.RelationToChildReference;
import org.openstreetmap.josm.gui.conflict.tags.CombinePrimitiveResolverDialog;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.tools.Shortcut;


/**
 * Merges a collection of nodes into one node.
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
        Set<Node> selectedNodes = OsmPrimitive.getFilteredSet(selection, Node.class);
        if (selectedNodes.size() < 2) {
            JOptionPane.showMessageDialog(
                    Main.parent,
                    tr("Please select at least two nodes to merge."),
                    tr("Warning"),
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        Node targetNode = selectTargetNode(selectedNodes);
        Command cmd = mergeNodes(Main.main.getEditLayer(), selectedNodes, targetNode);
        if (cmd != null) {
            Main.main.undoRedo.add(cmd);
            Main.main.getEditLayer().data.setSelected(targetNode);
        }
    }

    /**
     * Selects a node out of a collection of candidate nodes. The selected
     * node will become the target node the remaining nodes are merged to.
     * 
     * @param candidates the collection of candidate nodes
     * @return the selected target node
     */
    public static Node selectTargetNode(Collection<Node> candidates) {
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
        Node targetNode = null;
        for (Node n: candidates) {
            if (!n.isNew()) {
                targetNode = n;
                break;
            }
        }
        if (targetNode == null) {
            // an arbitrary node
            targetNode = candidates.iterator().next();
        }
        return targetNode;
    }

    /**
     * Merges the nodes in <code>node</code> onto one of the nodes. Uses the dataset
     * managed by <code>layer</code> as reference.
     * 
     * @param layer the reference data layer. Must not be null.
     * @param nodes the collection of nodes. Ignored if null.
     * @param targetNode the target node the collection of nodes is merged to. Must not be null.
     * @throws IllegalArgumentException thrown if layer is null
     * @throws IllegalArgumentException thrown if targetNode is null
     * 
     */
    public static Command mergeNodes(OsmDataLayer layer, Collection<Node> nodes, Node targetNode) throws IllegalArgumentException{
        if (layer == null)
            throw new IllegalArgumentException(tr("Parameter ''{0}'' must not be null.", "nodes"));
        if (targetNode == null)
            throw new IllegalArgumentException(tr("Parameter ''{0}'' must not be null.", "targetNode"));

        if (nodes == null)
            return null;
        nodes.remove(null); // just in case
        BackreferencedDataSet backreferences = new BackreferencedDataSet(layer.data);
        backreferences.build();
        return mergeNodes(layer,backreferences, nodes, targetNode);
    }

    /**
     * Fixes the parent ways referring to one of the nodes.
     * 
     * Replies null, if the ways could not be fixed, i.e. because a way would have to be deleted
     * which is referred to by a relation.
     * 
     * @param backreferences the backreference data set
     * @param nodesToDelete the collection of nodes to be deleted
     * @param targetNode the target node the other nodes are merged to
     * @return a list of commands; null, if the ways could not be fixed
     */
    protected static List<Command> fixParentWays(BackreferencedDataSet backreferences, Collection<OsmPrimitive> nodesToDelete, Node targetNode) {
        List<Command> cmds = new ArrayList<Command>();
        Set<Way> waysToDelete = new HashSet<Way>();

        for (Way w: OsmPrimitive.getFilteredList(backreferences.getParents(nodesToDelete), Way.class)) {
            ArrayList<Node> newNodes = new ArrayList<Node>(w.getNodesCount());
            for (Node n: w.getNodes()) {
                if (! nodesToDelete.contains(n) && n != targetNode) {
                    newNodes.add(n);
                } else if (newNodes.isEmpty()) {
                    newNodes.add(targetNode);
                } else if (newNodes.get(newNodes.size()-1) != targetNode) {
                    // make sure we collapse a sequence of deleted nodes
                    // to exactly one occurrence of the merged target node
                    //
                    newNodes.add(targetNode);
                } else {
                    // drop the node
                }
            }
            if (newNodes.size() < 2) {
                if (backreferences.getParents(w).isEmpty()) {
                    waysToDelete.add(w);
                } else {
                    JOptionPane.showMessageDialog(
                            Main.parent,
                            tr("Cannot merge nodes: " +
                            "Would have to delete a way that is still used."),
                            tr("Warning"),
                            JOptionPane.WARNING_MESSAGE
                    );
                    return null;
                }
            } else if(newNodes.size() < 2 && backreferences.getParents(w).isEmpty()) {
                waysToDelete.add(w);
            } else {
                Way newWay = new Way(w);
                newWay.setNodes(newNodes);
                cmds.add(new ChangeCommand(w, newWay));
            }
        }
        return cmds;
    }

    /**
     * Merges the nodes in <code>nodes</code> onto one of the nodes. Uses the dataset
     * managed by <code>layer</code> as reference. <code>backreferences</code> is a precomputed
     * collection of all parent/child references in the dataset.
     *
     * @param layer layer the reference data layer. Must not be null.
     * @param backreferences if null, backreferences are first computed from layer.data; otherwise
     *    backreferences.getSource() == layer.data must be true
     * @param nodes the collection of nodes. Ignored if null.
     * @param targetNode the target node the collection of nodes is merged to. Must not be null.
     * @throw IllegalArgumentException thrown if layer is null
     * @throw IllegalArgumentException thrown if  backreferences.getSource() != layer.data
     */
    public static Command mergeNodes(OsmDataLayer layer, BackreferencedDataSet backreferences, Collection<Node> nodes, Node targetNode) {
        if (layer == null)
            throw new IllegalArgumentException(tr("Parameter ''{0}'' must not be null.", "nodes"));
        if (targetNode == null)
            throw new IllegalArgumentException(tr("Parameter ''{0}'' must not be null.", "targetNode"));
        if (nodes == null)
            return null;
        if (backreferences == null) {
            backreferences = new BackreferencedDataSet(layer.data);
            backreferences.build();
        }

        Set<RelationToChildReference> relationToNodeReferences = backreferences.getRelationToChildReferences(nodes);

        // build the tag collection
        //
        TagCollection nodeTags = TagCollection.unionOfAllPrimitives(nodes);
        combineTigerTags(nodeTags);
        normalizeTagCollectionBeforeEditing(nodeTags, nodes);
        TagCollection nodeTagsToEdit = new TagCollection(nodeTags);
        completeTagCollectionForEditing(nodeTagsToEdit);

        // launch a conflict resolution dialog, if necessary
        //
        CombinePrimitiveResolverDialog dialog = CombinePrimitiveResolverDialog.getInstance();
        dialog.getTagConflictResolverModel().populate(nodeTagsToEdit, nodeTags.getKeysWithMultipleValues());
        dialog.getRelationMemberConflictResolverModel().populate(relationToNodeReferences);
        dialog.setTargetPrimitive(targetNode);
        dialog.prepareDefaultDecisions();
        // conflict resolution is necessary if there are conflicts in the merged tags
        // or if at least one of the merged nodes is referred to by a relation
        //
        if (! nodeTags.isApplicableToPrimitive() || relationToNodeReferences.size() > 1) {
            dialog.setVisible(true);
            if (dialog.isCancelled())
                return null;
        }
        LinkedList<Command> cmds = new LinkedList<Command>();

        // the nodes we will have to delete
        //
        Collection<OsmPrimitive> nodesToDelete = new HashSet<OsmPrimitive>(nodes);
        nodesToDelete.remove(targetNode);

        // fix the ways referring to at least one of the merged nodes
        //
        Collection<Way> waysToDelete= new HashSet<Way>();
        List<Command> wayFixCommands = fixParentWays(
                backreferences,
                nodesToDelete,
                targetNode
        );
        if (wayFixCommands == null)
            return null;
        cmds.addAll(wayFixCommands);

        // build the commands
        //
        if (!nodesToDelete.isEmpty()) {
            cmds.add(new DeleteCommand(nodesToDelete));
        }
        if (!waysToDelete.isEmpty()) {
            cmds.add(new DeleteCommand(waysToDelete));
        }
        cmds.addAll(dialog.buildResolutionCommands());
        Command cmd = new SequenceCommand(tr("Merge {0} nodes", nodes.size()), cmds);
        return cmd;
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
        if (selection == null || selection.isEmpty()) {
            setEnabled(false);
            return;
        }
        boolean ok = true;
        if (selection.size() < 2) {
            setEnabled(false);
            return;
        }
        for (OsmPrimitive osm : selection) {
            if (!(osm instanceof Node)) {
                ok = false;
                break;
            }
        }
        setEnabled(ok);
    }
}
