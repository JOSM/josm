// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.data.osm.visitor;

import java.util.Collection;
import java.util.HashSet;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;

/**
 * Helper that collect all ways a node is part of.
 *
 * Deleted objects are not collected.
 *
 * @author imi, Petr Dlouhý
 */
public class CollectBackReferencesVisitor extends AbstractVisitor {

    private final boolean indirectRefs;

    private Collection<OsmPrimitive> data = new HashSet<OsmPrimitive>();

    /**
     * @param ds This parameter is ignored
     */
    public CollectBackReferencesVisitor(DataSet ds) {
        this(true);
    }

    /**
     * @param ds This parameter is ignored
     * @param indirectRefs Make also indirect references?
     */
    public CollectBackReferencesVisitor(DataSet ds, boolean indirectRefs) {
        this.indirectRefs = indirectRefs;
    }

    public CollectBackReferencesVisitor(boolean indirectRefs) {
        this.indirectRefs = indirectRefs;
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
        Collection<OsmPrimitive> c = o.getReferrers();
        Collection<OsmPrimitive> oldData = new HashSet<OsmPrimitive>(data);
        data.addAll(c);
        if(indirectRefs) {
            for(OsmPrimitive oo : c)
                if(!oldData.contains(oo)) {
                    visit(oo);
                }
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
