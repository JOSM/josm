// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.changeset;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.DefaultListSelectionModel;
import javax.swing.table.AbstractTableModel;

import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.data.osm.ChangesetCache;
import org.openstreetmap.josm.data.osm.ChangesetCacheEvent;
import org.openstreetmap.josm.data.osm.ChangesetCacheListener;
import org.openstreetmap.josm.gui.util.GuiHelper;

/**
 * This is the model for the changeset cache manager dialog.
 *
 */
public class ChangesetCacheManagerModel extends AbstractTableModel implements ChangesetCacheListener{

    /** the name of the property for the currently selected changeset in the detail view */
    public final static String CHANGESET_IN_DETAIL_VIEW_PROP = ChangesetCacheManagerModel.class.getName() + ".changesetInDetailView";

    private final List<Changeset> data = new ArrayList<Changeset>();
    private DefaultListSelectionModel selectionModel;
    private Changeset changesetInDetailView;
    private final PropertyChangeSupport support = new PropertyChangeSupport(this);

    public ChangesetCacheManagerModel(DefaultListSelectionModel selectionModel) {
        this.selectionModel = selectionModel;
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        support.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        support.removePropertyChangeListener(listener);
    }

    /**
     * Sets the changeset currently displayed in the detail view. Fires a property change event
     * for the property {@link #CHANGESET_IN_DETAIL_VIEW_PROP} if necessary.
     *
     * @param cs the changeset currently displayed in the detail view.
     */
    public void setChangesetInDetailView(Changeset cs) {
        Changeset oldValue = changesetInDetailView;
        changesetInDetailView = cs;
        if (oldValue != cs) {
            support.firePropertyChange(CHANGESET_IN_DETAIL_VIEW_PROP, oldValue, changesetInDetailView);
        }
    }

    /**
     * Replies true if there is at least one selected changeset
     *
     * @return true if there is at least one selected changeset
     */
    public boolean hasSelectedChangesets() {
        return selectionModel.getMinSelectionIndex() >= 0;
    }

    /**
     * Replies the list of selected changesets
     *
     * @return the list of selected changesets
     */
    public List<Changeset> getSelectedChangesets() {
        List<Changeset> ret = new ArrayList<Changeset>();
        for (int i =0; i< data.size();i++) {
            Changeset cs = data.get(i);
            if (selectionModel.isSelectedIndex(i)) {
                ret.add(cs);
            }
        }
        return ret;
    }

    /**
     * Replies a set of ids of the selected changesets
     *
     * @return a set of ids of the selected changesets
     */
    public Set<Integer> getSelectedChangesetIds() {
        Set<Integer> ret = new HashSet<Integer>();
        for (Changeset cs: getSelectedChangesets()) {
            ret.add(cs.getId());
        }
        return ret;
    }

    /**
     * Selects the changesets in <code>selected</code>.
     *
     * @param selected the collection of changesets to select. Ignored if empty.
     */
    public void setSelectedChangesets(Collection<Changeset> selected) {
        GuiHelper.runInEDTAndWait(new Runnable() {
            @Override public void run() {
                selectionModel.clearSelection();
            }
        });
        if (selected == null || selected.isEmpty())
            return;
        for (Changeset cs: selected) {
            final int idx = data.indexOf(cs);
            if (idx >= 0) {
                GuiHelper.runInEDTAndWait(new Runnable() {
                    @Override public void run() {
                        selectionModel.addSelectionInterval(idx,idx);
                    }
                });
            }
        }
    }

    @Override
    public int getColumnCount() {
        return 5;
    }

    @Override
    public int getRowCount() {
        return data.size();
    }

    @Override
    public Object getValueAt(int row, int column) {
        return data.get(row);
    }

    public void init() {
        ChangesetCache cc = ChangesetCache.getInstance();
        List<Changeset> selected = getSelectedChangesets();
        data.clear();
        data.addAll(cc.getChangesets());
        sort();
        fireTableDataChanged();
        setSelectedChangesets(selected);

        cc.addChangesetCacheListener(this);
    }

    public void tearDown() {
        ChangesetCache.getInstance().removeChangesetCacheListener(this);
    }

    public DefaultListSelectionModel getSelectionModel() {
        return selectionModel;
    }

    protected void sort() {
        Collections.sort(
                this.data,
                new Comparator<Changeset>() {
                    @Override public int compare(Changeset o1, Changeset o2) {
                        if (o1.getId() < o2.getId()) return 1;
                        if (o1.getId() == o2.getId()) return 0;
                        return -1;
                    }
                }
        );
    }

    /* ------------------------------------------------------------------------------ */
    /* interface ChangesetCacheListener                                               */
    /* ------------------------------------------------------------------------------ */
    @Override
    public void changesetCacheUpdated(ChangesetCacheEvent event) {
        List<Changeset> selected = getSelectedChangesets();
        for (Changeset cs: event.getAddedChangesets()) {
            data.add(cs);
        }
        for (Changeset cs: event.getRemovedChangesets()) {
            data.remove(cs);
        }
        for (Changeset cs: event.getUpdatedChangesets()) {
            int idx = data.indexOf(cs);
            if (idx >= 0) {
                Changeset mine = data.get(idx);
                if (mine != cs) {
                    mine.mergeFrom(cs);
                }
            }
        }
        sort();
        fireTableDataChanged();
        setSelectedChangesets(selected);
    }
}
