// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.changeset;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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

public class ChangesetListModel extends DefaultListModel  implements ChangesetCacheListener{
    private final List<Changeset> data = new ArrayList<Changeset>();
    private final Storage<Changeset> shownChangesets = new Storage<Changeset>(true);
    private DefaultListSelectionModel selectionModel;

    public ChangesetListModel(DefaultListSelectionModel selectionModel) {
        this.selectionModel = selectionModel;
    }

    public Set<Changeset> getSelectedChangesets() {
        Set<Changeset> ret = new HashSet<Changeset>();
        for (int i=0; i < getSize(); i++) {
            if (selectionModel.isSelectedIndex(i)) {
                ret.add(data.get(i));
            }
        }
        return ret;
    }

    public Set<Integer> getSelectedChangesetIds() {
        Set<Integer> ret = new HashSet<Integer>();
        for (int i=0; i < getSize(); i++) {
            if (selectionModel.isSelectedIndex(i)) {
                ret.add(data.get(i).getId());
            }
        }
        return ret;
    }

    public void setSelectedChangesets(Collection<Changeset> changesets) {
        selectionModel.clearSelection();
        if (changesets == null) return;
        for (Changeset cs: changesets) {
            int idx = data.indexOf(cs);
            if (idx < 0) {
                continue;
            }
            selectionModel.addSelectionInterval(idx,idx);
        }
    }

    protected void setChangesets(Collection<Changeset> changesets) {
        shownChangesets.clear();
        if (changesets != null) {
            shownChangesets.addAll(changesets);
        }
        updateModel();
    }

    private void updateModel() {
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

    public void initFromChangesetIds(Collection<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            setChangesets(null);
            return;
        }
        Set<Changeset> changesets = new HashSet<Changeset>(ids.size());
        for (int id: ids) {
            if (id <= 0) {
                continue;
            }
            changesets.add(new Changeset(id));
        }
        setChangesets(changesets);
    }

    public void initFromPrimitives(Collection<? extends OsmPrimitive> primitives) {
        if (primitives == null) {
            setChangesets(null);
            return;
        }
        Set<Changeset> changesets = new HashSet<Changeset>();
        for (OsmPrimitive p: primitives) {
            if (p.getChangesetId() <= 0) {
                continue;
            }
            changesets.add(new Changeset(p.getChangesetId()));
        }
        setChangesets(changesets);
    }

    public void initFromDataSet(DataSet ds) {
        if (ds == null) {
            setChangesets(null);
            return;
        }
        Set<Changeset> changesets = new HashSet<Changeset>();
        for (OsmPrimitive p: ds.allPrimitives()) {
            if (p.getChangesetId() <=0 ) {
                continue;
            }
            changesets.add(new Changeset(p.getChangesetId()));
        }
        setChangesets(changesets);
    }

    @Override
    public Object getElementAt(int idx) {
        return data.get(idx);
    }

    @Override
    public int getSize() {
        return data.size();
    }

    protected void sort() {
        Collections.sort(
                data,
                new Comparator<Changeset>() {
                    @Override
                    public int compare(Changeset cs1, Changeset cs2) {
                        if (cs1.getId() > cs2.getId()) return -1;
                        if (cs1.getId() == cs2.getId()) return 0;
                        return 1;
                    }
                }
        );
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
    public List<Changeset> getSelectedOpenChangesets() {
        List<Changeset> ret = new ArrayList<Changeset>();
        for (int i=0; i< getSize(); i++) {
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
    public void changesetCacheUpdated(ChangesetCacheEvent event) {
        Set<Changeset> sel = getSelectedChangesets();
        for(Changeset cs: event.getAddedChangesets()) {
            int idx = data.indexOf(cs);
            if (idx >= 0 && data.get(idx) != cs) {
                data.get(idx).mergeFrom(cs);
            }
        }
        for(Changeset cs: event.getUpdatedChangesets()) {
            int idx = data.indexOf(cs);
            if (idx >= 0 && data.get(idx) != cs) {
                data.get(idx).mergeFrom(cs);
            }
        }
        for(Changeset cs: event.getRemovedChangesets()) {
            int idx = data.indexOf(cs);
            if (idx >= 0) {
                // replace with an incomplete changeset
                data.set(idx, new Changeset(cs.getId()));
            }
        }
        fireContentsChanged(this, 0, getSize());
        setSelectedChangesets(sel);
    }
}
