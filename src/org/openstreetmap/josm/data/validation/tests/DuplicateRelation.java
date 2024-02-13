// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.openstreetmap.josm.command.ChangeMembersCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.DeleteCommand;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.AbstractPrimitive;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.Test;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.tools.MultiMap;

/**
 * Tests if there are duplicate relations
 */
public class DuplicateRelation extends Test {

    private static final String DUPLICATED_RELATIONS = marktr("Duplicated relations");

    /**
     * Class to store one relation members and information about it
     */
    public static class RelMember {
        /** Role of the relation member */
        private final String role;

        /** Type of the relation member */
        private final OsmPrimitiveType type;

        /** Tags of the relation member */
        private Map<String, String> tags;

        /** Coordinates of the relation member */
        private List<LatLon> coor;

        /** ID of the relation member in case it is a {@link Relation} */
        private long relId;

        @Override
        public int hashCode() {
            return Objects.hash(role, type, tags, coor, relId);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            RelMember relMember = (RelMember) obj;
            return relId == relMember.relId &&
                    type == relMember.type &&
                    Objects.equals(role, relMember.role) &&
                    Objects.equals(tags, relMember.tags) &&
                    Objects.equals(coor, relMember.coor);
        }

        /** Extract and store relation information based on the relation member
         * @param src The relation member to store information about
         */
        public RelMember(RelationMember src) {
            role = src.getRole();
            type = src.getType();
            relId = 0;
            coor = new ArrayList<>();

            if (src.isNode()) {
                Node r = src.getNode();
                tags = r.getKeys();
                coor = new ArrayList<>(1);
                coor.add(r.getCoor());
            }
            if (src.isWay()) {
                Way r = src.getWay();
                tags = r.getKeys();
                List<Node> wNodes = r.getNodes();
                coor = new ArrayList<>(wNodes.size());
                for (Node wNode : wNodes) {
                    coor.add(wNode.getCoor());
                }
            }
            if (src.isRelation()) {
                Relation r = src.getRelation();
                tags = r.getKeys();
                relId = r.getId();
                coor = new ArrayList<>();
            }
        }
    }

    /**
     * Class to store relation members
     */
    private static class RelationMembers {
        /** Set of member objects of the relation */
        private final List<RelMember> members;

        /** Store relation information
         * @param members The list of relation members
         */
        RelationMembers(List<RelationMember> members) {
            this.members = new ArrayList<>(members.size());
            for (RelationMember member : members) {
                this.members.add(new RelMember(member));
            }
        }

        @Override
        public int hashCode() {
            return Objects.hash(members);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            RelationMembers that = (RelationMembers) obj;
            return Objects.equals(members, that.members);
        }
    }

    /**
     * Class to store relation data (keys are usually cleanup and may not be equal to original relation)
     */
    private static class RelationPair {
        /** Member objects of the relation */
        private final RelationMembers members;
        /** Tags of the relation */
        private final Map<String, String> keys;

        /** Store relation information
         * @param members The list of relation members
         * @param keys The set of tags of the relation
         */
        RelationPair(List<RelationMember> members, Map<String, String> keys) {
            this.members = new RelationMembers(members);
            this.keys = keys;
        }

        @Override
        public int hashCode() {
            return Objects.hash(members, keys);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            RelationPair that = (RelationPair) obj;
            return Objects.equals(members, that.members) &&
                    Objects.equals(keys, that.keys);
        }
    }

    /** Code number of completely duplicated relation error */
    protected static final int DUPLICATE_RELATION = 1901;

    /** Code number of relation with same members error */
    protected static final int SAME_RELATION = 1902;

    /** Code number of relation with same members error */
    protected static final int IDENTICAL_MEMBERLIST = 1903;


    /** List of all initially visited testable relations*/
    List<Relation> visited;

    /**
     * Default constructor
     */
    public DuplicateRelation() {
        super(tr(DUPLICATED_RELATIONS),
                tr("This test checks that there are no relations with same tags and same members with same roles."));
    }

    @Override
    public void startTest(ProgressMonitor monitor) {
        super.startTest(monitor);
        visited = new ArrayList<>();

    }

    @Override
    public void endTest() {
        if (!visited.isEmpty())
            performChecks();
        visited = null;
        super.endTest();
    }

