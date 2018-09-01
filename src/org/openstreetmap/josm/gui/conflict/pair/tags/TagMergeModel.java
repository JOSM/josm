// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.pair.tags;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.table.DefaultTableModel;

import org.openstreetmap.josm.command.conflict.TagConflictResolveCommand;
import org.openstreetmap.josm.data.conflict.Conflict;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.conflict.pair.MergeDecisionType;

/**
 * This is the {@link javax.swing.table.TableModel} used in the tables of the {@link TagMerger}.
 *
 * The model can {@link #populate(OsmPrimitive, OsmPrimitive)} itself from the conflicts
 * in the tag sets of two {@link OsmPrimitive}s. Internally, it keeps a list of {@link TagMergeItem}s.
 *
 *  {@link #decide(int, MergeDecisionType)} and {@link #decide(int[], MergeDecisionType)} can be used
 *  to remember a merge decision for a specific row in the model.
 *
 *  The model notifies {@link PropertyChangeListener}s about updates of the number of
 *  undecided tags (see {@link #PROP_NUM_UNDECIDED_TAGS}).
 *
 */
public class TagMergeModel extends DefaultTableModel {
    public static final String PROP_NUM_UNDECIDED_TAGS = TagMergeModel.class.getName() + ".numUndecidedTags";

    /** the list of tag merge items */
    private final transient List<TagMergeItem> tagMergeItems;

    /** the property change listeners */
    private final transient Set<PropertyChangeListener> listeners;

    private int numUndecidedTags;

    /**
     * Constructs a new {@code TagMergeModel}.
     */
    public TagMergeModel() {
        tagMergeItems = new ArrayList<>();
        listeners = new HashSet<>();
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        synchronized (listeners) {
            if (listener == null) return;
            if (listeners.contains(listener)) return;
            listeners.add(listener);
        }
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        synchronized (listeners) {
            if (listener == null) return;
            if (!listeners.contains(listener)) return;
            listeners.remove(listener);
        }
    }

    /**
     * notifies {@link PropertyChangeListener}s about an update of {@link TagMergeModel#PROP_NUM_UNDECIDED_TAGS}

     * @param oldValue the old value
     * @param newValue the new value
     */
    protected void fireNumUndecidedTagsChanged(int oldValue, int newValue) {
        PropertyChangeEvent evt = new PropertyChangeEvent(this, PROP_NUM_UNDECIDED_TAGS, oldValue, newValue);
        synchronized (listeners) {
            for (PropertyChangeListener l : listeners) {
                l.propertyChange(evt);
            }
        }
    }

    /**
     * refreshes the number of undecided tag conflicts after an update in the list of
     * {@link TagMergeItem}s. Notifies {@link PropertyChangeListener} if necessary.
     *
     */
    protected void refreshNumUndecidedTags() {
        int newValue = 0;
        for (TagMergeItem item: tagMergeItems) {
            if (MergeDecisionType.UNDECIDED == item.getMergeDecision()) {
                newValue++;
            }
        }
        int oldValue = numUndecidedTags;
        numUndecidedTags = newValue;
        fireNumUndecidedTagsChanged(oldValue, numUndecidedTags);
    }

    /**
     * Populate the model with conflicts between the tag sets of the two
     * {@link OsmPrimitive} <code>my</code> and <code>their</code>.
     *
     * @param my  my primitive (i.e. the primitive from the local dataset)
     * @param their their primitive (i.e. the primitive from the server dataset)
     *
     */
    public void populate(OsmPrimitive my, OsmPrimitive their) {
        tagMergeItems.clear();
        Set<String> keys = new HashSet<>();
        keys.addAll(my.keySet());
        keys.addAll(their.keySet());
        for (String key : keys) {
            String myValue = my.get(key);
            String theirValue = their.get(key);
            if (myValue == null || theirValue == null || !myValue.equals(theirValue)) {
                tagMergeItems.add(
                        new TagMergeItem(key, my, their)
                );
            }
        }
        fireTableDataChanged();
        refreshNumUndecidedTags();
    }

    /**
     * add a {@link TagMergeItem} to the model
     *
     * @param item the item
     */
    public void addItem(TagMergeItem item) {
        if (item != null) {
            tagMergeItems.add(item);
            fireTableDataChanged();
            refreshNumUndecidedTags();
        }
    }

    protected void rememberDecision(int row, MergeDecisionType decision) {
        TagMergeItem item = tagMergeItems.get(row);
        item.decide(decision);
    }

    /**
     * set the merge decision of the {@link TagMergeItem} in row <code>row</code>
     * to <code>decision</code>.
     *
     * @param row the row
     * @param decision the decision
     */
    public void decide(int row, MergeDecisionType decision) {
        rememberDecision(row, decision);
        fireTableRowsUpdated(row, row);
        refreshNumUndecidedTags();
    }

    /**
     * set the merge decision of all {@link TagMergeItem} given by indices in <code>rows</code>
     * to <code>decision</code>.
     *
     * @param rows the array of row indices
     * @param decision the decision
     */
    public void decide(int[] rows, MergeDecisionType decision) {
        if (rows == null || rows.length == 0)
            return;
        for (int row : rows) {
            rememberDecision(row, decision);
        }
        fireTableDataChanged();
        refreshNumUndecidedTags();
    }

    @Override
    public int getRowCount() {
        return tagMergeItems == null ? 0 : tagMergeItems.size();
    }

    @Override
    public Object getValueAt(int row, int column) {
        // return the tagMergeItem for both columns. The cell
        // renderer will dispatch on the column index and get
        // the key or the value from the TagMergeItem
        //
        return tagMergeItems.get(row);
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return false;
    }

    public TagConflictResolveCommand buildResolveCommand(Conflict<? extends OsmPrimitive> conflict) {
        return new TagConflictResolveCommand(conflict, tagMergeItems);
    }

    public boolean isResolvedCompletely() {
        for (TagMergeItem item: tagMergeItems) {
            if (item.getMergeDecision() == MergeDecisionType.UNDECIDED)
                return false;
        }
        return true;
    }

    public void decideRemaining(MergeDecisionType decision) {
        for (TagMergeItem item: tagMergeItems) {
            if (item.getMergeDecision() == MergeDecisionType.UNDECIDED)
                item.decide(decision);
        }
    }

    public int getNumResolvedConflicts() {
        int n = 0;
        for (TagMergeItem item: tagMergeItems) {
            if (item.getMergeDecision() != MergeDecisionType.UNDECIDED) {
                n++;
            }
        }
        return n;

    }

    public int getFirstUndecided(int startIndex) {
        for (int i = startIndex; i < tagMergeItems.size(); i++) {
            if (tagMergeItems.get(i).getMergeDecision() == MergeDecisionType.UNDECIDED)
                return i;
        }
        return -1;
    }
}
