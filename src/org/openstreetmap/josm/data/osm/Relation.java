// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.openstreetmap.josm.data.osm.visitor.OsmPrimitiveVisitor;
import org.openstreetmap.josm.data.osm.visitor.PrimitiveVisitor;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.CopyList;
import org.openstreetmap.josm.tools.SubclassFilteredCollection;
import org.openstreetmap.josm.tools.Utils;

/**
 * A relation, having a set of tags and any number (0...n) of members.
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
        return new CopyList<>(members);
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

    @Override
    public int getMembersCount() {
        return members.length;
    }

    /**
     * Returns the relation member at the specified index.
     * @param index the index of the relation member
     * @return relation member at the specified index
     */
    public RelationMember getMember(int index) {
        return members[index];
    }

    /**
     * Adds the specified relation member at the last position.
     * @param member the member to add
     */
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

    /**
     * Adds the specified relation member at the specified index.
     * @param member the member to add
     * @param index the index at which the specified element is to be inserted
     */
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
     * @param index index (positive integer)
     * @param member relation member to set
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
     * @param index index (positive integer)
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

    /**
     * @deprecated no longer supported
     */
    @Override
    @Deprecated
    public void accept(org.openstreetmap.josm.data.osm.visitor.Visitor visitor) {
        visitor.visit(this);
    }

    @Override
    public void accept(OsmPrimitiveVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public void accept(PrimitiveVisitor visitor) {
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
     * @param clearMetadata If {@code true}, clears the OSM id and other metadata as defined by {@link #clearOsmMetadata}.
     * If {@code false}, does nothing
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
     * @throws IllegalArgumentException if id &lt; 0
     */
    public Relation(long id) {
        super(id, false);
    }

    /**
     * Creates new relation
     * @param id the id
     * @param version version number (positive integer)
     */
    public Relation(long id, int version) {
        super(id, version, false);
    }

    @Override
    public void cloneFrom(OsmPrimitive osm) {
        if (!(osm instanceof Relation))
            throw new IllegalArgumentException("Not a relation: " + osm);
        boolean locked = writeLock();
        try {
            super.cloneFrom(osm);
            // It's not necessary to clone members as RelationMember class is immutable
            setMembers(((Relation) osm).getMembers());
        } finally {
            writeUnlock(locked);
        }
    }

    @Override
    public void load(PrimitiveData data) {
        if (!(data instanceof RelationData))
            throw new IllegalArgumentException("Not a relation data: " + data);
        boolean locked = writeLock();
        try {
            super.load(data);

            RelationData relationData = (RelationData) data;

            List<RelationMember> newMembers = new ArrayList<>();
            for (RelationMemberData member : relationData.getMembers()) {
                newMembers.add(new RelationMember(member.getRole(), Optional.ofNullable(getDataSet().getPrimitiveById(member))
                        .orElseThrow(() -> new AssertionError("Data consistency problem - relation with missing member detected"))));
            }
            setMembers(newMembers);
        } finally {
            writeUnlock(locked);
        }
    }

    @Override
    public RelationData save() {
        RelationData data = new RelationData();
        saveCommonAttributes(data);
        for (RelationMember member:getMembers()) {
            data.getMembers().add(new RelationMemberData(member.getRole(), member.getMember()));
        }
        return data;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder(32);
        result.append("{Relation id=")
              .append(getUniqueId())
              .append(" version=")
              .append(getVersion())
              .append(' ')
              .append(getFlagsAsString())
              .append(" [");
        for (RelationMember rm:getMembers()) {
            result.append(OsmPrimitiveType.from(rm.getMember()))
                  .append(' ')
                  .append(rm.getMember().getUniqueId())
                  .append(", ");
        }
        result.delete(result.length()-2, result.length())
              .append("]}");
        return result.toString();
    }

    @Override
    public boolean hasEqualSemanticAttributes(OsmPrimitive other, boolean testInterestingTagsOnly) {
        return (other instanceof Relation)
                && hasEqualSemanticFlags(other)
                && Arrays.equals(members, ((Relation) other).members)
                && super.hasEqualSemanticAttributes(other, testInterestingTagsOnly);
    }

    @Override
    public int compareTo(OsmPrimitive o) {
        return o instanceof Relation ? Long.compare(getUniqueId(), o.getUniqueId()) : -1;
    }

    /**
     * Returns the first member.
     * @return first member, or {@code null}
     */
    public RelationMember firstMember() {
        return (isIncomplete() || members.length == 0) ? null : members[0];
    }

    /**
     * Returns the last member.
     * @return last member, or {@code null}
     */
    public RelationMember lastMember() {
        return (isIncomplete() || members.length == 0) ? null : members[members.length - 1];
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
     * @return all relation members for the given primitives
     */
    public Collection<RelationMember> getMembersFor(final Collection<? extends OsmPrimitive> primitives) {
        return SubclassFilteredCollection.filter(getMembers(), member -> primitives.contains(member.getMember()));
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
     * @see #getMemberPrimitivesList()
     */
    public Set<OsmPrimitive> getMemberPrimitives() {
        return getMembers().stream().map(RelationMember::getMember).collect(Collectors.toSet());
    }

    /**
     * Returns the {@link OsmPrimitive}s of the specified type referred to by at least one member of this relation.
     * @param tClass the type of the primitive
     * @param <T> the type of the primitive
     * @return the primitives
     */
    public <T extends OsmPrimitive> Collection<T> getMemberPrimitives(Class<T> tClass) {
        return Utils.filteredCollection(getMemberPrimitivesList(), tClass);
    }

    /**
     * Returns an unmodifiable list of the {@link OsmPrimitive}s referred to by at least one member of this relation.
     * @return an unmodifiable list of the primitives
     */
    public List<OsmPrimitive> getMemberPrimitivesList() {
        return Utils.transform(getMembers(), RelationMember::getMember);
    }

    @Override
    public OsmPrimitiveType getType() {
        return OsmPrimitiveType.RELATION;
    }

    @Override
    public OsmPrimitiveType getDisplayType() {
        return isMultipolygon() && !isBoundary() ? OsmPrimitiveType.MULTIPOLYGON : OsmPrimitiveType.RELATION;
    }

    /**
     * Determines if this relation is a boundary.
     * @return {@code true} if a boundary relation
     */
    public boolean isBoundary() {
        return "boundary".equals(get("type"));
    }

    @Override
    public boolean isMultipolygon() {
        return "multipolygon".equals(get("type")) || isBoundary();
    }

    @Override
    public BBox getBBox() {
        if (getDataSet() != null && bbox != null)
            return new BBox(bbox); // use cached value

        BBox box = new BBox();
        addToBBox(box, new HashSet<PrimitiveId>());
        if (getDataSet() != null)
            setBBox(box); // set cache
        return new BBox(box);
    }

    private void setBBox(BBox bbox) {
        this.bbox = bbox;
    }

    @Override
    protected void addToBBox(BBox box, Set<PrimitiveId> visited) {
        for (RelationMember rm : members) {
            if (visited.add(rm.getMember()))
                rm.getMember().addToBBox(box, visited);
        }
    }

    @Override
    public void updatePosition() {
        setBBox(null); // make sure that it is recalculated
        setBBox(getBBox());
    }

    @Override
    void setDataset(DataSet dataSet) {
        super.setDataset(dataSet);
        checkMembers();
        setBBox(null); // bbox might have changed if relation was in ds, was removed, modified, added back to dataset
    }

    /**
     * Checks that members are part of the same dataset, and that they're not deleted.
     * @throws DataIntegrityProblemException if one the above conditions is not met
     */
    private void checkMembers() {
        DataSet dataSet = getDataSet();
        if (dataSet != null) {
            RelationMember[] members = this.members;
            for (RelationMember rm: members) {
                if (rm.getMember().getDataSet() != dataSet)
                    throw new DataIntegrityProblemException(
                            String.format("Relation member must be part of the same dataset as relation(%s, %s)",
                                    getPrimitiveId(), rm.getMember().getPrimitiveId()));
            }
            if (Config.getPref().getBoolean("debug.checkDeleteReferenced", true)) {
                for (RelationMember rm: members) {
                    if (rm.getMember().isDeleted())
                        throw new DataIntegrityProblemException("Deleted member referenced: " + toString());
                }
            }
        }
    }

    /**
     * Fires the {@code RelationMembersChangedEvent} to listeners.
     * @throws DataIntegrityProblemException if members are not valid
     * @see #checkMembers
     */
    private void fireMembersChanged() {
        checkMembers();
        if (getDataSet() != null) {
            getDataSet().fireRelationMembersChanged(this);
        }
    }

    /**
     * Determines if at least one child primitive is incomplete.
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
     * Replies a collection with the incomplete children this relation refers to.
     *
     * @return the incomplete children. Empty collection if no children are incomplete.
     */
    public Collection<OsmPrimitive> getIncompleteMembers() {
        Set<OsmPrimitive> ret = new HashSet<>();
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
        for (OsmPrimitive member : getMemberPrimitivesList()) {
            member.clearCachedStyle();
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

    /**
     * Returns the set of roles used in this relation.
     * @return the set of roles used in this relation. Can be empty but never null
     * @since 7556
     */
    public Set<String> getMemberRoles() {
        Set<String> result = new HashSet<>();
        for (RelationMember rm : members) {
            String role = rm.getRole();
            if (!role.isEmpty()) {
                result.add(role);
            }
        }
        return result;
    }
}
