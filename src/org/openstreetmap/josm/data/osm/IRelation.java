// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.openstreetmap.josm.tools.Utils;

/**
 * IRelation captures the common functions of {@link Relation} and {@link RelationData}.
 * @param <M> Type of OSM relation member
 * @since 4098
 */
public interface IRelation<M extends IRelationMember<?>> extends IPrimitive {

    /**
     * Returns the number of members.
     * @return number of members
     */
    int getMembersCount();

    /**
     * Determines if this relation is empty, i.e. it has no members.
     * @return {@code true} if this relation is empty, i.e. it has no members
     * @since 16119
     */
    default boolean isEmpty() {
        return getMembersCount() == 0;
    }

    /**
     * Returns the relation member at the specified index.
     * @param index the index of the relation member
     * @return relation member at the specified index
     * @since 13766 (IRelation)
     */
    M getMember(int index);

    /**
     * Returns members of the relation.
     * @return Members of the relation. Changes made in returned list are not mapped
     * back to the primitive, use {@link #setMembers} to modify the members
     * @since 1925
     * @since 13766 (IRelation)
     */
    List<M> getMembers();

    /**
     * Sets members of the relation.
     * @param members Can be null, in that case all members are removed
     */
    void setMembers(List<M> members);

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

    /**
     * Returns an unmodifiable list of the {@link OsmPrimitive}s referred to by at least one member of this relation.
     * @return an unmodifiable list of the primitives
     * @since 13957
     */
    default List<? extends IPrimitive> getMemberPrimitivesList() {
        return Utils.transform(getMembers(), IRelationMember::getMember);
    }

    /**
     * Replies a collection with the incomplete children this relation refers to.
     *
     * @return the incomplete children. Empty collection if no children are incomplete.
     * @since 13957
     */
    default Collection<? extends IPrimitive> getIncompleteMembers() {
        return getMembers().stream()
                .filter(rm -> rm.getMember().isIncomplete())
                .map(rm -> rm.getMember())
                .collect(Collectors.toSet());
    }

    /**
     * Returns a list of relation members having the specified role.
     * @param role role
     * @return a list of relation members having the specified role
     * @since 15418
     */
    default List<? extends IPrimitive> findRelationMembers(String role) {
        return getMembers().stream().filter(rmv -> role.equals(rmv.getRole()))
                .map(IRelationMember::getMember).collect(Collectors.toList());
    }
}
