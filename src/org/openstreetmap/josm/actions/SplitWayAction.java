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
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SplitWayCommand;
import org.openstreetmap.josm.data.osm.DefaultNameFormatter;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.PrimitiveId;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.WaySegment;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
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
     * @deprecated To be removed end of 2017. Use {@link SplitWayCommand} instead
     */
    @Deprecated
    public static class SplitWayResult {
        private final Command command;
        private final List<? extends PrimitiveId> newSelection;
        private final Way originalWay;
        private final List<Way> newWays;

        /**
         * Constructs a new {@code SplitWayResult}.
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
         * @param command The command to be performed to split the way (which is saved for later retrieval with {@link #getCommand})
         * @since 12828
         */
        protected SplitWayResult(SplitWayCommand command) {
            this.command = command;
            this.newSelection = command.getNewSelection();
            this.originalWay = command.getOriginalWay();
            this.newWays = command.getNewWays();
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
        final List<List<Node>> wayChunks = SplitWayCommand.buildSplitChunks(selectedWay, selectedNodes);
        if (wayChunks != null) {
            List<Relation> selectedRelations = OsmPrimitive.getFilteredList(selection, Relation.class);
            final List<OsmPrimitive> sel = new ArrayList<>(selectedWays.size() + selectedRelations.size());
            sel.addAll(selectedWays);
            sel.addAll(selectedRelations);

            final List<Way> newWays = createNewWaysFromChunks(selectedWay, wayChunks);
            final Way wayToKeep = SplitWayCommand.Strategy.keepLongestChunk().determineWayToKeep(newWays);

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
                doSplitWay(selectedWay, wayToKeep, newWays, sel);
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
                doSplitWay(selectedWay, list.getSelectedValue(), newWays, selection);
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
     * @deprecated to be removed end of 2017. Use {@link org.openstreetmap.josm.command.SplitWayCommand.Strategy} instead
     */
    @Deprecated
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
            return SplitWayCommand.Strategy.keepLongestChunk()::determineWayToKeep;
        }

        /**
         * Returns a strategy which selects the first way chunk.
         * @return strategy which selects the first way chunk
         */
        static Strategy keepFirstChunk() {
            return SplitWayCommand.Strategy.keepFirstChunk()::determineWayToKeep;
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
     * @deprecated To be removed end of 2017. Use {@link SplitWayCommand#buildSplitChunks} instead
     */
    @Deprecated
    public static List<List<Node>> buildSplitChunks(Way wayToSplit, List<Node> splitPoints) {
        return SplitWayCommand.buildSplitChunks(wayToSplit, splitPoints);
    }

    /**
     * Creates new way objects for the way chunks and transfers the keys from the original way.
     * @param way the original way whose  keys are transferred
     * @param wayChunks the way chunks
     * @return the new way objects
     * @deprecated To be removed end of 2017. Use {@link SplitWayCommand#createNewWaysFromChunks} instead
     */
    @Deprecated
    protected static List<Way> createNewWaysFromChunks(Way way, Iterable<List<Node>> wayChunks) {
        return SplitWayCommand.createNewWaysFromChunks(way, wayChunks);
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
     * @deprecated to be removed end of 2017. Use {@link SplitWayCommand#splitWay} instead
     */
    @Deprecated
    public static SplitWayResult splitWay(OsmDataLayer layer, Way way, List<List<Node>> wayChunks,
            Collection<? extends OsmPrimitive> selection) {
        return splitWay(way, wayChunks, selection);
    }

    /**
     * Splits the way {@code way} into chunks of {@code wayChunks} and replies
     * the result of this process in an instance of {@link SplitWayResult}.
     *
     * Note that changes are not applied to the data yet. You have to
     * submit the command in {@link SplitWayResult#getCommand()} first,
     * i.e. {@code Main.main.undoredo.add(result.getCommand())}.
     *
     * @param way the way to split. Must not be null.
     * @param wayChunks the list of way chunks into the way is split. Must not be null.
     * @param selection The list of currently selected primitives
     * @return the result from the split operation
     * @since 12718
     * @deprecated to be removed end of 2017. Use {@link SplitWayCommand#splitWay} instead
     */
    @Deprecated
    public static SplitWayResult splitWay(Way way, List<List<Node>> wayChunks,
            Collection<? extends OsmPrimitive> selection) {
        return splitWay(way, wayChunks, selection, Strategy.keepLongestChunk());
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
     * @deprecated to be removed end of 2017. Use {@link SplitWayCommand#splitWay} instead
     */
    @Deprecated
    public static SplitWayResult splitWay(OsmDataLayer layer, Way way, List<List<Node>> wayChunks,
            Collection<? extends OsmPrimitive> selection, Strategy splitStrategy) {
        return splitWay(way, wayChunks, selection, splitStrategy);
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
     * @param way the way to split. Must not be null.
     * @param wayChunks the list of way chunks into the way is split. Must not be null.
     * @param selection The list of currently selected primitives
     * @param splitStrategy The strategy used to determine which way chunk should reuse the old id and its history
     * @return the result from the split operation
     * @since 12718
     * @deprecated to be removed end of 2017. Use {@link SplitWayCommand#splitWay} instead
     */
    @Deprecated
    public static SplitWayResult splitWay(Way way, List<List<Node>> wayChunks,
            Collection<? extends OsmPrimitive> selection, Strategy splitStrategy) {
        SplitWayCommand cmd = SplitWayCommand.splitWay(way, wayChunks, selection, splitStrategy::determineWayToKeep);
        return cmd != null ? new SplitWayResult(cmd) : null;
    }

    static void doSplitWay(Way way, Way wayToKeep, List<Way> newWays, List<OsmPrimitive> newSelection) {
        final MapFrame map = MainApplication.getMap();
        final boolean isMapModeDraw = map != null && map.mapMode == map.mapModeDraw;
        final SplitWayCommand result = SplitWayCommand.doSplitWay(way, wayToKeep, newWays, !isMapModeDraw ? newSelection : null);
        MainApplication.undoRedo.add(result);
        List<? extends PrimitiveId> newSel = result.getNewSelection();
        if (newSel != null && !newSel.isEmpty()) {
            MainApplication.getLayerManager().getEditDataSet().setSelected(newSel);
        }
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
     * @deprecated to be removed end of 2017. Use {@link SplitWayCommand#split} instead
     */
    @Deprecated
    public static SplitWayResult split(OsmDataLayer layer, Way way, List<Node> atNodes, Collection<? extends OsmPrimitive> selection) {
        return split(way, atNodes, selection);
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
     * @param way the way to split. Must not be null.
     * @param atNodes the list of nodes where the way is split. Must not be null.
     * @param selection The list of currently selected primitives
     * @return the result from the split operation
     * @since 12718
     * @deprecated to be removed end of 2017. Use {@link SplitWayCommand#split} instead
     */
    @Deprecated
    public static SplitWayResult split(Way way, List<Node> atNodes, Collection<? extends OsmPrimitive> selection) {
        List<List<Node>> chunks = buildSplitChunks(way, atNodes);
        return chunks != null ? splitWay(way, chunks, selection) : null;
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
