// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.DefaultListSelectionModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

/**
 * ListMergeModel is a model for interactively comparing and merging two list of entries
 * of type T. It maintains three lists of entries of type T:
 * <ol>
 *   <li>the list of <em>my</em> entries</li>
 *   <li>the list of <em>their</em> entries</li>
 *   <li>the list of <em>merged</em> entries</li>
 * </ol>
 * 
 * A ListMergeModel is a factory for three {@see TableModel}s and three {@see ListSelectionModel}s:
 * <ol>
 *   <li>the table model and the list selection for for a  {@see JTable} which shows my entries.
 *    See {@see #getMyTableModel()}</li> and {@see ListMergeModel#getMySelectionModel()}</li>
 *   <li>dito for their entries and merged entries</li>
 * </ol>
 *
 * A ListMergeModel can be ''frozen''. If it's frozen, it doesn't accept additional merge
 * decisions. {@see PropertyChangeListener}s can register for property value changes of
 * {@see #PROP_FROZEN}.
 * 
 * ListMergeModel is an abstract class. Three methods have to be implemented by subclasses:
 * <ul>
 *   <li>{@see ListMergeModel#cloneEntry(Object)} - clones an entry of type T</li>
 *   <li>{@see ListMergeModel#isEqualEntry(Object, Object)} - checks whether two entries are equals </li>
 *   <li>{@see ListMergeModel#setValueAt(DefaultTableModel, Object, int, int)} - handles values edited in
 *     a JTable, dispatched from {@see TableModel#setValueAt(Object, int, int)} </li>
 * </ul>
 * A ListMergeModel is used in combination with a {@see ListMerger}.
 *
 * @param <T>  the type of the list entries
 * @see ListMerger
 */
public abstract class ListMergeModel<T> {
    private static final Logger logger = Logger.getLogger(ListMergeModel.class.getName());

    public static final String PROP_FROZEN = ListMergeModel.class.getName() + ".frozen";

    protected ArrayList<T> myEntries;
    protected ArrayList<T> theirEntries;
    protected ArrayList<T> mergedEntries;


    protected DefaultTableModel myEntriesTableModel;
    protected DefaultTableModel theirEntriesTableModel;
    protected DefaultTableModel mergedEntriesTableModel;

    protected EntriesSelectionModel<T> myEntriesSelectionModel;
    protected EntriesSelectionModel<T> theirEntriesSelectionModel;
    protected EntriesSelectionModel<T> mergedEntriesSelectionModel;

    private final ArrayList<PropertyChangeListener> listeners;
    private boolean isFrozen = false;

    /**
     * Clones an entry of type T
     * @param entry the entry
     * @return the cloned entry
     */
    protected abstract T cloneEntry(T entry);

    /**
     * checks whether two entries are equal. This is not necessarily the same as
     * e1.equals(e2).
     * 
     * @param e1  the first entry
     * @param e2  the second entry
     * @return true, if the entries are equal, false otherwise.
     */
    public abstract boolean isEqualEntry(T e1, T e2);

    /**
     * Handles method dispatches from {@see TableModel#setValueAt(Object, int, int)}.
     * 
     * @param model the table model
     * @param value  the value to be set
     * @param row  the row index
     * @param col the column index
     * 
     * @see TableModel#setValueAt(Object, int, int)
     */
    protected abstract void setValueAt(DefaultTableModel model, Object value, int row, int col);



    protected void buildMyEntriesTableModel() {
        myEntriesTableModel = new EntriesTableModel<T>(myEntries);
    }

    protected void buildTheirEntriesTableModel() {
        theirEntriesTableModel = new EntriesTableModel<T>(theirEntries);
    }

    protected void buildMergedEntriesTableModel() {
        mergedEntriesTableModel = new EntriesTableModel<T>(mergedEntries);
    }

    public ListMergeModel() {
        myEntries = new ArrayList<T>();
        theirEntries = new ArrayList<T>();
        mergedEntries = new ArrayList<T>();

        buildMyEntriesTableModel();
        buildTheirEntriesTableModel();
        buildMergedEntriesTableModel();

        myEntriesSelectionModel = new EntriesSelectionModel<T>(myEntries);
        theirEntriesSelectionModel = new EntriesSelectionModel<T>(theirEntries);
        mergedEntriesSelectionModel =  new EntriesSelectionModel<T>(mergedEntries);

        listeners = new ArrayList<PropertyChangeListener>();

        setFrozen(true);
    }


    public void addPropertyChangeListener(PropertyChangeListener listener) {
        synchronized(listeners) {
            if (listener != null && ! listeners.contains(listener)) {
                listeners.add(listener);
            }
        }
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        synchronized(listeners) {
            if (listener != null && listeners.contains(listener)) {
                listeners.remove(listener);
            }
        }
    }

