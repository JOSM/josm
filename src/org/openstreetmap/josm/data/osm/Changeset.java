// License: GPL. Copyright 2007 by Martijn van Oosterhout and others
package org.openstreetmap.josm.data.osm;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.io.XmlWriter;
import org.openstreetmap.josm.io.XmlWriter.OsmWriterInterface;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Map.Entry;
import java.util.HashMap;
import java.util.Collections;
import java.util.Collection;



/**
 * Represents a single changeset in JOSM. For now its only used during
 * upload but in the future we may do more.
 *
 */
public final class Changeset /*extends OsmPrimitive*/ implements OsmWriterInterface {
	/**
	 * The key/value list for this primitive.
	 */
	public Map<String, String> keys;

	public long id = 0;
	
	/** 
	 * User that created thos changeset, as specified by the server.
	 * Never changed by JOSM.
	 */
	public User user = null;
	
	/**
	 * Time of last modification to this object. This is not set by JOSM but
	 * read from the server and delivered back to the server unmodified. 
	 */
	public String end_timestamp = null;
	
	/**
	 * Time of first modification to this object. This is not set by JOSM but
	 * read from the server and delivered back to the server unmodified.
	 */
	public String start_timestamp = null;

        private void addTags(PrintWriter out) {
                if (this.keys != null) {
                        for (Entry<String, String> e : this.keys.entrySet())
                                out.println("    <tag k='"+ XmlWriter.encode(e.getKey()) +
                                                "' v='"+XmlWriter.encode(e.getValue())+ "' />");
                }
        }

	public final void header(PrintWriter out) {
        	out.print("<osm version='");
                out.print(Main.pref.get("osm-server.version", "0.6"));
                out.println("' generator='JOSM'>");
	}
	public final void write(PrintWriter out) {
		out.print("  <changeset");
		if (id != 0)
			out.print(" id="+id);
		if (this.user != null) {
			out.print(" user='"+XmlWriter.encode(this.user.name)+"'");
		}
		out.println(">\n");
		addTags( out );
		out.println("  </changeset>");
	}
	public final void footer(PrintWriter out) {
		out.println("</osm>");
	}
	
	/******************************************************
	 * This tag stuff is copied from OsmPrimitive. Perhaps a changeset
	 * really is a primitive, but it's not right now. Perhaps it should
	 * be...
	 ******************************************************/
	 
	/**
	 * Set the given value to the given key
	 * @param key The key, for which the value is to be set.
	 * @param value The value for the key.
	 */
	public final void put(String key, String value) {
		if (value == null)
			remove(key);
		else {
			if (keys == null)
				keys = new HashMap<String, String>();
			keys.put(key, value);
		}
	}
	/**
	 * Remove the given key from the list.
	 */
	public final void remove(String key) {
		if (keys != null) {
			keys.remove(key);
			if (keys.isEmpty())
				keys = null;
		}
	}

	public final String get(String key) {
		return keys == null ? null : keys.get(key);
	}

	public final Collection<Entry<String, String>> entrySet() {
		if (keys == null)
			return Collections.emptyList();
		return keys.entrySet();
	}

	public final Collection<String> keySet() {
		if (keys == null)
			return Collections.emptyList();
		return keys.keySet();
	}
}
