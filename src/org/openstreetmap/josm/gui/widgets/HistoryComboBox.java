// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.widgets;

import java.util.List;

import javax.swing.text.JTextComponent;

import org.openstreetmap.josm.gui.tagging.ac.AutoCompletingComboBox;

public class HistoryComboBox extends AutoCompletingComboBox {
    private ComboBoxHistory model;

    public HistoryComboBox() {
        setModel(model = new ComboBoxHistory(15));
        setEditable(true);
    }

    public String getText() {
        return ((JTextComponent)getEditor().getEditorComponent()).getText();
    }

    public void setText(String value) {
        setAutocompleteEnabled(false);
        ((JTextComponent)getEditor().getEditorComponent()).setText(value);
        setAutocompleteEnabled(true);
    }

    public void addCurrentItemToHistory() {
        String regex = (String)getEditor().getItem();
        model.addElement(regex);
    }

    public void setHistory(List<String> history) {
        model.setItems(history);
    }

    public List<String> getHistory() {
        return model.asList();
    }
}
