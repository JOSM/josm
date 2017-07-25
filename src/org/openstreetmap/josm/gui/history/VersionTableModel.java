// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.history;

import java.text.DateFormat;

import javax.swing.table.AbstractTableModel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.data.osm.User;
import org.openstreetmap.josm.data.osm.history.HistoryOsmPrimitive;
import org.openstreetmap.josm.tools.date.DateUtils;

/**
 * The table model for the list of versions in the current history
 * @since 11646 (extracted from HistoryBrowserModel)
 */
public final class VersionTableModel extends AbstractTableModel {

    private final HistoryBrowserModel model;

    /**
     * Constructs a new {@code VersionTableModel}.
     * @param model parent {@code HistoryBrowserModel}
     */
    public VersionTableModel(HistoryBrowserModel model) {
        this.model = model;
    }

    @Override
    public int getRowCount() {
        if (model.getHistory() == null)
            return 0;
        int ret = model.getHistory().getNumVersions();
        if (model.getLatest() != null) {
            ret++;
        }
        return ret;
    }

    @Override
    public Object getValueAt(int row, int column) {
        switch (column) {
        case VersionTableColumnModel.COL_VERSION:
            HistoryOsmPrimitive p1 = model.getPrimitive(row);
            if (p1 != null)
                return Long.toString(p1.getVersion());
            return null;
        case VersionTableColumnModel.COL_REFERENCE:
            return model.isReferencePointInTime(row);
        case VersionTableColumnModel.COL_CURRENT:
            return model.isCurrentPointInTime(row);
        case VersionTableColumnModel.COL_DATE:
            HistoryOsmPrimitive p3 = model.getPrimitive(row);
            if (p3 != null && p3.getTimestamp() != null)
                return DateUtils.formatDateTime(p3.getTimestamp(), DateFormat.SHORT, DateFormat.SHORT);
            return null;
        case VersionTableColumnModel.COL_USER:
            HistoryOsmPrimitive p4 = model.getPrimitive(row);
            if (p4 != null) {
                User user = p4.getUser();
                if (user != null)
                    return user.getName();
            }
            return null;
        case VersionTableColumnModel.COL_EDITOR:
            HistoryOsmPrimitive p5 = model.getPrimitive(row);
            if (p5 != null) {
                Changeset cs = p5.getChangeset();
                if (cs != null) {
                    return cs.get("created_by");
                }
            }
            return null;
        default:
            return null;
        }
    }

    @Override
    public void setValueAt(Object aValue, int row, int column) {
        if (!((Boolean) aValue))
            return;
        try {
            switch (column) {
            case 1:
                model.setReferencePointInTime(row);
                break;
            case 2:
                model.setCurrentPointInTime(row);
                break;
            default:
                return;
            }
        } catch (IllegalArgumentException e) {
            Main.error(e);
        }
        fireTableDataChanged();
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return column >= 1 && column <= 2;
    }

    @Override
    public int getColumnCount() {
        return 6;
    }
}
