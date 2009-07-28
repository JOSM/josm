// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableColumnModel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.AutoScaleAction;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.layer.Layer.LayerChangeListener;

import static org.openstreetmap.josm.tools.I18n.tr;

public class MemberTable extends JTable implements IMemberModelListener {

    /**
     * the data layer in whose context relation members are edited in this table
     */
    protected OsmDataLayer layer;

    /** the popup menu */
    protected JPopupMenu popupMenu;

    /**
     * constructor
     * 
     * @param model
     * @param columnModel
     */
    public MemberTable(OsmDataLayer layer, MemberTableModel model) {
        super(model, new MemberTableColumnModel(), model.getSelectionModel());
        this.layer = layer;
        model.addMemberModelListener(this);
        init();

    }

    /**
     * initialize the table
     */
    protected void init() {
        setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);

        // make ENTER behave like TAB
        //
        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, false), "selectNextColumnCell");

        // install custom navigation actions
        //
        getActionMap().put("selectNextColumnCell", new SelectNextColumnCellAction());
        getActionMap().put("selectPreviousColumnCell", new SelectPreviousColumnCellAction());

        addMouseListener(new PopupListener());
    }

    /**
     * adjusts the width of the columns for the tag name and the tag value to the width of the
     * scroll panes viewport.
     * 
     * Note: {@see #getPreferredScrollableViewportSize()} did not work as expected
     * 
     * @param scrollPaneWidth the width of the scroll panes viewport
     */
    public void adjustColumnWidth(int scrollPaneWidth) {
        TableColumnModel tcm = getColumnModel();
        int width = scrollPaneWidth;
        width = width / 3;
        if (width > 0) {
            tcm.getColumn(0).setMinWidth(width);
            tcm.getColumn(0).setMaxWidth(width);
            tcm.getColumn(1).setMinWidth(width);
            tcm.getColumn(1).setMaxWidth(width);
            tcm.getColumn(2).setMinWidth(width);
            tcm.getColumn(2).setMaxWidth(width);

        }
    }

    public void makeMemberVisible(int index) {
        scrollRectToVisible(getCellRect(index, 0, true));
    }

    /**
     * Action to be run when the user navigates to the next cell in the table, for instance by
     * pressing TAB or ENTER. The action alters the standard navigation path from cell to cell: <ul>
     * <li>it jumps over cells in the first column</li> <li>it automatically add a new empty row
     * when the user leaves the last cell in the table</li> <ul>
     * 
     * 
     */
    class SelectNextColumnCellAction extends AbstractAction {
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
            }
            changeSelection(row, col, false, false);
        }
    }

    /**
     * Action to be run when the user navigates to the previous cell in the table, for instance by
     * pressing Shift-TAB
     * 
     */
    class SelectPreviousColumnCellAction extends AbstractAction {

        public void actionPerformed(ActionEvent e) {
            int col = getSelectedColumn();
            int row = getSelectedRow();
            if (getCellEditor() != null) {
                getCellEditor().stopCellEditing();
            }

            if (col == 0 && row == 0) {
                // change nothing
            } else if (row > 0) {
                col = 0;
                row--;
            }
            changeSelection(row, col, false, false);
        }
    }

    /**
     * creates the popup men
     */
    protected void createPopupMenu() {
        popupMenu = new JPopupMenu();
        ZoomToAction zoomToAction = new ZoomToAction();
        Layer.listeners.add(zoomToAction);
        getSelectionModel().addListSelectionListener(zoomToAction);
        popupMenu.add(zoomToAction);
    }

    /**
     * Replies the popup menu for this table
     * 
     * @return the popup menu
     */
    protected JPopupMenu getPopUpMenu() {
        if (popupMenu == null) {
            createPopupMenu();
        }
        return popupMenu;
    }

    class PopupListener extends MouseAdapter {
        @Override
        public void mousePressed(MouseEvent e) {
            showPopup(e);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            showPopup(e);
        }

        private void showPopup(MouseEvent e) {
            if (e.isPopupTrigger()) {
                getPopUpMenu().show(e.getComponent(), e.getX(), e.getY());
            }
        }
    }

    class ZoomToAction extends AbstractAction implements LayerChangeListener, ListSelectionListener {
        public ZoomToAction() {
            putValue(NAME, tr("Zoom to"));
            putValue(SHORT_DESCRIPTION, tr("Zoom to primitive the first selected member refers to"));
            updateEnabledState();
        }

        public void actionPerformed(ActionEvent e) {
            if (! isEnabled())
                return;
            int rows[] = getSelectedRows();
            if (rows == null || rows.length == 0)
                return;
            int row = rows[0];
            OsmPrimitive primitive = getMemberTableModel().getReferredPrimitive(row);
            layer.data.setSelected(primitive);
            DataSet.fireSelectionChanged(layer.data.getSelected());
            AutoScaleAction action = new AutoScaleAction("selection");
            action.autoScale();
        }

        protected void updateEnabledState() {
            if (Main.main == null || Main.main.getEditLayer() != layer) {
                setEnabled(false);
                putValue(SHORT_DESCRIPTION, tr("Zooming disabled because layer of this relation is not active"));
                return;
            }
            if (getSelectedRowCount() == 0) {
                setEnabled(false);
                putValue(SHORT_DESCRIPTION, tr("Zooming disabled because there is no selected member"));
                return;
            }
            setEnabled(true);
            putValue(SHORT_DESCRIPTION, tr("Zoom to primitive the first selected member refers to"));
        }

        public void valueChanged(ListSelectionEvent e) {
            updateEnabledState();
        }

        public void activeLayerChange(Layer oldLayer, Layer newLayer) {
            updateEnabledState();
        }

        public void layerAdded(Layer newLayer) {
            updateEnabledState();
        }

        public void layerRemoved(Layer oldLayer) {
            updateEnabledState();
        }
    }

    protected MemberTableModel getMemberTableModel() {
        return (MemberTableModel) getModel();
    }
}
