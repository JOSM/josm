// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;

import org.openstreetmap.josm.command.SplitWayCommand;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.DefaultNameFormatter;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.data.osm.PrimitiveId;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.WaySegment;
import org.openstreetmap.josm.data.osm.event.AbstractDatasetChangedEvent;
import org.openstreetmap.josm.data.osm.event.DataChangedEvent;
import org.openstreetmap.josm.data.osm.event.DataSetListener;
import org.openstreetmap.josm.data.osm.event.NodeMovedEvent;
import org.openstreetmap.josm.data.osm.event.PrimitivesAddedEvent;
import org.openstreetmap.josm.data.osm.event.PrimitivesRemovedEvent;
import org.openstreetmap.josm.data.osm.event.RelationMembersChangedEvent;
import org.openstreetmap.josm.data.osm.event.TagsChangedEvent;
import org.openstreetmap.josm.data.osm.event.WayNodesChangedEvent;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.Shortcut;
import org.openstreetmap.josm.tools.Utils;

/**
 * Splits a way into multiple ways (all identical except for their node list).
 *
 * Ways are just split at the selected nodes.  The nodes remain in their
 * original order.  Selected nodes at the end of a way are ignored.
 */
public class SplitWayAction extends JosmAction {

    /**
     * Create a new SplitWayAction.
     */
    public SplitWayAction() {
        super(tr("Split Way"), "mapmode/splitway", tr("Split a way at the selected node."),
                Shortcut.registerShortcut("tools:splitway", tr("Tools: {0}", tr("Split Way")), KeyEvent.VK_P, Shortcut.DIRECT), true);
        setHelpId(ht("/Action/SplitWay"));
    }

