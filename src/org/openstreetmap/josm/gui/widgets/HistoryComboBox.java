// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.widgets;

import org.openstreetmap.josm.gui.tagging.ac.AutoCompComboBox;

/**
 * A History ComboBox
 * <p>
 * A HistoryComboBox is an {@link AutoCompComboBox} specialized in {@code String}s.
 */
public class HistoryComboBox extends AutoCompComboBox<String> {

    /**
     * Constructs a new {@code HistoryComboBox}.
     */
    public HistoryComboBox() {
        super(new HistoryComboBoxModel());
        setPrototypeDisplayValue("dummy");
    }

    @Override
    public HistoryComboBoxModel getModel() {
        return (HistoryComboBoxModel) dataModel;
    }

    /**
     * Adds the item in the editor to the top of the history. If the item is already present, don't
     * add another but move it to the top. The item is then selected.
     */
    public void addCurrentItemToHistory() {
        String newItem = getModel().addTopElement(getEditor().getItem().toString());
        getModel().setSelectedItem(newItem);
    }
}
