// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultComboBoxModel;

import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.data.osm.ChangesetCache;
import org.openstreetmap.josm.data.osm.ChangesetCacheEvent;
import org.openstreetmap.josm.data.osm.ChangesetCacheListener;
import org.openstreetmap.josm.gui.util.GuiHelper;

/**
 * A combobox model for the list of open changesets. The model is populated with the list
 * of open changesets kept in the {@link ChangesetCache}.
 *
 */
public class OpenChangesetComboBoxModel extends DefaultComboBoxModel<Changeset> implements ChangesetCacheListener {
    private final transient List<Changeset> changesets;
    private transient Changeset selectedChangeset;

    protected Changeset getChangesetById(long id) {
        return changesets.stream().filter(cs -> cs.getId() == id)
                .findFirst().orElse(null);
    }

    /**
     * Constructs a new {@code OpenChangesetComboBoxModel}.
     */
    public OpenChangesetComboBoxModel() {
        this.changesets = new ArrayList<>();
    }

    /**
     * Refreshes the content of the combobox model with the current list of open
     * changesets from the {@link ChangesetCache}.
     */
    public void refresh() {
        changesets.clear();
        changesets.addAll(ChangesetCache.getInstance().getOpenChangesetsForCurrentUser());
        fireContentsChanged(this, 0, getSize());
        int idx = changesets.indexOf(selectedChangeset);
        if (idx < 0) {
            selectFirstChangeset();
        } else {
            setSelectedItem(changesets.get(idx));
        }
    }

    /**
     * Selects the first changeset in the current list of open changesets
     */
    public void selectFirstChangeset() {
        if (changesets == null || changesets.isEmpty()) {
            setSelectedItem(null);
        } else {
            setSelectedItem(changesets.get(0));
        }
    }

    /* ------------------------------------------------------------------------------------ */
    /* ChangesetCacheListener                                                               */
    /* ------------------------------------------------------------------------------------ */
    @Override
    public void changesetCacheUpdated(ChangesetCacheEvent event) {
        GuiHelper.runInEDT(this::refresh);
    }

    /* ------------------------------------------------------------------------------------ */
    /* ComboBoxModel                                                                        */
    /* ------------------------------------------------------------------------------------ */
    @Override
    public Changeset getElementAt(int index) {
        return changesets.get(index);
    }

    @Override
    public int getIndexOf(Object anObject) {
        return changesets.indexOf(anObject);
    }

    @Override
    public int getSize() {
        return changesets.size();
    }

    @Override
    public Object getSelectedItem() {
        return selectedChangeset;
    }

    @Override
    public void setSelectedItem(Object anObject) {
        if (anObject == null) {
            this.selectedChangeset = null;
            super.setSelectedItem(null);
            return;
        }
        if (!(anObject instanceof Changeset)) return;
        Changeset cs = (Changeset) anObject;
        if (cs.getId() == 0 || !cs.isOpen()) return;
        Changeset candidate = getChangesetById(cs.getId());
        if (candidate == null) return;
        this.selectedChangeset = candidate;
        super.setSelectedItem(selectedChangeset);
    }
}
