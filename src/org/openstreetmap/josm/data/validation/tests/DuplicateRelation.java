// License: GPL. See LICENSE file for details.
package org.openstreetmap.josm.data.validation.tests;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.openstreetmap.josm.Main;
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
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.Test;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.tools.MultiMap;
/**
 * Tests if there are duplicate relations
 */
public class DuplicateRelation extends Test
{

    public class RelMember {
        private String role;
        private OsmPrimitiveType type;
        private Map<String, String> tags;
        private List<LatLon> coor;
        private long rel_id;

        @Override
        public int hashCode() {
            return role.hashCode()+(int)rel_id+tags.hashCode()+type.hashCode()+coor.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof RelMember)) return false;
            RelMember rm = (RelMember) obj;
            return rm.role.equals(role) && rm.type.equals(type) && rm.rel_id==rel_id && rm.tags.equals(tags) && rm.coor.equals(coor);
        }

        public RelMember(RelationMember src) {
            role=src.getRole();
            type=src.getType();
            rel_id=0;
            coor=new ArrayList<LatLon>();

            if (src.isNode()) {
                Node r=src.getNode();
                tags=r.getKeys();
                coor=new ArrayList<LatLon>(1);
                coor.add(r.getCoor());
            }
            if (src.isWay()) {
                Way r=src.getWay();
                tags=r.getKeys();
                List<Node> wNodes = r.getNodes();
                coor=new ArrayList<LatLon>(wNodes.size());
                for (int i=0;i<wNodes.size();i++) {
                    coor.add(wNodes.get(i).getCoor());
                }
            }
            if (src.isRelation()) {
                Relation r=src.getRelation();
                tags=r.getKeys();
                rel_id=r.getId();
                coor=new ArrayList<LatLon>();
            }
        }
    }

    private class RelationMembers {
        public List<RelMember> members;
        public RelationMembers(List<RelationMember> _members) {
            members=new ArrayList<RelMember>(_members.size());
            for (int i=0;i<_members.size();i++) {
                members.add(new RelMember(_members.get(i)));
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

    private class RelationPair {
        public RelationMembers members;
        public Map<String, String> keys;
        public RelationPair(List<RelationMember> _members,Map<String, String> _keys) {
            members=new RelationMembers(_members);
            keys=_keys;
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

    protected static int DUPLICATE_RELATION = 1901;
    protected static int SAME_RELATION = 1902;

    /** MultiMap of all relations */
    MultiMap<RelationPair, OsmPrimitive> relations;

    /** MultiMap of all relations, regardless of keys */
    MultiMap<List<RelationMember>, OsmPrimitive> relations_nokeys;

    /**
     * Constructor
     */
    public DuplicateRelation()
    {
        super(tr("Duplicated relations")+".",
              tr("This test checks that there are no relations with same tags and same members with same roles."));
    }


    @Override
    public void startTest(ProgressMonitor monitor)
    {
    	super.startTest(monitor);
        relations = new MultiMap<RelationPair, OsmPrimitive>(1000);
        relations_nokeys = new MultiMap<List<RelationMember>, OsmPrimitive>(1000);
    }

    @Override
    public void endTest()
    {
    	super.endTest();
        for(LinkedHashSet<OsmPrimitive> duplicated : relations.values() )
        {
            if( duplicated.size() > 1)
            {
                TestError testError = new TestError(this, Severity.ERROR, tr("Duplicated relations"), DUPLICATE_RELATION, duplicated);
                errors.add( testError );
            }
        }
        relations = null;
        for(LinkedHashSet<OsmPrimitive> duplicated : relations_nokeys.values() )
        {
            if( duplicated.size() > 1)
            {
                TestError testError = new TestError(this, Severity.WARNING, tr("Relations with same members"), SAME_RELATION, duplicated);
                errors.add( testError );
            }
        }
        relations_nokeys = null;
    }

    @Override
    public void visit(Relation r)
    {
        if( !r.isUsable() )
            return;
        List<RelationMember> rMembers=r.getMembers();
        Map<String, String> rkeys=r.getKeys();
        rkeys.remove("created_by");
        RelationPair rKey=new RelationPair(rMembers,rkeys);
        relations.put(rKey, r);
        relations_nokeys.put(rMembers, r);
    }

    /**
     * Fix the error by removing all but one instance of duplicate relations
     */
    @Override
    public Command fixError(TestError testError)
    {
        if (testError.getCode() == SAME_RELATION) return null;
        Collection<? extends OsmPrimitive> sel = testError.getPrimitives();
        HashSet<Relation> rel_fix = new HashSet<Relation>();

        for (OsmPrimitive osm : sel)
            if (osm instanceof Relation)
                rel_fix.add((Relation)osm);

        if( rel_fix.size() < 2 )
            return null;

        long idToKeep = 0;
        Relation relationToKeep = rel_fix.iterator().next();
        // Only one relation will be kept - the one with lowest positive ID, if such exist
        // or one "at random" if no such exists. Rest of the relations will be deleted
        for (Relation w: rel_fix) {
            if (!w.isNew()) {
                if (idToKeep == 0 || w.getId() < idToKeep) {
                    idToKeep = w.getId();
                    relationToKeep = w;
                }
            }
        }

        // Find the relation that is member of one or more relations. (If any)
        Relation relationWithRelations = null;
        List<Relation> rel_ref = null;
        for (Relation w : rel_fix) {
            List<Relation> rel = OsmPrimitive.getFilteredList(w.getReferrers(), Relation.class);
            if (!rel.isEmpty()) {
                if (relationWithRelations != null)
                    throw new AssertionError("Cannot fix duplicate relations: More than one relation is member of another relation.");
                relationWithRelations = w;
                rel_ref = rel;
            }
        }

        Collection<Command> commands = new LinkedList<Command>();

        // Fix relations.
        if (relationWithRelations != null && relationToKeep != relationWithRelations) {
            for (Relation rel : rel_ref) {
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
        rel_fix.remove(relationToKeep);
        commands.add(new DeleteCommand(rel_fix));
        return new SequenceCommand(tr("Delete duplicate relations"), commands);
    }

    @Override
    public boolean isFixable(TestError testError)
    {
        if (!(testError.getTester() instanceof DuplicateRelation))
            return false;

        if (testError.getCode() == SAME_RELATION) return false;

        // We fix it only if there is no more than one relation that is relation member.
        Collection<? extends OsmPrimitive> sel = testError.getPrimitives();
        HashSet<Relation> relations = new HashSet<Relation>();

        for (OsmPrimitive osm : sel)
            if (osm instanceof Relation)
                relations.add((Relation)osm);

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
