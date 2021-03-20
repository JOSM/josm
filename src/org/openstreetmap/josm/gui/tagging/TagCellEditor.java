// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging;

import java.awt.Component;

import javax.swing.AbstractCellEditor;
import javax.swing.BorderFactory;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;

import org.openstreetmap.josm.gui.tagging.ac.AutoCompletingTextField;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompletionList;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompletionManager;

/**
 * This is the table cell editor for the tag editor dialog.
 *
 */
public class TagCellEditor extends AbstractCellEditor implements TableCellEditor {

    protected AutoCompletingTextField editor;
    protected transient TagModel currentTag;

    /** the cache of auto completion items derived from the current JOSM data set */
    protected transient AutoCompletionManager autocomplete;

    /** user input is matched against this list of auto completion items */
    protected AutoCompletionList autoCompletionList;

    /**
     * constructor
     * @param maxCharacters maximum number of characters allowed, 0 for unlimited
     */
    public TagCellEditor(final int maxCharacters) {
        editor = new AutoCompletingTextField(0, false);
        if (maxCharacters > 0) {
            editor.setMaxChars(maxCharacters);
        }
        editor.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
    }

    /**
     * initializes  the auto completion list when the table cell editor starts
     * to edit the key of a tag. In this case the auto completion list is
     * initialized with the set of standard key values and the set of current key
     * values from the current JOSM data set. Keys already present in the
     * current tag model are removed from the auto completion list.
     *
     * @param model  the tag editor model
     * @param currentTag  the current tag
     */
    protected void initAutoCompletionListForKeys(TagEditorModel model, TagModel currentTag) {

        if (autoCompletionList == null)
            return;
        autoCompletionList.clear();

        // add the list of keys in the current data set
        //
        autocomplete.populateWithKeys(autoCompletionList);

        // remove the keys already present in the current tag model
        //
        for (String key : model.getKeys()) {
            if (!key.equals(currentTag.getName())) {
                autoCompletionList.remove(key);
            }
        }
        autoCompletionList.fireTableDataChanged();
    }

    /**
     * initializes the auto completion list when the cell editor starts to edit
     * a tag value. In this case the auto completion list is initialized with the
     * set of standard values for a given key and the set of values present in the
     * current data set for the given key.
     *
     * @param forKey the key
     */
    protected void initAutoCompletionListForValues(String forKey) {
        if (autoCompletionList == null) {
            return;
        }
        autoCompletionList.clear();
        autocomplete.populateWithTagValues(autoCompletionList, forKey);
    }

    /**
     * replies the table cell editor
     */
    @Override
    public Component getTableCellEditorComponent(JTable table,
            Object value, boolean isSelected, int row, int column) {
        currentTag = (TagModel) value;

        // no autocompletion for initial editor#setText()
        if (autoCompletionList != null) {
            autoCompletionList.clear();
        }
        if (column == 0) {
            editor.setText(currentTag.getName());
            TagEditorModel model = (TagEditorModel) table.getModel();
            initAutoCompletionListForKeys(model, currentTag);
            return editor;
        } else if (column == 1) {

            if (currentTag.getValueCount() > 1) {
                editor.setText("");
            } else {
                editor.setText(currentTag.getValue());
            }
            initAutoCompletionListForValues(currentTag.getName());
            return editor;
        } else {
            return null;
        }
    }

    @Override
    public Object getCellEditorValue() {
        return editor.getText();
    }

    /**
     * replies the {@link AutoCompletionList} this table cell editor synchronizes with
     *
     * @return the auto completion list
     */
    public AutoCompletionList getAutoCompletionList() {
        return autoCompletionList;
    }

    /**
     * sets the {@link AutoCompletionList} this table cell editor synchronizes with
     * @param autoCompletionList the auto completion list
     */
    public void setAutoCompletionList(AutoCompletionList autoCompletionList) {
        this.autoCompletionList = autoCompletionList;
        editor.setAutoCompletionList(autoCompletionList);
    }

    /**
     * Sets the manager that helps with auto completion
     * @param autocomplete The {@link AutoCompletionManager}
     */
    public void setAutoCompletionManager(AutoCompletionManager autocomplete) {
        this.autocomplete = autocomplete;
    }

    /**
     * Selects an item from the auto completion list and fills this cell with the value
     * @param item The text that was selected
     */
    public void autoCompletionItemSelected(String item) {
        editor.setText(item);
        editor.selectAll();
        editor.requestFocus();
    }

    /**
     * Gets the editor for this cell
     * @return The editor text field
     */
    public AutoCompletingTextField getEditor() {
        return editor;
    }
}
