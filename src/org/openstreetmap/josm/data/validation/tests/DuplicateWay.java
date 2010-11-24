// License: GPL. See LICENSE file for details.
package org.openstreetmap.josm.data.validation.tests;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.DeleteCommand;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.Test;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.data.validation.util.Bag;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;

/**
 * Tests if there are duplicate ways
 */
public class DuplicateWay extends Test
{

    private static class WayPair {
        public List<LatLon> coor;
        public Map<String, String> keys;
        public WayPair(List<LatLon> _coor,Map<String, String> _keys) {
            coor=_coor;
            keys=_keys;
        }
        @Override
        public int hashCode() {
            return coor.hashCode()+keys.hashCode();
        }
        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof WayPair)) return false;
            WayPair wp = (WayPair) obj;
            return wp.coor.equals(coor) && wp.keys.equals(keys);
        }
    }

    protected static int DUPLICATE_WAY = 1401;

    /** Bag of all ways */
    Bag<WayPair, OsmPrimitive> ways;

    /**
     * Constructor
     */
    public DuplicateWay()
    {
        super(tr("Duplicated ways")+".",
              tr("This test checks that there are no ways with same tags and same node coordinates."));
    }


    @Override
    public void startTest(ProgressMonitor monitor)
    {
        super.startTest(monitor);
        ways = new Bag<WayPair, OsmPrimitive>(1000);
    }

    @Override
    public void endTest()
    {
        super.endTest();
        for(List<OsmPrimitive> duplicated : ways.values() )
        {
            if( duplicated.size() > 1)
            {
                TestError testError = new TestError(this, Severity.ERROR, tr("Duplicated ways"), DUPLICATE_WAY, duplicated);
                errors.add( testError );
            }
        }
        ways = null;
    }

    @Override
    public void visit(Way w)
    {
        if( !w.isUsable() )
            return;
        List<Node> wNodes=w.getNodes();
        Vector<LatLon> wLat=new Vector<LatLon>(wNodes.size());
        for(int i=0;i<wNodes.size();i++) {
                 wLat.add(wNodes.get(i).getCoor());
        }
        Map<String, String> wkeys=w.getKeys();
        wkeys.remove("created_by");
        WayPair wKey=new WayPair(wLat,wkeys);
        ways.add(wKey, w);
    }

    /**
     * Fix the error by removing all but one instance of duplicate ways
     */
    @Override
    public Command fixError(TestError testError)
    {
        Collection<? extends OsmPrimitive> sel = testError.getPrimitives();
        HashSet<Way> ways = new HashSet<Way>();

        for (OsmPrimitive osm : sel)
            if (osm instanceof Way)
                ways.add((Way)osm);

        if( ways.size() < 2 )
            return null;

        long idToKeep = 0;
        Way wayToKeep = ways.iterator().next();
        // Only one way will be kept - the one with lowest positive ID, if such exist
        // or one "at random" if no such exists. Rest of the ways will be deleted
        for (Way w: ways) {
            if (!w.isNew()) {
                if (idToKeep == 0 || w.getId() < idToKeep) {
                    idToKeep = w.getId();
                    wayToKeep = w;
                }
            }
        }

        // Find the way that is member of one or more relations. (If any)
        Way wayWithRelations = null;
        List<Relation> relations = null;
        for (Way w : ways) {
            List<Relation> rel = OsmPrimitive.getFilteredList(w.getReferrers(), Relation.class);
            if (!rel.isEmpty()) {
                if (wayWithRelations != null)
                    throw new AssertionError("Cannot fix duplicate Ways: More than one way is relation member.");
                wayWithRelations = w;
                relations = rel;
            }
        }

        Collection<Command> commands = new LinkedList<Command>();

        // Fix relations.
        if (wayWithRelations != null && wayToKeep != wayWithRelations) {
            for (Relation rel : relations) {
                Relation newRel = new Relation(rel);
                for (int i = 0; i < newRel.getMembers().size(); ++i) {
                    RelationMember m = newRel.getMember(i);
                    if (wayWithRelations.equals(m.getMember())) {
                        newRel.setMember(i, new RelationMember(m.getRole(), wayToKeep));
                    }
                }
                commands.add(new ChangeCommand(rel, newRel));
            }
        }

        //Delete all ways in the list
        //Note: nodes are not deleted, these can be detected and deleted at next pass
        ways.remove(wayToKeep);
        commands.add(new DeleteCommand(ways));
        return new SequenceCommand(tr("Delete duplicate ways"), commands);
    }

    @Override
    public boolean isFixable(TestError testError)
    {
        if (!(testError.getTester() instanceof DuplicateWay))
            return false;

        // We fix it only if there is no more than one way that is relation member.
        Collection<? extends OsmPrimitive> sel = testError.getPrimitives();
        HashSet<Way> ways = new HashSet<Way>();

        for (OsmPrimitive osm : sel)
            if (osm instanceof Way)
                ways.add((Way)osm);

        if (ways.size() < 2)
            return false;

        int waysWithRelations = 0;
        for (Way w : ways) {
            List<Relation> rel = OsmPrimitive.getFilteredList(w.getReferrers(), Relation.class);
            if (!rel.isEmpty()) {
                ++waysWithRelations;
            }
        }
        return (waysWithRelations <= 1);
    }
}
