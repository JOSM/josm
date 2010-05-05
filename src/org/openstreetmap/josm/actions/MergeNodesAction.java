//License: GPL. Copyright 2007 by Immanuel Scholz and others. See LICENSE file for details.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.conflict.tags.TagConflictResolutionUtil.combineTigerTags;
import static org.openstreetmap.josm.gui.conflict.tags.TagConflictResolutionUtil.completeTagCollectionForEditing;
import static org.openstreetmap.josm.gui.conflict.tags.TagConflictResolutionUtil.normalizeTagCollectionBeforeEditing;
import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.command.ChangeNodesCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.DeleteCommand;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.RelationToChildReference;
import org.openstreetmap.josm.data.osm.TagCollection;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.DefaultNameFormatter;
import org.openstreetmap.josm.gui.HelpAwareOptionPane;
import org.openstreetmap.josm.gui.HelpAwareOptionPane.ButtonSpec;
import org.openstreetmap.josm.gui.conflict.tags.CombinePrimitiveResolverDialog;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Shortcut;
/**
 * Merges a collection of nodes into one node.
 *
 * The "surviving" node will be the one with the lowest positive id.
 * (I.e. it was uploaded to the server and is the oldest one.)
 * 
 * However we use the location of the node that was selected *last*.
 * The "surviving" node will be moved to that location if it is
 * different from the last selected node.
 */
public class MergeNodesAction extends JosmAction {

    public MergeNodesAction() {
        super(tr("Merge Nodes"), "mergenodes", tr("Merge nodes into the oldest one."),
                Shortcut.registerShortcut("tools:mergenodes", tr("Tool: {0}", tr("Merge Nodes")), KeyEvent.VK_M, Shortcut.GROUP_EDIT), true);
        putValue("help", ht("/Action/MergeNodesAction"));
    }

    public void actionPerformed(ActionEvent event) {
        if (!isEnabled())
            return;
        Collection<OsmPrimitive> selection = getCurrentDataSet().getSelected();
        LinkedHashSet<Node> selectedNodes = OsmPrimitive.getFilteredSet(selection, Node.class);
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
        Node targetLocationNode = selectTargetLocationNode(selectedNodes);
        Command cmd = mergeNodes(Main.main.getEditLayer(), selectedNodes, targetNode, targetLocationNode);
        if (cmd != null) {
            Main.main.undoRedo.add(cmd);
            Main.main.getEditLayer().data.setSelected(targetNode);
        }
    }

    /**
     * Select the location of the target node after merge.
     *
     * @param candidates the collection of candidate nodes
     * @return the coordinates of this node are later used for the target node
     */
    public static Node selectTargetLocationNode(LinkedHashSet<Node> candidates) {
        Node targetNode = null;
        for (Node n : candidates) { // pick last one
            targetNode = n;
        }
        return targetNode;
    }
    
    /**
     * Find which node to merge into (i.e. which one will be left)
     *
     * @param candidates the collection of candidate nodes
     * @return the selected target node
     */
    public static Node selectTargetNode(LinkedHashSet<Node> candidates) {
        Node targetNode = null;
        Node lastNode = null;
        for (Node n : candidates) {
            if (!n.isNew()) {
                if (targetNode == null) {
                    targetNode = n;
                } else if (n.getId() < targetNode.getId()) {
                    targetNode = n;
                }
            }
            lastNode = n;
        }
        if (targetNode == null) {
            targetNode = lastNode;
        }
        return targetNode;
    }
    

    /**
     * Fixes the parent ways referring to one of the nodes.
     *
     * Replies null, if the ways could not be fixed, i.e. because a way would have to be deleted
     * which is referred to by a relation.
     *
     * @param nodesToDelete the collection of nodes to be deleted
     * @param targetNode the target node the other nodes are merged to
     * @return a list of commands; null, if the ways could not be fixed
     */
    protected static List<Command> fixParentWays(Collection<Node> nodesToDelete, Node targetNode) {
        List<Command> cmds = new ArrayList<Command>();
        Set<Way> waysToDelete = new HashSet<Way>();

        for (Way w: OsmPrimitive.getFilteredList(OsmPrimitive.getReferrer(nodesToDelete), Way.class)) {
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
                if (w.getReferrers().isEmpty()) {
                    waysToDelete.add(w);
                } else {
                    ButtonSpec[] options = new ButtonSpec[] {
                            new ButtonSpec(
                                    tr("Abort Merging"),
                                    ImageProvider.get("cancel"),
                                    tr("Click to abort merging nodes"),
                                    null /* no special help topic */
                            )
                    };
                    HelpAwareOptionPane.showOptionDialog(
                            Main.parent,
                            tr(
                                    "Cannot merge nodes: Would have to delete way ''{0}'' which is still used.",
                                    w.getDisplayName(DefaultNameFormatter.getInstance())
                            ),
                            tr("Warning"),
                            JOptionPane.WARNING_MESSAGE,
                            null, /* no icon */
                            options,
                            options[0],
                            ht("/Action/MergeNodes#WaysToDeleteStillInUse")
                    );
                    return null;
                }
            } else if(newNodes.size() < 2 && w.getReferrers().isEmpty()) {
                waysToDelete.add(w);
            } else {
                cmds.add(new ChangeNodesCommand(w, newNodes));
            }
        }
        if (!waysToDelete.isEmpty()) {
            cmds.add(new DeleteCommand(waysToDelete));
        }
        return cmds;
    }

    public static Command mergeNodes(OsmDataLayer layer, Collection<Node> nodes, Node targetNode) {
        return mergeNodes(layer, nodes, targetNode, targetNode);
    }
    
    /**
     * Merges the nodes in <code>nodes</code> onto one of the nodes. Uses the dataset
     * managed by <code>layer</code> as reference.
     *
     * @param layer layer the reference data layer. Must not be null.
     * @param nodes the collection of nodes. Ignored if null.
     * @param targetNode the target node the collection of nodes is merged to. Must not be null.
     * @param targetLocationNode this node's location will be used for the targetNode.
     * @throw IllegalArgumentException thrown if layer is null
     */
    public static Command mergeNodes(OsmDataLayer layer, Collection<Node> nodes, Node targetNode, Node targetLocationNode) {
        CheckParameterUtil.ensureParameterNotNull(layer, "layer");
        CheckParameterUtil.ensureParameterNotNull(targetNode, "targetNode");
        if (nodes == null)
            return null;

        Set<RelationToChildReference> relationToNodeReferences = RelationToChildReference.getRelationToChildReferences(nodes);

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
        Collection<Node> nodesToDelete = new HashSet<Node>(nodes);
        nodesToDelete.remove(targetNode);

        // fix the ways referring to at least one of the merged nodes
        //
        Collection<Way> waysToDelete= new HashSet<Way>();
        List<Command> wayFixCommands = fixParentWays(
                nodesToDelete,
                targetNode
        );
        if (wayFixCommands == null)
            return null;
        cmds.addAll(wayFixCommands);

        // build the commands
        //
        if (targetNode != targetLocationNode) {
            Node newTargetNode = new Node(targetNode);
            newTargetNode.setCoor(targetLocationNode.getCoor());
            cmds.add(new ChangeCommand(targetNode, newTargetNode));
        }
        cmds.addAll(dialog.buildResolutionCommands());
        if (!nodesToDelete.isEmpty()) {
            cmds.add(new DeleteCommand(nodesToDelete));
        }
        if (!waysToDelete.isEmpty()) {
            cmds.add(new DeleteCommand(waysToDelete));
        }
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
