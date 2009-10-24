// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.data.osm;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;

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
abstract public class OsmPrimitive implements Comparable<OsmPrimitive>, Tagged {

    private static final AtomicLong idCounter = new AtomicLong(0);

    static long generateUniqueId() {
        return idCounter.decrementAndGet();
    }

    private static final int FLAG_MODIFIED = 1 << 0;
    private static final int FLAG_VISIBLE  = 1 << 1;
    private static final int FLAG_DISABLED = 1 << 2;
    private static final int FLAG_DELETED  = 1 << 3;
    private static final int FLAG_FILTERED = 1 << 4;
    private static final int FLAG_SELECTED = 1 << 5;
    private static final int FLAG_HAS_DIRECTIONS = 1 << 6;
    private static final int FLAG_TAGGED = 1 << 7;

    /**
     * Replies the sub-collection of {@see OsmPrimitive}s of type <code>type</code> present in
     * another collection of {@see OsmPrimitive}s. The result collection is a list.
     *
     * If <code>list</code> is null, replies an empty list.
     *
     * @param <T>
     * @param list  the original list
     * @param type the type to filter for
     * @return the sub-list of OSM primitives of type <code>type</code>
     */
    static public <T extends OsmPrimitive>  List<T> getFilteredList(Collection<OsmPrimitive> list, Class<T> type) {
        if (list == null) return Collections.emptyList();
        List<T> ret = new LinkedList<T>();
        for(OsmPrimitive p: list) {
            if (type.isInstance(p)) {
                ret.add(type.cast(p));
            }
        }
        return ret;
    }

    /**
     * Replies the sub-collection of {@see OsmPrimitive}s of type <code>type</code> present in
     * another collection of {@see OsmPrimitive}s. The result collection is a set.
     *
     * If <code>list</code> is null, replies an empty set.
     *
     * @param <T>
     * @param list  the original collection
     * @param type the type to filter for
     * @return the sub-set of OSM primitives of type <code>type</code>
     */
    static public <T extends OsmPrimitive>  Set<T> getFilteredSet(Collection<OsmPrimitive> set, Class<T> type) {
        if (set == null) return Collections.emptySet();
        HashSet<T> ret = new HashSet<T>();
        for(OsmPrimitive p: set) {
            if (type.isInstance(p)) {
                ret.add(type.cast(p));
            }
        }
        return ret;
    }


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
     */
    private long id = 0;

    private volatile byte flags = FLAG_VISIBLE;   // visible per default

    /**
     * User that last modified this primitive, as specified by the server.
     * Never changed by JOSM.
     */
    private User user = null;

    /**
     * If set to true, this object is incomplete, which means only the id
     * and type is known (type is the objects instance class)
     */
    public boolean incomplete = false;

    /**
     * Contains the version number as returned by the API. Needed to
     * ensure update consistency
     */
    private int version = 0;

    /**
     * Creates a new primitive for the given id. If the id > 0, the primitive is marked
     * as incomplete.
     *
     * @param id the id. > 0 required
     * @param allowNegativeId Allows to set negative id. For internal use
     * @throws IllegalArgumentException thrown if id < 0 and allowNegativeId is false
     */
    protected OsmPrimitive(long id, boolean allowNegativeId) throws IllegalArgumentException {
        if (allowNegativeId) {
            this.id = id;
        } else {
            if (id < 0)
                throw new IllegalArgumentException(tr("Expected ID >= 0. Got {0}.", id));
            else if (id == 0) {
                this.id = generateUniqueId();
            } else {
                this.id = id;
            }

        }
        this.version = 0;
        this.incomplete = id > 0;
    }

    protected OsmPrimitive(PrimitiveData data) {
        version = data.getVersion();
        id = data.getId();
    }

    /* ------------------------------------------------------------------------------------ */
    /* accessors                                                                            */
    /* ------------------------------------------------------------------------------------ */
    /**
     * Sets whether this primitive is disabled or not.
     *
     * @param selected  true, if this primitive is disabled; false, otherwise
     */
    public void setDisabled(boolean disabled) {
        if (disabled) {
            flags |= FLAG_DISABLED;
        } else {
            flags &= ~FLAG_DISABLED;
        }

    }

    /**
     * Replies true, if this primitive is disabled.
     *
     * @return true, if this primitive is disabled
     */
    public boolean isDisabled() {
        return (flags & FLAG_DISABLED) != 0;
    }
    /**
     * Sets whether this primitive is filtered out or not.
     *
     * @param selected  true, if this primitive is filtered out; false, otherwise
     */
    public void setFiltered(boolean filtered) {
        if (filtered) {
            flags |= FLAG_FILTERED;
        } else {
            flags &= ~FLAG_FILTERED;
        }
    }
    /**
     * Replies true, if this primitive is filtered out.
     *
     * @return true, if this primitive is filtered out
     */
    public boolean isFiltered() {
        return (flags & FLAG_FILTERED) != 0;
    }

