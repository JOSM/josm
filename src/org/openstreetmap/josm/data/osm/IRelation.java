// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

public interface IRelation extends IPrimitive {

    int getMembersCount();
    long getMemberId(int idx);
    String getRole(int idx);
    OsmPrimitiveType getMemberType(int idx);

}
