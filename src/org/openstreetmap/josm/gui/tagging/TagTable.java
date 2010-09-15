// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.applet.Applet;
import java.awt.AWTException;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.KeyboardFocusManager;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.EventObject;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.CellEditor;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;

import org.openstreetmap.josm.gui.dialogs.relation.RunnableAction;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompletionManager;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompletionList;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * This is the tabular editor component for OSM tags.
 *
 */
public class TagTable extends JTable  {

    private static final Logger logger = Logger.getLogger(TagTable.class.getName());

    /** the table cell editor used by this table */
    private TagCellEditor editor = null;

    /** a list of components to which focus can be transferred withouth stopping
     * cell editing this table.
     */
    private final CopyOnWriteArrayList<Component> doNotStopCellEditingWhenFocused = new CopyOnWriteArrayList<Component>();
    private CellEditorRemover editorRemover;

    /**
     * The table has two columns. The first column is used for editing rendering and
     * editing tag keys, the second for rendering and editing tag values.
     *
     */
    static class TagTableColumnModel extends DefaultTableColumnModel {
        public TagTableColumnModel(DefaultListSelectionModel selectionModel) {
            setSelectionModel(selectionModel);
            TableColumn col = null;
            TagCellRenderer renderer = new TagCellRenderer();

            // column 0 - tag key
            col = new TableColumn(0);
            col.setHeaderValue(tr("Key"));
            col.setResizable(true);
            col.setCellRenderer(renderer);
            addColumn(col);

            // column 1 - tag value
            col = new TableColumn(1);
            col.setHeaderValue(tr("Value"));
            col.setResizable(true);
            col.setCellRenderer(renderer);
            addColumn(col);
        }
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

            if (col == 0) {
                col++;
            } else if (col == 1 && row < getRowCount()-1) {
                col=0;
                row++;
            } else if (col == 1 && row == getRowCount()-1){
                // we are at the end. Append an empty row and move the focus
                // to its second column
                TagEditorModel model = (TagEditorModel)getModel();
                model.appendNewTag();
                col=0;
                row++;
            }
            changeSelection(row, col, false, false);
            requestFocusInCell(row,col);
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

            if (col <= 0 && row <= 0) {
                // change nothing
            } else if (col == 1) {
                col--;
            } else {
                col = 1;
                row--;
            }
            changeSelection(row, col, false, false);
            requestFocusInCell(row,col);
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
    class DeleteAction extends RunnableAction implements ListSelectionListener {

        public DeleteAction() {
            putValue(SMALL_ICON, ImageProvider.get("dialogs", "delete"));
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
            TagEditorModel model = (TagEditorModel)getModel();
            model.deleteTagNames(rows);
        }

        /**
         * delete a selection of tag values
         */
        protected void deleteTagValues() {
            int[] rows = getSelectedRows();
            TagEditorModel model = (TagEditorModel)getModel();
            model.deleteTagValues(rows);
        }

        /**
         * delete a selection of tags
         */
        protected void deleteTags() {
            int[] rows = getSelectedRows();
            TagEditorModel model = (TagEditorModel)getModel();
            model.deleteTags(rows);
        }

        @Override
        public void run() {
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
            }

            if (isEditing()) {
                CellEditor editor = getCellEditor();
                if (editor != null) {
                    editor.cancelCellEditing();
                }
            }

            TagEditorModel model = (TagEditorModel)getModel();
            if (model.getRowCount() == 0) {
                model.ensureOneTag();
                requestFocusInCell(0, 0);
            }
        }

        /**
         * listens to the table selection model
         */
        public void valueChanged(ListSelectionEvent e) {
            updateEnabledState();
        }

        protected void updateEnabledState() {
            if (isEditing() && getSelectedColumnCount() == 1 && getSelectedRowCount() == 1) {
                setEnabled(true);
            } else if (!isEditing() && getSelectedColumnCount() == 1 && getSelectedRowCount() == 1) {
                setEnabled(true);
            } else if (getSelectedColumnCount() > 1 || getSelectedRowCount() > 1) {
                setEnabled(true);
            } else {
                setEnabled(false);
            }
        }
    }

    /**
     * Action to be run when the user adds a new tag.
     *
     *
     */
    class AddAction extends RunnableAction implements PropertyChangeListener{
        public AddAction() {
            putValue(SMALL_ICON, ImageProvider.get("dialogs", "add"));
            putValue(SHORT_DESCRIPTION, tr("Add a new tag"));
            TagTable.this.addPropertyChangeListener(this);
            updateEnabledState();
        }

        @Override
        public void run() {
            CellEditor editor = getCellEditor();
            if (editor != null) {
                getCellEditor().stopCellEditing();
            }
            ((TagEditorModel)getModel()).appendNewTag();
            final int rowIdx = getModel().getRowCount()-1;
            requestFocusInCell(rowIdx, 0);
        }

        protected void updateEnabledState() {
            setEnabled(TagTable.this.isEnabled());
        }

        public void propertyChange(PropertyChangeEvent evt) {
            updateEnabledState();
        }
    }

    /** the delete action */
    private RunnableAction deleteAction = null;

    /** the add action */
    private RunnableAction addAction = null;

    /**
     *
     * @return the delete action used by this table
     */
    public RunnableAction getDeleteAction() {
        return deleteAction;
    }

    public RunnableAction getAddAction() {
        return addAction;
    }

