// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.properties;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.util.Map;
import java.util.Objects;
import java.util.function.IntFunction;
import java.util.function.Supplier;

import javax.swing.JTable;

import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.gui.MainApplication;

/**
 * Launch browser with wiki help for selected tag.
 * @since 15581
 */
public class HelpTagAction extends HelpAction {
    private final Supplier<Tag> tagSupplier;

    /**
     * Constructs a new {@code HelpAction}.
     * @param tagSupplier Supplies the tag for which the help should be shown
     * @since 16274
     */
    public HelpTagAction(Supplier<Tag> tagSupplier) {
        this.tagSupplier = Objects.requireNonNull(tagSupplier);
        putValue(NAME, tr("Go to OSM wiki for tag help"));
    }

    /**
     * Constructs a new {@code HelpAction}.
     * @param tagTable The tag table. Cannot be null
     * @param tagKeySupplier Finds the key from given row of tag table. Cannot be null
     * @param tagValuesSupplier Finds the values from given row of tag table (map of values and number of occurrences). Cannot be null
     */
    public HelpTagAction(JTable tagTable, IntFunction<String> tagKeySupplier, IntFunction<Map<String, Integer>> tagValuesSupplier) {
        this.tagSupplier = () -> {
            if (tagTable.getSelectedRowCount() == 1) {
                int row = tagTable.getSelectedRow();
                String key = tagKeySupplier.apply(row);
                Map<String, Integer> m = tagValuesSupplier.apply(row);
                if (!m.isEmpty()) {
                    String val = m.entrySet().iterator().next().getKey();
                    return new Tag(key, val);
                }
            }
            return null;
        };
        putValue(NAME, tr("Go to OSM wiki for tag help"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Tag tag = tagSupplier.get();
        if (tag != null) {
            MainApplication.worker.execute(() -> displayTagHelp(tag.getKey(), tag.getValue()));
        } else {
            super.actionPerformed(e);
        }
    }
}
