// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.DropMode;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.AutoScaleAction;
import org.openstreetmap.josm.actions.ZoomToAction;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.dialogs.relation.sort.WayConnectionType;
import org.openstreetmap.josm.gui.dialogs.relation.sort.WayConnectionType.Direction;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerAddEvent;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerChangeListener;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerOrderChangeEvent;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerRemoveEvent;
import org.openstreetmap.josm.gui.layer.MainLayerManager.ActiveLayerChangeEvent;
import org.openstreetmap.josm.gui.layer.MainLayerManager.ActiveLayerChangeListener;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.util.HighlightHelper;
import org.openstreetmap.josm.gui.widgets.OsmPrimitivesTable;

public class MemberTable extends OsmPrimitivesTable implements IMemberModelListener {

    /** the additional actions in popup menu */
    private ZoomToGapAction zoomToGap;
    private final transient HighlightHelper highlightHelper = new HighlightHelper();
    private boolean highlightEnabled;

    /**
     * constructor for relation member table
     *
     * @param layer the data layer of the relation. Must not be null
     * @param relation the relation. Can be null
     * @param model the table model
     */
    public MemberTable(OsmDataLayer layer, Relation relation, MemberTableModel model) {
        super(model, new MemberTableColumnModel(layer.data, relation), model.getSelectionModel());
        setLayer(layer);
        model.addMemberModelListener(this);

        MemberRoleCellEditor ce = (MemberRoleCellEditor) getColumnModel().getColumn(0).getCellEditor();
        setRowHeight(ce.getEditor().getPreferredSize().height);
        setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);

        installCustomNavigation(0);
        initHighlighting();

