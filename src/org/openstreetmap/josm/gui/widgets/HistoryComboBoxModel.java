// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.widgets;

import java.util.ArrayList;
import java.util.List;

import org.openstreetmap.josm.gui.tagging.ac.AutoCompComboBoxModel;
import org.openstreetmap.josm.spi.preferences.Config;

/**
 * A data model for the {@link HistoryComboBox}.
 * <p>
 * This model is an {@link AutoCompComboBoxModel} specialized in {@code String}s. It offers
 * convenience functions to serialize to and from the JOSM preferences.
 *
 * @since 18173
 */
public class HistoryComboBoxModel extends AutoCompComboBoxModel<String> {

    HistoryComboBoxModel() {
        // The user's preference for max. number of items in histories.
        setSize(Config.getPref().getInt("search.history-size", 15));
    }

    /**
     * Adds strings to the model.
     * <p>
     * Strings are added only until the max. history size is reached.
     *
     * @param strings the strings to add
     */
    public void addAllStrings(List<String> strings) {
        strings.forEach(s -> addElement(s));
    }

    /**
     * Gets all items in the history as a list of strings.
     *
     * @return the items in the history
     */
    public List<String> asStringList() {
        List<String> list = new ArrayList<>(getSize());
        this.forEach(item -> list.add(item));
        return list;
    }

    /**
     * Gets a preference loader and saver for this model.
     *
     * @return the instance
     */
    public Preferences prefs() {
        return prefs(x -> x, x -> x);
    }
}
