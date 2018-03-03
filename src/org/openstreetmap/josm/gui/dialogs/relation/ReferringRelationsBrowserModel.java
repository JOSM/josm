// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation;

import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractListModel;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.DataSet.DownloadPolicy;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;

/**
 * This is the model for the {@link ReferringRelationsBrowser}.
 * <p>
 * It holds all referrers to a given relation
 */
public class ReferringRelationsBrowserModel extends AbstractListModel<Relation> {

    /** the relation */
    private transient Relation relation;
    private final transient List<Relation> referrers = new ArrayList<>();

    /**
     * Constructs a new {@code ReferringRelationsBrowserModel}.
     * @param relation relation
     */
    public ReferringRelationsBrowserModel(Relation relation) {
        this.relation = relation;
    }

    protected void fireModelUpdate() {
        int upper = Math.max(0, referrers.size() -1);
        fireContentsChanged(this, 0, upper);
    }

    /**
     * Sets the relation.
     * @param relation the relation
     */
    public void setRelation(Relation relation) {
        this.relation = relation;
        referrers.clear();
        fireModelUpdate();
    }

    @Override
    public Relation getElementAt(int index) {
        return referrers.get(index);
    }

    @Override
    public int getSize() {
        return referrers.size();
    }

    protected boolean isReferringRelation(Relation parent) {
        if (parent == null) return false;
        for (RelationMember m: parent.getMembers()) {
            if (m.isRelation()) {
                Relation child = m.getRelation();
                if (child.equals(relation)) return true;
            }
        }
        return false;
    }

    /**
     * Populates the model with parent referrers.
     * @param parents parent referrers
     */
    public void populate(List<Relation> parents) {
        referrers.clear();
        if (parents != null) {
            for (Relation relation: parents) {
                if (isReferringRelation(relation)) {
                    referrers.add(relation);
                }
            }
        }
        fireModelUpdate();
    }

    /**
     * Populates the browser with the list of referring relations in the {@link DataSet} ds.
     *
     * @param ds the data set
     */
    public void populate(DataSet ds) {
        referrers.clear();
        if (ds == null) {
            fireModelUpdate();
            return;
        }
        for (Relation parent : ds.getRelations()) {
            if (isReferringRelation(parent)) {
                referrers.add(parent);
            }
        }
        fireModelUpdate();
    }

    /**
     * Determines if reloading the relation is possible/authorized.
     * @return {@code true} if reloading the relation is possible/authorized
     */
    public boolean canReload() {
        return relation != null && !relation.isNew()
                && !relation.getDataSet().isLocked()
                && !DownloadPolicy.BLOCKED.equals(relation.getDataSet().getDownloadPolicy());
    }

    /**
     * Returns the relation.
     * @return the relation
     */
    public Relation getRelation() {
        return relation;
    }
}
