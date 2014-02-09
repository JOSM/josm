// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.pair;

import static org.openstreetmap.josm.gui.conflict.pair.ComparePairType.MY_WITH_MERGED;
import static org.openstreetmap.josm.gui.conflict.pair.ComparePairType.MY_WITH_THEIR;
import static org.openstreetmap.josm.gui.conflict.pair.ComparePairType.THEIR_WITH_MERGED;
import static org.openstreetmap.josm.gui.conflict.pair.ListRole.MERGED_ENTRIES;
import static org.openstreetmap.josm.gui.conflict.pair.ListRole.MY_ENTRIES;
import static org.openstreetmap.josm.gui.conflict.pair.ListRole.THEIR_ENTRIES;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;

import javax.swing.AbstractListModel;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.PrimitiveId;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.gui.HelpAwareOptionPane;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.gui.widgets.OsmPrimitivesTableModel;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.Utils;

/**
 * ListMergeModel is a model for interactively comparing and merging two list of entries
 * of type T. It maintains three lists of entries of type T:
 * <ol>
 *   <li>the list of <em>my</em> entries</li>
 *   <li>the list of <em>their</em> entries</li>
 *   <li>the list of <em>merged</em> entries</li>
 * </ol>
 *
 * A ListMergeModel is a factory for three {@link TableModel}s and three {@link ListSelectionModel}s:
 * <ol>
 *   <li>the table model and the list selection for for a  {@link JTable} which shows my entries.
 *    See {@link #getMyTableModel()} and {@link ListMergeModel#getMySelectionModel()}</li>
 *   <li>dito for their entries and merged entries</li>
 * </ol>
 *
 * A ListMergeModel can be ''frozen''. If it's frozen, it doesn't accept additional merge
 * decisions. {@link PropertyChangeListener}s can register for property value changes of
 * {@link #FROZEN_PROP}.
 *
 * ListMergeModel is an abstract class. Three methods have to be implemented by subclasses:
 * <ul>
 *   <li>{@link ListMergeModel#cloneEntryForMergedList} - clones an entry of type T</li>
 *   <li>{@link ListMergeModel#isEqualEntry} - checks whether two entries are equals </li>
 *   <li>{@link ListMergeModel#setValueAt(DefaultTableModel, Object, int, int)} - handles values edited in
 *     a JTable, dispatched from {@link TableModel#setValueAt(Object, int, int)} </li>
 * </ul>
 * A ListMergeModel is used in combination with a {@link ListMerger}.
 *
 * @param <T>  the type of the list entries
 * @see ListMerger
 */
public abstract class ListMergeModel<T extends PrimitiveId> extends Observable {
    public static final String FROZEN_PROP = ListMergeModel.class.getName() + ".frozen";

    private static final int MAX_DELETED_PRIMITIVE_IN_DIALOG = 5;

    protected Map<ListRole, ArrayList<T>> entries;

    protected EntriesTableModel myEntriesTableModel;
    protected EntriesTableModel theirEntriesTableModel;
    protected EntriesTableModel mergedEntriesTableModel;

    protected EntriesSelectionModel myEntriesSelectionModel;
    protected EntriesSelectionModel theirEntriesSelectionModel;
    protected EntriesSelectionModel mergedEntriesSelectionModel;

    private final List<PropertyChangeListener> listeners;
    private boolean isFrozen = false;
    private final ComparePairListModel comparePairListModel;

    private DataSet myDataset;
    private Map<PrimitiveId, PrimitiveId> mergedMap;

