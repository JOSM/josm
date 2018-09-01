// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.changeset;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.DefaultListSelectionModel;
import javax.swing.table.AbstractTableModel;

import org.openstreetmap.josm.data.osm.ChangesetDataSet;
import org.openstreetmap.josm.data.osm.ChangesetDataSet.ChangesetDataSetEntry;
import org.openstreetmap.josm.data.osm.ChangesetDataSet.ChangesetModificationType;
import org.openstreetmap.josm.data.osm.history.HistoryOsmPrimitive;

/**
 * This is the table model for the content of a changeset.
 * @since 2689
 */
public class ChangesetContentTableModel extends AbstractTableModel {

    private final transient List<ChangesetContentEntry> data = new ArrayList<>();
    private final DefaultListSelectionModel selectionModel;

    /**
     * Constructs a new {@code ChangesetContentTableModel}.
     * @param selectionModel selection model
     */
    public ChangesetContentTableModel(DefaultListSelectionModel selectionModel) {
        this.selectionModel = selectionModel;
    }

    /**
     * Replies true if there is at least one selected primitive in the table model
     *
     * @return true if there is at least one selected primitive in the table model
     */
    public boolean hasSelectedPrimitives() {
        return selectionModel.getMinSelectionIndex() >= 0;
    }

    /**
     * Selects a single item by its index.
     * @param row index
     */
    public void setSelectedByIdx(int row) {
        selectionModel.setSelectionInterval(row, row);
    }

    /**
     * Replies the selection model
     * @return the selection model
     */
    public DefaultListSelectionModel getSelectionModel() {
        return selectionModel;
    }

    /**
     * Returns the selected history primitives.
     * @return the selected history primitives
     */
    public Set<HistoryOsmPrimitive> getSelectedPrimitives() {
        Set<HistoryOsmPrimitive> ret = new HashSet<>();
        for (int i = 0; i < data.size(); i++) {
            if (selectionModel.isSelectedIndex(i)) {
                ret.add(data.get(i).getPrimitive());
            }
        }
        return ret;
    }

    /**
     * Populates the model with the content of a changeset. If ds is null, the table is cleared.
     *
     * @param ds the changeset content.
     */
    public void populate(ChangesetDataSet ds) {
        this.data.clear();
        if (ds == null) {
            fireTableDataChanged();
            return;
        }
        for (Iterator<ChangesetDataSetEntry> it = ds.iterator(); it.hasNext();) {
            data.add(new ChangesetContentEntry(it.next()));
        }
        sort();
        fireTableDataChanged();
    }

    /**
     * Sort data.
     */
    protected void sort() {
        data.sort((c1, c2) -> {
                if (c1.getModificationType() == c2.getModificationType()) {
                    long id1 = c1.getPrimitive().getId();
                    long id2 = c2.getPrimitive().getId();

                    if (id1 == id2)
                        return 0;
                    else if (id1 < id2)
                        return -1;
                    return 1;
                }
                switch(c1.getModificationType()) {
                case CREATED: return -1;
                case UPDATED:
                    switch(c2.getModificationType()) {
                    case CREATED: return 1;
                    default: return -1;
                    }
                case DELETED:
                    return 1;
                }
                // should not happen
                return 0;
            }
        );
    }

    /* -------------------------------------------------------------- */
    /* interface TableModel                                           */
    /* -------------------------------------------------------------- */
    @Override
    public int getColumnCount() {
        return 3;
    }

    @Override
    public int getRowCount() {
        return data.size();
    }

    @Override
    public Object getValueAt(int row, int col) {
        switch(col) {
        case 0: return data.get(row).getModificationType();
        default: return data.get(row).getPrimitive();
        }
    }

    /**
     * The type used internally to keep information about {@link HistoryOsmPrimitive}
     * with their {@link ChangesetModificationType}.
     */
    private static class ChangesetContentEntry implements ChangesetDataSetEntry {
        private final ChangesetModificationType modificationType;
        private final HistoryOsmPrimitive primitive;

        ChangesetContentEntry(ChangesetModificationType modificationType, HistoryOsmPrimitive primitive) {
            this.modificationType = modificationType;
            this.primitive = primitive;
        }

        ChangesetContentEntry(ChangesetDataSetEntry entry) {
            this(entry.getModificationType(), entry.getPrimitive());
        }

        @Override
        public ChangesetModificationType getModificationType() {
            return modificationType;
        }

        @Override
        public HistoryOsmPrimitive getPrimitive() {
            return primitive;
        }
    }
}
