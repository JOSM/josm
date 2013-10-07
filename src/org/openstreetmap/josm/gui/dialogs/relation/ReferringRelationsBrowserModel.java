// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation;

import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractListModel;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;

public class ReferringRelationsBrowserModel extends AbstractListModel {

    /** the relation */
    private Relation relation;
    private List<Relation> referrers;

    public ReferringRelationsBrowserModel() {
        relation = null;
        referrers = new ArrayList<Relation>();
    }
    public ReferringRelationsBrowserModel(Relation relation) {
        this();
        this.relation = relation;
    }

    protected void fireModelUpdate() {
        int upper = Math.max(0, referrers.size() -1);
        fireContentsChanged(this, 0, upper);
    }

    public void setRelation(Relation relation) {
        this.relation = relation;
        referrers.clear();
        fireModelUpdate();
    }

    @Override
    public Object getElementAt(int index) {
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

    public boolean canReload() {
        return relation != null && !relation.isNew();
    }

    public Relation getRelation() {
        return relation;
    }

    public Relation get(int index) {
        return referrers.get(index);
    }
}
