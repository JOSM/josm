package org.openstreetmap.josm.data.osm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.visitor.Visitor;
import org.openstreetmap.josm.tools.CopyList;

/**
 * An relation, having a set of tags and any number (0...n) of members.
 *
 * @author Frederik Ramm <frederik@remote.org>
 */
public final class Relation extends OsmPrimitive {

    private final List<RelationMember> members = new ArrayList<RelationMember>();

    private BBox bbox;

    /**
     * @return Members of the relation. Changes made in returned list are not mapped
     * back to the primitive, use setMembers() to modify the members
     * @since 1925
     */
    public List<RelationMember> getMembers() {
        return new CopyList<RelationMember>(members.toArray(new RelationMember[members.size()]));
    }

    /**
     *
     * @param members Can be null, in that case all members are removed
     * @since 1925
     */
    public void setMembers(List<RelationMember> members) {
        for (RelationMember rm:this.members) {
            rm.getMember().removeReferrer(this);
        }

        this.members.clear();
        if (members != null) {
            this.members.addAll(members);
        }
        for (RelationMember rm:this.members) {
            rm.getMember().addReferrer(this);
        }

        fireMembersChanged();
    }

    /**
     *
     * @since 1926
     */
    public int getMembersCount() {
        return members.size();
    }

    /**
     *
     * @param index
     * @return
     * @since 1926
     */
    public RelationMember getMember(int index) {
        return members.get(index);
    }

    /**
     *
     * @param member
     * @since 1951
     */
    public void addMember(RelationMember member) {
        members.add(member);
        member.getMember().addReferrer(this);
        fireMembersChanged();
    }

    /**
     *
     * @param index
     * @param member
     * @since 1951
     */
    public void addMember(int index, RelationMember member) {
        members.add(index, member);
        member.getMember().addReferrer(this);
        fireMembersChanged();
    }

    /**
     * Replace member at position specified by index.
     * @param index
     * @param member
     * @return Member that was at the position
     * @since 1951
     */
    public RelationMember setMember(int index, RelationMember member) {
        RelationMember result = members.set(index, member);
        if (result.getMember() != member.getMember()) {
            member.getMember().addReferrer(this);
            result.getMember().removeReferrer(this);
            fireMembersChanged();
        }
        return result;
    }

    /**
     * Removes member at specified position.
     * @param index
     * @return Member that was at the position
     * @since 1951
     */
    public RelationMember removeMember(int index) {
        RelationMember result = members.remove(index);
        for (RelationMember rm:members) {
            // Do not remove referrer if this primitive is used in relation twice
            if (rm.getMember() == result.getMember())
                return result;
        }
        result.getMember().removeReferrer(this);
        fireMembersChanged();
        return result;
    }

    @Override public void visit(Visitor visitor) {
        visitor.visit(this);
    }

    protected Relation(long id, boolean allowNegative) {
        super(id, allowNegative);
    }

    /**
     * Create a new relation with id 0
     */
    public Relation() {
        super(0, false);
    }

    public Relation(Relation clone, boolean clearId) {
        super(clone.getUniqueId(), true);
        cloneFrom(clone);
        if (clearId) {
            clearOsmId();
        }
    }

    /**
     * Create an identical clone of the argument (including the id)
     */
    public Relation(Relation clone) {
        this(clone, false);
    }

    /**
     * Creates a new relation for the given id. If the id > 0, the way is marked
     * as incomplete.
     *
     * @param id the id. > 0 required
     * @throws IllegalArgumentException thrown if id < 0
     */
    public Relation(long id) throws IllegalArgumentException {
        super(id, false);
    }

    /**
     * Creates new relation
     * @param id
     * @param version
     */
    public Relation(long id, int version) {
        super(id, version, false);
    }

    @Override public void cloneFrom(OsmPrimitive osm) {
        super.cloneFrom(osm);
        // It's not necessary to clone members as RelationMember class is immutable
        setMembers(((Relation)osm).getMembers());
    }

    @Override public void load(PrimitiveData data) {
        super.load(data);

        RelationData relationData = (RelationData) data;

        List<RelationMember> newMembers = new ArrayList<RelationMember>();
        for (RelationMemberData member : relationData.getMembers()) {
            OsmPrimitive primitive = getDataSet().getPrimitiveById(member);
            if (primitive == null)
                throw new AssertionError("Data consistency problem - relation with missing member detected");
            newMembers.add(new RelationMember(member.getRole(), primitive));
        }
        setMembers(newMembers);
    }

    @Override public RelationData save() {
        RelationData data = new RelationData();
        saveCommonAttributes(data);
        for (RelationMember member:getMembers()) {
            data.getMembers().add(new RelationMemberData(member.getRole(), member.getMember()));
        }
        return data;
    }

