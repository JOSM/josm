// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation;

import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableColumnModel;

public class MemberTable extends JTable implements IMemberModelListener {

    /**
     * constructor
     * 
     * @param model
     * @param columnModel
     */
    public MemberTable(MemberTableModel model) {
        super(model, new MemberTableColumnModel(), model.getSelectionModel());
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
        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
        .put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, false), "selectNextColumnCell");

        // install custom navigation actions
        //
        getActionMap().put("selectNextColumnCell", new SelectNextColumnCellAction());
        getActionMap().put("selectPreviousColumnCell", new SelectPreviousColumnCellAction());
    }

    /**
     * adjusts the width of the columns for the tag name and the tag value
     * to the width of the scroll panes viewport.
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
        scrollRectToVisible(
                getCellRect(index, 0, true)
        );
    }

    /**
     * Action to be run when the user navigates to the next cell in the table,
     * for instance by pressing TAB or ENTER. The action alters the standard
     * navigation path from cell to cell:
     * <ul>
     *   <li>it jumps over cells in the first column</li>
     *   <li>it automatically add a new empty row when the user leaves the
     *   last cell in the table</li>
     * <ul>
     *
     *
     */
    class SelectNextColumnCellAction extends AbstractAction  {
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
            } else if (row < getRowCount()-1) {
                col=0;
                row++;
            }
            changeSelection(row, col, false, false);
        }
    }


    /**
     * Action to be run when the user navigates to the previous cell in the table,
     * for instance by pressing Shift-TAB
     *
     */
    class SelectPreviousColumnCellAction extends AbstractAction  {

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


}
