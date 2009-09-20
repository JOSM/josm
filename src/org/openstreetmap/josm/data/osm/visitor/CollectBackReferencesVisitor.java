// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.data.osm.visitor;

import java.util.Collection;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;

/**
 * Helper that collect all ways a node is part of.
 *
 * Deleted objects are not collected.
 *
 * @author imi, Petr Dlouhý
 */
public class CollectBackReferencesVisitor extends AbstractVisitor {

    private final DataSet ds;
    private final boolean indirectRefs;

    private Collection<OsmPrimitive> data = new HashSet<OsmPrimitive>();
    private Map<OsmPrimitive, Collection<OsmPrimitive>> lookupTable = new HashMap<OsmPrimitive, Collection<OsmPrimitive>>();


    /**
     * Construct a back reference counter.
     * has time complexity O(n) - so it is appropriate not to call it in cycle
     * @param ds The dataset to operate on.
     */
    public CollectBackReferencesVisitor(DataSet ds) {
       this(ds, true);
    }

    /**
     * Construct a back reference counter.
     * has time complexity O(n) - so it is appropriate not to call it in cycle
     * @param ds The dataset to operate on.
     * @param indirectRefs Make also indirect references?
     */
    public CollectBackReferencesVisitor(DataSet ds, boolean indirectRefs) {
       this.ds = ds;
       this.indirectRefs = indirectRefs;
       if(ds != null)
          makeLookupTable();
    }
    
    private void makeLookupTable(){
       for (Way w : ds.ways) {
          for (Node n : w.getNodes()) {
             if(!lookupTable.containsKey(n)) lookupTable.put(n, new HashSet<OsmPrimitive>());
             lookupTable.get(n).add(w);
          }
       }
       for (Relation r : ds.relations) {
          for (RelationMember m : r.getMembers()) {
             OsmPrimitive o = m.getMember();
             if(!lookupTable.containsKey(o)) lookupTable.put(o, new HashSet<OsmPrimitive>());
             lookupTable.get(o).add(r);
          }
       }
    }

    /**
     * Get the result collection
     */
    public Collection<OsmPrimitive> getData(){
       return data;
    }

    /**
     * Initialize data before associated visit calls 
     */
    public void initialize(){
       data = new HashSet<OsmPrimitive>();
    }

    public void visit(OsmPrimitive o) {
       if(lookupTable.containsKey(o)){
          Collection<OsmPrimitive> c = lookupTable.get(o);
          Collection<OsmPrimitive> oldData = new HashSet<OsmPrimitive>(data);
          data.addAll(c);
          if(indirectRefs)
             for(OsmPrimitive oo : c)
                if(!oldData.contains(oo))
                   visit(oo);
       }
    }
    
    public void visit(Node n) {
       visit((OsmPrimitive)n);
    }

    public void visit(Way w) {
       visit((OsmPrimitive)w);
    }

    public void visit(Relation r) {
       visit((OsmPrimitive)r);
    }
}
