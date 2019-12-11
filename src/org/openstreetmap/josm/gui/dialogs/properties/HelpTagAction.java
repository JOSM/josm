// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.properties;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.util.Map;
import java.util.Objects;
import java.util.function.IntFunction;

import javax.swing.JTable;

import org.openstreetmap.josm.gui.MainApplication;

/**
 * Launch browser with wiki help for selected tag.
 * @since 15581
 */
public class HelpTagAction extends HelpAction {
    private final JTable tagTable;
    private final IntFunction<String> tagKeySupplier;
    private final IntFunction<Map<String, Integer>> tagValuesSupplier;

    /**
     * Constructs a new {@code HelpAction}.
     * @param tagTable The tag table. Cannot be null
     * @param tagKeySupplier Finds the key from given row of tag table. Cannot be null
     * @param tagValuesSupplier Finds the values from given row of tag table (map of values and number of occurrences). Cannot be null
     */
    public HelpTagAction(JTable tagTable, IntFunction<String> tagKeySupplier, IntFunction<Map<String, Integer>> tagValuesSupplier) {
        this.tagTable = Objects.requireNonNull(tagTable);
        this.tagKeySupplier = Objects.requireNonNull(tagKeySupplier);
        this.tagValuesSupplier = Objects.requireNonNull(tagValuesSupplier);
        putValue(NAME, tr("Go to OSM wiki for tag help"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (tagTable.getSelectedRowCount() == 1) {
            int row = tagTable.getSelectedRow();
            String key = tagKeySupplier.apply(row);
            Map<String, Integer> m = tagValuesSupplier.apply(row);
            if (!m.isEmpty()) {
                String val = m.entrySet().iterator().next().getKey();
                MainApplication.worker.execute(() -> displayTagHelp(key, val));
            }
        } else {
            super.actionPerformed(e);
        }
    }
}
