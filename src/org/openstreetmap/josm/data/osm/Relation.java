package org.openstreetmap.josm.data.osm;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.util.ArrayList;
import java.util.List;

import org.openstreetmap.josm.data.osm.visitor.Visitor;

/**
 * An relation, having a set of tags and any number (0...n) of members.
 * 
 * @author Frederik Ramm <frederik@remote.org>
 */
public final class Relation extends OsmPrimitive {
	
	/**
	 * All members of this relation. Note that after changing this,
	 * makeBackReferences and/or removeBackReferences should be called.
	 */
	public final List<RelationMember> members = new ArrayList<RelationMember>();

	@Override public void visit(Visitor visitor) {
		visitor.visit(this);
	}

	/**
	 * Create an identical clone of the argument (including the id)
	 */
	public Relation(Relation clone) {
		cloneFrom(clone);
	}
	
	/**
	 * Create an incomplete Relation.
	 */
	public Relation(long id) {
		this.id = id;
		incomplete = true;
	}
	
	/** 
	 * Create an empty Relation. Use this only if you set meaningful values
	 * afterwards.
	 */
	public Relation() {	
	}
	
	@Override public void cloneFrom(OsmPrimitive osm) {
		super.cloneFrom(osm);
		members.clear();
		// we must not add the members themselves, but instead
		// add clones of the members
		for (RelationMember em : ((Relation)osm).members) {
			members.add(new RelationMember(em));
		}
	}

	@Override public String toString() {
		// return "{Relation id="+id+" version="+version+" members="+Arrays.toString(members.toArray())+"}";
		// adding members in string increases memory usage a lot and overflows for looped relations
		return "{Relation id="+id+" version="+version+"}";
	}

	@Override public boolean realEqual(OsmPrimitive osm, boolean semanticOnly) {
		return osm instanceof Relation ? super.realEqual(osm, semanticOnly) && members.equals(((Relation)osm).members) : false;
	}

	public int compareTo(OsmPrimitive o) {
	    return o instanceof Relation ? Long.valueOf(id).compareTo(o.id) : -1;
	}

	public String getName() {
		String name;
		if (incomplete) {
			name = tr("incomplete");
		} else {
			name = get("type");
			// FIXME add names of members
			if (name == null)
				name = tr("relation");
			
			name += " (";
			String nameTag = get("name");
			if (nameTag == null) nameTag = get("ref");
			if (nameTag == null) nameTag = get("note");
			if (nameTag != null) name += "\"" + nameTag + "\", ";
			int mbno = members.size();
			name += trn("{0} member", "{0} members", mbno, mbno) + ")";
		}
		return name;
	}

	public boolean isIncomplete() {
		for (RelationMember m : members)
			if (m.member == null)
				return true;
		return false;
	}
}
