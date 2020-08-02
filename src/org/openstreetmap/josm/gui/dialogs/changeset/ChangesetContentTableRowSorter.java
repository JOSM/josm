// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.changeset;

import java.util.Comparator;

import javax.swing.table.TableRowSorter;

import org.openstreetmap.josm.data.osm.DefaultNameFormatter;
import org.openstreetmap.josm.data.osm.history.HistoryOsmPrimitive;
import org.openstreetmap.josm.tools.AlphanumComparator;

/**
 * The row sorter for the changeset content
 * @since 16826
 */
class ChangesetContentTableRowSorter extends TableRowSorter<ChangesetContentTableModel> {

    ChangesetContentTableRowSorter(ChangesetContentTableModel model) {
        super(model);

        // column 1 - ID
        setComparator(1, Comparator.comparing(HistoryOsmPrimitive::getType).thenComparingLong(HistoryOsmPrimitive::getId));

        // column 2 - Name
        setComparator(2, Comparator.<HistoryOsmPrimitive, String>comparing(p ->
                p.getDisplayName(DefaultNameFormatter.getInstance()), AlphanumComparator.getInstance()));
    }
}
