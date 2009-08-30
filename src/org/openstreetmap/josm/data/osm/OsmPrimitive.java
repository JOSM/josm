// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.data.osm;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.visitor.Visitor;
import org.openstreetmap.josm.gui.mappaint.ElemStyle;


/**
 * An OSM primitive can be associated with a key/value pair. It can be created, deleted
 * and updated within the OSM-Server.
 *
 * Although OsmPrimitive is designed as a base class, it is not to be meant to subclass
 * it by any other than from the package {@link org.openstreetmap.josm.data.osm}. The available primitives are a fixed set that are given
 * by the server environment and not an extendible data stuff.
 *
 * @author imi
 */
abstract public class OsmPrimitive implements Comparable<OsmPrimitive> {

    /* mappaint data */
    public ElemStyle mappaintStyle = null;
    public Integer mappaintVisibleCode = 0;
    public Integer mappaintDrawnCode = 0;
    public Collection<String> errors;

    public void putError(String text, Boolean isError)
    {
        if(errors == null) {
            errors = new ArrayList<String>();
        }
        String s = isError ? tr("Error: {0}", text) : tr("Warning: {0}", text);
        errors.add(s);
    }
    public void clearErrors()
    {
        errors = null;
    }
    /* This should not be called from outside. Fixing the UI to add relevant
       get/set functions calling this implicitely is preferred, so we can have
       transparent cache handling in the future. */
    protected void clearCached()
    {
        mappaintVisibleCode = 0;
        mappaintDrawnCode = 0;
        mappaintStyle = null;
    }
    /* end of mappaint data */

    /**
     * Unique identifier in OSM. This is used to identify objects on the server.
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
     * <code>true</code> if the object has been modified since it was loaded from
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
     * (they will have visible=false).
     */
    public boolean visible = true;

    /**
     * User that last modified this primitive, as specified by the server.
     * Never changed by JOSM.
     */
    public User user = null;

    /**
     * If set to true, this object is currently selected.
     */
    @Deprecated
    public volatile boolean selected = false;

    /**
     * Sets whether this primitive is selected or not.
     * 
     * @param selected  true, if this primitive is selected; false, otherwise
     * @since 1899
     */
    public void setSelected(boolean selected) {
        this.selected = selected;
    }
    /**
     * Replies true, if this primitive is selected.
     * 
     * @return true, if this primitive is selected
     * @since 1899
     */
    public boolean isSelected() {
        return selected;
    }

    /**
     * If set to true, this object is highlighted. Currently this is only used to
     * show which ways/nodes will connect
     */
    public volatile boolean highlighted = false;

    private int timestamp;

    public void setTimestamp(Date timestamp) {
        this.timestamp = (int)(timestamp.getTime() / 1000);
    }

    /**
     * Time of last modification to this object. This is not set by JOSM but
     * read from the server and delivered back to the server unmodified. It is
     * used to check against edit conflicts.
     *
     */
    public Date getTimestamp() {
        return new Date(timestamp * 1000l);
    }

    public boolean isTimestampEmpty() {
        return timestamp == 0;
    }

    /**
     * If set to true, this object is incomplete, which means only the id
     * and type is known (type is the objects instance class)
     */
    public boolean incomplete = false;

    /**
     * Contains the version number as returned by the API. Needed to
     * ensure update consistency
     */
    public int version = -1;

    private static Collection<String> uninteresting = null;
    /**
     * Contains a list of "uninteresting" keys that do not make an object
     * "tagged".
     * Initialized by checkTagged()
     */
    public static Collection<String> getUninterestingKeys() {
        if (uninteresting == null) {
            uninteresting = Main.pref.getCollection("tags.uninteresting",
                    Arrays.asList(new String[]{"source","note","comment","converted_by","created_by"}));
        }
        return uninteresting;
    }


    private static Collection<String> directionKeys = null;

    /**
     * Contains a list of direction-dependent keys that make an object
     * direction dependent.
     * Initialized by checkDirectionTagged()
     */
    public static Collection<String> getDirectionKeys() {
        if(directionKeys == null) {
            directionKeys = Main.pref.getCollection("tags.direction",
                    Arrays.asList(new String[]{"oneway","incline","incline_steep","aerialway"}));
        }
        return directionKeys;
    }

