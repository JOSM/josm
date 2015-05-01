// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.changeset;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import org.openstreetmap.josm.data.osm.ChangesetDiscussionComment;

/**
 * Model of changeset discussion table.
 * @since 7715
 */
public class ChangesetDiscussionTableModel extends AbstractTableModel {

    private final transient List<ChangesetDiscussionComment> data = new ArrayList<>();

    @Override
    public int getRowCount() {
        return data.size();
    }

    @Override
    public int getColumnCount() {
        return 3;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (rowIndex < 0 || rowIndex >= data.size())
            return null;
        switch (columnIndex) {
        case 0:
            return data.get(rowIndex).getDate();
        case 1:
            return data.get(rowIndex).getUser();
        default:
            return data.get(rowIndex).getText();
        }
    }

    /**
     * Populates the model with the discussion of a changeset. If ds is null, the table is cleared.
     *
     * @param list the changeset discussion.
     */
    public void populate(List<ChangesetDiscussionComment> list) {
        data.clear();
        if (list != null) {
            data.addAll(list);
        }
        fireTableDataChanged();
    }
}
