// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.DefaultComboBoxModel;

import org.openstreetmap.josm.data.osm.Changeset;

/**
 * A combobox model for the list of open changesets
 *
 */
public class OpenChangesetComboBoxModel extends DefaultComboBoxModel {
    private List<Changeset> changesets;
    private long uid;
    private Changeset selectedChangeset = null;

    protected Changeset getChangesetById(long id) {
        for (Changeset cs : changesets) {
            if (cs.getId() == id) return cs;
        }
        return null;
    }

    public OpenChangesetComboBoxModel() {
        this.changesets = new ArrayList<Changeset>();
    }

    protected void internalAddOrUpdate(Changeset cs) {
        Changeset other = getChangesetById(cs.getId());
        if (other != null) {
            cs.cloneFrom(other);
        } else {
            changesets.add(cs);
        }
    }

    public void addOrUpdate(Changeset cs) {
        if (cs.getId() <= 0 )
            throw new IllegalArgumentException(tr("Changeset ID > 0 expected. Got {0}.", cs.getId()));
        internalAddOrUpdate(cs);
        fireContentsChanged(this, 0, getSize());
    }

    public void remove(long id) {
        Changeset cs = getChangesetById(id);
        if (cs != null) {
            changesets.remove(cs);
        }
        fireContentsChanged(this, 0, getSize());
    }

    public void setChangesets(Collection<Changeset> changesets) {
        this.changesets.clear();
        if (changesets != null) {
            for (Changeset cs: changesets) {
                internalAddOrUpdate(cs);
            }
        }
        fireContentsChanged(this, 0, getSize());
        if (getSelectedItem() == null && !this.changesets.isEmpty()) {
            setSelectedItem(this.changesets.get(0));
        } else if (getSelectedItem() != null) {
            if (changesets.contains(getSelectedItem())) {
                setSelectedItem(getSelectedItem());
            } else if (!this.changesets.isEmpty()){
                setSelectedItem(this.changesets.get(0));
            } else {
                setSelectedItem(null);
            }
        } else {
            setSelectedItem(null);
        }
    }

    public void setUserId(long uid) {
        this.uid = uid;
    }

    public long getUserId() {
        return uid;
    }

    public void selectFirstChangeset() {
        if (changesets == null || changesets.isEmpty()) {
            setSelectedItem(null);
        } else {
            setSelectedItem(changesets.get(0));
        }
    }

    public void removeChangeset(Changeset cs) {
        if (cs == null) return;
        changesets.remove(cs);
        if (selectedChangeset == cs) {
            selectFirstChangeset();
        }
        fireContentsChanged(this, 0, getSize());
    }
    /* ------------------------------------------------------------------------------------ */
    /* ComboBoxModel                                                                        */
    /* ------------------------------------------------------------------------------------ */
    @Override
    public Object getElementAt(int index) {
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
        if (! (anObject instanceof Changeset)) return;
        Changeset cs = (Changeset)anObject;
        if (cs.getId() == 0 || ! cs.isOpen()) return;
        Changeset candidate = getChangesetById(cs.getId());
        if (candidate == null) return;
        this.selectedChangeset = candidate;
        super.setSelectedItem(selectedChangeset);
    }
}