    /**
     * Implementation of the visitor scheme. Subclasses have to call the correct
     * visitor function.
     * @param visitor The visitor from which the visit() function must be called.
     */
    abstract public void visit(Visitor visitor);

    public final void delete(boolean deleted) {
        this.deleted = deleted;
        setSelected(false);
        modified = true;
    }

    /**
     * Equal, if the id (and class) is equal.
     *
     * An primitive is equal to its incomplete counter part.
     */
    @Override public boolean equals(Object obj) {
        if (id == 0) return obj == this;
        if (obj instanceof OsmPrimitive)
            return ((OsmPrimitive)obj).id == id && obj.getClass() == getClass();
        return false;
    }

    /**
     * Return the id plus the class type encoded as hashcode or super's hashcode if id is 0.
     *
     * An primitive has the same hashcode as its incomplete counterpart.
     */
    @Override public final int hashCode() {
        if (id == 0)
            return super.hashCode();
        final int[] ret = new int[1];
        Visitor v = new Visitor(){
            public void visit(Node n) { ret[0] = 0; }
            public void visit(Way w) { ret[0] = 1; }
            public void visit(Relation e) { ret[0] = 2; }
            public void visit(Changeset cs) { ret[0] = 3; }
        };
        visit(v);
        return id == 0 ? super.hashCode() : (int)(id<<2)+ret[0];
    }

    /*------------
     * Keys handling
     ------------*/

    /**
     * The key/value list for this primitive.
     *
     */
    private Map<String, String> keys;

    /**
     * Replies the map of key/value pairs. Never replies null. The map can be empty, though.
     * 
     * @return Keys of this primitive. Changes made in returned map are not mapped
     * back to the primitive, use setKeys() to modify the keys
     * @since 1924
     */
    public Map<String, String> getKeys() {
        // TODO More effective map
        // fix for #3218
        if (keys == null)
            return new HashMap<String, String>();
        else
            return new HashMap<String, String>(keys);
    }

    /**
     * Sets the keys of this primitives to the key/value pairs in <code>keys</code>.
     * If <code>keys</code> is null removes all existing key/value pairs.
     * 
     * @param keys the key/value pairs to set. If null, removes all existing key/value pairs.
     * @since 1924
     */
    public void setKeys(Map<String, String> keys) {
        if (keys == null) {
            this.keys = null;
        } else {
            this.keys = new HashMap<String, String>(keys);
        }
    }

    /**
     * Set the given value to the given key. If key is null, does nothing. If value is null,
     * removes the key and behaves like {@see #remove(String)}.
     * 
     * @param key  The key, for which the value is to be set. Can be null, does nothing in this case.
     * @param value The value for the key. If null, removes the respective key/value pair.
     * 
     * @see #remove(String)
     */
    public final void put(String key, String value) {
        if (key == null)
            return;
        else if (value == null) {
            remove(key);
        } else {
            if (keys == null) {
                keys = new HashMap<String, String>();
            }
            keys.put(key, value);
        }
        mappaintStyle = null;
    }
    /**
     * Remove the given key from the list
     * 
     * @param key  the key to be removed. Ignored, if key is null.
     */
    public final void remove(String key) {
        if (keys != null) {
            keys.remove(key);
            if (keys.isEmpty()) {
                keys = null;
            }
        }
        mappaintStyle = null;
    }

    /**
     * Removes all keys from this primitive.
     * 
     * @since 1843
     */
    public final void removeAll() {
        keys = null;
        mappaintStyle = null;
    }

