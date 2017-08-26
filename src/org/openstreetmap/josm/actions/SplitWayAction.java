// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.Component;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.AddCommand;
import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.osm.DefaultNameFormatter;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.PrimitiveId;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.WaySegment;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Splits a way into multiple ways (all identical except for their node list).
 *
 * Ways are just split at the selected nodes.  The nodes remain in their
 * original order.  Selected nodes at the end of a way are ignored.
 */
public class SplitWayAction extends JosmAction {

    /**
     * Represents the result of a {@link SplitWayAction}
     * @see SplitWayAction#splitWay
     * @see SplitWayAction#split
     */
    public static class SplitWayResult {
        private final Command command;
        private final List<? extends PrimitiveId> newSelection;
        private final Way originalWay;
        private final List<Way> newWays;

        /**
         * @param command The command to be performed to split the way (which is saved for later retrieval with {@link #getCommand})
         * @param newSelection The new list of selected primitives ids (which is saved for later retrieval with {@link #getNewSelection})
         * @param originalWay The original way being split (which is saved for later retrieval with {@link #getOriginalWay})
         * @param newWays The resulting new ways (which is saved for later retrieval with {@link #getOriginalWay})
         */
        public SplitWayResult(Command command, List<? extends PrimitiveId> newSelection, Way originalWay, List<Way> newWays) {
            this.command = command;
            this.newSelection = newSelection;
            this.originalWay = originalWay;
            this.newWays = newWays;
        }

        /**
         * Replies the command to be performed to split the way
         * @return The command to be performed to split the way
         */
        public Command getCommand() {
            return command;
        }

        /**
         * Replies the new list of selected primitives ids
         * @return The new list of selected primitives ids
         */
        public List<? extends PrimitiveId> getNewSelection() {
            return newSelection;
        }

        /**
         * Replies the original way being split
         * @return The original way being split
         */
        public Way getOriginalWay() {
            return originalWay;
        }

        /**
         * Replies the resulting new ways
         * @return The resulting new ways
         */
        public List<Way> getNewWays() {
            return newWays;
        }
    }

    /**
     * Create a new SplitWayAction.
     */
    public SplitWayAction() {
        super(tr("Split Way"), "splitway", tr("Split a way at the selected node."),
                Shortcut.registerShortcut("tools:splitway", tr("Tool: {0}", tr("Split Way")), KeyEvent.VK_P, Shortcut.DIRECT), true);
        putValue("help", ht("/Action/SplitWay"));
    }

