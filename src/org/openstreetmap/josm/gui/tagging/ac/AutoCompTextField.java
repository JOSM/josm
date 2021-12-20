// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.ac;

import java.awt.Component;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.EventObject;
import java.util.regex.Pattern;

import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.event.CellEditorListener;
import javax.swing.table.TableCellEditor;
import javax.swing.text.AbstractDocument;

import org.openstreetmap.josm.gui.datatransfer.ClipboardUtils;
import org.openstreetmap.josm.gui.util.CellEditorSupport;
import org.openstreetmap.josm.gui.widgets.JosmTextField;
import org.openstreetmap.josm.spi.preferences.Config;

/**
 * An auto-completing TextField.
 * <p>
 * When the user starts typing, this textfield will suggest the
 * {@link AutoCompComboBoxModel#findBestCandidate best matching item} from its model.  The items in
 * the model can be of any type while the items' {@code toString} values are used for
 * autocompletion.
 *
 * @param <E> the type of items in the model
 * @since 18221
 */
public class AutoCompTextField<E> extends JosmTextField implements TableCellEditor, KeyListener {

    /** true if the combobox should autocomplete */
    private boolean autocompleteEnabled = true;
    /** a filter to enforce max. text length */
    private transient MaxLengthDocumentFilter docFilter;
    /** the model */
    protected AutoCompComboBoxModel<E> model;
    /** Whether to autocomplete numbers */
    private final boolean AUTOCOMPLETE_NUMBERS = !Config.getPref().getBoolean("autocomplete.dont_complete_numbers", true);
    /** a regex that matches numbers */
    private static final Pattern IS_NUMBER = Pattern.compile("^\\d+$");

    protected final void init() {
        model = new AutoCompComboBoxModel<>();
        docFilter = new MaxLengthDocumentFilter();
        ((AbstractDocument) getDocument()).setDocumentFilter(docFilter);
        addKeyListener(this);
        tableCellEditorSupport = new CellEditorSupport(this);
    }

    /**
     * Constructs a new {@code AutoCompTextField}.
     */
    public AutoCompTextField() {
        this(0);
    }

    /**
     * Constructs a new {@code AutoCompTextField}.
     * @param model the model to use
     */
    public AutoCompTextField(AutoCompComboBoxModel<E> model) {
        this(0);
        this.model = model;
    }

    /**
     * Constructs a new {@code AutoCompTextField}.
     * @param columns the number of columns to use to calculate the preferred width;
     * if columns is set to zero, the preferred width will be whatever naturally results from the component implementation
     */
    public AutoCompTextField(int columns) {
        this(columns, true);
    }

    /**
     * Constructs a new {@code AutoCompTextField}.
     * @param columns the number of columns to use to calculate the preferred width;
     * if columns is set to zero, the preferred width will be whatever naturally results from the component implementation
     * @param undoRedo Enables or not Undo/Redo feature. Not recommended for table cell editors, unless each cell provides its own editor
     */
    public AutoCompTextField(int columns, boolean undoRedo) {
        super(null, null, columns, undoRedo);
        init();
    }

    /**
     * Returns the {@link AutoCompComboBoxModel} currently used.
     *
     * @return the model
     */
    public AutoCompComboBoxModel<E> getModel() {
        return model;
    }

    /**
     * Sets the data model that the {@code AutoCompTextField} uses to obtain the list of items.
     *
     * @param model the {@link AutoCompComboBoxModel} that provides the list of items used for autocomplete
     */
    public void setModel(AutoCompComboBoxModel<E> model) {
        AutoCompComboBoxModel<E> oldModel = this.model;
        this.model = model;
        firePropertyChange("model", oldModel, model);
    }

    /**
     * Returns {@code true} if autocompletion is enabled.
     *
     * @return {@code true} if autocompletion is enabled.
     */
    public final boolean isAutocompleteEnabled() {
        return autocompleteEnabled;
    }

    /**
     * Enables or disables the autocompletion.
     *
     * @param enabled {@code true} to enable autocompletion
     * @return {@code true} if autocomplete was enabled before calling this
     */
    public boolean setAutocompleteEnabled(boolean enabled) {
        boolean oldEnabled = this.autocompleteEnabled;
        this.autocompleteEnabled = enabled;
        return oldEnabled;
    }

    /**
     * Sets the maximum number of characters allowed.
     * @param length maximum number of characters allowed
     */
    public void setMaxTextLength(int length) {
        docFilter.setMaxLength(length);
    }