    protected void fireFrozenChanged(boolean oldValue, boolean newValue) {
        synchronized(listeners) {
            PropertyChangeEvent evt = new PropertyChangeEvent(this, PROP_FROZEN, oldValue, newValue);
            for (PropertyChangeListener listener: listeners) {
                listener.propertyChange(evt);
            }
        }
    }

    public void setFrozen(boolean isFrozen) {
        boolean oldValue = this.isFrozen;
        this.isFrozen = isFrozen;
        fireFrozenChanged(oldValue, this.isFrozen);
    }

    public boolean isFrozen() {
        return isFrozen;
    }

    public TableModel getMyTableModel() {
        return myEntriesTableModel;
    }

    public TableModel getTheirTableModel() {
        return theirEntriesTableModel;
    }

    public TableModel getMergedTableModel() {
        return mergedEntriesTableModel;
    }

    public EntriesSelectionModel getMySelectionModel() {
        return myEntriesSelectionModel;
    }

    public EntriesSelectionModel getTheirSelectionModel() {
        return theirEntriesSelectionModel;
    }

    public EntriesSelectionModel getMergedSelectionModel() {
        return mergedEntriesSelectionModel;
    }


    protected void fireModelDataChanged() {
        myEntriesTableModel.fireTableDataChanged();
        theirEntriesTableModel.fireTableDataChanged();
        mergedEntriesTableModel.fireTableDataChanged();
    }

    protected void copyToTop(List<T> source, int []rows) {
        if (rows == null || rows.length == 0)
            return;
        for (int i = rows.length - 1; i >= 0; i--) {
            int row = rows[i];
            T n = source.get(row);
            mergedEntries.add(0, cloneEntry(n));
        }
        fireModelDataChanged();
        mergedEntriesSelectionModel.setSelectionInterval(0, rows.length -1);
    }

    /**
     * Copies the nodes given by indices in rows from the list of my nodes to the
     * list of merged nodes. Inserts the nodes at the top of the list of merged
     * nodes.
     * 
     * @param rows the indices
     */
    public void copyMyToTop(int [] rows) {
        copyToTop(myEntries, rows);
    }

    /**
     * Copies the nodes given by indices in rows from the list of their nodes to the
     * list of merged nodes. Inserts the nodes at the top of the list of merged
     * nodes.
     * 
     * @param rows the indices
     */
    public void copyTheirToTop(int [] rows) {
        copyToTop(theirEntries, rows);
    }

    /**
     * Copies the nodes given by indices in rows from the list of  nodes in source to the
     * list of merged nodes. Inserts the nodes at the end of the list of merged
     * nodes.
     * 
     * @param source the list of nodes to copy from
     * @param rows the indices
     */

    public void copyToEnd(List<T> source, int [] rows) {
        if (rows == null || rows.length == 0)
            return;
        for (int row : rows) {
            T n = source.get(row);
            mergedEntries.add(cloneEntry(n));
        }
        fireModelDataChanged();
        mergedEntriesSelectionModel.setSelectionInterval(mergedEntries.size()-rows.length, mergedEntries.size() -1);

    }

    /**
     * Copies the nodes given by indices in rows from the list of my nodes to the
     * list of merged nodes. Inserts the nodes at the end of the list of merged
     * nodes.
     * 
     * @param rows the indices
     */
    public void copyMyToEnd(int [] rows) {
        copyToEnd(myEntries, rows);
    }

    /**
     * Copies the nodes given by indices in rows from the list of their nodes to the
     * list of merged nodes. Inserts the nodes at the end of the list of merged
     * nodes.
     * 
     * @param rows the indices
     */
    public void copyTheirToEnd(int [] rows) {
        copyToEnd(theirEntries, rows);
    }

    /**
     * Copies the nodes given by indices in rows from the list of  nodes <code>source</code> to the
     * list of merged nodes. Inserts the nodes before row given by current.
     * 
     * @param source the list of nodes to copy from
     * @param rows the indices
     * @param current the row index before which the nodes are inserted
     * @exception IllegalArgumentException thrown, if current < 0 or >= #nodes in list of merged nodes
     * 
     */
    protected void copyBeforeCurrent(List<T> source, int [] rows, int current) {
        if (rows == null || rows.length == 0)
            return;
        if (current < 0 || current >= mergedEntries.size())
            throw new IllegalArgumentException(tr("parameter current out of range: got {0}", current));
        for (int i=rows.length -1; i>=0; i--) {
            int row = rows[i];
            T n = source.get(row);
            mergedEntries.add(current, cloneEntry(n));
        }
        fireModelDataChanged();
        mergedEntriesSelectionModel.setSelectionInterval(current, current + rows.length-1);
    }

