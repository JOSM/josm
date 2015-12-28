// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.util.Comparator;

/**
 * Formats a name for a {@link OsmPrimitive}.
 * @since 1990
 */
public interface NameFormatter {

    /**
     * Formats a name for a {@link Node}.
     *
     * @param node the node
     * @return the name
     */
    String format(Node node);

    /**
     * Formats a name for a {@link Way}.
     *
     * @param way the way
     * @return the name
     */
    String format(Way way);

    /**
     * Formats a name for a {@link Relation}.
     *
     * @param relation the relation
     * @return the name
     */
    String format(Relation relation);

    /**
     * Formats a name for a {@link Changeset}.
     *
     * @param changeset the changeset
     * @return the name
     */
    String format(Changeset changeset);

    Comparator<Node> getNodeComparator();

    Comparator<Way> getWayComparator();

    Comparator<Relation> getRelationComparator();
}
