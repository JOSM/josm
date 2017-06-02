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

    /**
     * Gets a comparator that sorts the nodes by the string that this formatter would create for them
     * @return That comparator
     */
    Comparator<Node> getNodeComparator();

    /**
     * Gets a comparator that sorts the ways by the string that this formatter would create for them
     * @return That comparator
     */
    Comparator<Way> getWayComparator();

    /**
     * Gets a comparator that sorts the relations by the string that this formatter would create for them
     * @return That comparator
     */
    Comparator<Relation> getRelationComparator();
}
