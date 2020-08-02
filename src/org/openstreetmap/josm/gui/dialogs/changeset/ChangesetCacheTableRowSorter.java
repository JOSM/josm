// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.changeset;

import java.util.Comparator;

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
        setComparator(0, Comparator.comparingInt(Changeset::getId));

        // column 1 - Upload comment
        setComparator(1, Comparator.comparing(Changeset::getComment, AlphanumComparator.getInstance()));

        // column 2 - Open
        setComparator(2, Comparator.comparing(Changeset::isOpen));

        // column 3 - User
        setComparator(3, Comparator.comparing(Changeset::getUser, Comparator.comparing(User::getName)));

        // column 4 - Created at
        setComparator(4, Comparator.comparing(Changeset::getCreatedAt));

        // column 5 - Closed at
        setComparator(5, Comparator.comparing(Changeset::getClosedAt));

        // column 6 - Changes
        setComparator(6, Comparator.comparingInt(Changeset::getChangesCount));

        // column 7 - Discussions
        setComparator(7, Comparator.comparingInt(Changeset::getCommentsCount));
    }
}
