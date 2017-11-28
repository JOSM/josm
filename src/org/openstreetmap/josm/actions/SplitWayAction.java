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

            final List<Way> newWays = SplitWayCommand.createNewWaysFromChunks(selectedWay, wayChunks);
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
