// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.io;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map.Entry;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.DataSource;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.Visitor;

/**
 * Save the dataset into a stream as osm intern xml format. This is not using any
 * xml library for storing.
 * @author imi
 */
public class OsmWriter extends XmlWriter implements Visitor {

	/**
	 * The counter for new created objects. Starting at -1 and goes down.
	 */
	private long newIdCounter = -1;
	/**
	 * All newly created ids and their primitive that uses it. This is a back reference
	 * map to allow references to use the correnct primitives.
	 */
	private HashMap<OsmPrimitive, Long> usedNewIds = new HashMap<OsmPrimitive, Long>();

	private final boolean osmConform;
	private final Changeset changeset;

	public abstract static class Osm implements OsmWriterInterface {
		public void header(PrintWriter out) {
			out.print("<osm version='");
			out.print(Main.pref.get("osm-server.version", "0.5"));
			out.println("' generator='JOSM'>");
		}
		public void footer(PrintWriter out) {
			out.println("</osm>");
		}
	}
	
	// simple helper to write the object's class to the out stream
	private Visitor typeWriteVisitor = new Visitor() {
		public void visit(Node n) { out.print("node"); }
		public void visit(Way w) { out.print("way"); }
		public void visit(Relation e) { out.print("relation"); }
	};
	
	/**
	 * An output writer for function output that writes everything of the given dataset into
	 * the xml
	 */
	public static final class All extends Osm {
		private final DataSet ds;
		private final boolean osmConform;

		/**
		 * Construct an writer function
		 * @param osmConform <code>true</code>, if the xml should be 100% osm conform. In this
		 * 		case, not all information can be retrieved later (as example, modified state
		 * 		is lost and id's remain 0 instead of decrementing from -1)
		 */
		public All(DataSet ds, boolean osmConform) {
			this.ds = ds;
			this.osmConform = osmConform;
		}

		public void write(PrintWriter out) {
			Visitor writer = new OsmWriter(out, osmConform, null);
			for (Node n : ds.nodes)
				if (shouldWrite(n))
					writer.visit(n);
			for (Way w : ds.ways)
				if (shouldWrite(w))
					writer.visit(w);
			for (Relation e : ds.relations)
				if (shouldWrite(e))
					writer.visit(e);
        }

		private boolean shouldWrite(OsmPrimitive osm) {
	        return osm.id != 0 || !osm.deleted;
        }

		@Override public void header(PrintWriter out) {
	        super.header(out);
			for (DataSource s : ds.dataSources) {
                // TODO: remove <bound> output after a grace period (1st October 08)
				out.print("  <bound note='this tag is deprecated and only provided for backward compatiblity' box='"+
						s.bounds.min.lat()+","+
						s.bounds.min.lon()+","+
						s.bounds.max.lat()+","+
						s.bounds.max.lon()+"' ");
				out.println("origin='"+XmlWriter.encode(s.origin)+"' />");
				out.print("  <bounds minlat='" + 
						s.bounds.min.lat()+"' minlon='"+
						s.bounds.min.lon()+"' maxlat='"+
						s.bounds.max.lat()+"' maxlon='"+
						s.bounds.max.lon()+"' ");
				out.println("origin='"+XmlWriter.encode(s.origin)+"' />");
			}
        }
	}

	/**
	 * An output writer for functino output that writes only one specific primitive into
	 * the xml
	 */
	public static final class Single extends Osm {
		private final OsmPrimitive osm;
		private final boolean osmConform;
		private final Changeset changeset;

		public Single(OsmPrimitive osm, boolean osmConform, Changeset changeset) {
			this.osm = osm;
			this.osmConform = osmConform;
			this.changeset = changeset;
		}

		public void write(PrintWriter out) {
			osm.visit(new OsmWriter(out, osmConform, changeset));
        }
	}

	private OsmWriter(PrintWriter out, boolean osmConform, Changeset changeset) {
		super(out);
		this.osmConform = osmConform;
		this.changeset = changeset;
	}

	public void visit(Node n) {
		if (n.incomplete) return;
		addCommon(n, "node");
		out.print(" lat='"+n.coor.lat()+"' lon='"+n.coor.lon()+"'");
		addTags(n, "node", true);
	}

	public void visit(Way w) {
		if (w.incomplete) return;
		addCommon(w, "way");
		out.println(">");
		for (Node n : w.nodes)
			out.println("    <nd ref='"+getUsedId(n)+"' />");
		addTags(w, "way", false);
	}

	public void visit(Relation e) {
		if (e.incomplete) return;
		addCommon(e, "relation");
		out.println(">");
		for (RelationMember em : e.members) {
			out.print("    <member type='");
			em.member.visit(typeWriteVisitor);
			out.println("' ref='"+getUsedId(em.member)+"' role='" + 
				XmlWriter.encode(em.role) + "' />");
		}
		addTags(e, "relation", false);
	}
	

	/**
	 * Return the id for the given osm primitive (may access the usedId map)
	 */
	private long getUsedId(OsmPrimitive osm) {
		if (osm.id != 0)
			return osm.id;
		if (usedNewIds.containsKey(osm))
			return usedNewIds.get(osm);
		usedNewIds.put(osm, newIdCounter);
		return osmConform ? 0 : newIdCounter--;
	}


	private void addTags(OsmPrimitive osm, String tagname, boolean tagOpen) {
		if (osm.keys != null) {
			if (tagOpen)
				out.println(">");
			for (Entry<String, String> e : osm.keys.entrySet())
				out.println("    <tag k='"+ XmlWriter.encode(e.getKey()) +
						"' v='"+XmlWriter.encode(e.getValue())+ "' />");
			out.println("  </" + tagname + ">");
		} else if (tagOpen)
			out.println(" />");
		else
			out.println("  </" + tagname + ">");
	}

	/**
	 * Add the common part as the form of the tag as well as the XML attributes
	 * id, action, user, and visible.
	 */
	private void addCommon(OsmPrimitive osm, String tagname) {
		out.print("  <"+tagname+" id='"+getUsedId(osm)+"'");
		if (!osmConform) {
			String action = null;
			if (osm.deleted)
				action = "delete";
			else if (osm.modified)
				action = "modify";
			if (action != null)
				out.print(" action='"+action+"'");
		}
		if (osm.timestamp != null) {
			out.print(" timestamp='"+osm.timestamp+"'");
		}
		// user and visible added with 0.4 API
		if (osm.user != null) {
			out.print(" user='"+XmlWriter.encode(osm.user.name)+"'");
		}
		out.print(" visible='"+osm.visible+"'");
		if( osm.version != -1 )
			out.print( " old_version='"+osm.version+"'");
		if( this.changeset != null && this.changeset.id != 0)
			out.print( " changeset='"+this.changeset.id+"'" );
	}
}