    /**
     * Creates a clone of an entry of type T suitable to be included in the
     * list of merged entries
     *
     * @param entry the entry
     * @return the cloned entry
     */
    protected abstract T cloneEntryForMergedList(T entry);

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
     * Handles method dispatches from {@link TableModel#setValueAt(Object, int, int)}.
     *
     * @param model the table model
     * @param value  the value to be set
     * @param row  the row index
     * @param col the column index
     *
     * @see TableModel#setValueAt(Object, int, int)
     */
    protected abstract void setValueAt(DefaultTableModel model, Object value, int row, int col);

    /**
     *
     * @param entry
     * @return Primitive from my dataset referenced by entry
     */
    public OsmPrimitive getMyPrimitive(T entry) {
        return getMyPrimitiveById(entry);
    }

    public final OsmPrimitive getMyPrimitiveById(PrimitiveId entry) {
        OsmPrimitive result = myDataset.getPrimitiveById(entry);
        if (result == null && mergedMap != null) {
            PrimitiveId id = mergedMap.get(entry);
            if (id == null && entry instanceof OsmPrimitive) {
                id = mergedMap.get(((OsmPrimitive)entry).getPrimitiveId());
            }
            if (id != null) {
                result = myDataset.getPrimitiveById(id);
            }
        }
        return result;
    }

    protected void buildMyEntriesTableModel() {
        myEntriesTableModel = new EntriesTableModel(MY_ENTRIES);
    }

    protected void buildTheirEntriesTableModel() {
        theirEntriesTableModel = new EntriesTableModel(THEIR_ENTRIES);
    }

    protected void buildMergedEntriesTableModel() {
        mergedEntriesTableModel = new EntriesTableModel(MERGED_ENTRIES);
    }

    protected List<T> getMergedEntries() {
        return entries.get(MERGED_ENTRIES);
    }

    protected List<T> getMyEntries() {
        return entries.get(MY_ENTRIES);
    }

    protected List<T> getTheirEntries() {
        return entries.get(THEIR_ENTRIES);
    }

    public int getMyEntriesSize() {
        return getMyEntries().size();
    }

    public int getMergedEntriesSize() {
        return getMergedEntries().size();
    }

    public int getTheirEntriesSize() {
        return getTheirEntries().size();
    }

    public ListMergeModel() {
        entries = new HashMap<ListRole, ArrayList<T>>();
        for (ListRole role : ListRole.values()) {
            entries.put(role, new ArrayList<T>());
        }

        buildMyEntriesTableModel();
        buildTheirEntriesTableModel();
        buildMergedEntriesTableModel();

        myEntriesSelectionModel = new EntriesSelectionModel(entries.get(MY_ENTRIES));
        theirEntriesSelectionModel = new EntriesSelectionModel(entries.get(THEIR_ENTRIES));
        mergedEntriesSelectionModel =  new EntriesSelectionModel(entries.get(MERGED_ENTRIES));

        listeners = new ArrayList<PropertyChangeListener>();
        comparePairListModel = new ComparePairListModel();

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
            PropertyChangeEvent evt = new PropertyChangeEvent(this, FROZEN_PROP, oldValue, newValue);
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

    public OsmPrimitivesTableModel getMyTableModel() {
        return myEntriesTableModel;
    }

    public OsmPrimitivesTableModel getTheirTableModel() {
        return theirEntriesTableModel;
    }

    public OsmPrimitivesTableModel getMergedTableModel() {
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
        setChanged();
        notifyObservers();
    }

    protected void copyToTop(ListRole role, int []rows) {
        copy(role, rows, 0);
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
        copyToTop(MY_ENTRIES, rows);
    }

    /**
     * Copies the nodes given by indices in rows from the list of their nodes to the
     * list of merged nodes. Inserts the nodes at the top of the list of merged
     * nodes.
     *
     * @param rows the indices
     */
    public void copyTheirToTop(int [] rows) {
        copyToTop(THEIR_ENTRIES, rows);
    }

    /**
     * Copies the nodes given by indices in rows from the list of  nodes in source to the
     * list of merged nodes. Inserts the nodes at the end of the list of merged
     * nodes.
     *
     * @param source the list of nodes to copy from
     * @param rows the indices
     */

    public void copyToEnd(ListRole source, int [] rows) {
        copy(source, rows, getMergedEntriesSize());
        mergedEntriesSelectionModel.setSelectionInterval(getMergedEntriesSize()-rows.length, getMergedEntriesSize() -1);

    }

    /**
     * Copies the nodes given by indices in rows from the list of my nodes to the
     * list of merged nodes. Inserts the nodes at the end of the list of merged
     * nodes.
     *
     * @param rows the indices
     */
    public void copyMyToEnd(int [] rows) {
        copyToEnd(MY_ENTRIES, rows);
    }

    /**
     * Copies the nodes given by indices in rows from the list of their nodes to the
     * list of merged nodes. Inserts the nodes at the end of the list of merged
     * nodes.
     *
     * @param rows the indices
     */
    public void copyTheirToEnd(int [] rows) {
        copyToEnd(THEIR_ENTRIES, rows);
    }

    public void clearMerged() {
        getMergedEntries().clear();
        fireModelDataChanged();
    }

    protected final void initPopulate(OsmPrimitive my, OsmPrimitive their, Map<PrimitiveId, PrimitiveId> mergedMap) {
        CheckParameterUtil.ensureParameterNotNull(my, "my");
        CheckParameterUtil.ensureParameterNotNull(their, "their");
        this.myDataset = my.getDataSet();
        this.mergedMap = mergedMap;
        getMergedEntries().clear();
        getMyEntries().clear();
        getTheirEntries().clear();
    }

    protected void alertCopyFailedForDeletedPrimitives(List<PrimitiveId> deletedIds) {
        List<String> items = new ArrayList<String>();
        for (int i=0; i<Math.min(MAX_DELETED_PRIMITIVE_IN_DIALOG, deletedIds.size()); i++) {
            items.add(deletedIds.get(i).toString());
        }
        if (deletedIds.size() > MAX_DELETED_PRIMITIVE_IN_DIALOG) {
            items.add(tr("{0} more...", deletedIds.size() - MAX_DELETED_PRIMITIVE_IN_DIALOG));
        }
        StringBuilder sb = new StringBuilder();
        sb.append("<html>");
        sb.append(tr("The following objects could not be copied to the target object<br>because they are deleted in the target dataset:"));
        sb.append(Utils.joinAsHtmlUnorderedList(items));
        sb.append("</html>");
        HelpAwareOptionPane.showOptionDialog(
                Main.parent,
                sb.toString(),
                tr("Merging deleted objects failed"),
                JOptionPane.WARNING_MESSAGE,
                HelpUtil.ht("/Dialog/Conflict#MergingDeletedPrimitivesFailed")
        );
    }

    private void copy(ListRole sourceRole, int[] rows, int position) {
        if (position < 0 || position > getMergedEntriesSize())
            throw new IllegalArgumentException();
        List<T> newItems = new ArrayList<T>(rows.length);
        List<T> source = entries.get(sourceRole);
        List<PrimitiveId> deletedIds = new ArrayList<PrimitiveId>();
        for (int row: rows) {
            T entry = source.get(row);
            OsmPrimitive primitive = getMyPrimitive(entry);
            if (!primitive.isDeleted()) {
                T clone = cloneEntryForMergedList(entry);
                newItems.add(clone);
            } else {
                deletedIds.add(primitive.getPrimitiveId());
            }
        }
        getMergedEntries().addAll(position, newItems);
        fireModelDataChanged();
        if (!deletedIds.isEmpty()) {
            alertCopyFailedForDeletedPrimitives(deletedIds);
        }
    }

    public void copyAll(ListRole source) {
        getMergedEntries().clear();

        int[] rows = new int[entries.get(source).size()];
        for (int i=0; i<rows.length; i++) {
            rows[i] = i;
        }
        copy(source, rows, 0);
    }

    /**
     * Copies the nodes given by indices in rows from the list of  nodes <code>source</code> to the
     * list of merged nodes. Inserts the nodes before row given by current.
     *
     * @param source the list of nodes to copy from
     * @param rows the indices
     * @param current the row index before which the nodes are inserted
     * @exception IllegalArgumentException thrown, if current &lt; 0 or &gt;= #nodes in list of merged nodes
     *
     */
    protected void copyBeforeCurrent(ListRole source, int [] rows, int current) {
        copy(source, rows, current);
        mergedEntriesSelectionModel.setSelectionInterval(current, current + rows.length-1);
    }

    /**
     * Copies the nodes given by indices in rows from the list of my nodes to the
     * list of merged nodes. Inserts the nodes before row given by current.
     *
     * @param rows the indices
     * @param current the row index before which the nodes are inserted
     * @exception IllegalArgumentException thrown, if current &lt; 0 or &gt;= #nodes in list of merged nodes
     *
     */
    public void copyMyBeforeCurrent(int [] rows, int current) {
        copyBeforeCurrent(MY_ENTRIES,rows,current);
    }

    /**
     * Copies the nodes given by indices in rows from the list of their nodes to the
     * list of merged nodes. Inserts the nodes before row given by current.
     *
     * @param rows the indices
     * @param current the row index before which the nodes are inserted
     * @exception IllegalArgumentException thrown, if current &lt; 0 or &gt;= #nodes in list of merged nodes
     *
     */
    public void copyTheirBeforeCurrent(int [] rows, int current) {
        copyBeforeCurrent(THEIR_ENTRIES,rows,current);
    }

    /**
     * Copies the nodes given by indices in rows from the list of  nodes <code>source</code> to the
     * list of merged nodes. Inserts the nodes after the row given by current.
     *
     * @param source the list of nodes to copy from
     * @param rows the indices
     * @param current the row index after which the nodes are inserted
     * @exception IllegalArgumentException thrown, if current &lt; 0 or &gt;= #nodes in list of merged nodes
     *
     */
    protected void copyAfterCurrent(ListRole source, int [] rows, int current) {
        copy(source, rows, current + 1);
        mergedEntriesSelectionModel.setSelectionInterval(current+1, current + rows.length-1);
        notifyObservers();
    }

    /**
     * Copies the nodes given by indices in rows from the list of my nodes to the
     * list of merged nodes. Inserts the nodes after the row given by current.
     *
     * @param rows the indices
     * @param current the row index after which the nodes are inserted
     * @exception IllegalArgumentException thrown, if current &lt; 0 or &gt;= #nodes in list of merged nodes
     *
     */
    public void copyMyAfterCurrent(int [] rows, int current) {
        copyAfterCurrent(MY_ENTRIES, rows, current);
    }

    /**
     * Copies the nodes given by indices in rows from the list of my nodes to the
     * list of merged nodes. Inserts the nodes after the row given by current.
     *
     * @param rows the indices
     * @param current the row index after which the nodes are inserted
     * @exception IllegalArgumentException thrown, if current &lt; 0 or &gt;= #nodes in list of merged nodes
     *
     */
    public void copyTheirAfterCurrent(int [] rows, int current) {
        copyAfterCurrent(THEIR_ENTRIES, rows, current);
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
        List<T> mergedEntries = getMergedEntries();
        for (int row: rows) {
            T n = mergedEntries.get(row);
            mergedEntries.remove(row);
            mergedEntries.add(row -1, n);
        }
        fireModelDataChanged();
        notifyObservers();
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
        List<T> mergedEntries = getMergedEntries();
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
        notifyObservers();
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

        List<T> mergedEntries = getMergedEntries();

        for (int i = rows.length-1; i>=0;i--) {
            mergedEntries.remove(rows[i]);
        }
        fireModelDataChanged();
        notifyObservers();
        mergedEntriesSelectionModel.clearSelection();
    }

    /**
     * Replies true if the list of my entries and the list of their
     * entries are equal
     *
     * @return true, if the lists are equal; false otherwise
     */
    protected boolean myAndTheirEntriesEqual() {

        if (getMyEntriesSize() != getTheirEntriesSize())
            return false;
        for (int i=0; i < getMyEntriesSize(); i++) {
            if (! isEqualEntry(getMyEntries().get(i), getTheirEntries().get(i)))
                return false;
        }
        return true;
    }

    /**
     * This an adapter between a {@link JTable} and one of the three entry lists
     * in the role {@link ListRole} managed by the {@link ListMergeModel}.
     *
     * From the point of view of the {@link JTable} it is a {@link TableModel}.
     *
     * @see ListMergeModel#getMyTableModel()
     * @see ListMergeModel#getTheirTableModel()
     * @see ListMergeModel#getMergedTableModel()
     */
    public class EntriesTableModel extends DefaultTableModel implements OsmPrimitivesTableModel {
        private final ListRole role;

        /**
         *
         * @param role the role
         */
        public EntriesTableModel(ListRole role) {
            this.role = role;
        }

        @Override
        public int getRowCount() {
            int count = Math.max(getMyEntries().size(), getMergedEntries().size());
            count = Math.max(count, getTheirEntries().size());
            return count;
        }

        @Override
        public Object getValueAt(int row, int column) {
            if (row < entries.get(role).size())
                return entries.get(role).get(row);
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

        public ListMergeModel<T> getListMergeModel() {
            return ListMergeModel.this;
        }

        /**
         * replies true if the {@link ListRole} of this {@link EntriesTableModel}
         * participates in the current {@link ComparePairType}
         *
         * @return true, if the if the {@link ListRole} of this {@link EntriesTableModel}
         * participates in the current {@link ComparePairType}
         *
         * @see ListMergeModel.ComparePairListModel#getSelectedComparePair()
         */
        public boolean isParticipatingInCurrentComparePair() {
            return getComparePairListModel()
            .getSelectedComparePair()
            .isParticipatingIn(role);
        }

        /**
         * replies true if the entry at <code>row</code> is equal to the entry at the
         * same position in the opposite list of the current {@link ComparePairType}.
         *
         * @param row  the row number
         * @return true if the entry at <code>row</code> is equal to the entry at the
         * same position in the opposite list of the current {@link ComparePairType}
         * @exception IllegalStateException thrown, if this model is not participating in the
         *   current  {@link ComparePairType}
         * @see ComparePairType#getOppositeRole(ListRole)
         * @see #getRole()
         * @see #getOppositeEntries()
         */
        public boolean isSamePositionInOppositeList(int row) {
            if (!isParticipatingInCurrentComparePair())
                throw new IllegalStateException(tr("List in role {0} is currently not participating in a compare pair.", role.toString()));
            if (row >= getEntries().size()) return false;
            if (row >= getOppositeEntries().size()) return false;

            T e1 = getEntries().get(row);
            T e2 = getOppositeEntries().get(row);
            return isEqualEntry(e1, e2);
        }

        /**
         * replies true if the entry at the current position is present in the opposite list
         * of the current {@link ComparePairType}.
         *
         * @param row the current row
         * @return true if the entry at the current position is present in the opposite list
         * of the current {@link ComparePairType}.
         * @exception IllegalStateException thrown, if this model is not participating in the
         *   current  {@link ComparePairType}
         * @see ComparePairType#getOppositeRole(ListRole)
         * @see #getRole()
         * @see #getOppositeEntries()
         */
        public boolean isIncludedInOppositeList(int row) {
            if (!isParticipatingInCurrentComparePair())
                throw new IllegalStateException(tr("List in role {0} is currently not participating in a compare pair.", role.toString()));

            if (row >= getEntries().size()) return false;
            T e1 = getEntries().get(row);
            for (T e2: getOppositeEntries()) {
                if (isEqualEntry(e1, e2)) return true;
            }
            return false;
        }

        protected List<T> getEntries() {
            return entries.get(role);
        }

        /**
         * replies the opposite list of entries with respect to the current {@link ComparePairType}
         *
         * @return the opposite list of entries
         */
        protected List<T> getOppositeEntries() {
            ListRole opposite = getComparePairListModel().getSelectedComparePair().getOppositeRole(role);
            return entries.get(opposite);
        }

        public ListRole getRole() {
            return role;
        }

        @Override
        public OsmPrimitive getReferredPrimitive(int idx) {
            Object value = getValueAt(idx, 1);
            if (value instanceof OsmPrimitive) {
                return (OsmPrimitive) value;
            } else if (value instanceof RelationMember) {
                return ((RelationMember)value).getMember();
            } else {
                Main.error("Unknown object type: "+value);
                return null;
            }
        }
    }

    /**
     * This is the selection model to be used in a {@link JTable} which displays
     * an entry list managed by {@link ListMergeModel}.
     *
     * The model ensures that only rows displaying an entry in the entry list
     * can be selected. "Empty" rows can't be selected.
     *
     * @see ListMergeModel#getMySelectionModel()
     * @see ListMergeModel#getMergedSelectionModel()
     * @see ListMergeModel#getTheirSelectionModel()
     *
     */
    protected class EntriesSelectionModel extends DefaultListSelectionModel {
        private final List<T> entries;

        public EntriesSelectionModel(ArrayList<T> nodes) {
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

    public ComparePairListModel getComparePairListModel() {
        return this.comparePairListModel;
    }

    public class ComparePairListModel extends AbstractListModel implements ComboBoxModel {

        private int selectedIdx;
        private final List<ComparePairType> compareModes;

        /**
         * Constructs a new {@code ComparePairListModel}.
         */
        public ComparePairListModel() {
            this.compareModes = new ArrayList<ComparePairType>();
            compareModes.add(MY_WITH_THEIR);
            compareModes.add(MY_WITH_MERGED);
            compareModes.add(THEIR_WITH_MERGED);
            selectedIdx = 0;
        }

        @Override
        public Object getElementAt(int index) {
            if (index < compareModes.size())
                return compareModes.get(index);
            throw new IllegalArgumentException(tr("Unexpected value of parameter ''index''. Got {0}.", index));
        }

        @Override
        public int getSize() {
            return compareModes.size();
        }

        @Override
        public Object getSelectedItem() {
            return compareModes.get(selectedIdx);
        }

        @Override
        public void setSelectedItem(Object anItem) {
            int i = compareModes.indexOf(anItem);
            if (i < 0)
                throw new IllegalStateException(tr("Item {0} not found in list.", anItem));
            selectedIdx = i;
            fireModelDataChanged();
        }

        public ComparePairType getSelectedComparePair() {
            return compareModes.get(selectedIdx);
        }
    }
}