    /**
     * Replies the value for key <code>key</code>. Replies null, if <code>key</code> is null.
     * Replies null, if there is no value for the given key.
     * 
     * @param key the key. Can be null, replies null in this case.
     * @return the value for key <code>key</code>.
     */
    public final String get(String key) {
        if (key == null) return null;
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
     * Replies true, if the map of key/value pairs of this primitive is not empty.
     * 
     * @return true, if the map of key/value pairs of this primitive is not empty; false
     *   otherwise
     * 
     * @since 1843
     */
    public final boolean hasKeys() {
        return keys != null && !keys.isEmpty();
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
        setSelected(osm.isSelected());
        timestamp = osm.timestamp;
        version = osm.version;
        incomplete = osm.incomplete;
        visible = osm.visible;
        clearCached();
        clearErrors();
    }

    /**
     * Replies true if this primitive and other are equal with respect to their
     * semantic attributes.
     * <ol>
     *   <li>equal id</ol>
     *   <li>both are complete or both are incomplete</li>
     *   <li>both have the same tags</li>
     * </ol>
     * @param other
     * @return true if this primitive and other are equal with respect to their
     * semantic attributes.
     */
    public boolean hasEqualSemanticAttributes(OsmPrimitive other) {
        if (id != other.id)
            return false;
        if (incomplete && ! other.incomplete || !incomplete  && other.incomplete)
            return false;
        return (keys == null ? other.keys==null : keys.equals(other.keys));
    }

    /**
     * Replies true if this primitive and other are equal with respect to their
     * technical attributes. The attributes:
     * <ol>
     *   <li>deleted</ol>
     *   <li>modified</ol>
     *   <li>timestamp</ol>
     *   <li>version</ol>
     *   <li>visible</ol>
     *   <li>user</ol>
     * </ol>
     * have to be equal
     * @param other the other primitive
     * @return true if this primitive and other are equal with respect to their
     * technical attributes
     */
    public boolean hasEqualTechnicalAttributes(OsmPrimitive other) {
        if (other == null) return false;

        return
        deleted == other.deleted
        && modified == other.modified
        && timestamp == other.timestamp
        && version == other.version
        && visible == other.visible
        && (user == null ? other.user==null : user==other.user);
    }

    /**
     * true if this object is considered "tagged". To be "tagged", an object
     * must have one or more "interesting" tags. "created_by" and "source"
     * are typically considered "uninteresting" and do not make an object
     * "tagged".
     */
    public boolean isTagged() {
        // TODO Cache value after keys are made private
        getUninterestingKeys();
        if (keys != null) {
            for (Entry<String,String> e : keys.entrySet()) {
                if (!uninteresting.contains(e.getKey()))
                    return true;
            }
        }
        return false;
    }
    /**
     * true if this object has direction dependent tags (e.g. oneway)
     */
    public boolean hasDirectionKeys() {
        // TODO Cache value after keys are made private
        getDirectionKeys();
        if (keys != null) {
            for (Entry<String,String> e : keys.entrySet()) {
                if (directionKeys.contains(e.getKey()))
                    return true;
            }
        }
        return false;
    }


    /**
     * Replies the name of this primitive. The default implementation replies the value
     * of the tag <tt>name</tt> or null, if this tag is not present.
     * 
     * @return the name of this primitive
     */
    public String getName() {
        if (get("name") != null)
            return get("name");
        return null;
    }

    /**
     * Replies the a localized name for this primitive given by the value of the tags (in this order)
     * <ul>
     *   <li>name:lang_COUNTRY_Variant  of the current locale</li>
     *   <li>name:lang_COUNTRY of the current locale</li>
     *   <li>name:lang of the current locale</li>
     *   <li>name of the current locale</li>
     * </ul>
     * 
     * null, if no such tag exists
     * 
     * @return the name of this primitive
     */
    public String getLocalName() {
        String key = "name:" + Locale.getDefault().toString();
        if (get(key) != null)
            return get(key);
        key = "name:" + Locale.getDefault().getLanguage() + "_" + Locale.getDefault().getCountry();
        if (get(key) != null)
            return get(key);
        key = "name:" + Locale.getDefault().getLanguage();
        if (get(key) != null)
            return get(key);
        return getName();
    }

    /**
     * Replies the display name of a primitive formatted by <code>formatter</code>
     * 
     * @return the display name
     */
    public abstract String getDisplayName(NameFormatter formatter);
}
