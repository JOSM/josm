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
@SuppressWarnings("serial")
public class TagCellEditor extends AbstractCellEditor implements TableCellEditor{

    protected AutoCompletingTextField editor = null;
    protected TagModel currentTag = null;

    /** the cache of auto completion items derived from the current JOSM data set */
    protected AutoCompletionManager autocomplete = null;

    /** user input is matched against this list of auto completion items */
    protected AutoCompletionList autoCompletionList = null;

    /**
     * constructor
     */
    public TagCellEditor() {
        editor = new AutoCompletingTextField();
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
            if (! key.equals(currentTag.getName())) {
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
        if(autoCompletionList != null) {
            autoCompletionList.clear();
        }
        if (column == 0) {
            editor.setText(currentTag.getName());
            TagEditorModel model = (TagEditorModel)table.getModel();
            initAutoCompletionListForKeys(model, currentTag);
            return editor;
        } else if (column == 1) {

            if (currentTag.getValueCount() == 0) {
                editor.setText("");
            } else if (currentTag.getValueCount() == 1) {
                editor.setText(currentTag.getValues().get(0));
            } else {
                editor.setText("");
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

    public void setAutoCompletionManager(AutoCompletionManager autocomplete) {
        this.autocomplete = autocomplete;
    }

    public void autoCompletionItemSelected(String item) {
        editor.setText(item);
        editor.selectAll();
        editor.requestFocus();
    }

    public AutoCompletingTextField getEditor() {
        return editor;
    }
}
