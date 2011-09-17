// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.util.Comparator;

public interface NameFormatter {
    String format(Node node);
    String format(Way way);
    String format(Relation relation);
    String format(Changeset changeset);

    Comparator<Node> getNodeComparator();
    Comparator<Way> getWayComparator();
    Comparator<Relation> getRelationComparator();
}
