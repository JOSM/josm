// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.pair.tags;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import javax.swing.table.DefaultTableModel;

import org.openstreetmap.josm.command.TagConflictResolveCommand;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.conflict.pair.MergeDecisionType;

/**
 * This is the {@see TableModel} used in the tables of the {@see TagMerger}.
 *
 * The model can {@see #populate(OsmPrimitive, OsmPrimitive)} itself from the conflicts
 * in the tag sets of two {@see OsmPrimitive}s. Internally, it keeps a list of {@see TagMergeItem}s.
 *
 *  {@see #decide(int, MergeDecisionType)} and {@see #decide(int[], MergeDecisionType)} can be used
 *  to remember a merge decision for a specific row in the model.
 *
 *  The model notifies {@see PropertyChangeListener}s about updates of the number of
 *  undecided tags (see {@see #PROP_NUM_UNDECIDED_TAGS}).
 *
 */
public class TagMergeModel extends DefaultTableModel {
    //private static final Logger logger = Logger.getLogger(TagMergeModel.class.getName());

    static public final String PROP_NUM_UNDECIDED_TAGS = TagMergeModel.class.getName() + ".numUndecidedTags";

    /** the list of tag merge items */
    private final ArrayList<TagMergeItem> tagMergeItems;

    /** the property change listeners */
    private final ArrayList<PropertyChangeListener> listeners;

    private int numUndecidedTags = 0;

    public TagMergeModel() {
        tagMergeItems = new ArrayList<TagMergeItem>();
        listeners = new ArrayList<PropertyChangeListener>();
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        synchronized(listeners) {
            if (listener == null) return;
            if (listeners.contains(listener)) return;
            listeners.add(listener);
        }
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        synchronized(listeners) {
            if (listener == null) return;
            if (!listeners.contains(listener)) return;
            listeners.remove(listener);
        }
    }

    /**
     * notifies {@see PropertyChangeListener}s about an update of {@see TagMergeModel#PROP_NUM_UNDECIDED_TAGS}

     * @param oldValue the old value
     * @param newValue the new value
     */
    protected void fireNumUndecidedTagsChanged(int oldValue, int newValue) {
        PropertyChangeEvent evt = new PropertyChangeEvent(this,PROP_NUM_UNDECIDED_TAGS,oldValue, newValue);
        synchronized(listeners) {
            for(PropertyChangeListener l : listeners) {
                l.propertyChange(evt);
            }
        }
    }

    /**
     * refreshes the number of undecided tag conflicts after an update in the list of
     * {@see TagMergeItem}s. Notifies {@see PropertyChangeListener} if necessary.
     *
     */
    protected void refreshNumUndecidedTags() {
        int newValue=0;
        for(TagMergeItem item: tagMergeItems) {
            if (MergeDecisionType.UNDECIDED.equals(item.getMergeDecision())) {
                newValue++;
            }
        }
        int oldValue = numUndecidedTags;
        numUndecidedTags = newValue;
        fireNumUndecidedTagsChanged(oldValue, numUndecidedTags);

    }

    /**
     * Populate the model with conflicts between the tag sets of the two
     * {@see OsmPrimitive} <code>my</code> and <code>their</code>.
     *
     * @param my  my primitive (i.e. the primitive from the local dataset)
     * @param their their primitive (i.e. the primitive from the server dataset)
     *
     */
    public void populate(OsmPrimitive my, OsmPrimitive their) {
        tagMergeItems.clear();
        Set<String> keys = new HashSet<String>();
        keys.addAll(my.keySet());
        keys.addAll(their.keySet());
        for(String key : keys) {
            String myValue = my.get(key);
            String theirValue = their.get(key);
            if (myValue == null || theirValue == null || ! myValue.equals(theirValue)) {
                tagMergeItems.add(
                        new TagMergeItem(key, my, their)
                );
            }
        }
        fireTableDataChanged();
        refreshNumUndecidedTags();
    }

    /**
     * add a {@see TagMergeItem} to the model
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
     * set the merge decision of the {@see TagMergeItem} in row <code>row</code>
     * to <code>decision</code>.
     *
     * @param row  the row
     * @param decision the decision
     */
    public void decide(int row, MergeDecisionType decision) {
        rememberDecision(row, decision);
        fireTableRowsUpdated(row, row);
        refreshNumUndecidedTags();
    }

    /**
     * set the merge decision of all {@see TagMergeItem} given by indices in <code>rows</code>
     * to <code>decision</code>.
     *
     * @param row  the array of row indices
     * @param decision the decision
     */

    public void decide(int [] rows, MergeDecisionType decision) {
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

    public TagConflictResolveCommand buildResolveCommand(OsmPrimitive my, OsmPrimitive their) {
        return new TagConflictResolveCommand(my,  their, tagMergeItems);
    }

    public boolean isResolvedCompletely() {
        for (TagMergeItem item: tagMergeItems) {
            if (item.getMergeDecision().equals(MergeDecisionType.UNDECIDED))
                return false;
        }
        return true;
    }

    public int getNumResolvedConflicts() {
        int n = 0;
        for (TagMergeItem item: tagMergeItems) {
            if (!item.getMergeDecision().equals(MergeDecisionType.UNDECIDED)) {
                n++;
            }
        }
        return n;

    }
}