    private void performChecks() {
        MultiMap<RelationPair, Relation> relations = new MultiMap<>(1000);
        // MultiMap of all relations, regardless of keys
        MultiMap<List<RelationMember>, Relation> sameMembers = new MultiMap<>(1000);

        for (Relation r : visited) {
            final List<RelationMember> rMembers = getSortedMembers(r);
            sameMembers.put(rMembers, r);
            addToRelations(relations, r, true);
        }

        if (partialSelection) {
            // add data for relations which were not in the initial selection when
            // they can be duplicates
            DataSet ds = visited.iterator().next().getDataSet();
            for (Relation r : ds.getRelations()) {
                if (r.isDeleted() || r.getMembers().isEmpty() || visited.contains(r))
                    continue;
                final List<RelationMember> rMembers = getSortedMembers(r);
                if (sameMembers.containsKey(rMembers))
                    sameMembers.put(rMembers, r);

                addToRelations(relations, r, false);
            }
        }

        for (Set<Relation> duplicated : sameMembers.values()) {
            if (duplicated.size() > 1) {
                checkOrderAndTags(duplicated);
            }
        }

        performGeometryTest(relations);
    }

    private void performGeometryTest(MultiMap<RelationPair, Relation> relations) {
        // perform special test to find relations with different members but (possibly) same geometry
        // this test is rather speculative and works only with complete members
        for (Set<Relation> duplicated : relations.values()) {
            if (duplicated.size() > 1) {
                TestError testError = TestError.builder(this, Severity.ERROR, DUPLICATE_RELATION)
                        .message(tr(DUPLICATED_RELATIONS))
                        .primitives(duplicated)
                        .build();
                if (errors.stream().noneMatch(e -> e.isSimilar(testError))) {
                    errors.add(testError);
                }
            }
        }
    }

    private static void addToRelations(MultiMap<RelationPair, Relation> relations, Relation r, boolean forceAdd) {
        if (r.isUsable() && !r.hasIncompleteMembers()) {
            RelationPair rKey = new RelationPair(r.getMembers(), cleanedKeys(r));
            if (forceAdd || (!relations.isEmpty() && !relations.containsKey(rKey)))
                relations.put(rKey, r);
        }
    }

    @Override
    public void visit(Relation r) {
        if (!r.isDeleted() && r.getMembersCount() > 0)
            visited.add(r);
    }

    /**
     * Check a list of relations which are guaranteed to have the same members, possibly in different order
     * and possibly different tags.
     * @param sameMembers the list of relations
     */
    private void checkOrderAndTags(Set<Relation> sameMembers) {
        MultiMap<List<RelationMember>, Relation> sameOrder = new MultiMap<>();
        MultiMap<Map<String, String>, Relation> sameKeys = new MultiMap<>();
        for (Relation r : sameMembers) {
            sameOrder.put(r.getMembers(), r);
            sameKeys.put(cleanedKeys(r), r);
        }
        for (Set<Relation> duplicated : sameKeys.values()) {
            if (duplicated.size() > 1) {
                reportDuplicateKeys(duplicated);
            }
        }
        for (Set<Relation> duplicated : sameOrder.values()) {
            if (duplicated.size() > 1) {
                reportDuplicateMembers(duplicated);
            }
        }
        List<Relation> primitives = sameMembers.stream().filter(r -> !ignoredType(r)).collect(Collectors.toList());
        // report this collection if not already reported
        if (primitives.size() > 1 && errors.stream().noneMatch(e -> e.getPrimitives().containsAll(primitives))) {
            // same members, possibly different order
            TestError testError = TestError.builder(this, Severity.OTHER, SAME_RELATION)
                    .message(tr("Relations with same members"))
                    .primitives(primitives)
                    .build();
            errors.add(testError);
        }
    }

    /**
     * Check collection of relations with the same keys and members, possibly in different order
     * @param duplicated collection of relations, caller must make sure that they have the same keys and members
     */
    private void reportDuplicateKeys(Collection<Relation> duplicated) {
        Relation first = duplicated.iterator().next();
        if (memberOrderMatters(first)) {
            List<Relation> toCheck = new ArrayList<>(duplicated);
            while (toCheck.size() > 1) {
                Relation ref = toCheck.iterator().next();
                List<Relation> same = toCheck.stream()
                        .filter(r -> r.getMembers().equals(ref.getMembers())).collect(Collectors.toList());
                if (same.size() > 1) {
                    // same members and keys, members in same order
                    TestError testError = TestError.builder(this, Severity.ERROR, DUPLICATE_RELATION)
                            .message(tr(DUPLICATED_RELATIONS))
                            .primitives(same)
                            .build();
                    errors.add(testError);
                }
                toCheck.removeAll(same);
            }
        } else {
            // same members and keys, possibly different order
            TestError testError = TestError.builder(this, Severity.ERROR, DUPLICATE_RELATION)
                    .message(tr(DUPLICATED_RELATIONS))
                    .primitives(duplicated)
                    .build();
            errors.add(testError);
        }
    }

