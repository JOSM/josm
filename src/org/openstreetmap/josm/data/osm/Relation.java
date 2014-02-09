// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.visitor.PrimitiveVisitor;
import org.openstreetmap.josm.data.osm.visitor.Visitor;
import org.openstreetmap.josm.tools.CopyList;
import org.openstreetmap.josm.tools.Predicate;
import org.openstreetmap.josm.tools.Utils;

/**
 * An relation, having a set of tags and any number (0...n) of members.
 *
 * @author Frederik Ramm
 */
public final class Relation extends OsmPrimitive implements IRelation {

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
        boolean locked = writeLock();
        try {
            for (RelationMember rm : this.members) {
                rm.getMember().removeReferrer(this);
                rm.getMember().clearCachedStyle();
            }

            if (members != null) {
                this.members = members.toArray(new RelationMember[members.size()]);
            } else {
                this.members = new RelationMember[0];
            }
            for (RelationMember rm : this.members) {
                rm.getMember().addReferrer(this);
                rm.getMember().clearCachedStyle();
            }

            fireMembersChanged();
        } finally {
            writeUnlock(locked);
        }
    }

    /**
     * @return number of members
     */
    @Override
    public int getMembersCount() {
        return members.length;
    }

    public RelationMember getMember(int index) {
        return members[index];
    }

    public void addMember(RelationMember member) {
        boolean locked = writeLock();
        try {
            members = Utils.addInArrayCopy(members, member);
            member.getMember().addReferrer(this);
            member.getMember().clearCachedStyle();
            fireMembersChanged();
        } finally {
            writeUnlock(locked);
        }
    }

    public void addMember(int index, RelationMember member) {
        boolean locked = writeLock();
        try {
            RelationMember[] newMembers = new RelationMember[members.length + 1];
            System.arraycopy(members, 0, newMembers, 0, index);
            System.arraycopy(members, index, newMembers, index + 1, members.length - index);
            newMembers[index] = member;
            members = newMembers;
            member.getMember().addReferrer(this);
            member.getMember().clearCachedStyle();
            fireMembersChanged();
        } finally {
            writeUnlock(locked);
        }
    }

    /**
     * Replace member at position specified by index.
     * @param index
     * @param member
     * @return Member that was at the position
     */
    public RelationMember setMember(int index, RelationMember member) {
        boolean locked = writeLock();
        try {
            RelationMember originalMember = members[index];
            members[index] = member;
            if (originalMember.getMember() != member.getMember()) {
                member.getMember().addReferrer(this);
                member.getMember().clearCachedStyle();
                originalMember.getMember().removeReferrer(this);
                originalMember.getMember().clearCachedStyle();
                fireMembersChanged();
            }
            return originalMember;
        } finally {
            writeUnlock(locked);
        }
    }

    /**
     * Removes member at specified position.
     * @param index
     * @return Member that was at the position
     */
    public RelationMember removeMember(int index) {
        boolean locked = writeLock();
        try {
            List<RelationMember> members = getMembers();
            RelationMember result = members.remove(index);
            setMembers(members);
            return result;
        } finally {
            writeUnlock(locked);
        }
    }

    @Override
    public long getMemberId(int idx) {
        return members[idx].getUniqueId();
    }

    @Override
    public String getRole(int idx) {
        return members[idx].getRole();
    }

    @Override
    public OsmPrimitiveType getMemberType(int idx) {
        return members[idx].getType();
    }

    @Override public void accept(Visitor visitor) {
        visitor.visit(this);
    }

    @Override public void accept(PrimitiveVisitor visitor) {
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

    /**
     * Constructs an identical clone of the argument.
     * @param clone The relation to clone
     * @param clearMetadata If {@code true}, clears the OSM id and other metadata as defined by {@link #clearOsmMetadata}. If {@code false}, does nothing
     */
    public Relation(Relation clone, boolean clearMetadata) {
        super(clone.getUniqueId(), true);
        cloneFrom(clone);
        if (clearMetadata) {
            clearOsmMetadata();
        }
    }

    /**
     * Create an identical clone of the argument (including the id)
     * @param clone The relation to clone, including its id
     */
    public Relation(Relation clone) {
        this(clone, false);
    }

    /**
     * Creates a new relation for the given id. If the id &gt; 0, the way is marked
     * as incomplete.
     *
     * @param id the id. &gt; 0 required
     * @throws IllegalArgumentException thrown if id &lt; 0
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
        boolean locked = writeLock();
        try {
            super.cloneFrom(osm);
            // It's not necessary to clone members as RelationMember class is immutable
            setMembers(((Relation)osm).getMembers());
        } finally {
            writeUnlock(locked);
        }
    }

    @Override public void load(PrimitiveData data) {
        boolean locked = writeLock();
        try {
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
        } finally {
            writeUnlock(locked);
        }
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
        if (!(other instanceof Relation))
            return false;
        if (! super.hasEqualSemanticAttributes(other))
            return false;
        Relation r = (Relation)other;
        return Arrays.equals(members, r.members);
    }

    @Override
    public int compareTo(OsmPrimitive o) {
        return o instanceof Relation ? Long.valueOf(getUniqueId()).compareTo(o.getUniqueId()) : -1;
    }

    public RelationMember firstMember() {
        if (isIncomplete()) return null;

        RelationMember[] members = this.members;
        return (members.length == 0) ? null : members[0];
    }
    public RelationMember lastMember() {
        if (isIncomplete()) return null;

        RelationMember[] members = this.members;
        return (members.length == 0) ? null : members[members.length - 1];
    }

    /**
     * removes all members with member.member == primitive
     *
     * @param primitive the primitive to check for
     */
    public void removeMembersFor(OsmPrimitive primitive) {
        removeMembersFor(Collections.singleton(primitive));
    }

    @Override
    public void setDeleted(boolean deleted) {
        boolean locked = writeLock();
        try {
            for (RelationMember rm:members) {
                if (deleted) {
                    rm.getMember().removeReferrer(this);
                } else {
                    rm.getMember().addReferrer(this);
                }
            }
            super.setDeleted(deleted);
        } finally {
            writeUnlock(locked);
        }
    }

    /**
     * Obtains all members with member.member == primitive
     * @param primitives the primitives to check for
     */
    public Collection<RelationMember> getMembersFor(final Collection<? extends OsmPrimitive> primitives) {
        return Utils.filter(getMembers(), new Predicate<RelationMember>() {
            @Override
            public boolean evaluate(RelationMember member) {
                return primitives.contains(member.getMember());
            }
        });
    }

    /**
     * removes all members with member.member == primitive
     *
     * @param primitives the primitives to check for
     * @since 5613
     */
    public void removeMembersFor(Collection<? extends OsmPrimitive> primitives) {
        if (primitives == null || primitives.isEmpty())
            return;

        boolean locked = writeLock();
        try {
            List<RelationMember> members = getMembers();
            members.removeAll(getMembersFor(primitives));
            setMembers(members);
        } finally {
            writeUnlock(locked);
        }
    }

    @Override
    public String getDisplayName(NameFormatter formatter) {
        return formatter.format(this);
    }

    /**
     * Replies the set of  {@link OsmPrimitive}s referred to by at least one
     * member of this relation
     *
     * @return the set of  {@link OsmPrimitive}s referred to by at least one
     * member of this relation
     */
    public Set<OsmPrimitive> getMemberPrimitives() {
        HashSet<OsmPrimitive> ret = new HashSet<OsmPrimitive>();
        RelationMember[] members = this.members;
        for (RelationMember m: members) {
            if (m.getMember() != null) {
                ret.add(m.getMember());
            }
        }
        return ret;
    }

    public <T extends OsmPrimitive> Collection<T> getMemberPrimitives(Class<T> tClass) {
        return Utils.filteredCollection(getMemberPrimitives(), tClass);
    }

    public List<OsmPrimitive> getMemberPrimitivesList() {
        return Utils.transform(getMembers(), new Utils.Function<RelationMember, OsmPrimitive>() {
            @Override
            public OsmPrimitive apply(RelationMember x) {
                return x.getMember();
            }
        });
    }

    @Override
    public OsmPrimitiveType getType() {
        return OsmPrimitiveType.RELATION;
    }

    @Override
    public OsmPrimitiveType getDisplayType() {
        return isMultipolygon() ? OsmPrimitiveType.MULTIPOLYGON
        : OsmPrimitiveType.RELATION;
    }

    public boolean isMultipolygon() {
        return "multipolygon".equals(get("type")) || "boundary".equals(get("type"));
    }

    @Override
    public BBox getBBox() {
        RelationMember[] members = this.members;

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

        RelationMember[] members = this.members;
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

    private void checkMembers() throws DataIntegrityProblemException {
        DataSet dataSet = getDataSet();
        if (dataSet != null) {
            RelationMember[] members = this.members;
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

    private void fireMembersChanged() throws DataIntegrityProblemException {
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
        RelationMember[] members = this.members;
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
        RelationMember[] members = this.members;
        for (RelationMember rm: members) {
            if (!rm.getMember().isIncomplete()) {
                continue;
            }
            ret.add(rm.getMember());
        }
        return ret;
    }

    @Override
    protected void keysChangedImpl(Map<String, String> originalKeys) {
        super.keysChangedImpl(originalKeys);
        // fix #8346 - Clear style cache for multipolygon members after a tag change
        if (isMultipolygon()) {
            for (OsmPrimitive member : getMemberPrimitives()) {
                member.clearCachedStyle();
            }
        }
    }

    @Override
    public boolean concernsArea() {
        return isMultipolygon() && hasAreaTags();
    }

    @Override
    public boolean isOutsideDownloadArea() {
        return false;
    }
}