        if (!GraphicsEnvironment.isHeadless()) {
            setTransferHandler(new MemberTransferHandler());
            setFillsViewportHeight(true); // allow drop on empty table
            if (!GraphicsEnvironment.isHeadless()) {
                setDragEnabled(true);
            }
            setDropMode(DropMode.INSERT_ROWS);
        }
    }

    @Override
    protected ZoomToAction buildZoomToAction() {
        return new ZoomToAction(this);
    }

    @Override
    protected JPopupMenu buildPopupMenu() {
        JPopupMenu menu = super.buildPopupMenu();
        zoomToGap = new ZoomToGapAction();
        registerListeners();
        menu.addSeparator();
        getSelectionModel().addListSelectionListener(zoomToGap);
        menu.add(zoomToGap);
        menu.addSeparator();
        menu.add(new SelectPreviousGapAction());
        menu.add(new SelectNextGapAction());
        return menu;
    }

    @Override
    public Dimension getPreferredSize() {
        return getPreferredFullWidthSize();
    }

    @Override
    public void makeMemberVisible(int index) {
        scrollRectToVisible(getCellRect(index, 0, true));
    }

    private transient ListSelectionListener highlighterListener = lse -> {
        if (MainApplication.isDisplayingMapView()) {
            Collection<RelationMember> sel = getMemberTableModel().getSelectedMembers();
            final Set<OsmPrimitive> toHighlight = new HashSet<>();
            for (RelationMember r: sel) {
                if (r.getMember().isUsable()) {
                    toHighlight.add(r.getMember());
                }
            }
            SwingUtilities.invokeLater(() -> {
                if (MainApplication.isDisplayingMapView() && highlightHelper.highlightOnly(toHighlight)) {
                    MainApplication.getMap().mapView.repaint();
                }
            });
        }
    };

    private void initHighlighting() {
        highlightEnabled = Main.pref.getBoolean("draw.target-highlight", true);
        if (!highlightEnabled) return;
        getMemberTableModel().getSelectionModel().addListSelectionListener(highlighterListener);
        if (MainApplication.isDisplayingMapView()) {
            HighlightHelper.clearAllHighlighted();
            MainApplication.getMap().mapView.repaint();
        }
    }

    @Override
    public void registerListeners() {
        MainApplication.getLayerManager().addLayerChangeListener(zoomToGap);
        MainApplication.getLayerManager().addActiveLayerChangeListener(zoomToGap);
        super.registerListeners();
    }

    @Override
    public void unregisterListeners() {
        super.unregisterListeners();
        MainApplication.getLayerManager().removeLayerChangeListener(zoomToGap);
        MainApplication.getLayerManager().removeActiveLayerChangeListener(zoomToGap);
    }

    public void stopHighlighting() {
        if (highlighterListener == null) return;
        if (!highlightEnabled) return;
        getMemberTableModel().getSelectionModel().removeListSelectionListener(highlighterListener);
        highlighterListener = null;
        if (MainApplication.isDisplayingMapView()) {
            HighlightHelper.clearAllHighlighted();
            MainApplication.getMap().mapView.repaint();
        }
    }

    private class SelectPreviousGapAction extends AbstractAction {

        SelectPreviousGapAction() {
            putValue(NAME, tr("Select previous Gap"));
            putValue(SHORT_DESCRIPTION, tr("Select the previous relation member which gives rise to a gap"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            int i = getSelectedRow() - 1;
            while (i >= 0 && getMemberTableModel().getWayConnection(i).linkPrev) {
                i--;
            }
            if (i >= 0) {
                getSelectionModel().setSelectionInterval(i, i);
            }
        }
    }

    private class SelectNextGapAction extends AbstractAction {

        SelectNextGapAction() {
            putValue(NAME, tr("Select next Gap"));
            putValue(SHORT_DESCRIPTION, tr("Select the next relation member which gives rise to a gap"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            int i = getSelectedRow() + 1;
            while (i < getRowCount() && getMemberTableModel().getWayConnection(i).linkNext) {
                i++;
            }
            if (i < getRowCount()) {
                getSelectionModel().setSelectionInterval(i, i);
            }
        }
    }

    private class ZoomToGapAction extends AbstractAction implements LayerChangeListener, ActiveLayerChangeListener, ListSelectionListener {

        /**
         * Constructs a new {@code ZoomToGapAction}.
         */
        ZoomToGapAction() {
            putValue(NAME, tr("Zoom to Gap"));
            putValue(SHORT_DESCRIPTION, tr("Zoom to the gap in the way sequence"));
            updateEnabledState();
        }

        private WayConnectionType getConnectionType() {
            return getMemberTableModel().getWayConnection(getSelectedRows()[0]);
        }

        private final Collection<Direction> connectionTypesOfInterest = Arrays.asList(
                WayConnectionType.Direction.FORWARD, WayConnectionType.Direction.BACKWARD);

        private boolean hasGap() {
            WayConnectionType connectionType = getConnectionType();
            return connectionTypesOfInterest.contains(connectionType.direction)
                    && !(connectionType.linkNext && connectionType.linkPrev);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            WayConnectionType connectionType = getConnectionType();
            Way way = (Way) getMemberTableModel().getReferredPrimitive(getSelectedRows()[0]);
            if (!connectionType.linkPrev) {
                getLayer().data.setSelected(WayConnectionType.Direction.FORWARD.equals(connectionType.direction)
                        ? way.firstNode() : way.lastNode());
                AutoScaleAction.autoScale("selection");
            } else if (!connectionType.linkNext) {
                getLayer().data.setSelected(WayConnectionType.Direction.FORWARD.equals(connectionType.direction)
                        ? way.lastNode() : way.firstNode());
                AutoScaleAction.autoScale("selection");
            }
        }

        private void updateEnabledState() {
            setEnabled(Main.main != null
                    && MainApplication.getLayerManager().getEditLayer() == getLayer()
                    && getSelectedRowCount() == 1
                    && hasGap());
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {
            updateEnabledState();
        }

        @Override
        public void activeOrEditLayerChanged(ActiveLayerChangeEvent e) {
            updateEnabledState();
        }

        @Override
        public void layerAdded(LayerAddEvent e) {
            updateEnabledState();
        }

        @Override
        public void layerRemoving(LayerRemoveEvent e) {
            updateEnabledState();
        }

        @Override
        public void layerOrderChanged(LayerOrderChangeEvent e) {
            // Do nothing
        }
    }

    protected MemberTableModel getMemberTableModel() {
        return (MemberTableModel) getModel();
    }
}