    /**
     * Report relations with the same member(s) in the same order, keys may be different
     * @param duplicated collection of relations with identical member list
     */
    private void reportDuplicateMembers(Set<Relation> duplicated) {
        List<Relation> primitives = duplicated.stream().filter(r -> !ignoredType(r)).collect(Collectors.toList());
        if (primitives.size() > 1 && errors.stream().noneMatch(e -> e.getPrimitives().containsAll(primitives))) {
            TestError testError = TestError.builder(this, Severity.OTHER, IDENTICAL_MEMBERLIST)
                    .message(tr("Identical members"))
                    .primitives(primitives)
                    .build();
            errors.add(testError);
        }

    }

    private static boolean memberOrderMatters(Relation r) {
        return r.hasTag("type", "route", "waterway"); // add more types?
    }

    private static boolean ignoredType(Relation r) {
        return r.hasTag("type", "tmc", "TMC", "destination_sign"); // see r11783
    }

    /** return tags of a primitive after removing all discardable tags
     *
     * @param p the primitive
     * @return map with cleaned tags
     */
    private static Map<String, String> cleanedKeys(OsmPrimitive p) {
        Map<String, String> cleaned = p.getKeys();
        for (String key : AbstractPrimitive.getDiscardableKeys()) {
            cleaned.remove(key);
        }
        return cleaned;
    }

    /**
     *  Order members of given relation by type, unique id, and role.
     *  @param r the relation
     * @return sorted list of members
     */
    private static List<RelationMember> getSortedMembers(Relation r) {
        return r.getMembers().stream().sorted((m1, m2) -> {
            int d = m1.getMember().compareTo(m2.getMember());
            if (d != 0)
                return d;
            return m1.getRole().compareTo(m2.getRole());
        }).collect(Collectors.toList());
    }

    /**
     * Fix the error by removing all but one instance of duplicate relations
     * @param testError The error to fix, must be of type {@link #DUPLICATE_RELATION}
     */
    @Override
    public Command fixError(TestError testError) {
        if (!isFixable(testError)) return null;

        Set<Relation> relFix = testError.primitives(Relation.class)
                .filter(r -> !r.isDeleted() || r.getDataSet() == null || r.getDataSet().getPrimitiveById(r) == null)
                .collect(Collectors.toSet());

        if (relFix.size() < 2)
            return null;

        long idToKeep = 0;
        Relation relationToKeep = relFix.iterator().next();
        // Find the relation that is member of one or more relations. (If any)
        Relation relationWithRelations = null;
        Collection<Relation> relRef = null;
        for (Relation r : relFix) {
            Collection<Relation> rel = r.referrers(Relation.class).collect(Collectors.toList());
            if (!rel.isEmpty()) {
                if (relationWithRelations != null)
                    throw new AssertionError("Cannot fix duplicate relations: More than one relation is member of another relation.");
                relationWithRelations = r;
                relRef = rel;
            }
            // Only one relation will be kept - the one with lowest positive ID, if such exist
            // or one "at random" if no such exists. Rest of the relations will be deleted
            if (!r.isNew() && (idToKeep == 0 || r.getId() < idToKeep)) {
                idToKeep = r.getId();
                relationToKeep = r;
            }
        }

        Collection<Command> commands = new LinkedList<>();

        // Fix relations.
        if (relationWithRelations != null && relRef != null && relationToKeep != relationWithRelations) {
            for (Relation rel : relRef) {
                List<RelationMember> members = new ArrayList<>(rel.getMembers());
                for (int i = 0; i < rel.getMembers().size(); ++i) {
                    RelationMember m = rel.getMember(i);
                    if (relationWithRelations.equals(m.getMember())) {
                        members.set(i, new RelationMember(m.getRole(), relationToKeep));
                    }
                }
                commands.add(new ChangeMembersCommand(rel, members));
            }
        }

        // Delete all relations in the list
        relFix.remove(relationToKeep);
        commands.add(new DeleteCommand(relFix));
        return new SequenceCommand(tr("Delete duplicate relations"), commands);
    }

    @Override
    public boolean isFixable(TestError testError) {
        if (!(testError.getTester() instanceof DuplicateRelation)
            || testError.getCode() != DUPLICATE_RELATION) return false;

        // We fix it only if there is no more than one relation that is relation member.
        Set<Relation> rels = testError.primitives(Relation.class)
                .collect(Collectors.toSet());

        if (rels.size() < 2)
            return false;

        // count relations with relations
        return rels.stream()
                .filter(x -> x.referrers(Relation.class).anyMatch(y -> true))
                .limit(2)
                .count() <= 1;
    }
}
