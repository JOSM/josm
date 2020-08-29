// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.widgets;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import org.openstreetmap.josm.gui.util.TableHelper;

/**
 * Generic table offering custom cell navigation features.
 * @since 9497
 */
public abstract class JosmTable extends JTable {

    private int colEnd;

    protected SelectNextColumnCellAction selectNextColumnCellAction;
    protected SelectPreviousColumnCellAction selectPreviousColumnCellAction;

    protected JosmTable(TableModel dm, TableColumnModel cm) {
        this(dm, cm, null);
    }

    protected JosmTable(TableModel dm, TableColumnModel cm, ListSelectionModel sm) {
        super(dm, cm, sm);
        TableHelper.setFont(this, getClass());
    }

    protected void installCustomNavigation(int colEnd) {
        // make ENTER behave like TAB
        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, false), "selectNextColumnCell");

        // install custom navigation actions
        this.colEnd = colEnd;
        selectNextColumnCellAction = new SelectNextColumnCellAction();
        selectPreviousColumnCellAction = new SelectPreviousColumnCellAction();
        getActionMap().put("selectNextColumnCell", selectNextColumnCellAction);
        getActionMap().put("selectPreviousColumnCell", selectPreviousColumnCellAction);
    }

    /**
     * Action to be run when the user navigates to the next cell in the table, for instance by
     * pressing TAB or ENTER. The action alters the standard navigation path from cell to cell: <ul>
     * <li>it jumps over cells in the first column</li>
     * <li>it automatically add a new empty row when the user leaves the last cell in the table</li></ul>
     */
    protected class SelectNextColumnCellAction extends AbstractAction {
        @Override
        public void actionPerformed(ActionEvent e) {
            int col = getSelectedColumn();
            int row = getSelectedRow();
            if (getCellEditor() != null) {
                getCellEditor().stopCellEditing();
            }

            if (col == colEnd && row < getRowCount() - 1) {
                row++;
            } else if (row < getRowCount() - 1) {
                col = colEnd;
                row++;
            } else {
                // go to next component, no more rows in this table
                KeyboardFocusManager manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
                manager.focusNextComponent();
                return;
            }
            changeSelection(row, col, false, false);
            if (editCellAt(getSelectedRow(), getSelectedColumn())) {
                getEditorComponent().requestFocusInWindow();
            }
        }
    }

    /**
     * Action to be run when the user navigates to the previous cell in the table, for instance by
     * pressing Shift-TAB
     */
    protected class SelectPreviousColumnCellAction extends AbstractAction {

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
                col = colEnd;
                row--;
            }
            changeSelection(row, col, false, false);
            if (editCellAt(getSelectedRow(), getSelectedColumn())) {
                getEditorComponent().requestFocusInWindow();
            }
        }
    }

    protected Dimension getPreferredFullWidthSize() {
        Container c = getParent();
        while (c != null && !(c instanceof JViewport)) {
            c = c.getParent();
        }
        if (c != null) {
            Dimension d = super.getPreferredSize();
            d.width = c.getSize().width;
            return d;
        }
        return super.getPreferredSize();
    }
}
