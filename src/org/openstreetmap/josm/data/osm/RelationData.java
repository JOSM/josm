// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.util.ArrayList;
import java.util.List;

public class RelationData extends PrimitiveData {

    private final List<RelationMemberData> members = new ArrayList<RelationMemberData>();

    public RelationData() {

    }

    public RelationData(RelationData data) {
        super(data);
        members.addAll(data.members);
    }

    public List<RelationMemberData> getMembers() {
        return members;
    }

    @Override
    public RelationData makeCopy() {
        return new RelationData(this);
    }

    @Override
    public Relation makePrimitive(DataSet dataSet) {
        return new Relation(this, dataSet);
    }

    @Override
    public String toString() {
        return super.toString() + " REL " + members;
    }

}
