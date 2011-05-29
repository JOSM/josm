// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.util.Comparator;

public interface NameFormatter {
    String format(INode node);
    String format(IWay way);
    String format(IRelation relation);
    String format(Changeset changeset);

    Comparator<Node> getNodeComparator();
    Comparator<Way> getWayComparator();
    Comparator<Relation> getRelationComparator();
}
