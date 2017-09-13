// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.widgets;

import java.util.List;

import javax.swing.text.JTextComponent;

import org.openstreetmap.josm.gui.tagging.ac.AutoCompletingComboBox;
import org.openstreetmap.josm.spi.preferences.Config;

/**
 * An {@link AutoCompletingComboBox} which keeps a history
 */
public class HistoryComboBox extends AutoCompletingComboBox {
    private final ComboBoxHistory model;

    /**
     * The default size of the search history.
     */
    public static final int DEFAULT_SEARCH_HISTORY_SIZE = 15;

    /**
     * Constructs a new {@code HistoryComboBox}.
     */
    public HistoryComboBox() {
        int maxsize = Config.getPref().getInt("search.history-size", DEFAULT_SEARCH_HISTORY_SIZE);
        model = new ComboBoxHistory(maxsize);
        setModel(model);
        setEditable(true);
    }

    /**
     * Returns the text contained in this component
     * @return the text
     * @see JTextComponent#getText()
     */
    public String getText() {
        return getEditorComponent().getText();
    }

    /**
     * Sets the text of this component to the specified text
     * @param value the text to set
     * @see JTextComponent#setText(java.lang.String)
     */
    public void setText(String value) {
        setAutocompleteEnabled(false);
        getEditorComponent().setText(value);
        setAutocompleteEnabled(true);
    }

    /**
     * Adds or moves the current element to the top of the history
     * @see ComboBoxHistory#addElement(java.lang.String)
     */
    public void addCurrentItemToHistory() {
        model.addElement((String) getEditor().getItem());
    }

    /**
     * Sets the elements of the ComboBox to the given items
     * @param history the items to set
     * @see ComboBoxHistory#setItemsAsString(java.util.List)
     */
    public void setHistory(List<String> history) {
        model.setItemsAsString(history);
    }

    /**
     * Returns the items as strings
     * @return the items as strings
     * @see ComboBoxHistory#asStringList()
     */
    public List<String> getHistory() {
        return model.asStringList();
    }
}
