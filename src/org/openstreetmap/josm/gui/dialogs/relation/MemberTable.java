// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.AutoScaleAction;
import org.openstreetmap.josm.actions.ZoomToAction;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.MapView.LayerChangeListener;
import org.openstreetmap.josm.gui.dialogs.relation.sort.WayConnectionType;
import org.openstreetmap.josm.gui.dialogs.relation.sort.WayConnectionType.Direction;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.util.HighlightHelper;
import org.openstreetmap.josm.gui.widgets.OsmPrimitivesTable;

public class MemberTable extends OsmPrimitivesTable implements IMemberModelListener {

    /** the additional actions in popup menu */
    private ZoomToGapAction zoomToGap;
    private HighlightHelper highlightHelper = new HighlightHelper();
    private boolean highlightEnabled;

    /**
     * constructor for relation member table
     *
     * @param layer the data layer of the relation
     * @param model the table model
     */
    public MemberTable(OsmDataLayer layer, MemberTableModel model) {
        super(model, new MemberTableColumnModel(layer.data), model.getSelectionModel());
        setLayer(layer);
        model.addMemberModelListener(this);
        init();
    }

    /**
     * initialize the table
     */
    protected void init() {
        MemberRoleCellEditor ce = (MemberRoleCellEditor)getColumnModel().getColumn(0).getCellEditor();
        setRowHeight(ce.getEditor().getPreferredSize().height);
        setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);

        // make ENTER behave like TAB
        //
        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, false), "selectNextColumnCell");

        initHighlighting();

        // install custom navigation actions
        //
        getActionMap().put("selectNextColumnCell", new SelectNextColumnCellAction());
        getActionMap().put("selectPreviousColumnCell", new SelectPreviousColumnCellAction());
    }

    @Override
    protected ZoomToAction buildZoomToAction() {
        return new ZoomToAction(this);
    }

    @Override
    protected JPopupMenu buildPopupMenu() {
        JPopupMenu menu = super.buildPopupMenu();
        zoomToGap = new ZoomToGapAction();
        MapView.addLayerChangeListener(zoomToGap);
        getSelectionModel().addListSelectionListener(zoomToGap);
        menu.add(zoomToGap);
        menu.addSeparator();
        menu.add(new SelectPreviousGapAction());
        menu.add(new SelectNextGapAction());
        return menu;
    }

    @Override
    public Dimension getPreferredSize(){
        Container c = getParent();
        while(c != null && ! (c instanceof JViewport)) {
            c = c.getParent();
        }
        if (c != null) {
            Dimension d = super.getPreferredSize();
            d.width = c.getSize().width;
            return d;
        }
        return super.getPreferredSize();
    }

    @Override
    public void makeMemberVisible(int index) {
        scrollRectToVisible(getCellRect(index, 0, true));
    }

    ListSelectionListener highlighterListener = new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent lse) {
                if (Main.isDisplayingMapView()) {
                    Collection<RelationMember> sel = getMemberTableModel().getSelectedMembers();
                    final List<OsmPrimitive> toHighlight = new ArrayList<OsmPrimitive>();
                    for (RelationMember r: sel) {
                        if (r.getMember().isUsable()) {
                            toHighlight.add(r.getMember());
                        }
                    }
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            if (highlightHelper.highlightOnly(toHighlight)) {
                                Main.map.mapView.repaint();
                            }
                        }
                    });
                }
            }};

    private void initHighlighting() {
        highlightEnabled = Main.pref.getBoolean("draw.target-highlight", true);
        if (!highlightEnabled) return;
        getMemberTableModel().getSelectionModel().addListSelectionListener(highlighterListener);
        if (Main.isDisplayingMapView()) {
            HighlightHelper.clearAllHighlighted();
            Main.map.mapView.repaint();
        }
    }

    /**
     * Action to be run when the user navigates to the next cell in the table, for instance by
     * pressing TAB or ENTER. The action alters the standard navigation path from cell to cell: <ul>
     * <li>it jumps over cells in the first column</li> <li>it automatically add a new empty row
     * when the user leaves the last cell in the table</li></ul>
     *
     *
     */
    class SelectNextColumnCellAction extends AbstractAction {
        @Override
        public void actionPerformed(ActionEvent e) {
            run();
        }

        public void run() {
            int col = getSelectedColumn();
            int row = getSelectedRow();
            if (getCellEditor() != null) {
                getCellEditor().stopCellEditing();
            }

            if (col == 0 && row < getRowCount() - 1) {
                row++;
            } else if (row < getRowCount() - 1) {
                col = 0;
                row++;
            } else {
                // go to next component, no more rows in this table
                KeyboardFocusManager manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
                manager.focusNextComponent();
                return;
            }
            changeSelection(row, col, false, false);
        }
    }

    /**
     * Action to be run when the user navigates to the previous cell in the table, for instance by
     * pressing Shift-TAB
     *
     */
    private class SelectPreviousColumnCellAction extends AbstractAction {

        @Override
        public void actionPerformed(ActionEvent e) {
            int col = getSelectedColumn();
            int row = getSelectedRow();
            if (getCellEditor() != null) {
                getCellEditor().stopCellEditing();
            }

            if (col <= 0 && row <= 0) {
                // change nothing
            } else if (row > 0) {
                col = 0;
                row--;
            }
            changeSelection(row, col, false, false);
        }
    }

    @Override
    public void unlinkAsListener() {
        super.unlinkAsListener();
        MapView.removeLayerChangeListener(zoomToGap);
    }

    public void stopHighlighting() {
        if (highlighterListener == null) return;
        if (!highlightEnabled) return;
        getMemberTableModel().getSelectionModel().removeListSelectionListener(highlighterListener);
        highlighterListener = null;
        if (Main.isDisplayingMapView()) {
            HighlightHelper.clearAllHighlighted();
            Main.map.mapView.repaint();
        }
    }

    private class SelectPreviousGapAction extends AbstractAction {

        public SelectPreviousGapAction() {
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

        public SelectNextGapAction() {
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

    private class ZoomToGapAction extends AbstractAction implements LayerChangeListener, ListSelectionListener {

        public ZoomToGapAction() {
            putValue(NAME, tr("Zoom to Gap"));
            putValue(SHORT_DESCRIPTION, tr("Zoom to the gap in the way sequence"));
            updateEnabledState();
        }

        private WayConnectionType getConnectionType() {
            return getMemberTableModel().getWayConnection(getSelectedRows()[0]);
        }

        private final Collection<Direction> connectionTypesOfInterest = Arrays.asList(WayConnectionType.Direction.FORWARD, WayConnectionType.Direction.BACKWARD);

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
                    && Main.main.getEditLayer() == getLayer()
                    && getSelectedRowCount() == 1
                    && hasGap());
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {
            updateEnabledState();
        }

        @Override
        public void activeLayerChange(Layer oldLayer, Layer newLayer) {
            updateEnabledState();
        }

        @Override
        public void layerAdded(Layer newLayer) {
            updateEnabledState();
        }

        @Override
        public void layerRemoved(Layer oldLayer) {
            updateEnabledState();
        }
    }

    protected MemberTableModel getMemberTableModel() {
        return (MemberTableModel) getModel();
    }
}
