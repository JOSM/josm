// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.DefaultListSelectionModel;
import javax.swing.ListSelectionModel;
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
 * ListMergeModel is an abstract class. There methods have to be implemented by subclasses:
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

    protected DefaultListSelectionModel myEntriesSelectionModel;
    protected DefaultListSelectionModel theirEntriesSelectionModel;
    protected DefaultListSelectionModel mergedEntriesSelectionModel;

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
        myEntriesTableModel = new ListTableModel<T>(myEntries);
    }

    protected void buildTheirEntriesTableModel() {
        theirEntriesTableModel = new ListTableModel<T>(theirEntries);
    }

    protected void buildMergedEntriesTableModel() {
        mergedEntriesTableModel = new ListTableModel<T>(mergedEntries);
    }

    public ListMergeModel() {
        myEntries = new ArrayList<T>();
        theirEntries = new ArrayList<T>();
        mergedEntries = new ArrayList<T>();

        buildMyEntriesTableModel();
        buildTheirEntriesTableModel();
        buildMergedEntriesTableModel();

        myEntriesSelectionModel = new DefaultListSelectionModel();
        theirEntriesSelectionModel = new DefaultListSelectionModel();
        mergedEntriesSelectionModel = new DefaultListSelectionModel();

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

    public ListSelectionModel getMySelectionModel() {
        return myEntriesSelectionModel;
    }

    public ListSelectionModel getTheirSelectionModel() {
        return theirEntriesSelectionModel;
    }

    public ListSelectionModel getMergedSelectionModel() {
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
            copyMyToEnd(rows);
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



    protected class ListTableModel<T> extends DefaultTableModel {
        private final ArrayList<T> entries;

        public ListTableModel(ArrayList<T> nodes) {
            this.entries = nodes;
        }

        @Override
        public int getRowCount() {
            return entries == null ? 0 : entries.size();
        }

        @Override
        public Object getValueAt(int row, int column) {
            return entries.get(row);
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



}