    /**
     * Copies the nodes given by indices in rows from the list of my nodes to the
     * list of merged nodes. Inserts the nodes before row given by current.
     * 
     * @param rows the indices
     * @param current the row index before which the nodes are inserted
     * @exception IllegalArgumentException thrown, if current < 0 or >= #nodes in list of merged nodes
     * 
     */
    public void copyMyBeforeCurrent(int [] rows, int current) {
        copyBeforeCurrent(myEntries,rows,current);
    }

    /**
     * Copies the nodes given by indices in rows from the list of their nodes to the
     * list of merged nodes. Inserts the nodes before row given by current.
     * 
     * @param rows the indices
     * @param current the row index before which the nodes are inserted
     * @exception IllegalArgumentException thrown, if current < 0 or >= #nodes in list of merged nodes
     * 
     */
    public void copyTheirBeforeCurrent(int [] rows, int current) {
        copyBeforeCurrent(theirEntries,rows,current);
    }

    /**
     * Copies the nodes given by indices in rows from the list of  nodes <code>source</code> to the
     * list of merged nodes. Inserts the nodes after the row given by current.
     * 
     * @param source the list of nodes to copy from
     * @param rows the indices
     * @param current the row index after which the nodes are inserted
     * @exception IllegalArgumentException thrown, if current < 0 or >= #nodes in list of merged nodes
     * 
     */
    protected void copyAfterCurrent(List<T> source, int [] rows, int current) {
        if (rows == null || rows.length == 0)
            return;
        if (current < 0 || current >= mergedEntries.size())
            throw new IllegalArgumentException(tr("parameter current out of range: got {0}", current));
        if (current == mergedEntries.size() -1) {
            if (source == myEntries) {
                copyMyToEnd(rows);
            } else if (source == theirEntries) {
                copyTheirToEnd(rows);
            }
        } else {
            for (int i=rows.length -1; i>=0; i--) {
                int row = rows[i];
                T n = source.get(row);
                mergedEntries.add(current+1, cloneEntry(n));
            }
        }
        fireModelDataChanged();
        mergedEntriesSelectionModel.setSelectionInterval(current+1, current + rows.length-1);
    }

    /**
     * Copies the nodes given by indices in rows from the list of my nodes to the
     * list of merged nodes. Inserts the nodes after the row given by current.
     * 
     * @param rows the indices
     * @param current the row index after which the nodes are inserted
     * @exception IllegalArgumentException thrown, if current < 0 or >= #nodes in list of merged nodes
     * 
     */
    public void copyMyAfterCurrent(int [] rows, int current) {
        copyAfterCurrent(myEntries, rows, current);
    }

    /**
     * Copies the nodes given by indices in rows from the list of my nodes to the
     * list of merged nodes. Inserts the nodes after the row given by current.
     * 
     * @param rows the indices
     * @param current the row index after which the nodes are inserted
     * @exception IllegalArgumentException thrown, if current < 0 or >= #nodes in list of merged nodes
     * 
     */
    public void copyTheirAfterCurrent(int [] rows, int current) {
        copyAfterCurrent(theirEntries, rows, current);
    }

    /**
     * Moves the nodes given by indices in rows  up by one position in the list
     * of merged nodes.
     * 
     * @param rows the indices
     * 
     */
    public void moveUpMerged(int [] rows) {
        if (rows == null || rows.length == 0)
            return;
        if (rows[0] == 0)
            // can't move up
            return;
        for (int row: rows) {
            T n = mergedEntries.get(row);
            mergedEntries.remove(row);
            mergedEntries.add(row -1, n);
        }
        fireModelDataChanged();
        mergedEntriesSelectionModel.clearSelection();
        for (int row: rows) {
            mergedEntriesSelectionModel.addSelectionInterval(row-1, row-1);
        }
    }

    /**
     * Moves the nodes given by indices in rows down by one position in the list
     * of merged nodes.
     * 
     * @param rows the indices
     */
    public void moveDownMerged(int [] rows) {
        if (rows == null || rows.length == 0)
            return;
        if (rows[rows.length -1] == mergedEntries.size() -1)
            // can't move down
            return;
        for (int i = rows.length-1; i>=0;i--) {
            int row = rows[i];
            T n = mergedEntries.get(row);
            mergedEntries.remove(row);
            mergedEntries.add(row +1, n);
        }
        fireModelDataChanged();
        mergedEntriesSelectionModel.clearSelection();
        for (int row: rows) {
            mergedEntriesSelectionModel.addSelectionInterval(row+1, row+1);
        }
    }