    @Override public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("{Relation id=");
        result.append(getUniqueId());
        result.append(" version=");
        result.append(getVersion());
        result.append(" ");
        result.append(getFlagsAsString());
        result.append(" [");
        for (RelationMember rm:getMembers()) {
            result.append(OsmPrimitiveType.from(rm.getMember()));
            result.append(" ");
            result.append(rm.getMember().getUniqueId());
            result.append(", ");
        }
        result.delete(result.length()-2, result.length());
        result.append("]");
        result.append("}");
        return result.toString();
    }

    @Override
    public boolean hasEqualSemanticAttributes(OsmPrimitive other) {
        if (other == null || ! (other instanceof Relation) )
            return false;
        if (! super.hasEqualSemanticAttributes(other))
            return false;
        Relation r = (Relation)other;
        return members.equals(r.members);
    }

    public int compareTo(OsmPrimitive o) {
        return o instanceof Relation ? Long.valueOf(getUniqueId()).compareTo(o.getUniqueId()) : -1;
    }

    public RelationMember firstMember() {
        if (isIncomplete()) return null;
        return (members.size() == 0) ? null : members.get(0);
    }
    public RelationMember lastMember() {
        if (isIncomplete()) return null;
        return (members.size() == 0) ? null : members.get(members.size() -1);
    }

    /**
     * removes all members with member.member == primitive
     *
     * @param primitive the primitive to check for
     */
    public void removeMembersFor(OsmPrimitive primitive) {
        if (primitive == null)
            return;

        ArrayList<RelationMember> todelete = new ArrayList<RelationMember>();
        for (RelationMember member: members) {
            if (member.getMember() == primitive) {
                todelete.add(member);
            }
        }
        primitive.removeReferrer(this);
        members.removeAll(todelete);
        fireMembersChanged();
    }

    @Override
    public void setDeleted(boolean deleted) {
        for (RelationMember rm:members) {
            if (deleted) {
                rm.getMember().removeReferrer(this);
            } else {
                rm.getMember().addReferrer(this);
            }
        }
        super.setDeleted(deleted);
    }

    /**
     * removes all members with member.member == primitive
     *
     * @param primitives the primitives to check for
     */
    public void removeMembersFor(Collection<OsmPrimitive> primitives) {
        if (primitives == null || primitives.isEmpty())
            return;

        ArrayList<RelationMember> todelete = new ArrayList<RelationMember>();
        for (RelationMember member: members) {
            if (primitives.contains(member.getMember())) {
                todelete.add(member);
            }
        }
        members.removeAll(todelete);
        for (OsmPrimitive primitive:primitives) {
            primitive.removeReferrer(this);
        }
        fireMembersChanged();
    }

    @Override
    public String getDisplayName(NameFormatter formatter) {
        return formatter.format(this);
    }

    /**
     * Replies the set of  {@see OsmPrimitive}s referred to by at least one
     * member of this relation
     *
     * @return the set of  {@see OsmPrimitive}s referred to by at least one
     * member of this relation
     */
    public Set<OsmPrimitive> getMemberPrimitives() {
        HashSet<OsmPrimitive> ret = new HashSet<OsmPrimitive>();
        for (RelationMember m: members) {
            if (m.getMember() != null) {
                ret.add(m.getMember());
            }
        }
        return ret;
    }

    public OsmPrimitiveType getType() {
        return OsmPrimitiveType.RELATION;
    }

    @Override
    public BBox getBBox() {
        if (members.isEmpty())
            return new BBox(0, 0, 0, 0);
        if (getDataSet() == null)
            return calculateBBox(new HashSet<PrimitiveId>());
        else {
            if (bbox == null) {
                bbox = calculateBBox(new HashSet<PrimitiveId>());
            }
            return new BBox(bbox);
        }
    }

    private BBox calculateBBox(Set<PrimitiveId> visitedRelations) {
        if (visitedRelations.contains(this))
            return null;
        visitedRelations.add(this);
        if (members.isEmpty())
            return null;
        else {
            BBox result = null;
            for (RelationMember rm:members) {
                BBox box = rm.isRelation()?rm.getRelation().calculateBBox(visitedRelations):rm.getMember().getBBox();
                if (box != null) {
                    if (result == null) {
                        result = box;
                    } else {
                        result.add(box);
                    }
                }
            }
            return result;
        }
    }

    @Override
    public void updatePosition() {
        bbox = calculateBBox(new HashSet<PrimitiveId>());
    }

    @Override
    public void setDataset(DataSet dataSet) {
        super.setDataset(dataSet);
        checkMembers();
        bbox = null; // bbox might have changed if relation was in ds, was removed, modified, added back to dataset
    }

    private void checkMembers() {
        DataSet dataSet = getDataSet();
        if (dataSet != null) {
            for (RelationMember rm: members) {
                if (rm.getMember().getDataSet() != dataSet)
                    throw new DataIntegrityProblemException(String.format("Relation member must be part of the same dataset as relation(%s, %s)", getPrimitiveId(), rm.getMember().getPrimitiveId()));
            }
            if (Main.pref.getBoolean("debug.checkDeleteReferenced", true)) {
                for (RelationMember rm: members) {
                    if (rm.getMember().isDeleted())
                        throw new DataIntegrityProblemException("Deleted member referenced: " + toString());
                }
            }
        }
    }

    private void fireMembersChanged() {
        checkMembers();
        if (getDataSet() != null) {
            getDataSet().fireRelationMembersChanged(this);
        }
    }

    /**
     * Replies true if at least one child primitive is incomplete
     *
     * @return true if at least one child primitive is incomplete
     */
    public boolean hasIncompleteMembers() {
        for (RelationMember rm: members) {
            if (rm.getMember().isIncomplete()) return true;
        }
        return false;
    }

    /**
     * Replies a collection with the incomplete children this relation
     * refers to
     *
     * @return the incomplete children. Empty collection if no children are incomplete.
     */
    public Collection<OsmPrimitive> getIncompleteMembers() {
        Set<OsmPrimitive> ret = new HashSet<OsmPrimitive>();
        for (RelationMember rm: members) {
            if (!rm.getMember().isIncomplete()) {
                continue;
            }
            ret.add(rm.getMember());
        }
        return ret;
    }
}
