// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.widgets;

import java.util.List;

import javax.swing.text.JTextComponent;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompletingComboBox;

public class HistoryComboBox extends AutoCompletingComboBox {
    private ComboBoxHistory model;

    public static final int DEFAULT_SEARCH_HISTORY_SIZE = 15;

    /**
     * Constructs a new {@code HistoryComboBox}.
     */
    public HistoryComboBox() {
        int maxsize = Main.pref.getInteger("search.history-size", DEFAULT_SEARCH_HISTORY_SIZE);
        setModel(model = new ComboBoxHistory(maxsize));
        setEditable(true);
    }

    public String getText() {
        return ((JTextComponent) getEditor().getEditorComponent()).getText();
    }

    public void setText(String value) {
        setAutocompleteEnabled(false);
        ((JTextComponent) getEditor().getEditorComponent()).setText(value);
        setAutocompleteEnabled(true);
    }

    public void addCurrentItemToHistory() {
        model.addElement((String) getEditor().getItem());
    }

    public void setHistory(List<String> history) {
        model.setItemsAsString(history);
    }

    public List<String> getHistory() {
        return model.asStringList();
    }
}