    /**
     * Called when the action is executed.
     *
     * This method performs an expensive check whether the selection clearly defines one
     * of the split actions outlined above, and if yes, calls the splitWay method.
     */
    @Override
    public void actionPerformed(ActionEvent e) {

        if (SegmentToKeepSelectionDialog.DISPLAY_COUNT.get() > 0) {
            new Notification(tr("Cannot split since another split operation is already in progress"))
                    .setIcon(JOptionPane.WARNING_MESSAGE).show();
            return;
        }

        Collection<OsmPrimitive> selection = getLayerManager().getEditDataSet().getSelected();

        List<Node> selectedNodes = OsmPrimitive.getFilteredList(selection, Node.class);
        List<Way> selectedWays = OsmPrimitive.getFilteredList(selection, Way.class);
        List<Way> applicableWays = getApplicableWays(selectedWays, selectedNodes);

        if (applicableWays == null) {
            new Notification(
                    tr("The current selection cannot be used for splitting - no node is selected."))
                    .setIcon(JOptionPane.WARNING_MESSAGE)
                    .show();
            return;
        } else if (applicableWays.isEmpty()) {
            new Notification(
                    tr("The selected nodes do not share the same way."))
                    .setIcon(JOptionPane.WARNING_MESSAGE)
                    .show();
            return;
        }

        // If several ways have been found, remove ways that doesn't have selected
        // node in the middle
        if (applicableWays.size() > 1) {
            for (Iterator<Way> it = applicableWays.iterator(); it.hasNext();) {
                Way w = it.next();
                for (Node n : selectedNodes) {
                    if (!w.isInnerNode(n)) {
                        it.remove();
                        break;
                    }
                }
            }
        }

        if (applicableWays.isEmpty()) {
            new Notification(
                    trn("The selected node is not in the middle of any way.",
                        "The selected nodes are not in the middle of any way.",
                        selectedNodes.size()))
                    .setIcon(JOptionPane.WARNING_MESSAGE)
                    .show();
            return;
        } else if (applicableWays.size() > 1) {
            new Notification(
                    trn("There is more than one way using the node you selected. Please select the way also.",
                        "There is more than one way using the nodes you selected. Please select the way also.",
                        selectedNodes.size()))
                    .setIcon(JOptionPane.WARNING_MESSAGE)
                    .show();
            return;
        }

        // Finally, applicableWays contains only one perfect way
        final Way selectedWay = applicableWays.get(0);
        final List<List<Node>> wayChunks = buildSplitChunks(selectedWay, selectedNodes);
        if (wayChunks != null) {
            List<Relation> selectedRelations = OsmPrimitive.getFilteredList(selection, Relation.class);
            final List<OsmPrimitive> sel = new ArrayList<>(selectedWays.size() + selectedRelations.size());
            sel.addAll(selectedWays);
            sel.addAll(selectedRelations);

            final List<Way> newWays = createNewWaysFromChunks(selectedWay, wayChunks);
            final Way wayToKeep = Strategy.keepLongestChunk().determineWayToKeep(newWays);

            if (ExpertToggleAction.isExpert() && !selectedWay.isNew()) {
                final ExtendedDialog dialog = new SegmentToKeepSelectionDialog(selectedWay, newWays, wayToKeep, sel);
                dialog.toggleEnable("way.split.segment-selection-dialog");
                if (!dialog.toggleCheckState()) {
                    dialog.setModal(false);
                    dialog.showDialog();
                    return; // splitting is performed in SegmentToKeepSelectionDialog.buttonAction()
                }
            }
            if (wayToKeep != null) {
                final SplitWayResult result = doSplitWay(getLayerManager().getEditLayer(), selectedWay, wayToKeep, newWays, sel);
                MainApplication.undoRedo.add(result.getCommand());
                if (!result.getNewSelection().isEmpty()) {
                    getLayerManager().getEditDataSet().setSelected(result.getNewSelection());
                }
            }
        }
    }

    /**
     * A dialog to query which way segment should reuse the history of the way to split.
     */
    static class SegmentToKeepSelectionDialog extends ExtendedDialog {
        static final AtomicInteger DISPLAY_COUNT = new AtomicInteger();
        final transient Way selectedWay;
        final transient List<Way> newWays;
        final JList<Way> list;
        final transient List<OsmPrimitive> selection;
        final transient Way wayToKeep;

        SegmentToKeepSelectionDialog(Way selectedWay, List<Way> newWays, Way wayToKeep, List<OsmPrimitive> selection) {
            super(Main.parent, tr("Which way segment should reuse the history of {0}?", selectedWay.getId()),
                    new String[]{tr("Ok"), tr("Cancel")}, true);

            this.selectedWay = selectedWay;
            this.newWays = newWays;
            this.selection = selection;
            this.wayToKeep = wayToKeep;
            this.list = new JList<>(newWays.toArray(new Way[newWays.size()]));
            configureList();

            setButtonIcons("ok", "cancel");
            final JPanel pane = new JPanel(new GridBagLayout());
            pane.add(new JLabel(getTitle()), GBC.eol().fill(GBC.HORIZONTAL));
            pane.add(list, GBC.eop().fill(GBC.HORIZONTAL));
            setContent(pane);
            setDefaultCloseOperation(HIDE_ON_CLOSE);
        }

        private void configureList() {
            list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            list.addListSelectionListener(e -> {
                final Way selected = list.getSelectedValue();
                if (selected != null && MainApplication.isDisplayingMapView() && selected.getNodesCount() > 1) {
                    final Collection<WaySegment> segments = new ArrayList<>(selected.getNodesCount() - 1);
                    final Iterator<Node> it = selected.getNodes().iterator();
                    Node previousNode = it.next();
                    while (it.hasNext()) {
                        final Node node = it.next();
                        segments.add(WaySegment.forNodePair(selectedWay, previousNode, node));
                        previousNode = node;
                    }
                    setHighlightedWaySegments(segments);
                }
            });
            list.setCellRenderer(new SegmentListCellRenderer());
        }

