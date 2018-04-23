// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

/**
 * IRelation captures the common functions of {@link Relation} and {@link RelationData}.
 * @since 4098
 */
public interface IRelation extends IPrimitive {

    /**
     * Returns the number of members.
     * @return number of members
     */
    int getMembersCount();

    /**
     * Returns id of the member at given index.
     * @param idx member index
     * @return id of the member at given index
     */
    long getMemberId(int idx);

    /**
     * Returns role of the member at given index.
     * @param idx member index
     * @return role of the member at given index
     */
    String getRole(int idx);

    /**
     * Returns type of the member at given index.
     * @param idx member index
     * @return type of the member at given index
     */
    OsmPrimitiveType getMemberType(int idx);

    /**
     * Determines if at least one child primitive is incomplete.
     *
     * @return true if at least one child primitive is incomplete
     * @since 13564
     */
    default boolean hasIncompleteMembers() {
        return false;
    }

    @Override
    default int compareTo(IPrimitive o) {
        return o instanceof IRelation ? Long.compare(getUniqueId(), o.getUniqueId()) : -1;
    }

    @Override
    default String getDisplayName(NameFormatter formatter) {
        return formatter.format(this);
    }

    /**
     * Determines if this relation is a boundary.
     * @return {@code true} if a boundary relation
     */
    default boolean isBoundary() {
        return "boundary".equals(get("type"));
    }

    @Override
    default boolean isMultipolygon() {
        return "multipolygon".equals(get("type")) || isBoundary();
    }
}
