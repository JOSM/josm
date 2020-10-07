// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.changeset;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.swing.DefaultListSelectionModel;
import javax.swing.table.AbstractTableModel;

import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.data.osm.ChangesetCache;
import org.openstreetmap.josm.data.osm.ChangesetCacheEvent;
import org.openstreetmap.josm.data.osm.ChangesetCacheListener;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.gui.util.TableHelper;

/**
 * This is the model for the changeset cache manager dialog.
 */
public class ChangesetCacheManagerModel extends AbstractTableModel implements ChangesetCacheListener {

    /** the name of the property for the currently selected changeset in the detail view */
    public static final String CHANGESET_IN_DETAIL_VIEW_PROP = ChangesetCacheManagerModel.class.getName() + ".changesetInDetailView";

    private final transient List<Changeset> data = new ArrayList<>();
    private final DefaultListSelectionModel selectionModel;
    private transient Changeset changesetInDetailView;
    private final PropertyChangeSupport support = new PropertyChangeSupport(this);

    /**
     * Creates a new ChangesetCacheManagerModel that is based on the selectionModel
     * @param selectionModel A new selection model that should be used.
     */
    public ChangesetCacheManagerModel(DefaultListSelectionModel selectionModel) {
        this.selectionModel = selectionModel;
    }

    /**
     * Adds a property change listener to this model.
     * @param listener The listener
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        support.addPropertyChangeListener(listener);
    }

    /**
     * Removes a property change listener from this model.
     * @param listener The listener
     */
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
        List<Changeset> ret = new ArrayList<>();
        for (int i = 0; i < data.size(); i++) {
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
        return getSelectedChangesets().stream().map(Changeset::getId).collect(Collectors.toSet());
    }

    /**
     * Selects the changesets in <code>selected</code>.
     *
     * @param selected the collection of changesets to select. Ignored if empty.
     */
    public void setSelectedChangesets(Collection<Changeset> selected) {
        GuiHelper.runInEDTAndWait(() -> TableHelper.setSelectedIndices(selectionModel,
                selected != null ? selected.stream().mapToInt(data::indexOf) : IntStream.empty()));
    }

    @Override
    public int getColumnCount() {
        return 8;
    }

    @Override
    public int getRowCount() {
        return data.size();
    }

    @Override
    public Changeset getValueAt(int row, int column) {
        return data.get(row);
    }

    /**
     * Initializes the data that is displayed using the changeset cache.
     */
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

    /**
     * Destroys and unregisters this model.
     */
    public void tearDown() {
        ChangesetCache.getInstance().removeChangesetCacheListener(this);
    }

    /**
     * Gets the selection model this table is based on.
     * @return The selection model.
     */
    public DefaultListSelectionModel getSelectionModel() {
        return selectionModel;
    }

    protected void sort() {
        data.sort(Comparator.comparingInt(Changeset::getId).reversed());
    }

    /* ------------------------------------------------------------------------------ */
    /* interface ChangesetCacheListener                                               */
    /* ------------------------------------------------------------------------------ */
    @Override
    public void changesetCacheUpdated(ChangesetCacheEvent event) {
        List<Changeset> selected = getSelectedChangesets();
        data.addAll(event.getAddedChangesets());
        data.removeAll(event.getRemovedChangesets());
        for (Changeset cs: event.getUpdatedChangesets()) {
            int idx = data.indexOf(cs);
            if (idx >= 0) {
                Changeset mine = data.get(idx);
                if (mine != cs) {
                    mine.mergeFrom(cs);
                }
            }
        }
        GuiHelper.runInEDT(() -> {
            sort();
            fireTableDataChanged();
            setSelectedChangesets(selected);
        });
    }
}
