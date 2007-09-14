// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.io;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map.Entry;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.DataSource;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Segment;
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

	public abstract static class Osm implements OsmWriterInterface {
		public void header(PrintWriter out) {
			out.print("<osm version='");
			out.print(Main.pref.get("osm-server.version", "0.4"));
			out.println("' generator='JOSM'>");
		}
		public void footer(PrintWriter out) {
			out.println("</osm>");
		}
	}
	
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
			Visitor writer = new OsmWriter(out, osmConform);
			for (Node n : ds.nodes)
				if (shouldWrite(n))
					writer.visit(n);
			for (Segment ls : ds.segments)
				if (shouldWrite(ls))
					writer.visit(ls);
			for (Way w : ds.ways)
				if (shouldWrite(w))
					writer.visit(w);
        }

		private boolean shouldWrite(OsmPrimitive osm) {
	        return osm.id != 0 || !osm.deleted;
        }

		@Override public void header(PrintWriter out) {
	        super.header(out);
			for (DataSource s : ds.dataSources) {
				out.print("  <bound box='"+
						s.bounds.min.lat()+","+
						s.bounds.min.lon()+","+
						s.bounds.max.lat()+","+
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

		public Single(OsmPrimitive osm, boolean osmConform) {
			this.osm = osm;
			this.osmConform = osmConform;
		}

		public void write(PrintWriter out) {
			osm.visit(new OsmWriter(out, osmConform));
        }
	}

	private OsmWriter(PrintWriter out, boolean osmConform) {
		super(out);
		this.osmConform = osmConform;
	}

	public void visit(Node n) {
		addCommon(n, "node");
		out.print(" lat='"+n.coor.lat()+"' lon='"+n.coor.lon()+"'");
		addTags(n, "node", true);
	}

	public void visit(Segment ls) {
		if (ls.incomplete)
			return; // Do not write an incomplete segment
		addCommon(ls, "segment");
		out.print(" from='"+getUsedId(ls.from)+"' to='"+getUsedId(ls.to)+"'");
		addTags(ls, "segment", true);
	}

	public void visit(Way w) {
		addCommon(w, "way");
		out.println(">");
		for (Segment ls : w.segments)
			out.println("    <seg id='"+getUsedId(ls)+"' />");
		addTags(w, "way", false);
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
			String time = osm.getTimeStr();
			out.print(" timestamp='"+time+"'");
		}
		// user and visible added with 0.4 API
		if (osm.user != null) {
			out.print(" user='"+XmlWriter.encode(osm.user.name)+"'");
		}
		out.print(" visible='"+osm.visible+"'");
		
	}
}
