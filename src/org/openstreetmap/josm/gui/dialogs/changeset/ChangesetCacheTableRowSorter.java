// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.changeset;

import static java.util.Comparator.comparing;
import static java.util.Comparator.comparingInt;
import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsLast;

import javax.swing.table.TableRowSorter;

import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.data.osm.User;
import org.openstreetmap.josm.tools.AlphanumComparator;

/**
 * The row sorter for the changeset table
 * @since 16826
 */
class ChangesetCacheTableRowSorter extends TableRowSorter<ChangesetCacheManagerModel> {

    ChangesetCacheTableRowSorter(ChangesetCacheManagerModel model) {
        super(model);

        // column 0 - Id
        setComparator(0, comparingInt(Changeset::getId));

        // column 1 - Upload comment
        setComparator(1, comparing(Changeset::getComment, AlphanumComparator.getInstance()));

        // column 2 - Open
        setComparator(2, comparing(Changeset::isOpen));

        // column 3 - User
        setComparator(3, comparing(Changeset::getUser, comparing(User::getName)));

        // column 4 - Created at
        setComparator(4, comparing(Changeset::getCreatedAt));

        // column 5 - Closed at
        setComparator(5, comparing(Changeset::getClosedAt, nullsLast(naturalOrder())));

        // column 6 - Changes
        setComparator(6, comparingInt(Changeset::getChangesCount));

        // column 7 - Discussions
        setComparator(7, comparingInt(Changeset::getCommentsCount));
    }
}
