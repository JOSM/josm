// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.util.ArrayList;
import java.util.List;

public class RelationData extends PrimitiveData {

    private final List<RelationMemberData> members = new ArrayList<RelationMemberData>();

    public List<RelationMemberData> getMembers() {
        return members;
    }

    @Override
    public String toString() {
        return super.toString() + " REL " + members;
    }

}
