// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.util.Arrays;

/**
 * IRelationMember captures the common functions of {@link RelationMember} and {@link RelationMemberData}.
 * @param <P> the base type of OSM primitives
 * @since 13677
 */
public interface IRelationMember<P extends IPrimitive> extends PrimitiveId {

    /**
     * Returns the role of this relation member.
     * @return Role name or "". Never returns null
     */
    String getRole();

    /**
     * Determines if this relation member has a role.
     * @return True if role is set
     */
    default boolean hasRole() {
        return !"".equals(getRole());
    }

    /**
     * Determines if this relation member's role is in the given list.
     * @param roles The roles to look after
     * @return True if role is in the given list
     */
    default boolean hasRole(String... roles) {
        return Arrays.asList(roles).contains(getRole());
    }

    /**
     * Determines if this relation member is a node.
     * @return True if member is node
     */
    boolean isNode();

    /**
     * Determines if this relation member is a way.
     * @return True if member is way
     */
    boolean isWay();

    /**
     * Determines if this relation member is a relation.
     * @return True if member is relation
     */
    boolean isRelation();

    /**
     * Returns type of member for icon display.
     * @return type of member for icon display
     * @since 13766 (IRelationMember)
     */
    default OsmPrimitiveType getDisplayType() {
        return getMember().getDisplayType();
    }

    /**
     * Returns the relation member.
     * @return Member. Returned value is never null.
     * @since 13766 (IRelationMember)
     */
    P getMember();
}
