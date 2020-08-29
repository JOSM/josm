// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.changeset;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.DefaultListSelectionModel;
import javax.swing.JTable;
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
     * @param table the JTable used with this model
     * @return the selected history primitives
     */
    public Set<HistoryOsmPrimitive> getSelectedPrimitives(JTable table) {
        int[] selection = table.getSelectedRows();
        return Arrays.stream(selection)
                .mapToObj(i -> data.get(table.convertRowIndexToModel(i)).getPrimitive())
                .collect(Collectors.toSet());
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
        data.sort(Comparator.comparing(ChangesetContentEntry::getModificationType).thenComparingLong(c -> c.getPrimitive().getId())
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
