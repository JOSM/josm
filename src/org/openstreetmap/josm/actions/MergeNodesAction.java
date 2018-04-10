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
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.command.ChangeNodesCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.DeleteCommand;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DefaultNameFormatter;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.data.osm.TagCollection;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.HelpAwareOptionPane;
import org.openstreetmap.josm.gui.HelpAwareOptionPane.ButtonSpec;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.gui.conflict.tags.CombinePrimitiveResolverDialog;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Shortcut;
import org.openstreetmap.josm.tools.UserCancelException;

/**
 * Merges a collection of nodes into one node.
 *
 * The "surviving" node will be the one with the lowest positive id.
 * (I.e. it was uploaded to the server and is the oldest one.)
 *
 * However we use the location of the node that was selected *last*.
 * The "surviving" node will be moved to that location if it is
 * different from the last selected node.
 *
 * @since 422
 */
public class MergeNodesAction extends JosmAction {

    /**
     * Constructs a new {@code MergeNodesAction}.
     */
    public MergeNodesAction() {
        super(tr("Merge Nodes"), "mergenodes", tr("Merge nodes into the oldest one."),
                Shortcut.registerShortcut("tools:mergenodes", tr("Tool: {0}", tr("Merge Nodes")), KeyEvent.VK_M, Shortcut.DIRECT), true);
        putValue("help", ht("/Action/MergeNodes"));
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        if (!isEnabled())
            return;
        Collection<OsmPrimitive> selection = getLayerManager().getEditDataSet().getAllSelected();
        List<Node> selectedNodes = OsmPrimitive.getFilteredList(selection, Node.class);
        selectedNodes.removeIf(n -> n.isDeleted() || n.isIncomplete());

        if (selectedNodes.size() == 1) {
            MapView mapView = MainApplication.getMap().mapView;
            List<Node> nearestNodes = mapView.getNearestNodes(
                    mapView.getPoint(selectedNodes.get(0)), selectedNodes, OsmPrimitive::isUsable);
            if (nearestNodes.isEmpty()) {
                new Notification(
                        tr("Please select at least two nodes to merge or one node that is close to another node."))
                        .setIcon(JOptionPane.WARNING_MESSAGE)
                        .show();
                return;
            }
            selectedNodes.addAll(nearestNodes);
        }

        Node targetNode = selectTargetNode(selectedNodes);
        if (targetNode != null) {
            Node targetLocationNode = selectTargetLocationNode(selectedNodes);
            Command cmd = mergeNodes(selectedNodes, targetNode, targetLocationNode);
            if (cmd != null) {
                MainApplication.undoRedo.add(cmd);
                getLayerManager().getEditLayer().data.setSelected(targetNode);
            }
        }
    }

    /**
     * Select the location of the target node after merge.
     *
     * @param candidates the collection of candidate nodes
     * @return the coordinates of this node are later used for the target node
     */
    public static Node selectTargetLocationNode(List<Node> candidates) {
        int size = candidates.size();
        if (size == 0)
            throw new IllegalArgumentException("empty list");
        if (size == 1) // to avoid division by 0 in mode 2
            return candidates.get(0);

        switch (Config.getPref().getInt("merge-nodes.mode", 0)) {
        case 0:
            return candidates.get(size - 1);
        case 1:
            double east1 = 0;
            double north1 = 0;
            for (final Node n : candidates) {
                EastNorth en = n.getEastNorth();
                east1 += en.east();
                north1 += en.north();
            }

            return new Node(new EastNorth(east1 / size, north1 / size));
        case 2:
            final double[] weights = new double[size];

            for (int i = 0; i < size; i++) {
                final LatLon c1 = candidates.get(i).getCoor();
                for (int j = i + 1; j < size; j++) {
                    final LatLon c2 = candidates.get(j).getCoor();
                    final double d = c1.distance(c2);
                    weights[i] += d;
                    weights[j] += d;
                }
            }

            double east2 = 0;
            double north2 = 0;
            double weight = 0;
            for (int i = 0; i < size; i++) {
                final EastNorth en = candidates.get(i).getEastNorth();
                final double w = weights[i];
                east2 += en.east() * w;
                north2 += en.north() * w;
                weight += w;
            }

            if (weight == 0) // to avoid division by 0
                return candidates.get(0);

            return new Node(new EastNorth(east2 / weight, north2 / weight));
        default:
            throw new IllegalStateException("unacceptable merge-nodes.mode");
        }
    }

