// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.util.Comparator;

/**
 * Formats a name for an {@link IPrimitive}.
 * @since 1990
 */
public interface NameFormatter {

    /**
     * Formats a name for a {@link INode}.
     *
     * @param node the node
     * @return the name
     * @since 13564 (signature)
     */
    String format(INode node);

    /**
     * Formats a name for a {@link IWay}.
     *
     * @param way the way
     * @return the name
     * @since 13564 (signature)
     */
    String format(IWay<?> way);

    /**
     * Formats a name for a {@link IRelation}.
     *
     * @param relation the relation
     * @return the name
     * @since 13564 (signature)
     */
    String format(IRelation<?> relation);

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
     * @since 13564 (signature)
     */
    Comparator<INode> getNodeComparator();

    /**
     * Gets a comparator that sorts the ways by the string that this formatter would create for them
     * @return That comparator
     * @since 13564 (signature)
     */
    Comparator<IWay<?>> getWayComparator();

    /**
     * Gets a comparator that sorts the relations by the string that this formatter would create for them
     * @return That comparator
     * @since 13564 (signature)
     */
    Comparator<IRelation<?>> getRelationComparator();
}