        protected void setHighlightedWaySegments(Collection<WaySegment> segments) {
            selectedWay.getDataSet().setHighlightedWaySegments(segments);
            MainApplication.getMap().mapView.repaint();
        }

        @Override
        public void setVisible(boolean visible) {
            super.setVisible(visible);
            if (visible) {
                DISPLAY_COUNT.incrementAndGet();
                list.setSelectedValue(wayToKeep, true);
            } else {
                setHighlightedWaySegments(Collections.<WaySegment>emptyList());
                DISPLAY_COUNT.decrementAndGet();
            }
        }

        @Override
        protected void buttonAction(int buttonIndex, ActionEvent evt) {
            super.buttonAction(buttonIndex, evt);
            toggleSaveState(); // necessary since #showDialog() does not handle it due to the non-modal dialog
            if (getValue() == 1) {
                SplitWayResult result = doSplitWay(MainApplication.getLayerManager().getEditLayer(),
                        selectedWay, list.getSelectedValue(), newWays, selection);
                MainApplication.undoRedo.add(result.getCommand());
                if (!result.getNewSelection().isEmpty()) {
                    MainApplication.getLayerManager().getEditDataSet().setSelected(result.getNewSelection());
                }
            }
        }
    }

    static class SegmentListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            final Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            final String name = DefaultNameFormatter.getInstance().format((Way) value);
            // get rid of id from DefaultNameFormatter.decorateNameWithId()
            final String nameWithoutId = name
                    .replace(tr(" [id: {0}]", ((Way) value).getId()), "")
                    .replace(tr(" [id: {0}]", ((Way) value).getUniqueId()), "");
            ((JLabel) c).setText(tr("Segment {0}: {1}", index + 1, nameWithoutId));
            return c;
        }
    }

    /**
     * Determines which way chunk should reuse the old id and its history
     *
     * @since 8954
     * @since 10599 (functional interface)
     */
    @FunctionalInterface
    public interface Strategy {

        /**
         * Determines which way chunk should reuse the old id and its history.
         *
         * @param wayChunks the way chunks
         * @return the way to keep
         */
        Way determineWayToKeep(Iterable<Way> wayChunks);

        /**
         * Returns a strategy which selects the way chunk with the highest node count to keep.
         * @return strategy which selects the way chunk with the highest node count to keep
         */
        static Strategy keepLongestChunk() {
            return wayChunks -> {
                    Way wayToKeep = null;
                    for (Way i : wayChunks) {
                        if (wayToKeep == null || i.getNodesCount() > wayToKeep.getNodesCount()) {
                            wayToKeep = i;
                        }
                    }
                    return wayToKeep;
                };
        }

        /**
         * Returns a strategy which selects the first way chunk.
         * @return strategy which selects the first way chunk
         */
        static Strategy keepFirstChunk() {
            return wayChunks -> wayChunks.iterator().next();
        }
    }

    /**
     * Determine which ways to split.
     * @param selectedWays List of user selected ways.
     * @param selectedNodes List of user selected nodes.
     * @return List of ways to split
     */
    static List<Way> getApplicableWays(List<Way> selectedWays, List<Node> selectedNodes) {
        if (selectedNodes.isEmpty())
            return null;

        // Special case - one of the selected ways touches (not cross) way that we want to split
        if (selectedNodes.size() == 1) {
            Node n = selectedNodes.get(0);
            List<Way> referredWays = n.getParentWays();
            Way inTheMiddle = null;
            for (Way w: referredWays) {
                // Need to look at all nodes see #11184 for a case where node n is
                // firstNode, lastNode and also in the middle
                if (selectedWays.contains(w) && w.isInnerNode(n)) {
                    if (inTheMiddle == null) {
                        inTheMiddle = w;
                    } else {
                        inTheMiddle = null;
                        break;
                    }
                }
            }
            if (inTheMiddle != null)
                return Collections.singletonList(inTheMiddle);
        }

        // List of ways shared by all nodes
        return UnJoinNodeWayAction.getApplicableWays(selectedWays, selectedNodes);
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
    public static List<List<Node>> buildSplitChunks(Way wayToSplit, List<Node> splitPoints) {
        CheckParameterUtil.ensureParameterNotNull(wayToSplit, "wayToSplit");
        CheckParameterUtil.ensureParameterNotNull(splitPoints, "splitPoints");

        Set<Node> nodeSet = new HashSet<>(splitPoints);
        List<List<Node>> wayChunks = new LinkedList<>();
        List<Node> currentWayChunk = new ArrayList<>();
        wayChunks.add(currentWayChunk);

        Iterator<Node> it = wayToSplit.getNodes().iterator();
        while (it.hasNext()) {
            Node currentNode = it.next();
            boolean atEndOfWay = currentWayChunk.isEmpty() || !it.hasNext();
            currentWayChunk.add(currentNode);
            if (nodeSet.contains(currentNode) && !atEndOfWay) {
                currentWayChunk = new ArrayList<>();
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
                new Notification(
                        tr("You must select two or more nodes to split a circular way."))
                        .setIcon(JOptionPane.WARNING_MESSAGE)
                        .show();
                return null;
            }
            lastWayChunk.remove(lastWayChunk.size() - 1);
            lastWayChunk.addAll(wayChunks.get(0));
            wayChunks.remove(wayChunks.size() - 1);
            wayChunks.set(0, lastWayChunk);
        }

        if (wayChunks.size() < 2) {
            if (wayChunks.get(0).get(0) == wayChunks.get(0).get(wayChunks.get(0).size() - 1)) {
                new Notification(
                        tr("You must select two or more nodes to split a circular way."))
                        .setIcon(JOptionPane.WARNING_MESSAGE)
                        .show();
            } else {
                new Notification(
                        tr("The way cannot be split at the selected nodes. (Hint: Select nodes in the middle of the way.)"))
                        .setIcon(JOptionPane.WARNING_MESSAGE)
                        .show();
            }
            return null;
        }
        return wayChunks;
    }

    /**
     * Creates new way objects for the way chunks and transfers the keys from the original way.
     * @param way the original way whose  keys are transferred
     * @param wayChunks the way chunks
     * @return the new way objects
     */
    protected static List<Way> createNewWaysFromChunks(Way way, Iterable<List<Node>> wayChunks) {
        final List<Way> newWays = new ArrayList<>();
        for (List<Node> wayChunk : wayChunks) {
            Way wayToAdd = new Way();
            wayToAdd.setKeys(way.getKeys());
            wayToAdd.setNodes(wayChunk);
            newWays.add(wayToAdd);
        }
        return newWays;
    }

    /**
     * Splits the way {@code way} into chunks of {@code wayChunks} and replies
     * the result of this process in an instance of {@link SplitWayResult}.
     *
     * Note that changes are not applied to the data yet. You have to
     * submit the command in {@link SplitWayResult#getCommand()} first,
     * i.e. {@code Main.main.undoredo.add(result.getCommand())}.
     *
     * @param layer the layer which the way belongs to.
     * @param way the way to split. Must not be null.
     * @param wayChunks the list of way chunks into the way is split. Must not be null.
     * @param selection The list of currently selected primitives
     * @return the result from the split operation
     */
    public static SplitWayResult splitWay(OsmDataLayer layer, Way way, List<List<Node>> wayChunks,
            Collection<? extends OsmPrimitive> selection) {
        return splitWay(layer, way, wayChunks, selection, Strategy.keepLongestChunk());
    }

    /**
     * Splits the way {@code way} into chunks of {@code wayChunks} and replies
     * the result of this process in an instance of {@link SplitWayResult}.
     * The {@link org.openstreetmap.josm.actions.SplitWayAction.Strategy} is used to determine which
     * way chunk should reuse the old id and its history.
     *
     * Note that changes are not applied to the data yet. You have to
     * submit the command in {@link SplitWayResult#getCommand()} first,
     * i.e. {@code Main.main.undoredo.add(result.getCommand())}.
     *
     * @param layer the layer which the way belongs to.
     * @param way the way to split. Must not be null.
     * @param wayChunks the list of way chunks into the way is split. Must not be null.
     * @param selection The list of currently selected primitives
     * @param splitStrategy The strategy used to determine which way chunk should reuse the old id and its history
     * @return the result from the split operation
     * @since 8954
     */
    public static SplitWayResult splitWay(OsmDataLayer layer, Way way, List<List<Node>> wayChunks,
            Collection<? extends OsmPrimitive> selection, Strategy splitStrategy) {
        // build a list of commands, and also a new selection list
        final List<OsmPrimitive> newSelection = new ArrayList<>(selection.size() + wayChunks.size());
        newSelection.addAll(selection);

        // Create all potential new ways
        final List<Way> newWays = createNewWaysFromChunks(way, wayChunks);

        // Determine which part reuses the existing way
        final Way wayToKeep = splitStrategy.determineWayToKeep(newWays);

        return wayToKeep != null ? doSplitWay(layer, way, wayToKeep, newWays, newSelection) : null;
    }

    static SplitWayResult doSplitWay(OsmDataLayer layer, Way way, Way wayToKeep, List<Way> newWays,
                                   List<OsmPrimitive> newSelection) {

        Collection<Command> commandList = new ArrayList<>(newWays.size());
        Collection<String> nowarnroles = Main.pref.getCollection("way.split.roles.nowarn",
                Arrays.asList("outer", "inner", "forward", "backward", "north", "south", "east", "west"));

        final MapFrame map = MainApplication.getMap();
        final boolean isMapModeDraw = map != null && map.mapMode == map.mapModeDraw;

        // Change the original way
        final Way changedWay = new Way(way);
        changedWay.setNodes(wayToKeep.getNodes());
        commandList.add(layer != null ? new ChangeCommand(layer, way, changedWay) : new ChangeCommand(way.getDataSet(), way, changedWay));
        if (!isMapModeDraw && !newSelection.contains(way)) {
            newSelection.add(way);
        }
        final int indexOfWayToKeep = newWays.indexOf(wayToKeep);
        newWays.remove(wayToKeep);

        if (!isMapModeDraw) {
            newSelection.addAll(newWays);
        }
        for (Way wayToAdd : newWays) {
            commandList.add(layer != null ? new AddCommand(layer, wayToAdd) : new AddCommand(way.getDataSet(), wayToAdd));
        }

        boolean warnmerole = false;
        boolean warnme = false;
        // now copy all relations to new way also

        for (Relation r : OsmPrimitive.getFilteredList(way.getReferrers(), Relation.class)) {
            if (!r.isUsable()) {
                continue;
            }
            Relation c = null;
            String type = Optional.ofNullable(r.get("type")).orElse("");

            int ic = 0;
            int ir = 0;
            List<RelationMember> relationMembers = r.getMembers();
            for (RelationMember rm: relationMembers) {
                if (rm.isWay() && rm.getMember() == way) {
                    boolean insert = true;
                    if ("restriction".equals(type) || "destination_sign".equals(type)) {
                        /* this code assumes the restriction is correct. No real error checking done */
                        String role = rm.getRole();
                        if ("from".equals(role) || "to".equals(role)) {
                            OsmPrimitive via = findVia(r, type);
                            List<Node> nodes = new ArrayList<>();
                            if (via != null) {
                                if (via instanceof Node) {
                                    nodes.add((Node) via);
                                } else if (via instanceof Way) {
                                    nodes.add(((Way) via).lastNode());
                                    nodes.add(((Way) via).firstNode());
                                }
                            }
                            Way res = null;
                            for (Node n : nodes) {
                                if (changedWay.isFirstLastNode(n)) {
                                    res = way;
                                }
                            }
                            if (res == null) {
                                for (Way wayToAdd : newWays) {
                                    for (Node n : nodes) {
                                        if (wayToAdd.isFirstLastNode(n)) {
                                            res = wayToAdd;
                                        }
                                    }
                                }
                                if (res != null) {
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
                        } else if (!"via".equals(role)) {
                            warnme = true;
                        }
                    } else if (!("route".equals(type)) && !("multipolygon".equals(type))) {
                        warnme = true;
                    }
                    if (c == null) {
                        c = new Relation(r);
                    }

                    if (insert) {
                        if (rm.hasRole() && !nowarnroles.contains(rm.getRole())) {
                            warnmerole = true;
                        }

                        Boolean backwards = null;
                        int k = 1;
                        while (ir - k >= 0 || ir + k < relationMembers.size()) {
                            if ((ir - k >= 0) && relationMembers.get(ir - k).isWay()) {
                                Way w = relationMembers.get(ir - k).getWay();
                                if ((w.lastNode() == way.firstNode()) || w.firstNode() == way.firstNode()) {
                                    backwards = Boolean.FALSE;
                                } else if ((w.firstNode() == way.lastNode()) || w.lastNode() == way.lastNode()) {
                                    backwards = Boolean.TRUE;
                                }
                                break;
                            }
                            if ((ir + k < relationMembers.size()) && relationMembers.get(ir + k).isWay()) {
                                Way w = relationMembers.get(ir + k).getWay();
                                if ((w.lastNode() == way.firstNode()) || w.firstNode() == way.firstNode()) {
                                    backwards = Boolean.TRUE;
                                } else if ((w.firstNode() == way.lastNode()) || w.lastNode() == way.lastNode()) {
                                    backwards = Boolean.FALSE;
                                }
                                break;
                            }
                            k++;
                        }

                        int j = ic;
                        final List<Way> waysToAddBefore = newWays.subList(0, indexOfWayToKeep);
                        for (Way wayToAdd : waysToAddBefore) {
                            RelationMember em = new RelationMember(rm.getRole(), wayToAdd);
                            j++;
                            if (Boolean.TRUE.equals(backwards)) {
                                c.addMember(ic + 1, em);
                            } else {
                                c.addMember(j - 1, em);
                            }
                        }
                        final List<Way> waysToAddAfter = newWays.subList(indexOfWayToKeep, newWays.size());
                        for (Way wayToAdd : waysToAddAfter) {
                            RelationMember em = new RelationMember(rm.getRole(), wayToAdd);
                            j++;
                            if (Boolean.TRUE.equals(backwards)) {
                                c.addMember(ic, em);
                            } else {
                                c.addMember(j, em);
                            }
                        }
                        ic = j;
                    }
                }
                ic++;
                ir++;
            }

            if (c != null) {
                commandList.add(layer != null ? new ChangeCommand(layer, r, c) : new ChangeCommand(r.getDataSet(), r, c));
            }
        }
        if (warnmerole) {
            new Notification(
                    tr("A role based relation membership was copied to all new ways.<br>You should verify this and correct it when necessary."))
                    .setIcon(JOptionPane.WARNING_MESSAGE)
                    .show();
        } else if (warnme) {
            new Notification(
                    tr("A relation membership was copied to all new ways.<br>You should verify this and correct it when necessary."))
                    .setIcon(JOptionPane.WARNING_MESSAGE)
                    .show();
        }

        return new SplitWayResult(
                new SequenceCommand(
                        /* for correct i18n of plural forms - see #9110 */
                        trn("Split way {0} into {1} part", "Split way {0} into {1} parts", newWays.size() + 1,
                                way.getDisplayName(DefaultNameFormatter.getInstance()), newWays.size() + 1),
                        commandList
                        ),
                        newSelection,
                        way,
                        newWays
                );
    }

    static OsmPrimitive findVia(Relation r, String type) {
        for (RelationMember rmv : r.getMembers()) {
            if (("restriction".equals(type) && "via".equals(rmv.getRole()))
             || ("destination_sign".equals(type) && rmv.hasRole("sign", "intersection"))) {
                return rmv.getMember();
            }
        }
        return null;
    }

    /**
     * Splits the way {@code way} at the nodes in {@code atNodes} and replies
     * the result of this process in an instance of {@link SplitWayResult}.
     *
     * Note that changes are not applied to the data yet. You have to
     * submit the command in {@link SplitWayResult#getCommand()} first,
     * i.e. {@code Main.main.undoredo.add(result.getCommand())}.
     *
     * Replies null if the way couldn't be split at the given nodes.
     *
     * @param layer the layer which the way belongs to.
     * @param way the way to split. Must not be null.
     * @param atNodes the list of nodes where the way is split. Must not be null.
     * @param selection The list of currently selected primitives
     * @return the result from the split operation
     */
    public static SplitWayResult split(OsmDataLayer layer, Way way, List<Node> atNodes, Collection<? extends OsmPrimitive> selection) {
        List<List<Node>> chunks = buildSplitChunks(way, atNodes);
        return chunks != null ? splitWay(layer, way, chunks, selection) : null;
    }

    @Override
    protected void updateEnabledState() {
        updateEnabledStateOnCurrentSelection();
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
