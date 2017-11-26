// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.ac;

import java.awt.Component;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.EventObject;
import java.util.Objects;

import javax.swing.ComboBoxEditor;
import javax.swing.JTable;
import javax.swing.event.CellEditorListener;
import javax.swing.table.TableCellEditor;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;
import javax.swing.text.StyleConstants;

import org.openstreetmap.josm.gui.util.CellEditorSupport;
import org.openstreetmap.josm.gui.widgets.JosmTextField;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.Logging;

/**
 * AutoCompletingTextField is a text field with autocompletion behaviour. It
 * can be used as table cell editor in {@link JTable}s.
 *
 * Autocompletion is controlled by a list of {@link AutoCompletionListItem}s
 * managed in a {@link AutoCompletionList}.
 *
 * @since 1762
 */
public class AutoCompletingTextField extends JosmTextField implements ComboBoxEditor, TableCellEditor {

    private Integer maxChars;

    /**
     * The document model for the editor
     */
    class AutoCompletionDocument extends PlainDocument {

        @Override
        public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {

            // If a maximum number of characters is specified, avoid to exceed it
            if (maxChars != null && str != null && getLength() + str.length() > maxChars) {
                int allowedLength = maxChars-getLength();
                if (allowedLength > 0) {
                    str = str.substring(0, allowedLength);
                } else {
                    return;
                }
            }

            if (autoCompletionList == null) {
                super.insertString(offs, str, a);
                return;
            }

            // input method for non-latin characters (e.g. scim)
            if (a != null && a.isDefined(StyleConstants.ComposedTextAttribute)) {
                super.insertString(offs, str, a);
                return;
            }

            // if the current offset isn't at the end of the document we don't autocomplete.
            // If a highlighted autocompleted suffix was present and we get here Swing has
            // already removed it from the document. getLength() therefore doesn't include the
            // autocompleted suffix.
            //
            if (offs < getLength()) {
                super.insertString(offs, str, a);
                return;
            }

            String currentText = getText(0, getLength());
            // if the text starts with a number we don't autocomplete
            if (Config.getPref().getBoolean("autocomplete.dont_complete_numbers", true)) {
                try {
                    Long.parseLong(str);
                    if (currentText.isEmpty()) {
                        // we don't autocomplete on numbers
                        super.insertString(offs, str, a);
                        return;
                    }
                    Long.parseLong(currentText);
                    super.insertString(offs, str, a);
                    return;
                } catch (NumberFormatException e) {
                    // either the new text or the current text isn't a number. We continue with autocompletion
                    Logging.trace(e);
                }
            }
            String prefix = currentText.substring(0, offs);
            autoCompletionList.applyFilter(prefix+str);
            if (autoCompletionList.getFilteredSize() > 0 && !Objects.equals(str, noAutoCompletionString)) {
                // there are matches. Insert the new text and highlight the auto completed suffix
                String matchingString = autoCompletionList.getFilteredItemAt(0).getValue();
                remove(0, getLength());
                super.insertString(0, matchingString, a);

                // highlight from insert position to end position to put the caret at the end
                setCaretPosition(offs + str.length());
                moveCaretPosition(getLength());
            } else {
                // there are no matches. Insert the new text, do not highlight
                //
                String newText = prefix + str;
                remove(0, getLength());
                super.insertString(0, newText, a);
                setCaretPosition(getLength());
            }
        }
    }

    /** the auto completion list user input is matched against */
    protected AutoCompletionList autoCompletionList;
    /** a string which should not be auto completed */
    protected String noAutoCompletionString;

    @Override
    protected Document createDefaultModel() {
        return new AutoCompletionDocument();
    }

    protected final void init() {
        addFocusListener(
                new FocusAdapter() {
                    @Override
                    public void focusGained(FocusEvent e) {
                        if (e != null && e.getOppositeComponent() != null) {
                            // Select all characters when the change of focus occurs inside JOSM only.
                            // When switching from another application, it is annoying, see #13747
                            selectAll();
                        }
                        applyFilter(getText());
                    }
                }
        );

        addKeyListener(
                new KeyAdapter() {
                    @Override
                    public void keyReleased(KeyEvent e) {
                        if (getText().isEmpty()) {
                            applyFilter("");
                        }
                    }
                }
        );
        tableCellEditorSupport = new CellEditorSupport(this);
    }

    /**
     * Constructs a new {@code AutoCompletingTextField}.
     */
    public AutoCompletingTextField() {
        this(0);
    }

    /**
     * Constructs a new {@code AutoCompletingTextField}.
     * @param columns the number of columns to use to calculate the preferred width;
     * if columns is set to zero, the preferred width will be whatever naturally results from the component implementation
     */
    public AutoCompletingTextField(int columns) {
        this(columns, true);
    }

    /**
     * Constructs a new {@code AutoCompletingTextField}.
     * @param columns the number of columns to use to calculate the preferred width;
     * if columns is set to zero, the preferred width will be whatever naturally results from the component implementation
     * @param undoRedo Enables or not Undo/Redo feature. Not recommended for table cell editors, unless each cell provides its own editor
     */
    public AutoCompletingTextField(int columns, boolean undoRedo) {
        super(null, null, columns, undoRedo);
        init();
    }

    protected void applyFilter(String filter) {
        if (autoCompletionList != null) {
            autoCompletionList.applyFilter(filter);
        }
    }

    /**
     * Returns the auto completion list.
     * @return the auto completion list; may be null, if no auto completion list is set
     */
    public AutoCompletionList getAutoCompletionList() {
        return autoCompletionList;
    }

    /**
     * Sets the auto completion list.
     * @param autoCompletionList the auto completion list; if null, auto completion is
     *   disabled
     */
    public void setAutoCompletionList(AutoCompletionList autoCompletionList) {
        this.autoCompletionList = autoCompletionList;
    }

    @Override
    public Component getEditorComponent() {
        return this;
    }

    @Override
    public Object getItem() {
        return getText();
    }

    @Override
    public void setItem(Object anObject) {
        if (anObject == null) {
            setText("");
        } else {
            setText(anObject.toString());
        }
    }

    @Override
    public void setText(String t) {
        // disallow auto completion for this explicitly set string
        this.noAutoCompletionString = t;
        super.setText(t);
    }

    /**
     * Sets the maximum number of characters allowed.
     * @param max maximum number of characters allowed
     * @since 5579
     */
    public void setMaxChars(Integer max) {
        maxChars = max;
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