    /**
     * Find which node to merge into (i.e. which one will be left)
     *
     * @param candidates the collection of candidate nodes
     * @return the selected target node
     */
    public static Node selectTargetNode(Collection<Node> candidates) {
        Node oldestNode = null;
        Node targetNode = null;
        Node lastNode = null;
        for (Node n : candidates) {
            if (!n.isNew()) {
                // Among existing nodes, try to keep the oldest used one
                if (!n.getReferrers().isEmpty()) {
                    if (targetNode == null || n.getId() < targetNode.getId()) {
                        targetNode = n;
                    }
                } else if (oldestNode == null || n.getId() < oldestNode.getId()) {
                    oldestNode = n;
                }
            }
            lastNode = n;
        }
        return Optional.ofNullable(targetNode).orElse(oldestNode != null ? oldestNode : lastNode);
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
        List<Command> cmds = new ArrayList<>();
        Set<Way> waysToDelete = new HashSet<>();

        for (Way w: OsmPrimitive.getFilteredList(OsmPrimitive.getReferrer(nodesToDelete), Way.class)) {
            List<Node> newNodes = new ArrayList<>(w.getNodesCount());
            for (Node n: w.getNodes()) {
                if (!nodesToDelete.contains(n) && !n.equals(targetNode)) {
                    newNodes.add(n);
                } else if (newNodes.isEmpty() || !newNodes.get(newNodes.size()-1).equals(targetNode)) {
                    // make sure we collapse a sequence of deleted nodes
                    // to exactly one occurrence of the merged target node
                    newNodes.add(targetNode);
                }
                // else: drop the node
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
                            tr("Cannot merge nodes: Would have to delete way {0} which is still used by {1}",
                                DefaultNameFormatter.getInstance().formatAsHtmlUnorderedList(w),
                                DefaultNameFormatter.getInstance().formatAsHtmlUnorderedList(w.getReferrers(), 20)),
                            tr("Warning"),
                            JOptionPane.WARNING_MESSAGE,
                            null, /* no icon */
                            options,
                            options[0],
                            ht("/Action/MergeNodes#WaysToDeleteStillInUse")
                    );
                    return null;
                }
            } else if (newNodes.size() < 2 && w.getReferrers().isEmpty()) {
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

    /**
     * Merges the nodes in {@code nodes} at the specified node's location. Uses the dataset
     * managed by {@code layer} as reference.
     * @param layer layer the reference data layer. Must not be null
     * @param nodes the collection of nodes. Ignored if null
     * @param targetLocationNode this node's location will be used for the target node
     * @throws IllegalArgumentException if {@code layer} is null
     */
    public static void doMergeNodes(OsmDataLayer layer, Collection<Node> nodes, Node targetLocationNode) {
        if (nodes == null) {
            return;
        }
        Set<Node> allNodes = new HashSet<>(nodes);
        allNodes.add(targetLocationNode);
        Node target;
        if (nodes.contains(targetLocationNode) && !targetLocationNode.isNew()) {
            target = targetLocationNode; // keep existing targetLocationNode as target to avoid unnecessary changes (see #2447)
        } else {
            target = selectTargetNode(allNodes);
        }

        if (target != null) {
            Command cmd = mergeNodes(nodes, target, targetLocationNode);
            if (cmd != null) {
                MainApplication.undoRedo.add(cmd);
                layer.data.setSelected(target);
            }
        }
    }

    /**
     * Merges the nodes in {@code nodes} at the specified node's location.
     *
     * @param nodes the collection of nodes. Ignored if null.
     * @param targetLocationNode this node's location will be used for the targetNode.
     * @return The command necessary to run in order to perform action, or {@code null} if there is nothing to do
     * @throws IllegalArgumentException if {@code layer} is null
     * @since 12689
     */
    public static Command mergeNodes(Collection<Node> nodes, Node targetLocationNode) {
        if (nodes == null) {
            return null;
        }
        Set<Node> allNodes = new HashSet<>(nodes);
        allNodes.add(targetLocationNode);
        Node targetNode = selectTargetNode(allNodes);
        if (targetNode == null) {
            return null;
        }
        return mergeNodes(nodes, targetNode, targetLocationNode);
    }

    /**
     * Merges the nodes in <code>nodes</code> onto one of the nodes.
     *
     * @param nodes the collection of nodes. Ignored if null.
     * @param targetNode the target node the collection of nodes is merged to. Must not be null.
     * @param targetLocationNode this node's location will be used for the targetNode.
     * @return The command necessary to run in order to perform action, or {@code null} if there is nothing to do
     * @throws IllegalArgumentException if layer is null
     */
    public static Command mergeNodes(Collection<Node> nodes, Node targetNode, Node targetLocationNode) {
        CheckParameterUtil.ensureParameterNotNull(targetNode, "targetNode");
        if (nodes == null) {
            return null;
        }

        try {
            TagCollection nodeTags = TagCollection.unionOfAllPrimitives(nodes);

            // the nodes we will have to delete
            //
            Collection<Node> nodesToDelete = new HashSet<>(nodes);
            nodesToDelete.remove(targetNode);

            // fix the ways referring to at least one of the merged nodes
            //
            List<Command> wayFixCommands = fixParentWays(nodesToDelete, targetNode);
            if (wayFixCommands == null) {
                return null;
            }
            List<Command> cmds = new LinkedList<>(wayFixCommands);

            // build the commands
            //
            if (!targetNode.equals(targetLocationNode)) {
                LatLon targetLocationCoor = targetLocationNode.getCoor();
                if (!Objects.equals(targetNode.getCoor(), targetLocationCoor)) {
                    Node newTargetNode = new Node(targetNode);
                    newTargetNode.setCoor(targetLocationCoor);
                    cmds.add(new ChangeCommand(targetNode, newTargetNode));
                }
            }
            cmds.addAll(CombinePrimitiveResolverDialog.launchIfNecessary(nodeTags, nodes, Collections.singleton(targetNode)));
            if (!nodesToDelete.isEmpty()) {
                cmds.add(new DeleteCommand(nodesToDelete));
            }
            return new SequenceCommand(/* for correct i18n of plural forms - see #9110 */
                    trn("Merge {0} node", "Merge {0} nodes", nodes.size(), nodes.size()), cmds);
        } catch (UserCancelException ex) {
            Logging.trace(ex);
            return null;
        }
    }

    @Override
    protected void updateEnabledState() {
        updateEnabledStateOnCurrentSelection();
    }

    @Override
    protected void updateEnabledState(Collection<? extends OsmPrimitive> selection) {
        boolean ok = OsmUtils.isOsmCollectionEditable(selection);
        if (ok) {
            for (OsmPrimitive osm : selection) {
                if (!(osm instanceof Node)) {
                    ok = false;
                    break;
                }
            }
        }
        setEnabled(ok);
    }
}
