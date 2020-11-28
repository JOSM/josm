// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.changeset;

import java.util.Comparator;

import javax.swing.table.TableRowSorter;

/**
 * The row sorter for the changeset discussion
 * @since 17370
 */
class ChangesetDiscussionTableRowSorter extends TableRowSorter<ChangesetDiscussionTableModel> {

    ChangesetDiscussionTableRowSorter(ChangesetDiscussionTableModel model) {
        super(model);

        // column 0 - Date
        setComparator(0, Comparator.naturalOrder());
    }
}