    /**
     * Sets whether this primitive is selected or not.
     *
     * @param selected  true, if this primitive is selected; false, otherwise
     * @since 1899
     */
    @Deprecated public void setSelected(boolean selected) {
        if (selected) {
            flags |= FLAG_SELECTED;
        } else {
            flags &= ~FLAG_SELECTED;
        }
    }
    /**
     * Replies true, if this primitive is selected.
     *
     * @return true, if this primitive is selected
     * @since 1899
     */
    @Deprecated public boolean isSelected() {
        return (flags & FLAG_SELECTED) != 0;
    }

    /**
     * Marks this primitive as being modified.
     *
     * @param modified true, if this primitive is to be modified
     */
    public void setModified(boolean modified) {
        if (modified) {
            flags |= FLAG_MODIFIED;
        } else {
            flags &= ~FLAG_MODIFIED;
        }
    }

    /**
     * Replies <code>true</code> if the object has been modified since it was loaded from
     * the server. In this case, on next upload, this object will be updated.
     *
     * Deleted objects are deleted from the server. If the objects are added (id=0),
     * the modified is ignored and the object is added to the server.
     *
     * @return <code>true</code> if the object has been modified since it was loaded from
     * the server
     */
    public boolean isModified() {
        return (flags & FLAG_MODIFIED) != 0;
    }

    /**
     * Replies <code>true</code>, if the object has been deleted.
     *
     * @return <code>true</code>, if the object has been deleted.
     * @see #setDeleted(boolean)
     */
    public boolean isDeleted() {
        return (flags & FLAG_DELETED) != 0;
    }

    /**
     * Replies <code>true</code>, if the object is usable.
     *
     * @return <code>true</code>, if the object is unusable.
     * @see #delete(boolean)
     */
    public boolean isUsable() {
        return !isDeleted() && !incomplete && !isDisabled();
    }

    /**
     * Replies true if this primitive is either unknown to the server (i.e. its id
     * is 0) or it is known to the server and it hasn't be deleted on the server.
     * Replies false, if this primitive is known on the server and has been deleted
     * on the server.
     *
     * @see #setVisible(boolean)
     */
    public boolean isVisible() {
        return (flags & FLAG_VISIBLE) != 0;
    }

    /**
     * Sets whether this primitive is visible, i.e. whether it is known on the server
     * and not deleted on the server.
     *
     * @see #isVisible()
     * @throws IllegalStateException thrown if visible is set to false on an primitive with
     * id==0
     */
    public void setVisible(boolean visible) throws IllegalStateException{
        if (isNew() && visible == false)
            throw new IllegalStateException(tr("A primitive with ID = 0 can't be invisible."));
        if (visible) {
            flags |= FLAG_VISIBLE;
        } else {
            flags &= ~FLAG_VISIBLE;
        }
    }

    /**
     * Replies the version number as returned by the API. The version is 0 if the id is 0 or
     * if this primitive is incomplete.
     *
     * @see #setVersion(int)
     */
    public long getVersion() {
        return version;
    }

    /**
     * Replies the id of this primitive.
     *
     * @return the id of this primitive.
     */
    public long getId() {
        return id >= 0?id:0;
    }

    /**
     *
     * @return Osm id if primitive already exists on the server. Unique negative value if primitive is new
     */
    public long getUniqueId() {
        return id;
    }

    /**
     *
     * @return True if primitive is new (not yet uploaded the server, id <= 0)
     */
    public boolean isNew() {
        return id <= 0;
    }

    /**
     * Sets the id and the version of this primitive if it is known to the OSM API.
     *
     * Since we know the id and its version it can't be incomplete anymore. incomplete
     * is set to false.
     *
     * @param id the id. > 0 required
     * @param version the version > 0 required
     * @throws IllegalArgumentException thrown if id <= 0
     * @throws IllegalArgumentException thrown if version <= 0
     */
    public void setOsmId(long id, int version) {
        if (id <= 0)
            throw new IllegalArgumentException(tr("ID > 0 expected. Got {0}.", id));
        if (version <= 0)
            throw new IllegalArgumentException(tr("Version > 0 expected. Got {0}.", version));
        this.id = id;
        this.version = version;
        this.incomplete = false;
    }

    /**
     * Clears the id and version known to the OSM API. The id and the version is set to 0.
     * incomplete is set to false.
     *
     * <strong>Caution</strong>: Do not use this method on primitives which are already added to a {@see DataSet}.
     * Ways and relations might already refer to the primitive and clearing the OSM ID
     * result in corrupt data.
     *
     * Here's an example use case:
     * <pre>
     *     // create a clone of an already existing node
     *     Node copy = new Node(otherExistingNode);
     *
     *     // reset the clones OSM id
     *     copy.clearOsmId();
     * </pre>
     *
     */
    public void clearOsmId() {
        this.id = generateUniqueId();
        this.version = 0;
        this.incomplete = false;
    }

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
     * If set to true, this object is highlighted. Currently this is only used to
     * show which ways/nodes will connect
     */
    public volatile boolean highlighted = false;

