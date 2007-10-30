// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.data.osm;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

import org.openstreetmap.josm.data.osm.visitor.Visitor;
import org.openstreetmap.josm.tools.DateParser;


/**
 * An OSM primitive can be associated with a key/value pair. It can be created, deleted
 * and updated within the OSM-Server.
 *
 * Although OsmPrimitive is designed as a base class, it is not to be meant to subclass
 * it by any other than from the package org.openstreetmap.josm.data.osm (thus the
 * visibility of the constructor). The available primitives are a fixed set that are given
 * by the server environment and not an extendible data stuff. 
 *
 * @author imi
 */
abstract public class OsmPrimitive implements Comparable<OsmPrimitive> {

	/**
	 * The key/value list for this primitive.
	 */
	public Map<String, String> keys;

	/**
	 * Unique identifier in OSM. This is used to reidentify objects in the server.
	 * An id of 0 means an unknown id. The object has not been uploaded yet to 
	 * know what id it will get.
	 * 
	 * Do not write to this attribute except you know exactly what you are doing.
	 * More specific, it is not good to set this to 0 and think the object is now
	 * new to the server! To create a new object, call the default constructor of
	 * the respective class.
	 */
	public long id = 0;

	/**
	 * <code>true</code>, if the object has been modified since it was loaded from
	 * the server. In this case, on next upload, this object will be updated.
	 * Deleted objects are deleted from the server. If the objects are added (id=0),
	 * the modified is ignored and the object is added to the server. 
	 */
	public boolean modified = false;

	/**
	 * <code>true</code>, if the object has been deleted.
	 */
	public boolean deleted = false;

	/**
	 * Visibility status as specified by the server. The visible attribute was
	 * introduced with the 0.4 API to be able to communicate deleted objects
	 * (they will have visible=false). Currently JOSM does never deal with
	 * these, so this is really for future use only.
	 */
	public boolean visible = true;
	
	/** 
	 * User that last modified this primitive, as specified by the server.
	 * Never changed by JOSM.
	 */
	public User user = null;
	
	/**
	 * <code>true</code>, if the object has been shown. This property is not used
	 * internally by JOSM, but can be used by plugins that take over the object
	 * rendering.
	 */
	public volatile boolean shown = false;

	/**
	 * true if this object is considered "tagged". To be "tagged", an object
	 * must have one or more "non-standard" tags. "created_by" and "source"
	 * are typically considered "standard" tags and do not make an object 
	 * "tagged".
	 */
	public boolean tagged = false;
	
	/**
	 * If set to true, this object is currently selected.
	 */
	public volatile boolean selected = false;

	/**
	 * Time of last modification to this object. This is not set by JOSM but
	 * read from the server and delivered back to the server unmodified. It is
	 * used to check against edit conflicts.
	 */
	public String timestamp = null;
	
	/**
	 * The timestamp is only parsed when this is really necessary, and this 
	 * is the cache for the result.
	 */
	public Date parsedTimestamp = null;
	
	/**
	 * If set to true, this object is incomplete, which means only the id
	 * and type is known (type is the objects instance class)
	 */
	public boolean incomplete = false; 

	/**
	 * Contains a list of "uninteresting" keys that do not make an object
	 * "tagged".
	 */
	public static Collection<String> uninteresting = 
		new HashSet<String>(Arrays.asList(new String[] {"source", "note", "created_by"}));
	
	/**
	 * Implementation of the visitor scheme. Subclases have to call the correct
	 * visitor function.
	 * @param visitor The visitor from which the visit() function must be called.
	 */
	abstract public void visit(Visitor visitor);

	public final void delete(boolean deleted) {
		this.deleted = deleted;
		selected = false;
		modified = true;
	}
	
	/**
	 * Returns the timestamp for this object, or the current time if none is set.
	 * Internally, parses the timestamp from XML into a Date object and caches it
	 * for possible repeated calls.
	 */
	public Date getTimestamp() {
		if (parsedTimestamp == null) {
			try {
				parsedTimestamp = DateParser.parse(timestamp);
			} catch (ParseException ex) {
				parsedTimestamp = new Date();
			}
		}
		return parsedTimestamp;
 	}

	/**
	 * Equal, if the id (and class) is equal. If both ids are 0, use the super classes
	 * equal instead.
	 * 
	 * An primitive is equal to its incomplete counter part.
	 */
	@Override public final boolean equals(Object obj) {
		if (obj == null || getClass() != obj.getClass() || id == 0 || ((OsmPrimitive)obj).id == 0)
			return super.equals(obj);
		return id == ((OsmPrimitive)obj).id;
	}

	/**
	 * Return the id plus the class type encoded as hashcode or supers hashcode if id is 0.
	 * 
	 * An primitive has the same hashcode as its incomplete counter part.
	 */
	@Override public final int hashCode() {
		if (id == 0)
			return super.hashCode();
		final int[] ret = new int[1];
		Visitor v = new Visitor(){
			public void visit(Node n) { ret[0] = 1; }
			public void visit(Way w) { ret[0] = 2; }
			public void visit(Relation e) { ret[0] = 3; }
		};
		visit(v);
		return id == 0 ? super.hashCode() : (int)(id<<2)+ret[0];
	}

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
		checkTagged();
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
		checkTagged();
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

	/**
	 * Get and write all attributes from the parameter. Does not fire any listener, so
	 * use this only in the data initializing phase
	 */
	public void cloneFrom(OsmPrimitive osm) {
		keys = osm.keys == null ? null : new HashMap<String, String>(osm.keys);
		id = osm.id;
		modified = osm.modified;
		deleted = osm.deleted;
		selected = osm.selected;
		timestamp = osm.timestamp;
		tagged = osm.tagged;
		incomplete = osm.incomplete;
	}

	/**
	 * Perform an equality compare for all non-volatile fields not only for the id 
	 * but for the whole object (for conflict resolving)
	 * @param semanticOnly if <code>true</code>, modified flag and timestamp are not compared
	 */
	public boolean realEqual(OsmPrimitive osm, boolean semanticOnly) {
		return
			id == osm.id &&
			incomplete == osm.incomplete &&
			(semanticOnly || (modified == osm.modified)) && 
			deleted == osm.deleted &&
			(semanticOnly || (timestamp == null ? osm.timestamp==null : timestamp.equals(osm.timestamp))) &&
			(semanticOnly || (user == null ? osm.user==null : user==osm.user)) &&
			(semanticOnly || (visible == osm.visible)) &&
			(keys == null ? osm.keys==null : keys.equals(osm.keys));
	}
	
	public String getTimeStr() {
		return timestamp == null ? null : new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(timestamp);
	}
	
	/**
	 * Updates the "tagged" flag. "keys" property should probably be made private
	 * to make sure this gets called when keys are set.
	 */
	public void checkTagged() {
		tagged = false;
		if (keys != null) {
			for (Entry<String,String> e : keys.entrySet()) {
				if (!uninteresting.contains(e.getKey())) {
					tagged = true;
					break;
				}
			}
		}
	}
	
}