    /**
     * Autocompletes what the user typed in.
     * <p>
     * Gets the user input from the editor, finds the best matching item in the model, sets the
     * editor text to it, and highlights the autocompleted part. If there is no matching item, removes the
     * list selection.
     *
     * @param oldText the text before the last keypress was processed
     */
    private void autocomplete(String oldText) {
        String newText = getText();
        if (getSelectionEnd() != newText.length())
            // selection not at the end
            return;
        // if the user typed some control character (eg. Alt+A) the selection may still be there
        String unSelected = newText.substring(0, getSelectionStart());
        if (unSelected.length() <= oldText.length())
            // do not autocomplete on control or deleted chars
            return;
        if (getInputMethodRequests().getCommittedTextLength() != getDocument().getLength()) {
            // do not autocomplete if there is uncommitted text (breaks Microsoft Japanese IME, see #21507)
            return;
        }
        if (!AUTOCOMPLETE_NUMBERS && IS_NUMBER.matcher(newText).matches())
            return;

        fireAutoCompEvent(AutoCompEvent.AUTOCOMP_BEFORE, null);
        E item = getModel().findBestCandidate(newText);
        fireAutoCompEvent(AutoCompEvent.AUTOCOMP_DONE, item);

        if (item != null) {
            String text = item.toString();
            setText(text);
            // select the autocompleted suffix in the editor
            select(newText.length(), text.length());
            // copy the whole autocompleted string to the unix system-wide selection (aka
            // middle-click), else only the selected suffix would be copied
            copyToSysSel(text);
        }
    }

    /**
     * Copies a String to the UNIX system-wide selection (aka middle-click).
     *
     * @param s the string to copy
     */
    void copyToSysSel(String s) {
        Clipboard sysSel = ClipboardUtils.getSystemSelection();
        if (sysSel != null) {
            Transferable transferable = new StringSelection(s);
            sysSel.setContents(transferable, null);
        }
    }

    /**
     * Adds an AutoCompListener.
     *
     * @param l the AutoComp listener to be added
     */
    public synchronized void addAutoCompListener(AutoCompListener l) {
        listenerList.add(AutoCompListener.class, l);
    }

    /**
     * Removes the specified AutoCompListener.
     *
     * @param l the autoComp listener to be removed
     */
    public synchronized void removeAutoCompListener(AutoCompListener l) {
        listenerList.remove(AutoCompListener.class, l);
    }

    /**
     * Returns an array of all the current <code>AutoCompListener</code>s.
     *
     * @return all of the <code>AutoCompListener</code>s added or an empty
     *         array if no listeners have been added
     */
    public synchronized AutoCompListener[] getAutoCompListeners() {
        return listenerList.getListeners(AutoCompListener.class);
    }

    /**
     * Notifies all listeners that have registered interest for notification on this event type.
     * The event instance is lazily created. The listener list is processed in last to first order.
     *
     * @param id The Autocomp event id
     * @param item The item selected for autocompletion.
     * @see javax.swing.event.EventListenerList
     */
    protected void fireAutoCompEvent(int id, Object item) {
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        AutoCompEvent e = new AutoCompEvent(this, id, item);

        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == AutoCompListener.class) {
                switch (id) {
                    case AutoCompEvent.AUTOCOMP_DONE:
                        ((AutoCompListener) listeners[i + 1]).autoCompPerformed(e);
                        break;
                    case AutoCompEvent.AUTOCOMP_BEFORE:
                        ((AutoCompListener) listeners[i + 1]).autoCompBefore(e);
                        break;
                    default:
                        break;
                }
            }
        }
    }

    /* ------------------------------------------------------------------------------------ */
    /* KeyListener interface                                                                */
    /* ------------------------------------------------------------------------------------ */

    /**
     * Listens to key events and eventually schedules an autocomplete.
     *
     * @param e the key event
     */
    @Override
    public void keyTyped(KeyEvent e) {
        // if selection is at the end
        if (autocompleteEnabled && getSelectionEnd() == getText().length()) {
            final String oldText = getText().substring(0, getSelectionStart());
            // We got the event before the editor component could see it. Let the editor do its job first.
            SwingUtilities.invokeLater(() -> autocomplete(oldText));
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        // not interested
    }

    @Override
    public void keyReleased(KeyEvent e) {
        // not interested
    }

    /* ------------------------------------------------------------------------------------ */
    /* TableCellEditor interface                                                            */
    /* ------------------------------------------------------------------------------------ */

    private transient CellEditorSupport tableCellEditorSupport;
    private String originalValue;

    @Override
    public void addCellEditorListener(CellEditorListener l) {
        tableCellEditorSupport.addCellEditorListener(l);
    }

    protected void rememberOriginalValue(String value) {
        this.originalValue = value;
    }

    protected void restoreOriginalValue() {
        setText(originalValue);
    }

    @Override
    public void removeCellEditorListener(CellEditorListener l) {
        tableCellEditorSupport.removeCellEditorListener(l);
    }

    @Override
    public void cancelCellEditing() {
        restoreOriginalValue();
        tableCellEditorSupport.fireEditingCanceled();
    }

    @Override
    public Object getCellEditorValue() {
        return getText();
    }

    @Override
    public boolean isCellEditable(EventObject anEvent) {
        return true;
    }

    @Override
    public boolean shouldSelectCell(EventObject anEvent) {
        return true;
    }

    @Override
    public boolean stopCellEditing() {
        tableCellEditorSupport.fireEditingStopped();
        return true;
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        setText(value == null ? "" : value.toString());
        rememberOriginalValue(getText());
        return this;
    }
}
