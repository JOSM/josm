// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.KeyboardFocusManager;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collections;
import java.util.EventObject;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.AbstractAction;
import javax.swing.CellEditor;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.JTextComponent;

import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.TagMap;
import org.openstreetmap.josm.gui.datatransfer.OsmTransferHandler;
import org.openstreetmap.josm.gui.tagging.TagEditorModel.EndEditListener;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompletionList;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompletionManager;
import org.openstreetmap.josm.gui.widgets.JosmTable;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Logging;

/**
 * This is the tabular editor component for OSM tags.
 * @since 1762
 */
public class TagTable extends JosmTable implements EndEditListener {
    /** the table cell editor used by this table */
    private TagCellEditor editor;
    private final TagEditorModel model;
    private Component nextFocusComponent;

    /** a list of components to which focus can be transferred without stopping
     * cell editing this table.
     */
    private final CopyOnWriteArrayList<Component> doNotStopCellEditingWhenFocused = new CopyOnWriteArrayList<>();
    private transient CellEditorRemover editorRemover;

    /**
     * Action to be run when the user navigates to the next cell in the table,
     * for instance by pressing TAB or ENTER. The action alters the standard
     * navigation path from cell to cell:
     * <ul>
     *   <li>it jumps over cells in the first column</li>
     *   <li>it automatically add a new empty row when the user leaves the
     *   last cell in the table</li>
     * </ul>
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

            if (row == -1 && col == -1) {
                requestFocusInCell(0, 0);
                return;
            }

            if (col == 0) {
                col++;
            } else if (col == 1 && row < getRowCount()-1) {
                col = 0;
                row++;
            } else if (col == 1 && row == getRowCount()-1) {
                // we are at the end. Append an empty row and move the focus to its second column
                String key = ((TagModel) model.getValueAt(row, 0)).getName();
                if (!key.trim().isEmpty()) {
                    model.appendNewTag();
                    col = 0;
                    row++;
                } else {
                    clearSelection();
                    if (nextFocusComponent != null)
                        nextFocusComponent.requestFocusInWindow();
                    return;
                }
            }
            requestFocusInCell(row, col);
        }
    }

    /**
     * Action to be run when the user navigates to the previous cell in the table,
     * for instance by pressing Shift-TAB
     */
    class SelectPreviousColumnCellAction extends AbstractAction {

        @Override
        public void actionPerformed(ActionEvent e) {
            int col = getSelectedColumn();
            int row = getSelectedRow();
            if (getCellEditor() != null) {
                getCellEditor().stopCellEditing();
            }

            if (col <= 0 && row <= 0) {
                // change nothing
            } else if (col == 1) {
                col--;
            } else {
                col = 1;
                row--;
            }
            requestFocusInCell(row, col);
        }
    }

    /**
     * Action to be run when the user invokes a delete action on the table, for
     * instance by pressing DEL.
     *
     * Depending on the shape on the current selection the action deletes individual
     * values or entire tags from the model.
     *
     * If the current selection consists of cells in the second column only, the keys of
     * the selected tags are set to the empty string.
     *
     * If the current selection consists of cell in the third column only, the values of the
     * selected tags are set to the empty string.
     *
     *  If the current selection consists of cells in the second and the third column,
     *  the selected tags are removed from the model.
     *
     *  This action listens to the table selection. It becomes enabled when the selection
     *  is non-empty, otherwise it is disabled.
     *
     *
     */
    class DeleteAction extends AbstractAction implements ListSelectionListener {

        DeleteAction() {
            new ImageProvider("dialogs", "delete").getResource().attachImageIcon(this);
            putValue(SHORT_DESCRIPTION, tr("Delete the selection in the tag table"));
            getSelectionModel().addListSelectionListener(this);
            getColumnModel().getSelectionModel().addListSelectionListener(this);
            updateEnabledState();
        }

        /**
         * delete a selection of tag names
         */
        protected void deleteTagNames() {
            int[] rows = getSelectedRows();
            model.deleteTagNames(rows);
        }