    private int timestamp;

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
                    Arrays.asList("oneway","incline","incline_steep","aerialway"));
        }
        return directionKeys;
    }

    /**
     * Implementation of the visitor scheme. Subclasses have to call the correct
     * visitor function.
     * @param visitor The visitor from which the visit() function must be called.
     */
    abstract public void visit(Visitor visitor);

    /**
     * Sets whether this primitive is deleted or not.
     *
     * Also marks this primitive as modified if deleted is true and sets selected to false.
     *
     * @param deleted  true, if this primitive is deleted; false, otherwise
     */
    public void setDeleted(boolean deleted) {
        if (deleted) {
            flags |= FLAG_DELETED;
        } else {
            flags &= ~FLAG_DELETED;
        }
        setModified(deleted);
        setSelected(false);
    }

    /**
     * Replies the user who has last touched this object. May be null.
     *
     * @return the user who has last touched this object. May be null.
     */
    public User getUser() {
        return user;
    }

    /**
     * Sets the user who has last touched this object.
     *
     * @param user the user
     */
    public void setUser(User user) {
        this.user = user;
    }

    /**
     * Equal, if the id (and class) is equal.
     *
     * An primitive is equal to its incomplete counter part.
     */
    @Override public boolean equals(Object obj) {
        if (isNew()) return obj == this;
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
        keysChangedImpl();
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
        keysChangedImpl();
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
        keysChangedImpl();
    }

    /**
     * Removes all keys from this primitive.
     *
     * @since 1843
     */
    public final void removeAll() {
        keys = null;
        mappaintStyle = null;
        keysChangedImpl();
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

    private void keysChangedImpl() {
        updateHasDirectionKeys();
        updateTagged();
    }

    /**
     * Get and write all attributes from the parameter. Does not fire any listener, so
     * use this only in the data initializing phase
     */
    public void cloneFrom(OsmPrimitive osm) {
        keys = osm.keys == null ? null : new HashMap<String, String>(osm.keys);
        id = osm.id;
        timestamp = osm.timestamp;
        version = osm.version;
        incomplete = osm.incomplete;
        flags = osm.flags;
        user= osm.user;
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
        if (!isNew() &&  id != other.id)
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
        isDeleted() == other.isDeleted()
        && isModified() == other.isModified()
        && timestamp == other.timestamp
        && version == other.version
        && isVisible() == other.isVisible()
        && (user == null ? other.user==null : user==other.user);
    }

    private void updateTagged() {
        getUninterestingKeys();
        if (keys != null) {
            for (Entry<String,String> e : keys.entrySet()) {
                if (!uninteresting.contains(e.getKey())) {
                    flags |= FLAG_TAGGED;
                    return;
                }
            }
        }
        flags &= ~FLAG_TAGGED;
    }

    /**
     * true if this object is considered "tagged". To be "tagged", an object
     * must have one or more "interesting" tags. "created_by" and "source"
     * are typically considered "uninteresting" and do not make an object
     * "tagged".
     */
    public boolean isTagged() {
        return (flags & FLAG_TAGGED) != 0;
    }

    private void updateHasDirectionKeys() {
        getDirectionKeys();
        if (keys != null) {
            for (Entry<String,String> e : keys.entrySet()) {
                if (directionKeys.contains(e.getKey())) {
                    flags |= FLAG_HAS_DIRECTIONS;
                    return;
                }
            }
        }
        flags &= ~FLAG_HAS_DIRECTIONS;
    }
    /**
     * true if this object has direction dependent tags (e.g. oneway)
     */
    public boolean hasDirectionKeys() {
        return (flags & FLAG_HAS_DIRECTIONS) != 0;
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

    /**
     * Loads (clone) this primitive from provided PrimitiveData
     * @param data
     * @param dataSet Dataset this primitive is part of. This parameter is used only
     * temporarily. OsmPrimitive will have final field dataset in future
     */
    public void load(PrimitiveData data, DataSet dataSet) {
        setKeys(data.getKeys());
        timestamp = data.getTimestamp();
        user = data.getUser();
        setDeleted(data.isDeleted());
        setModified(data.isModified());
        setVisible(data.isVisible());
    }

    /**
     * Save parameters of this primitive to the transport object
     * @return
     */
    public abstract PrimitiveData save();

    protected void saveCommonAttributes(PrimitiveData data) {
        data.setId(id);
        data.getKeys().clear();
        data.getKeys().putAll(getKeys());
        data.setTimestamp(timestamp);
        data.setUser(user);
        data.setDeleted(isDeleted());
        data.setModified(isModified());
        data.setVisible(isVisible());
    }

}

