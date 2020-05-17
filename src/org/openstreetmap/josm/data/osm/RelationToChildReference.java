// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * This is an extension of {@link RelationMember} that stores the parent relation and the index in it in addition to the role/child.
 */
public class RelationToChildReference {

    /**
     * Replies a set of all {@link RelationToChildReference}s for a given child primitive.
     *
     * @param child the child primitive
     * @return  a set of all {@link RelationToChildReference}s for a given child primitive
     */
    public static Set<RelationToChildReference> getRelationToChildReferences(OsmPrimitive child) {
        Set<Relation> parents = child.referrers(Relation.class).collect(Collectors.toSet());
        return parents.stream().flatMap(parent1 -> IntStream.range(0, parent1.getMembersCount())
                .filter(i -> parent1.getMember(i).refersTo(child))
                .mapToObj(i -> new RelationToChildReference(parent1, i, parent1.getMember(i))))
                .collect(Collectors.toSet());
    }

    /**
     * Replies a set of all {@link RelationToChildReference}s for a collection of child primitives
     *
     * @param children the collection of child primitives
     * @return  a set of all {@link RelationToChildReference}s to the children in the collection of child
     * primitives
     */
    public static Set<RelationToChildReference> getRelationToChildReferences(Collection<? extends OsmPrimitive> children) {
        return children.stream()
                .flatMap(child -> getRelationToChildReferences(child).stream())
                .collect(Collectors.toSet());
    }

    private final Relation parent;
    private final int position;
    private final String role;
    private final OsmPrimitive child;

    /**
     * Create a new {@link RelationToChildReference}
     * @param parent The parent relation
     * @param position The position of the child in the parent
     * @param role The role of the child
     * @param child The actual child (member of parent)
     */
    public RelationToChildReference(Relation parent, int position, String role, OsmPrimitive child) {
        this.parent = parent;
        this.position = position;
        this.role = role;
        this.child = child;
    }

    /**
     * Create a new {@link RelationToChildReference}
     * @param parent The parent relation
     * @param position The position of the child in the parent
     * @param member The role and relation for the child
     */
    public RelationToChildReference(Relation parent, int position, RelationMember member) {
        this(parent, position, member.getRole(), member.getMember());
    }

    /**
     * Get the parent relation
     * @return The parent
     */
    public Relation getParent() {
        return parent;
    }

    /**
     * Get the position of the child in the parent
     * @return The position of the child
     */
    public int getPosition() {
        return position;
    }

    /**
     * Get the role of the child
     * @return The role
     */
    public String getRole() {
        return role;
    }

    /**
     * Get the actual child
     * @return The child
     */
    public OsmPrimitive getChild() {
        return child;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        RelationToChildReference that = (RelationToChildReference) obj;
        return position == that.position &&
                Objects.equals(parent, that.parent) &&
                Objects.equals(role, that.role) &&
                Objects.equals(child, that.child);
    }

    @Override
    public int hashCode() {
        return Objects.hash(parent, position, role, child);
    }
}