    /**
     * Removes the nodes given by indices in rows from the list
     * of merged nodes.
     * 
     * @param rows the indices
     */
    public void removeMerged(int [] rows) {
        if (rows == null || rows.length == 0)
            return;
        for (int i = rows.length-1; i>=0;i--) {
            mergedEntries.remove(rows[i]);
        }
        fireModelDataChanged();
        mergedEntriesSelectionModel.clearSelection();
    }


    /**
     * Replies true if the list of my entries and the list of their
     * entries are equal
     * 
     * @return true, if the lists are equal; false otherwise
     */
    protected boolean myAndTheirEntriesEqual() {
        if (myEntries.size() != theirEntries.size())
            return false;
        for (int i=0; i < myEntries.size(); i++) {
            if (! isEqualEntry(myEntries.get(i), theirEntries.get(i)))
                return false;
        }
        return true;
    }


    protected class EntriesTableModel<T1> extends DefaultTableModel {
        private final ArrayList<T1> entries;

        public EntriesTableModel(ArrayList<T1> nodes) {
            this.entries = nodes;
        }

        @Override
        public int getRowCount() {
            int count = myEntries.size();
            count = Math.max(count, mergedEntries.size());
            count = Math.max(count, theirEntries.size());
            return count;
        }

        @Override
        public Object getValueAt(int row, int column) {
            if (row < entries.size())
                return entries.get(row);
            return null;
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }

        @Override
        public void setValueAt(Object value, int row, int col) {
            ListMergeModel.this.setValueAt(this, value,row,col);
        }
    }

    protected class EntriesSelectionModel<T1> extends DefaultListSelectionModel {
        private final ArrayList<T1> entries;

        public EntriesSelectionModel(ArrayList<T1> nodes) {
            this.entries = nodes;
        }

        @Override
        public void addSelectionInterval(int index0, int index1) {
            if (entries.isEmpty()) return;
            if (index0 > entries.size() - 1) return;
            index0 = Math.min(entries.size()-1, index0);
            index1 = Math.min(entries.size()-1, index1);
            super.addSelectionInterval(index0, index1);
        }

        @Override
        public void insertIndexInterval(int index, int length, boolean before) {
            if (entries.isEmpty()) return;
            if (before) {
                int newindex = Math.min(entries.size()-1, index);
                if (newindex < index - length) return;
                length = length - (index - newindex);
                super.insertIndexInterval(newindex, length, before);
            } else {
                if (index > entries.size() -1) return;
                length = Math.min(entries.size()-1 - index, length);
                super.insertIndexInterval(index, length, before);
            }
        }

        @Override
        public void moveLeadSelectionIndex(int leadIndex) {
            if (entries.isEmpty()) return;
            leadIndex = Math.max(0, leadIndex);
            leadIndex = Math.min(entries.size() - 1, leadIndex);
            super.moveLeadSelectionIndex(leadIndex);
        }

        @Override
        public void removeIndexInterval(int index0, int index1) {
            if (entries.isEmpty()) return;
            index0 = Math.max(0, index0);
            index0 = Math.min(entries.size() - 1, index0);

            index1 = Math.max(0, index1);
            index1 = Math.min(entries.size() - 1, index1);
            super.removeIndexInterval(index0, index1);
        }

        @Override
        public void removeSelectionInterval(int index0, int index1) {
            if (entries.isEmpty()) return;
            index0 = Math.max(0, index0);
            index0 = Math.min(entries.size() - 1, index0);

            index1 = Math.max(0, index1);
            index1 = Math.min(entries.size() - 1, index1);
            super.removeSelectionInterval(index0, index1);
        }

        @Override
        public void setAnchorSelectionIndex(int anchorIndex) {
            if (entries.isEmpty()) return;
            anchorIndex = Math.min(entries.size() - 1, anchorIndex);
            super.setAnchorSelectionIndex(anchorIndex);
        }

        @Override
        public void setLeadSelectionIndex(int leadIndex) {
            if (entries.isEmpty()) return;
            leadIndex = Math.min(entries.size() - 1, leadIndex);
            super.setLeadSelectionIndex(leadIndex);
        }

        @Override
        public void setSelectionInterval(int index0, int index1) {
            if (entries.isEmpty()) return;
            index0 = Math.max(0, index0);
            index0 = Math.min(entries.size() - 1, index0);

            index1 = Math.max(0, index1);
            index1 = Math.min(entries.size() - 1, index1);

            super.setSelectionInterval(index0, index1);
        }
    }
}
