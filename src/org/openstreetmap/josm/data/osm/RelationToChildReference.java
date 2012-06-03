// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class RelationToChildReference {

    /**
     * Replies a set of all {@link RelationToChildReference}s for a given child primitive.
     *
     * @param child the child primitive
     * @return  a set of all {@link RelationToChildReference}s for a given child primitive
     */
    static public Set<RelationToChildReference> getRelationToChildReferences(OsmPrimitive child) {
        Set<Relation> parents = OsmPrimitive.getFilteredSet(child.getReferrers(), Relation.class);
        Set<RelationToChildReference> references = new HashSet<RelationToChildReference>();
        for (Relation parent: parents) {
            for (int i=0; i < parent.getMembersCount(); i++) {
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
    static public Set<RelationToChildReference> getRelationToChildReferences(Collection<? extends OsmPrimitive> children) {
        Set<RelationToChildReference> references = new HashSet<RelationToChildReference>();
        for (OsmPrimitive child: children) {
            references.addAll(getRelationToChildReferences(child));
        }
        return references;
    }

    private Relation parent;
    private int position;
    private String role;
    private OsmPrimitive child;

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
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((child == null) ? 0 : child.hashCode());
        result = prime * result + ((parent == null) ? 0 : parent.hashCode());
        result = prime * result + position;
        result = prime * result + ((role == null) ? 0 : role.hashCode());
        return result;
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        RelationToChildReference other = (RelationToChildReference) obj;
        if (child == null) {
            if (other.child != null)
                return false;
        } else if (!child.equals(other.child))
            return false;
        if (parent == null) {
            if (other.parent != null)
                return false;
        } else if (!parent.equals(other.parent))
            return false;
        if (position != other.position)
            return false;
        if (role == null) {
            if (other.role != null)
                return false;
        } else if (!role.equals(other.role))
            return false;
        return true;
    }
}