        /**
         * delete a selection of tag values
         */
        protected void deleteTagValues() {
            int[] rows = getSelectedRows();
            model.deleteTagValues(rows);
        }

        /**
         * delete a selection of tags
         */
        protected void deleteTags() {
            int[] rows = getSelectedRows();
            model.deleteTags(rows);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (!isEnabled())
                return;
            switch(getSelectedColumnCount()) {
            case 1:
                if (getSelectedColumn() == 0) {
                    deleteTagNames();
                } else if (getSelectedColumn() == 1) {
                    deleteTagValues();
                }
                break;
            case 2:
                deleteTags();
                break;
            default: // Do nothing
            }

            endCellEditing();

            if (model.getRowCount() == 0) {
                model.ensureOneTag();
                requestFocusInCell(0, 0);
            }
        }

        /**
         * listens to the table selection model
         */
        @Override
        public void valueChanged(ListSelectionEvent e) {
            updateEnabledState();
        }

        protected final void updateEnabledState() {
            if (getSelectedColumnCount() >= 1 && getSelectedRowCount() >= 1) {
                setEnabled(true);
            } else {
                setEnabled(false);
            }
        }
    }

    /**
     * Action to be run when the user adds a new tag.
     *
     */
    class AddAction extends AbstractAction implements PropertyChangeListener {
        AddAction() {
            new ImageProvider("dialogs", "add").getResource().attachImageIcon(this);
            putValue(SHORT_DESCRIPTION, tr("Add a new tag"));
            TagTable.this.addPropertyChangeListener(this);
            updateEnabledState();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            CellEditor cEditor = getCellEditor();
            if (cEditor != null) {
                cEditor.stopCellEditing();
            }
            final int rowIdx = model.getRowCount()-1;
            if (rowIdx < 0 || !((TagModel) model.getValueAt(rowIdx, 0)).getName().trim().isEmpty()) {
                model.appendNewTag();
            }
            requestFocusInCell(model.getRowCount()-1, 0);
        }

        protected final void updateEnabledState() {
            setEnabled(TagTable.this.isEnabled());
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            updateEnabledState();
        }
    }

    /**
     * Action to be run when the user wants to paste tags from buffer
     */
    class PasteAction extends AbstractAction implements PropertyChangeListener {
        PasteAction() {
            new ImageProvider("pastetags").getResource().attachImageIcon(this);
            putValue(SHORT_DESCRIPTION, tr("Paste tags from buffer"));
            TagTable.this.addPropertyChangeListener(this);
            updateEnabledState();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Relation relation = new Relation();
            model.applyToPrimitive(relation);
            new OsmTransferHandler().pasteTags(Collections.singleton(relation));
            model.updateTags(new TagMap(relation.getKeys()).getTags());
        }

        protected final void updateEnabledState() {
            setEnabled(TagTable.this.isEnabled());
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            updateEnabledState();
        }
    }

    /** the delete action */
    private DeleteAction deleteAction;

    /** the add action */
    private AddAction addAction;

    /** the tag paste action */
    private PasteAction pasteAction;

    /**
     * Returns the delete action.
     * @return the delete action used by this table
     */
    public DeleteAction getDeleteAction() {
        return deleteAction;
    }

    /**
     * Returns the add action.
     * @return the add action used by this table
     */
    public AddAction getAddAction() {
        return addAction;
    }

    /**
     * Returns the paste action.
     * @return the paste action used by this table
     */
    public PasteAction getPasteAction() {
        return pasteAction;
    }

    /**
     * initialize the table
     * @param maxCharacters maximum number of characters allowed for keys and values, 0 for unlimited
     */
    protected final void init(final int maxCharacters) {
        setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        setRowSelectionAllowed(true);
        setColumnSelectionAllowed(true);
        setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);

