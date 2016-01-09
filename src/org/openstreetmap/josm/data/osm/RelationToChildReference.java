// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class RelationToChildReference {

    /**
     * Replies a set of all {@link RelationToChildReference}s for a given child primitive.
     *
     * @param child the child primitive
     * @return  a set of all {@link RelationToChildReference}s for a given child primitive
     */
    public static Set<RelationToChildReference> getRelationToChildReferences(OsmPrimitive child) {
        Set<Relation> parents = OsmPrimitive.getFilteredSet(child.getReferrers(), Relation.class);
        Set<RelationToChildReference> references = new HashSet<>();
        for (Relation parent: parents) {
            for (int i = 0; i < parent.getMembersCount(); i++) {
                if (parent.getMember(i).refersTo(child)) {
                    references.add(new RelationToChildReference(parent, i, parent.getMember(i)));
                }
            }
        }
        return references;
    }

    /**
     * Replies a set of all {@link RelationToChildReference}s for a collection of child primitives
     *
     * @param children the collection of child primitives
     * @return  a set of all {@link RelationToChildReference}s to the children in the collection of child
     * primitives
     */
    public static Set<RelationToChildReference> getRelationToChildReferences(Collection<? extends OsmPrimitive> children) {
        Set<RelationToChildReference> references = new HashSet<>();
        for (OsmPrimitive child: children) {
            references.addAll(getRelationToChildReferences(child));
        }
        return references;
    }

    private final Relation parent;
    private final int position;
    private final String role;
    private final OsmPrimitive child;

    public RelationToChildReference(Relation parent, int position, String role, OsmPrimitive child) {
        this.parent = parent;
        this.position = position;
        this.role = role;
        this.child = child;
    }

    public RelationToChildReference(Relation parent, int position, RelationMember member) {
        this.parent = parent;
        this.position = position;
        this.role = member.getRole();
        this.child = member.getMember();
    }

    public Relation getParent() {
        return parent;
    }

    public int getPosition() {
        return position;
    }

    public String getRole() {
        return role;
    }

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
