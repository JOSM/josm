// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.changeset;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.DefaultListModel;
import javax.swing.DefaultListSelectionModel;

import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.data.osm.ChangesetCache;
import org.openstreetmap.josm.data.osm.ChangesetCacheEvent;
import org.openstreetmap.josm.data.osm.ChangesetCacheListener;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Storage;
import org.openstreetmap.josm.gui.util.GuiHelper;

/**
 * This is the model that backs a list of changesets
 */
public class ChangesetListModel extends DefaultListModel<Changeset> implements ChangesetCacheListener {
    private final transient List<Changeset> data = new ArrayList<>();
    private final transient Storage<Changeset> shownChangesets = new Storage<>(true);
    private final DefaultListSelectionModel selectionModel;

    /**
     * Creates a new {@link ChangesetListModel}
     * @param selectionModel The selection model to use for this list
     */
    public ChangesetListModel(DefaultListSelectionModel selectionModel) {
        this.selectionModel = selectionModel;
    }

    /**
     * Gets the list of changesets that are currently selected
     * @return The selected changesets
     */
    public synchronized Set<Changeset> getSelectedChangesets() {
        Set<Changeset> ret = new HashSet<>();
        for (int i = 0; i < getSize(); i++) {
            if (selectionModel.isSelectedIndex(i)) {
                ret.add(data.get(i));
            }
        }
        return ret;
    }

    /**
     * Gets the IDs of the changesets that are selected
     * @return The selected ids
     */
    public synchronized Set<Integer> getSelectedChangesetIds() {
        Set<Integer> ret = new HashSet<>();
        for (int i = 0; i < getSize(); i++) {
            if (selectionModel.isSelectedIndex(i)) {
                ret.add(data.get(i).getId());
            }
        }
        return ret;
    }

    /**
     * Sets the changesets to select
     * @param changesets The changesets
     */
    public synchronized void setSelectedChangesets(Collection<Changeset> changesets) {
        selectionModel.setValueIsAdjusting(true);
        selectionModel.clearSelection();
        if (changesets != null) {
            for (Changeset cs: changesets) {
                int idx = data.indexOf(cs);
                if (idx >= 0) {
                    selectionModel.addSelectionInterval(idx, idx);
                }
            }
        }
        selectionModel.setValueIsAdjusting(false);
    }

    protected void setChangesets(Collection<Changeset> changesets) {
        shownChangesets.clear();
        if (changesets != null) {
            shownChangesets.addAll(changesets);
        }
        updateModel();
    }

    private synchronized void updateModel() {
        Set<Changeset> sel = getSelectedChangesets();
        data.clear();
        data.addAll(shownChangesets);
        ChangesetCache cache = ChangesetCache.getInstance();
        for (Changeset cs: data) {
            if (cache.contains(cs) && cache.get(cs.getId()) != cs) {
                cs.mergeFrom(cache.get(cs.getId()));
            }
        }
        sort();
        fireIntervalAdded(this, 0, getSize());
        setSelectedChangesets(sel);
    }

    /**
     * Loads this list with the given changesets
     * @param ids The ids of the changesets to display
     */
    public void initFromChangesetIds(Collection<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            setChangesets(null);
            return;
        }
        Set<Changeset> changesets = new HashSet<>(ids.size());
        for (int id: ids) {
            if (id <= 0) {
                continue;
            }
            changesets.add(new Changeset(id));
        }
        setChangesets(changesets);
    }

    /**
     * Loads this list with the given changesets
     * @param primitives The primitives of which the changesets should be displayed
     */
    public void initFromPrimitives(Collection<? extends OsmPrimitive> primitives) {
        if (primitives == null) {
            setChangesets(null);
            return;
        }
        Set<Changeset> changesets = new HashSet<>();
        for (OsmPrimitive p: primitives) {
            if (p.getChangesetId() <= 0) {
                continue;
            }
            changesets.add(new Changeset(p.getChangesetId()));
        }
        setChangesets(changesets);
    }

    /**
     * Loads this list with the given changesets
     * @param ds The data set to get all changesets from
     */
    public void initFromDataSet(DataSet ds) {
        if (ds == null) {
            setChangesets(null);
            return;
        }
        Set<Changeset> changesets = new HashSet<>();
        for (OsmPrimitive p: ds.allPrimitives()) {
            if (p.getChangesetId() <= 0) {
                continue;
            }
            changesets.add(new Changeset(p.getChangesetId()));
        }
        setChangesets(changesets);
    }

    @Override
    public synchronized Changeset getElementAt(int idx) {
        return data.get(idx);
    }

    @Override
    public synchronized int getSize() {
        return data.size();
    }

    protected synchronized void sort() {
        data.sort(Comparator.comparingInt(Changeset::getId).reversed());
    }

    /**
     * Replies true if  there is at least one selected open changeset
     *
     * @return true if  there is at least one selected open changeset
     */
    public boolean hasSelectedOpenChangesets() {
        return !getSelectedOpenChangesets().isEmpty();
    }

    /**
     * Replies the selected open changesets
     *
     * @return the selected open changesets
     */
    public synchronized List<Changeset> getSelectedOpenChangesets() {
        List<Changeset> ret = new ArrayList<>();
        for (int i = 0; i < getSize(); i++) {
            if (selectionModel.isSelectedIndex(i)) {
                Changeset cs = data.get(i);
                if (cs.isOpen()) {
                    ret.add(cs);
                }
            }
        }
        return ret;
    }

    /* ---------------------------------------------------------------------------- */
    /* Interface ChangesetCacheListener                                             */
    /* ---------------------------------------------------------------------------- */
    @Override
    public synchronized void changesetCacheUpdated(ChangesetCacheEvent event) {
        Set<Changeset> sel = getSelectedChangesets();
        for (Changeset cs: event.getAddedChangesets()) {
            int idx = data.indexOf(cs);
            if (idx >= 0 && data.get(idx) != cs) {
                data.get(idx).mergeFrom(cs);
            }
        }
        for (Changeset cs: event.getUpdatedChangesets()) {
            int idx = data.indexOf(cs);
            if (idx >= 0 && data.get(idx) != cs) {
                data.get(idx).mergeFrom(cs);
            }
        }
        for (Changeset cs: event.getRemovedChangesets()) {
            int idx = data.indexOf(cs);
            if (idx >= 0) {
                // replace with an incomplete changeset
                data.set(idx, new Changeset(cs.getId()));
            }
        }
        GuiHelper.runInEDT(() -> {
            fireContentsChanged(this, 0, getSize());
            setSelectedChangesets(sel);
        });
    }
}