        // make ENTER behave like TAB
        //
        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
        .put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, false), "selectNextColumnCell");

        // install custom navigation actions
        //
        getActionMap().put("selectNextColumnCell", new SelectNextColumnCellAction());
        getActionMap().put("selectPreviousColumnCell", new SelectPreviousColumnCellAction());

        // create a delete action. Installing this action in the input and action map
        // didn't work. We therefore handle delete requests in processKeyBindings(...)
        //
        deleteAction = new DeleteAction();

        // create the add action
        //
        addAction = new AddAction();
        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
        .put(KeyStroke.getKeyStroke(KeyEvent.VK_ADD, KeyEvent.CTRL_DOWN_MASK), "addTag");
        getActionMap().put("addTag", addAction);

        pasteAction = new PasteAction();

        // create the table cell editor and set it to key and value columns
        //
        TagCellEditor tmpEditor = new TagCellEditor(maxCharacters);
        setRowHeight(tmpEditor.getEditor().getPreferredSize().height);
        setTagCellEditor(tmpEditor);
    }

    /**
     * Creates a new tag table
     *
     * @param model the tag editor model
     * @param maxCharacters maximum number of characters allowed for keys and values, 0 for unlimited
     */
    public TagTable(TagEditorModel model, final int maxCharacters) {
        super(model, new TagTableColumnModelBuilder(new TagCellRenderer(), tr("Key"), tr("Value"))
                  .setSelectionModel(model.getColumnSelectionModel()).build(),
              model.getRowSelectionModel());
        this.model = model;
        model.setEndEditListener(this);
        init(maxCharacters);
    }

    @Override
    public Dimension getPreferredSize() {
        return getPreferredFullWidthSize();
    }

    @Override
    protected boolean processKeyBinding(KeyStroke ks, KeyEvent e, int condition, boolean pressed) {

        // handle delete key
        //
        if (e.getKeyCode() == KeyEvent.VK_DELETE) {
            if (isEditing() && getSelectedColumnCount() == 1 && getSelectedRowCount() == 1)
                // if DEL was pressed and only the currently edited cell is selected,
                // don't run the delete action. DEL is handled by the CellEditor as normal
                // DEL in the text input.
                //
                return super.processKeyBinding(ks, e, condition, pressed);
            getDeleteAction().actionPerformed(null);
        }
        return super.processKeyBinding(ks, e, condition, pressed);
    }

    /**
     * Sets the editor autocompletion list
     * @param autoCompletionList autocompletion list
     */
    public void setAutoCompletionList(AutoCompletionList autoCompletionList) {
        if (autoCompletionList == null)
            return;
        if (editor != null) {
            editor.setAutoCompletionList(autoCompletionList);
        }
    }

    /**
     * Sets the autocompletion manager that should be used for editing the cells
     * @param autocomplete The {@link AutoCompletionManager}
     */
    public void setAutoCompletionManager(AutoCompletionManager autocomplete) {
        if (autocomplete == null) {
            Logging.warn("argument autocomplete should not be null. Aborting.");
            Logging.error(new Exception());
            return;
        }
        if (editor != null) {
            editor.setAutoCompletionManager(autocomplete);
        }
    }

    /**
     * Gets the {@link AutoCompletionList} the cell editor is synchronized with
     * @return The list
     */
    public AutoCompletionList getAutoCompletionList() {
        if (editor != null)
            return editor.getAutoCompletionList();
        else
            return null;
    }

    /**
     * Sets the next component to request focus after navigation (with tab or enter).
     * @param nextFocusComponent next component to request focus after navigation (with tab or enter)
     */
    public void setNextFocusComponent(Component nextFocusComponent) {
        this.nextFocusComponent = nextFocusComponent;
    }

    /**
     * Gets the editor that is used for the table cells
     * @return The editor that is used when the user wants to enter text into a cell
     */
    public TagCellEditor getTableCellEditor() {
        return editor;
    }

    /**
     * Inject a tag cell editor in the tag table
     *
     * @param editor tag cell editor
     */
    public void setTagCellEditor(TagCellEditor editor) {
        endCellEditing();
        this.editor = editor;
        getColumnModel().getColumn(0).setCellEditor(editor);
        getColumnModel().getColumn(1).setCellEditor(editor);
    }

    /**
     * Request the focus in a specific cell
     * @param row The row index
     * @param col The column index
     */
    public void requestFocusInCell(final int row, final int col) {
        changeSelection(row, col, false, false);
        editCellAt(row, col);
        Component c = getEditorComponent();
        if (c != null) {
            c.requestFocusInWindow();
            if (c instanceof JTextComponent) {
                 ((JTextComponent) c).selectAll();
            }
        }
        // there was a bug here - on older 1.6 Java versions Tab was not working
        // after such activation. In 1.7 it works OK,
        // previous solution of using awt.Robot was resetting mouse speed on Windows
    }

    /**
     * Marks a component that may be focused without stopping the cell editing
     * @param component The component
     */
    public void addComponentNotStoppingCellEditing(Component component) {
        if (component == null) return;
        doNotStopCellEditingWhenFocused.addIfAbsent(component);
    }

    /**
     * Removes a component added with {@link #addComponentNotStoppingCellEditing(Component)}
     * @param component The component
     */
    public void removeComponentNotStoppingCellEditing(Component component) {
        if (component == null) return;
        doNotStopCellEditingWhenFocused.remove(component);
    }

    @Override
    public boolean editCellAt(int row, int column, EventObject e) {

        // a snipped copied from the Java 1.5 implementation of JTable
        //
        if (cellEditor != null && !cellEditor.stopCellEditing())
            return false;

        if (row < 0 || row >= getRowCount() ||
                column < 0 || column >= getColumnCount())
            return false;

        if (!isCellEditable(row, column))
            return false;

        // make sure our custom implementation of CellEditorRemover is created
        if (editorRemover == null) {
            KeyboardFocusManager fm =
                KeyboardFocusManager.getCurrentKeyboardFocusManager();
            editorRemover = new CellEditorRemover(fm);
            fm.addPropertyChangeListener("permanentFocusOwner", editorRemover);
        }

        // delegate to the default implementation
        return super.editCellAt(row, column, e);
    }

    @Override
    public void endCellEditing() {
        if (isEditing()) {
            CellEditor cEditor = getCellEditor();
            if (cEditor != null) {
                // First attempt to commit. If this does not work, cancel.
                cEditor.stopCellEditing();
                cEditor.cancelCellEditing();
            }
        }
    }

    @Override
    public void removeEditor() {
        // make sure we unregister our custom implementation of CellEditorRemover
        KeyboardFocusManager.getCurrentKeyboardFocusManager().
        removePropertyChangeListener("permanentFocusOwner", editorRemover);
        editorRemover = null;
        super.removeEditor();
    }

    @Override
    public void removeNotify() {
        // make sure we unregister our custom implementation of CellEditorRemover
        KeyboardFocusManager.getCurrentKeyboardFocusManager().
        removePropertyChangeListener("permanentFocusOwner", editorRemover);
        editorRemover = null;
        super.removeNotify();
    }

    /**
     * This is a custom implementation of the CellEditorRemover used in JTable
     * to handle the client property <tt>terminateEditOnFocusLost</tt>.
     *
     * This implementation also checks whether focus is transferred to one of a list
     * of dedicated components, see {@link TagTable#doNotStopCellEditingWhenFocused}.
     * A typical example for such a component is a button in {@link TagEditorPanel}
     * which isn't a child component of {@link TagTable} but which should respond to
     * to focus transfer in a similar way to a child of TagTable.
     *
     */
    class CellEditorRemover implements PropertyChangeListener {
        private final KeyboardFocusManager focusManager;

        CellEditorRemover(KeyboardFocusManager fm) {
            this.focusManager = fm;
        }

        @Override
        public void propertyChange(PropertyChangeEvent ev) {
            if (!isEditing())
                return;

            Component c = focusManager.getPermanentFocusOwner();
            while (c != null) {
                if (c == TagTable.this)
                    // focus remains inside the table
                    return;
                if (doNotStopCellEditingWhenFocused.contains(c))
                    // focus remains on one of the associated components
                    return;
                else if (c instanceof Window) {
                    if (c == SwingUtilities.getRoot(TagTable.this) && !getCellEditor().stopCellEditing()) {
                        getCellEditor().cancelCellEditing();
                    }
                    break;
                }
                c = c.getParent();
            }
        }
    }
}
