// License: GPL. See LICENSE file for details.
package org.openstreetmap.josm.data.validation.tests;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
        private String role;

        /** Type of the relation member */
        private OsmPrimitiveType type;

        /** Tags of the relation member */
        private Map<String, String> tags;

        /** Coordinates of the relation member */
        private List<LatLon> coor;

        /** ID of the relation member in case it is a {@link Relation} */
        private long relId;

        @Override
        public int hashCode() {
            return role.hashCode()+(int)relId+tags.hashCode()+type.hashCode()+coor.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof RelMember)) return false;
            RelMember rm = (RelMember) obj;
            return rm.role.equals(role) && rm.type.equals(type) && rm.relId==relId && rm.tags.equals(tags) && rm.coor.equals(coor);
        }

        /** Extract and store relation information based on the relation member
         * @param src The relation member to store information about
         */
        public RelMember(RelationMember src) {
            role = src.getRole();
            type = src.getType();
            relId = 0;
            coor = new ArrayList<LatLon>();

            if (src.isNode()) {
                Node r = src.getNode();
                tags = r.getKeys();
                coor = new ArrayList<LatLon>(1);
                coor.add(r.getCoor());
            }
            if (src.isWay()) {
                Way r = src.getWay();
                tags = r.getKeys();
                List<Node> wNodes = r.getNodes();
                coor = new ArrayList<LatLon>(wNodes.size());
                for (Node wNode : wNodes) {
                    coor.add(wNode.getCoor());
                }
            }
            if (src.isRelation()) {
                Relation r = src.getRelation();
                tags = r.getKeys();
                relId = r.getId();
                coor = new ArrayList<LatLon>();
            }
        }
    }

    /**
     * Class to store relation members
     */
    private class RelationMembers {
        /** List of member objects of the relation */
        private List<RelMember> members;

        /** Store relation information
         * @param members The list of relation members
         */
        public RelationMembers(List<RelationMember> members) {
            this.members = new ArrayList<RelMember>(members.size());
            for (RelationMember member : members) {
                this.members.add(new RelMember(member));
            }
        }

        @Override
        public int hashCode() {
            return members.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof RelationMembers)) return false;
            RelationMembers rm = (RelationMembers) obj;
            return rm.members.equals(members);
        }
    }

    /**
     * Class to store relation data (keys are usually cleanup and may not be equal to original relation)
     */
    private class RelationPair {
        /** Member objects of the relation */
        private RelationMembers members;
        /** Tags of the relation */
        private Map<String, String> keys;

        /** Store relation information
         * @param members The list of relation members
         * @param keys The set of tags of the relation
         */
        public RelationPair(List<RelationMember> members, Map<String, String> keys) {
            this.members = new RelationMembers(members);
            this.keys = keys;
        }

        @Override
        public int hashCode() {
            return members.hashCode()+keys.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof RelationPair)) return false;
            RelationPair rp = (RelationPair) obj;
            return rp.members.equals(members) && rp.keys.equals(keys);
        }
    }

    /** Code number of completely duplicated relation error */
    protected static final int DUPLICATE_RELATION = 1901;

    /** Code number of relation with same members error */
    protected static final int SAME_RELATION = 1902;

    /** MultiMap of all relations */
    private MultiMap<RelationPair, OsmPrimitive> relations;

    /** MultiMap of all relations, regardless of keys */
    private MultiMap<List<RelationMember>, OsmPrimitive> relations_nokeys;

    /** List of keys without useful information */
    private final Set<String> ignoreKeys = new HashSet<String>(OsmPrimitive.getUninterestingKeys());

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
        relations = new MultiMap<RelationPair, OsmPrimitive>(1000);
        relations_nokeys = new MultiMap<List<RelationMember>, OsmPrimitive>(1000);
    }

    @Override
    public void endTest() {
        super.endTest();
        for (Set<OsmPrimitive> duplicated : relations.values()) {
            if (duplicated.size() > 1) {
                TestError testError = new TestError(this, Severity.ERROR, tr("Duplicated relations"), DUPLICATE_RELATION, duplicated);
                errors.add( testError );
            }
        }
        relations = null;
        for (Set<OsmPrimitive> duplicated : relations_nokeys.values()) {
            if (duplicated.size() > 1) {
                TestError testError = new TestError(this, Severity.WARNING, tr("Relations with same members"), SAME_RELATION, duplicated);
                errors.add( testError );
            }
        }
        relations_nokeys = null;
    }

    @Override
    public void visit(Relation r) {
        if (!r.isUsable() || r.hasIncompleteMembers())
            return;
        List<RelationMember> rMembers = r.getMembers();
        Map<String, String> rkeys = r.getKeys();
        for (String key : ignoreKeys)
            rkeys.remove(key);
        RelationPair rKey = new RelationPair(rMembers, rkeys);
        relations.put(rKey, r);
        relations_nokeys.put(rMembers, r);
    }

    /**
     * Fix the error by removing all but one instance of duplicate relations
     * @param testError The error to fix, must be of type {@link #DUPLICATE_RELATION}
     */
    @Override
    public Command fixError(TestError testError) {
        if (testError.getCode() == SAME_RELATION) return null;
        Collection<? extends OsmPrimitive> sel = testError.getPrimitives();
        HashSet<Relation> relFix = new HashSet<Relation>();

        for (OsmPrimitive osm : sel)
            if (osm instanceof Relation && !osm.isDeleted()) {
                relFix.add((Relation)osm);
            }

        if (relFix.size() < 2)
            return null;

        long idToKeep = 0;
        Relation relationToKeep = relFix.iterator().next();
        // Only one relation will be kept - the one with lowest positive ID, if such exist
        // or one "at random" if no such exists. Rest of the relations will be deleted
        for (Relation w: relFix) {
            if (!w.isNew() && (idToKeep == 0 || w.getId() < idToKeep)) {
                idToKeep = w.getId();
                relationToKeep = w;
            }
        }

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
        }

        Collection<Command> commands = new LinkedList<Command>();

        // Fix relations.
        if (relationWithRelations != null && relationToKeep != relationWithRelations) {
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

        //Delete all relations in the list
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
        HashSet<Relation> relations = new HashSet<Relation>();

        for (OsmPrimitive osm : sel)
            if (osm instanceof Relation) {
                relations.add((Relation)osm);
            }

        if (relations.size() < 2)
            return false;

        int relationsWithRelations = 0;
        for (Relation w : relations) {
            List<Relation> rel = OsmPrimitive.getFilteredList(w.getReferrers(), Relation.class);
            if (!rel.isEmpty()) {
                ++relationsWithRelations;
            }
        }
        return (relationsWithRelations <= 1);
    }
}
