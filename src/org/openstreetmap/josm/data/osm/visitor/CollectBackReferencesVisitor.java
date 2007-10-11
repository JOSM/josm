// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.data.osm.visitor;

import java.util.Collection;
import java.util.HashSet;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;

/**
 * Helper that collect all ways a node is part of.
 * 
 * Deleted objects are not collected.
 * 
 * @author imi
 */
public class CollectBackReferencesVisitor implements Visitor {

	private final DataSet ds;
	private final boolean indirectRefs;

	/**
	 * The result list of primitives stored here.
	 */
	public final Collection<OsmPrimitive> data = new HashSet<OsmPrimitive>();


	/**
	 * Construct a back reference counter.
	 * @param ds The dataset to operate on.
	 */
	public CollectBackReferencesVisitor(DataSet ds) {
		this.ds = ds;
		this.indirectRefs = true;
	}

	public CollectBackReferencesVisitor(DataSet ds, boolean indirectRefs) {
		this.ds = ds;
		this.indirectRefs = indirectRefs;
	}

	public void visit(Node n) {
		for (Way w : ds.ways) {
			if (w.deleted)
				continue;
			for (Node n2 : w.nodes) {
				if (n == n2) {
					data.add(w);
					if (indirectRefs) {
						visit(w);
					}
				}
			}
		}
		checkRelationMembership(n);
	}
	
	public void visit(Way w) {
		checkRelationMembership(w);
	}
	
	public void visit(Relation r) {
		checkRelationMembership(r);
	}
	
	private void checkRelationMembership(OsmPrimitive p) {
		// FIXME - this might be a candidate for optimisation 
		// if OSM primitives are made to hold a list of back
		// references.
		for (Relation r : ds.relations) {
			for (RelationMember m : r.members) {
				if (m.member == p) {
					if (!data.contains(r)) {
						data.add(r);
						if (indirectRefs) {
							// move up the tree (there might be relations
							// referring to this relation)
							checkRelationMembership(r);
						}
					}
					break;
				}
			}
		}
	}
}