    /**
     * Called when the action is executed.
     *
     * This method performs an expensive check whether the selection clearly defines one
     * of the split actions outlined above, and if yes, calls the splitWay method.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        runOn(getLayerManager().getEditDataSet());
    }

    /**
     * Run the action on the given dataset.
     * @param ds dataset
     * @since 14542
     */
    public static void runOn(DataSet ds) {

        if (SegmentToKeepSelectionDialog.DISPLAY_COUNT.get() > 0) {
            new Notification(tr("Cannot split since another split operation is already in progress"))
                    .setIcon(JOptionPane.WARNING_MESSAGE).show();
            return;
        }

        List<Node> selectedNodes = new ArrayList<>(ds.getSelectedNodes());
        List<Way> selectedWays = new ArrayList<>(ds.getSelectedWays());
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

        // If several ways have been found, remove ways that do not have selected node in the middle
        if (applicableWays.size() > 1) {
             applicableWays.removeIf(w -> selectedNodes.stream().noneMatch(w::isInnerNode));
        }

        // Smart way selection: if only one highway/railway/waterway is applicable, use that one
        if (applicableWays.size() > 1) {
            final List<Way> mainWays = applicableWays.stream()
                    .filter(w -> w.hasKey("highway", "railway", "waterway"))
                    .collect(Collectors.toList());
            if (mainWays.size() == 1) {
                applicableWays = mainWays;
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
        } else if (!checkAndConfirmOutlyingOperation("splitway", tr("Split way confirmation"),
                tr("You are about to split a way that may have referrers that are not yet downloaded.")
                        + "<br/>"
                        + tr("This can lead to broken relations.") + "<br/>"
                        + tr("Do you really want to split?"),
                tr("The selected area is incomplete. Continue?"),
                applicableWays, null)) {
            return;
        }

        // Finally, applicableWays contains only one perfect way
        final Way selectedWay = applicableWays.get(0);
        final List<OsmPrimitive> sel = new ArrayList<>(ds.getSelectedRelations());
        sel.addAll(selectedWays);
        doSplitWayShowSegmentSelection(selectedWay, selectedNodes, sel);
    }

    /**
     * Perform way splitting after presenting the user with a choice which way segment history should be preserved (in expert mode)
     * @param splitWay The way to split
     * @param splitNodes The nodes at which the way should be split
     * @param selection (Optional) selection which should be updated
     *
     * @since 18759
     */
    public static void doSplitWayShowSegmentSelection(Way splitWay, List<Node> splitNodes, List<OsmPrimitive> selection) {
        final List<List<Node>> wayChunks = SplitWayCommand.buildSplitChunks(splitWay, splitNodes);
        if (wayChunks != null) {
            final List<Way> newWays = SplitWayCommand.createNewWaysFromChunks(splitWay, wayChunks);
            final Way wayToKeep = SplitWayCommand.Strategy.keepLongestChunk().determineWayToKeep(newWays);

            if (ExpertToggleAction.isExpert() && !splitWay.isNew()) {
                final ExtendedDialog dialog = new SegmentToKeepSelectionDialog(splitWay, newWays, wayToKeep, splitNodes, selection);
                dialog.toggleEnable("way.split.segment-selection-dialog");
                if (!dialog.toggleCheckState()) {
                    dialog.setModal(false);
                    dialog.showDialog();
                    return; // splitting is performed in SegmentToKeepSelectionDialog.buttonAction()
                }
            }
            if (wayToKeep != null) {
                doSplitWay(splitWay, wayToKeep, newWays, selection);
            }
        }
    }

    /**
     * A dialog to query which way segment should reuse the history of the way to split.
     */
    static class SegmentToKeepSelectionDialog extends ExtendedDialog {
        static final AtomicInteger DISPLAY_COUNT = new AtomicInteger();
        final transient Way selectedWay;
        final JList<Way> list;
        final transient List<OsmPrimitive> selection;
        final transient List<Node> selectedNodes;
        final SplitWayDataSetListener dataSetListener;
        transient List<Way> newWays;
        transient Way wayToKeep;

        SegmentToKeepSelectionDialog(
                Way selectedWay, List<Way> newWays, Way wayToKeep, List<Node> selectedNodes, List<OsmPrimitive> selection) {
            super(MainApplication.getMainFrame(), tr("Which way segment should reuse the history of {0}?", selectedWay.getId()),
                    new String[]{tr("Ok"), tr("Cancel")}, true);

            this.selectedWay = selectedWay;
            this.newWays = newWays;
            this.selectedNodes = selectedNodes;
            this.selection = selection;
            this.wayToKeep = wayToKeep;
            this.list = new JList<>(newWays.toArray(new Way[0]));
            this.dataSetListener = new SplitWayDataSetListener();

            configureList();

            setButtonIcons("ok", "cancel");
            final JPanel pane = new JPanel(new GridBagLayout());
            pane.add(new JLabel(getTitle()), GBC.eol().fill(GridBagConstraints.HORIZONTAL));
            pane.add(list, GBC.eop().fill(GridBagConstraints.HORIZONTAL));
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
            final DataSet ds = selectedWay.getDataSet();
            if (ds != null) {
                ds.setHighlightedWaySegments(segments);
                MainApplication.getMap().mapView.repaint();
            }
        }

        @Override
        public void setVisible(boolean visible) {
            super.setVisible(visible);
            final DataSet ds = selectedWay.getDataSet();
            if (visible) {
                DISPLAY_COUNT.incrementAndGet();
                list.setSelectedValue(wayToKeep, true);
                if (ds != null) {
                    ds.addDataSetListener(dataSetListener);
                }
                list.requestFocusInWindow();
            } else {
                if (ds != null) {
                    ds.removeDataSetListener(dataSetListener);
                }
                setHighlightedWaySegments(Collections.emptyList());
                DISPLAY_COUNT.decrementAndGet();
                if (getValue() != 1 && selectedWay.getDataSet() != null) {
                    newWays.forEach(w -> w.setNodes(null)); // see 19885
                }
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

        private final class SplitWayDataSetListener implements DataSetListener {

            @Override
            public void primitivesAdded(PrimitivesAddedEvent event) {
            }

            @Override
            public void primitivesRemoved(PrimitivesRemovedEvent event) {
                if (event.getPrimitives().stream().anyMatch(p -> p instanceof Way)) {
                    updateWaySegments();
                }
            }

            @Override
            public void tagsChanged(TagsChangedEvent event) {}

            @Override
            public void nodeMoved(NodeMovedEvent event) {}

            @Override
            public void wayNodesChanged(WayNodesChangedEvent event) {
                updateWaySegments();
            }

            @Override
            public void relationMembersChanged(RelationMembersChangedEvent event) {}

            @Override
            public void otherDatasetChange(AbstractDatasetChangedEvent event) {}

            @Override
            public void dataChanged(DataChangedEvent event) {}

            private void updateWaySegments() {
                if (!selectedWay.isUsable()) {
                    setVisible(false);
                    return;
                }

                List<List<Node>> chunks = SplitWayCommand.buildSplitChunks(selectedWay, selectedNodes);
                if (chunks == null) {
                    setVisible(false);
                    return;
                }

                newWays = SplitWayCommand.createNewWaysFromChunks(selectedWay, chunks);
                if (list.getSelectedIndex() < newWays.size()) {
                    wayToKeep = newWays.get(list.getSelectedIndex());
                } else {
                    wayToKeep = SplitWayCommand.Strategy.keepLongestChunk().determineWayToKeep(newWays);
                }
                list.setListData(newWays.toArray(new Way[0]));
                list.setSelectedValue(wayToKeep, true);
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
            final Node n = selectedNodes.get(0);
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

    static void doSplitWay(Way way, Way wayToKeep, List<Way> newWays, List<OsmPrimitive> newSelection) {
        final MapFrame map = MainApplication.getMap();
        final boolean isMapModeDraw = map != null && map.mapMode == map.mapModeDraw;

        Optional<SplitWayCommand> splitWayCommand = SplitWayCommand.doSplitWay(
                way,
                wayToKeep,
                newWays,
                !isMapModeDraw ? newSelection : null,
                SplitWayCommand.WhenRelationOrderUncertain.ASK_USER_FOR_CONSENT_TO_DOWNLOAD
        );

        splitWayCommand.ifPresent(result -> {
            UndoRedoHandler.getInstance().add(result);
            List<? extends PrimitiveId> newSel = result.getNewSelection();
            if (!Utils.isEmpty(newSel)) {
                way.getDataSet().setSelected(newSel);
            }
        });
        if (!splitWayCommand.isPresent()) {
            newWays.forEach(w -> w.setNodes(null)); // see 19885
        }
    }

    @Override
    protected void updateEnabledState() {
        updateEnabledStateOnCurrentSelection();
    }

    @Override
    protected void updateEnabledState(Collection<? extends OsmPrimitive> selection) {
        // Selection still can be wrong, but let SplitWayAction process and tell user what's wrong
        setEnabled(OsmUtils.isOsmCollectionEditable(selection)
                && selection.stream().anyMatch(o -> o instanceof Node && !o.isIncomplete()));
    }
}