    /**
     * initialize the table
     */
    protected void init() {
        setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        //setCellSelectionEnabled(true);
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
        .put(KeyStroke.getKeyStroke(KeyEvent.VK_ADD, KeyEvent.CTRL_MASK), "addTag");
        getActionMap().put("addTag", addAction);

        // create the table cell editor and set it to key and value columns
        //
        setTagCellEditor(new TagCellEditor());
    }

    /**
     * Creates a new tag table
     *
     * @param model the tag editor model
     */
    public TagTable(TagEditorModel model) {
        super(model, new TagTableColumnModel(model.getColumnSelectionModel()), model.getRowSelectionModel());
        init();
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

    @Override protected boolean processKeyBinding(KeyStroke ks, KeyEvent e,
            int condition, boolean pressed) {

        // handle delete key
        //
        if (e.getKeyCode() == KeyEvent.VK_DELETE) {
            if (isEditing() && getSelectedColumnCount() == 1 && getSelectedRowCount() == 1)
                // if DEL was pressed and only the currently edited cell is selected,
                // don't run the delete action. DEL is handled by the CellEditor as normal
                // DEL in the text input.
                //
                return super.processKeyBinding(ks, e, condition, pressed);
            getDeleteAction().run();
        }
        return super.processKeyBinding(ks, e, condition, pressed);
    }

    /**
     * @param autoCompletionList
     */
    public void setAutoCompletionList(AutoCompletionList autoCompletionList) {
        if (autoCompletionList == null)
            return;
        if (editor != null) {
            editor.setAutoCompletionList(autoCompletionList);
        }
    }

    public void setAutoCompletionManager(AutoCompletionManager autocomplete) {
        if (autocomplete == null) {
            logger.warning("argument autocomplete should not be null. Aborting.");
            Thread.dumpStack();
            return;
        }
        if (editor != null) {
            editor.setAutoCompletionManager(autocomplete);
        }
    }

    public AutoCompletionList getAutoCompletionList() {
        if (editor != null)
            return editor.getAutoCompletionList();
        else
            return null;
    }

    public TagCellEditor getTableCellEditor() {
        return editor;
    }

    public void  addOKAccelatorListener(KeyListener l) {
        addKeyListener(l);
        if (editor == null) {
            logger.warning("editor is null. cannot register OK accelator listener.");
        } else {
            editor.getEditor().addKeyListener(l);
        }
    }

    /**
     * Inject a tag cell editor in the tag table
     *
     * @param editor
     */
    public void setTagCellEditor(TagCellEditor editor) {
        if (isEditing()) {
            this.editor.cancelCellEditing();
        }
        this.editor = editor;
        getColumnModel().getColumn(0).setCellEditor(editor);
        getColumnModel().getColumn(1).setCellEditor(editor);
    }

    public void requestFocusInCell(final int row, final int col) {

        // the following code doesn't work reliably. If a table cell
        // gains focus using editCellAt() and requestFocusInWindow()
        // it isn't possible to tab to the next table cell using TAB or
        // ENTER. Don't know why.
        //
        // tblTagEditor.editCellAt(row, col);
        // if (tblTagEditor.getEditorComponent() != null) {
        //  tblTagEditor.getEditorComponent().requestFocusInWindow();
        // }

        // this is a workaround. We move the focus to the respective cell
        // using a simulated mouse click. In this case one can tab out of
        // the cell using TAB and ENTER.
        //
        Rectangle r = getCellRect(row,col, false);
        Point p = new Point(r.x + r.width/2, r.y + r.height/2);
        SwingUtilities.convertPointToScreen(p, this);
        Point before = MouseInfo.getPointerInfo().getLocation();

        try {
            Robot robot = new Robot();
            robot.mouseMove(p.x,p.y);
            robot.mousePress(InputEvent.BUTTON1_MASK);
            robot.mouseRelease(InputEvent.BUTTON1_MASK);
            robot.mouseMove(before.x, before.y);
        } catch(AWTException e) {
            logger.log(Level.SEVERE, "failed to simulate mouse click event at (" + r.x + "," + r.y + "). Exception: " + e.toString());
            return;
        }
    }

    public void addComponentNotStoppingCellEditing(Component component) {
        if (component == null) return;
        doNotStopCellEditingWhenFocused.addIfAbsent(component);
    }

    public void removeComponentNotStoppingCellEditing(Component component) {
        if (component == null) return;
        doNotStopCellEditingWhenFocused.remove(component);
    }

    @Override
    public boolean editCellAt(int row, int column, EventObject e){

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
        return super.editCellAt(row, column,e);
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
     * of dedicated components, see {@see TagTable#doNotStopCellEditingWhenFocused}.
     * A typical example for such a component is a button in {@see TagEditorPanel}
     * which isn't a child component of {@see TagTable} but which should respond to
     * to focus transfer in a similar way to a child of TagTable.
     *
     */
    class CellEditorRemover implements PropertyChangeListener {
        KeyboardFocusManager focusManager;

        public CellEditorRemover(KeyboardFocusManager fm) {
            this.focusManager = fm;
        }

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
                else if ((c instanceof Window) ||
                        (c instanceof Applet && c.getParent() == null)) {
                    if (c == SwingUtilities.getRoot(TagTable.this)) {
                        if (!getCellEditor().stopCellEditing()) {
                            getCellEditor().cancelCellEditing();
                        }
                    }
                    break;
                }
                c = c.getParent();
            }
        }
    }
}
