package org.openstreetmap.josm.data.osm;

import java.util.ArrayList;
import java.util.Arrays;
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

    private RelationMember[] members = new RelationMember[0];

    private BBox bbox;

    /**
     * @return Members of the relation. Changes made in returned list are not mapped
     * back to the primitive, use setMembers() to modify the members
     * @since 1925
     */
    public List<RelationMember> getMembers() {
        return new CopyList<RelationMember>(members);
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

        if (members != null) {
            this.members = members.toArray(new RelationMember[members.size()]);
        }
        for (RelationMember rm:this.members) {
            rm.getMember().addReferrer(this);
        }

        fireMembersChanged();
    }

    /**
     * @return number of members
     */
    public int getMembersCount() {
        return members.length;
    }

    public RelationMember getMember(int index) {
        return members[index];
    }

    public void addMember(RelationMember member) {
        RelationMember[] newMembers = new RelationMember[members.length + 1];
        System.arraycopy(members, 0, newMembers, 0, members.length);
        newMembers[members.length] = member;
        members = newMembers;
        member.getMember().addReferrer(this);
        fireMembersChanged();
    }

    public void addMember(int index, RelationMember member) {
        RelationMember[] newMembers = new RelationMember[members.length + 1];
        System.arraycopy(members, 0, newMembers, 0, index);
        System.arraycopy(members, index, newMembers, index + 1, members.length - index);
        newMembers[index] = member;
        members = newMembers;
        member.getMember().addReferrer(this);
        fireMembersChanged();
    }

    /**
     * Replace member at position specified by index.
     * @param index
     * @param member
     * @return Member that was at the position
     */
    public RelationMember setMember(int index, RelationMember member) {
        RelationMember originalMember = members[index];
        members[index] = member;
        if (originalMember.getMember() != member.getMember()) {
            member.getMember().addReferrer(this);
            originalMember.getMember().removeReferrer(this);
            fireMembersChanged();
        }
        return originalMember;
    }

    /**
     * Removes member at specified position.
     * @param index
     * @return Member that was at the position
     */
    public RelationMember removeMember(int index) {
        List<RelationMember> members = getMembers();
        RelationMember result = members.remove(index);
        setMembers(members);
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
        return Arrays.equals(members, r.members);
    }

    public int compareTo(OsmPrimitive o) {
        return o instanceof Relation ? Long.valueOf(getUniqueId()).compareTo(o.getUniqueId()) : -1;
    }

    public RelationMember firstMember() {
        if (isIncomplete()) return null;
        return (members.length == 0) ? null : members[0];
    }
    public RelationMember lastMember() {
        if (isIncomplete()) return null;
        return (members.length == 0) ? null : members[members.length - 1];
    }

    /**
     * removes all members with member.member == primitive
     *
     * @param primitive the primitive to check for
     */
    public void removeMembersFor(OsmPrimitive primitive) {
        if (primitive == null)
            return;

        List<RelationMember> todelete = new ArrayList<RelationMember>();
        for (RelationMember member: members) {
            if (member.getMember() == primitive) {
                todelete.add(member);
            }
        }
        List<RelationMember> members = getMembers();
        members.removeAll(todelete);
        setMembers(members);
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
        List<RelationMember> members = getMembers();
        members.removeAll(todelete);
        setMembers(members);
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
        if (members.length == 0)
            return new BBox(0, 0, 0, 0);
        if (getDataSet() == null)
            return calculateBBox(new HashSet<PrimitiveId>());
        else {
            if (bbox == null) {
                bbox = calculateBBox(new HashSet<PrimitiveId>());
            }
            if (bbox == null)
                return new BBox(0, 0, 0, 0); // No real members
            else
                return new BBox(bbox);
        }
    }

    private BBox calculateBBox(Set<PrimitiveId> visitedRelations) {
        if (visitedRelations.contains(this))
            return null;
        visitedRelations.add(this);
        if (members.length == 0)
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
