// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.DeleteCommand;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.coor.LatLon;
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
        /** List of member objects of the relation */
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

    /** MultiMap of all relations */
    private MultiMap<RelationPair, OsmPrimitive> relations;

    /** MultiMap of all relations, regardless of keys */
    private MultiMap<List<RelationMember>, OsmPrimitive> relationsNoKeys;

    /** List of keys without useful information */
    private final Set<String> ignoreKeys = new HashSet<>(OsmPrimitive.getUninterestingKeys());

    /**
     * Default constructor
     */
    public DuplicateRelation() {
        super(tr("Duplicated relations"),
                tr("This test checks that there are no relations with same tags and same members with same roles."));
    }

    @Override
    public void startTest(ProgressMonitor monitor) {
        super.startTest(monitor);
        relations = new MultiMap<>(1000);
        relationsNoKeys = new MultiMap<>(1000);
    }

    @Override
    public void endTest() {
        super.endTest();
        for (Set<OsmPrimitive> duplicated : relations.values()) {
            if (duplicated.size() > 1) {
                TestError testError = TestError.builder(this, Severity.ERROR, DUPLICATE_RELATION)
                        .message(tr("Duplicated relations"))
                        .primitives(duplicated)
                        .build();
                errors.add(testError);
            }
        }
        relations = null;
        for (Set<OsmPrimitive> duplicated : relationsNoKeys.values()) {
            if (duplicated.size() > 1) {
                TestError testError = TestError.builder(this, Severity.WARNING, SAME_RELATION)
                        .message(tr("Relations with same members"))
                        .primitives(duplicated)
                        .build();
                errors.add(testError);
            }
        }
        relationsNoKeys = null;
    }

    @Override
    public void visit(Relation r) {
        if (!r.isUsable() || r.hasIncompleteMembers() || "tmc".equals(r.get("type")) || "TMC".equals(r.get("type")))
            return;
        List<RelationMember> rMembers = r.getMembers();
        Map<String, String> rkeys = r.getKeys();
        for (String key : ignoreKeys) {
            rkeys.remove(key);
        }
        RelationPair rKey = new RelationPair(rMembers, rkeys);
        relations.put(rKey, r);
        relationsNoKeys.put(rMembers, r);
    }

    /**
     * Fix the error by removing all but one instance of duplicate relations
     * @param testError The error to fix, must be of type {@link #DUPLICATE_RELATION}
     */
    @Override
    public Command fixError(TestError testError) {
        if (testError.getCode() == SAME_RELATION) return null;
        Collection<? extends OsmPrimitive> sel = testError.getPrimitives();
        Set<Relation> relFix = new HashSet<>();

        for (OsmPrimitive osm : sel) {
            if (osm instanceof Relation && !osm.isDeleted()) {
                relFix.add((Relation) osm);
            }
        }

        if (relFix.size() < 2)
            return null;

        long idToKeep = 0;
        Relation relationToKeep = relFix.iterator().next();
        // Find the relation that is member of one or more relations. (If any)
        Relation relationWithRelations = null;
        List<Relation> relRef = null;
        for (Relation w : relFix) {
            List<Relation> rel = OsmPrimitive.getFilteredList(w.getReferrers(), Relation.class);
            if (!rel.isEmpty()) {
                if (relationWithRelations != null)
                    throw new AssertionError("Cannot fix duplicate relations: More than one relation is member of another relation.");
                relationWithRelations = w;
                relRef = rel;
            }
            // Only one relation will be kept - the one with lowest positive ID, if such exist
            // or one "at random" if no such exists. Rest of the relations will be deleted
            if (!w.isNew() && (idToKeep == 0 || w.getId() < idToKeep)) {
                idToKeep = w.getId();
                relationToKeep = w;
            }
        }

        Collection<Command> commands = new LinkedList<>();

        // Fix relations.
        if (relationWithRelations != null && relRef != null && relationToKeep != relationWithRelations) {
            for (Relation rel : relRef) {
                Relation newRel = new Relation(rel);
                for (int i = 0; i < newRel.getMembers().size(); ++i) {
                    RelationMember m = newRel.getMember(i);
                    if (relationWithRelations.equals(m.getMember())) {
                        newRel.setMember(i, new RelationMember(m.getRole(), relationToKeep));
                    }
                }
                commands.add(new ChangeCommand(rel, newRel));
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
            || testError.getCode() == SAME_RELATION) return false;

        // We fix it only if there is no more than one relation that is relation member.
        Collection<? extends OsmPrimitive> sel = testError.getPrimitives();
        Set<Relation> rels = new HashSet<>();

        for (OsmPrimitive osm : sel) {
            if (osm instanceof Relation) {
                rels.add((Relation) osm);
            }
        }

        if (rels.size() < 2)
            return false;

        int relationsWithRelations = 0;
        for (Relation w : rels) {
            List<Relation> rel = OsmPrimitive.getFilteredList(w.getReferrers(), Relation.class);
            if (!rel.isEmpty()) {
                ++relationsWithRelations;
            }
        }
        return relationsWithRelations <= 1;
    }
}
