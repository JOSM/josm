// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.changeset;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.swing.DefaultListSelectionModel;
import javax.swing.table.AbstractTableModel;

import org.openstreetmap.josm.data.osm.ChangesetDataSet;
import org.openstreetmap.josm.data.osm.ChangesetDataSet.ChangesetDataSetEntry;
import org.openstreetmap.josm.data.osm.ChangesetDataSet.ChangesetModificationType;
import org.openstreetmap.josm.data.osm.history.HistoryOsmPrimitive;

/**
 * This is the table model for the content of a changeset.
 *
 */
public class ChangesetContentTableModel extends AbstractTableModel {

    private final ArrayList<ChangesetContentEntry> data = new ArrayList<ChangesetContentEntry>();
    private DefaultListSelectionModel selectionModel;

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

    public Set<HistoryOsmPrimitive> getSelectedPrimitives() {
        Set<HistoryOsmPrimitive> ret = new HashSet<HistoryOsmPrimitive>();
        for (int i=0;i < data.size();i++) {
            if (selectionModel.isSelectedIndex(i)) {
                ret.add(data.get(i).getPrimitive());
            }
        }
        return ret;
    }

    /**
     * Populates the model with the content of a model. If ds is null, the
     * table is cleared.
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

    protected void sort() {
        Collections.sort(
                data,
                new Comparator<ChangesetDataSetEntry>() {
                    public int compare(ChangesetDataSetEntry c1, ChangesetDataSetEntry c2) {
                        if (c1.getModificationType().equals(c2.getModificationType())) {
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
                }
        );
    }

    /* -------------------------------------------------------------- */
    /* interface TableModel                                           */
    /* -------------------------------------------------------------- */
    public int getColumnCount() {
        return 3;
    }

    public int getRowCount() {
        return data.size();
    }

    public Object getValueAt(int row, int col) {
        switch(col) {
        case 0: return data.get(row).getModificationType();
        default: return data.get(row).getPrimitive();
        }
    }

    /**
     * The type used internally to keep information about {@see HistoryOsmPrimitive}
     * with their {@see ChangesetModificationType}.
     *
     */
    static private class ChangesetContentEntry implements ChangesetDataSetEntry{
        private ChangesetModificationType modificationType;
        private HistoryOsmPrimitive primitive;

        public ChangesetContentEntry(ChangesetModificationType modificationType, HistoryOsmPrimitive primitive) {
            this.modificationType = modificationType;
            this.primitive = primitive;
        }

        public ChangesetContentEntry(ChangesetDataSetEntry entry) {
            this.modificationType = entry.getModificationType();
            this.primitive = entry.getPrimitive();
        }

        public ChangesetModificationType getModificationType() {
            return modificationType;
        }
        public void setModificationType(ChangesetModificationType modificationType) {
            this.modificationType = modificationType;
        }
        public HistoryOsmPrimitive getPrimitive() {
            return primitive;
        }
        public void setPrimitive(HistoryOsmPrimitive primitive) {
            this.primitive = primitive;
        }
    }
}
