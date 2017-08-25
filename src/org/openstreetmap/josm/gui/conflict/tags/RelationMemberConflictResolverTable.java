// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.tags;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;

import org.openstreetmap.josm.gui.widgets.JosmComboBox;

/**
 * The main table displayed in the {@link RelationMemberConflictResolver}
 *
 * @see RelationMemberConflictResolverColumnModel
 */
public class RelationMemberConflictResolverTable extends JTable implements MultiValueCellEditor.NavigationListener {

    private SelectNextColumnCellAction selectNextColumnCellAction;
    private SelectPreviousColumnCellAction selectPreviousColumnCellAction;

    public RelationMemberConflictResolverTable(RelationMemberConflictResolverModel model) {
        super(model, new RelationMemberConflictResolverColumnModel());
        build();
    }

    protected final void build() {
        setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);

        // make ENTER behave like TAB
        //
        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, false), "selectNextColumnCell");

        // install custom navigation actions
        //
        selectNextColumnCellAction = new SelectNextColumnCellAction();
        selectPreviousColumnCellAction = new SelectPreviousColumnCellAction();
        getActionMap().put("selectNextColumnCell", selectNextColumnCellAction);
        getActionMap().put("selectPreviousColumnCell", selectPreviousColumnCellAction);

        setRowHeight((int) new JosmComboBox<String>().getPreferredSize().getHeight());
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

            if (col == 2 && row < getRowCount() - 1) {
                row++;
            } else if (row < getRowCount() - 1) {
                col = 2;
                row++;
            }
            changeSelection(row, col, false, false);
            editCellAt(getSelectedRow(), getSelectedColumn());
            getEditorComponent().requestFocusInWindow();
        }
    }

    /**
     * Action to be run when the user navigates to the previous cell in the table, for instance by
     * pressing Shift-TAB
     *
     */
    class SelectPreviousColumnCellAction extends AbstractAction {

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

            if (col <= 0 && row <= 0) {
                // change nothing
            } else if (row > 0) {
                col = 2;
                row--;
            }
            changeSelection(row, col, false, false);
            editCellAt(getSelectedRow(), getSelectedColumn());
            getEditorComponent().requestFocusInWindow();
        }
    }

    @Override
    public void gotoNextDecision() {
        selectNextColumnCellAction.run();
    }

    @Override
    public void gotoPreviousDecision() {
        selectPreviousColumnCellAction.run();
    }
}
