// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.openstreetmap.josm.tools.Utils;

/**
* Abstract class to represent common features of the datatypes primitives.
*
* @since 4099
*/
public abstract class AbstractPrimitive implements IPrimitive {

    private static final AtomicLong idCounter = new AtomicLong(0);

    static long generateUniqueId() {
        return idCounter.decrementAndGet();
    }

    /**
     * This flag shows, that the properties have been changed by the user
     * and on upload the object will be send to the server.
     */
    protected static final int FLAG_MODIFIED = 1 << 0;

    /**
     * This flag is false, if the object is marked
     * as deleted on the server.
     */
    protected static final int FLAG_VISIBLE  = 1 << 1;

    /**
     * An object that was deleted by the user.
     * Deleted objects are usually hidden on the map and a request
     * for deletion will be send to the server on upload.
     * An object usually cannot be deleted if it has non-deleted
     * objects still referring to it.
     */
    protected static final int FLAG_DELETED  = 1 << 2;

    /**
     * A primitive is incomplete if we know its id and type, but nothing more.
     * Typically some members of a relation are incomplete until they are
     * fetched from the server.
     */
    protected static final int FLAG_INCOMPLETE = 1 << 3;

    /**
     * Put several boolean flags to one short int field to save memory.
     * Other bits of this field are used in subclasses.
     */
    protected volatile short flags = FLAG_VISIBLE;   // visible per default

    /*-------------------
     * OTHER PROPERTIES
     *-------------------*/

    /**
     * Unique identifier in OSM. This is used to identify objects on the server.
     * An id of 0 means an unknown id. The object has not been uploaded yet to
     * know what id it will get.
     */
    protected long id = 0;

    /**
     * User that last modified this primitive, as specified by the server.
     * Never changed by JOSM.
     */
    protected User user = null;

    /**
     * Contains the version number as returned by the API. Needed to
     * ensure update consistency
     */
    protected int version = 0;

    /**
     * The id of the changeset this primitive was last uploaded to.
     * 0 if it wasn't uploaded to a changeset yet of if the changeset
     * id isn't known.
     */
    protected int changesetId;

    protected int timestamp;

    /**
     * Get and write all attributes from the parameter. Does not fire any listener, so
     * use this only in the data initializing phase
     * @param other the primitive to clone data from
     */
    public void cloneFrom(AbstractPrimitive other) {
        setKeys(other.getKeys());
        id = other.id;
        if (id <=0) {
            // reset version and changeset id
            version = 0;
            changesetId = 0;
        }
        timestamp = other.timestamp;
        if (id > 0) {
            version = other.version;
        }
        flags = other.flags;
        user= other.user;
        if (id > 0 && other.changesetId > 0) {
            // #4208: sometimes we cloned from other with id < 0 *and*
            // an assigned changeset id. Don't know why yet. For primitives
            // with id < 0 we don't propagate the changeset id any more.
            //
            setChangesetId(other.changesetId);
        }
    }

    /**
     * Replies the version number as returned by the API. The version is 0 if the id is 0 or
     * if this primitive is incomplete.
     *
     * @see PrimitiveData#setVersion(int)
     */
    @Override
    public int getVersion() {
        return version;
    }

    /**
     * Replies the id of this primitive.
     *
     * @return the id of this primitive.
     */
    @Override
    public long getId() {
        long id = this.id;
        return id >= 0?id:0;
    }

    /**
     * Gets a unique id representing this object.
     *
     * @return Osm id if primitive already exists on the server. Unique negative value if primitive is new
     */
    @Override
    public long getUniqueId() {
        return id;
    }

    /**
     *
     * @return True if primitive is new (not yet uploaded the server, id &lt;= 0)
     */
    @Override
    public boolean isNew() {
        return id <= 0;
    }

    /**
     *
     * @return True if primitive is new or undeleted
     * @see #isNew()
     * @see #isUndeleted()
     */
    @Override
    public boolean isNewOrUndeleted() {
        return (id <= 0) || ((flags & (FLAG_VISIBLE + FLAG_DELETED)) == 0);
    }

    /**
     * Sets the id and the version of this primitive if it is known to the OSM API.
     *
     * Since we know the id and its version it can't be incomplete anymore. incomplete
     * is set to false.
     *
     * @param id the id. &gt; 0 required
     * @param version the version &gt; 0 required
     * @throws IllegalArgumentException thrown if id &lt;= 0
     * @throws IllegalArgumentException thrown if version &lt;= 0
     * @throws DataIntegrityProblemException If id is changed and primitive was already added to the dataset
     */
    @Override
    public void setOsmId(long id, int version) {
        if (id <= 0)
            throw new IllegalArgumentException(tr("ID > 0 expected. Got {0}.", id));
        if (version <= 0)
            throw new IllegalArgumentException(tr("Version > 0 expected. Got {0}.", version));
        this.id = id;
        this.version = version;
        this.setIncomplete(false);
    }

    /**
     * Clears the metadata, including id and version known to the OSM API.
     * The id is a new unique id. The version, changeset and timestamp are set to 0.
     * incomplete and deleted are set to false. It's preferred to use copy constructor with clearMetadata set to true instead
     * of calling this method.
     * @since 6140
     */
    public void clearOsmMetadata() {
        // Not part of dataset - no lock necessary
        this.id = generateUniqueId();
        this.version = 0;
        this.user = null;
        this.changesetId = 0; // reset changeset id on a new object
        this.timestamp = 0;
        this.setIncomplete(false);
        this.setDeleted(false);
        this.setVisible(true);
    }

    /**
     * Replies the user who has last touched this object. May be null.
     *
     * @return the user who has last touched this object. May be null.
     */
    @Override
    public User getUser() {
        return user;
    }

    /**
     * Sets the user who has last touched this object.
     *
     * @param user the user
     */
    @Override
    public void setUser(User user) {
        this.user = user;
    }

    /**
     * Replies the id of the changeset this primitive was last uploaded to.
     * 0 if this primitive wasn't uploaded to a changeset yet or if the
     * changeset isn't known.
     *
     * @return the id of the changeset this primitive was last uploaded to.
     */
    @Override
    public int getChangesetId() {
        return changesetId;
    }

    /**
     * Sets the changeset id of this primitive. Can't be set on a new
     * primitive.
     *
     * @param changesetId the id. &gt;= 0 required.
     * @throws IllegalStateException thrown if this primitive is new.
     * @throws IllegalArgumentException thrown if id &lt; 0
     */
    @Override
    public void setChangesetId(int changesetId) throws IllegalStateException, IllegalArgumentException {
        if (this.changesetId == changesetId)
            return;
        if (changesetId < 0)
            throw new IllegalArgumentException(MessageFormat.format("Parameter ''{0}'' >= 0 expected, got {1}", "changesetId", changesetId));
        if (isNew() && changesetId > 0)
            throw new IllegalStateException(tr("Cannot assign a changesetId > 0 to a new primitive. Value of changesetId is {0}", changesetId));

        this.changesetId = changesetId;
    }

    /**
     * Replies the unique primitive id for this primitive
     *
     * @return the unique primitive id for this primitive
     */
    @Override
    public PrimitiveId getPrimitiveId() {
        return new SimplePrimitiveId(getUniqueId(), getType());
    }

    public OsmPrimitiveType getDisplayType() {
        return getType();
    }

    @Override
    public void setTimestamp(Date timestamp) {
        this.timestamp = (int)(timestamp.getTime() / 1000);
    }

    /**
     * Time of last modification to this object. This is not set by JOSM but
     * read from the server and delivered back to the server unmodified. It is
     * used to check against edit conflicts.
     *
     * @return date of last modification
     */
    @Override
    public Date getTimestamp() {
        return new Date(timestamp * 1000L);
    }

    @Override
    public boolean isTimestampEmpty() {
        return timestamp == 0;
    }

    /* -------
    /* FLAGS
    /* ------*/

    protected void updateFlags(int flag, boolean value) {
        if (value) {
            flags |= flag;
        } else {
            flags &= ~flag;
        }
    }

    /**
     * Marks this primitive as being modified.
     *
     * @param modified true, if this primitive is to be modified
     */
    @Override
    public void setModified(boolean modified) {
        updateFlags(FLAG_MODIFIED, modified);
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
    @Override
    public boolean isModified() {
        return (flags & FLAG_MODIFIED) != 0;
    }

    /**
     * Replies <code>true</code>, if the object has been deleted.
     *
     * @return <code>true</code>, if the object has been deleted.
     * @see #setDeleted(boolean)
     */
    @Override
    public boolean isDeleted() {
        return (flags & FLAG_DELETED) != 0;
    }

    /**
     * Replies <code>true</code> if the object has been deleted on the server and was undeleted by the user.
     * @return <code>true</code> if the object has been undeleted
     */
    public boolean isUndeleted() {
        return (flags & (FLAG_VISIBLE + FLAG_DELETED)) == 0;
    }

    /**
     * Replies <code>true</code>, if the object is usable
     * (i.e. complete and not deleted).
     *
     * @return <code>true</code>, if the object is usable.
     * @see #setDeleted(boolean)
     */
    public boolean isUsable() {
        return (flags & (FLAG_DELETED + FLAG_INCOMPLETE)) == 0;
    }

    /**
     * Checks if object is known to the server.
     * Replies true if this primitive is either unknown to the server (i.e. its id
     * is 0) or it is known to the server and it hasn't be deleted on the server.
     * Replies false, if this primitive is known on the server and has been deleted
     * on the server.
     *
     * @return <code>true</code>, if the object is visible on server.
     * @see #setVisible(boolean)
     */
    @Override
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
    @Override
    public void setVisible(boolean visible) throws IllegalStateException{
        if (isNew() && !visible)
            throw new IllegalStateException(tr("A primitive with ID = 0 cannot be invisible."));
        updateFlags(FLAG_VISIBLE, visible);
    }

    /**
     * Sets whether this primitive is deleted or not.
     *
     * Also marks this primitive as modified if deleted is true.
     *
     * @param deleted  true, if this primitive is deleted; false, otherwise
     */
    @Override
    public void setDeleted(boolean deleted) {
        updateFlags(FLAG_DELETED, deleted);
        setModified(deleted ^ !isVisible());
    }

    /**
     * If set to true, this object is incomplete, which means only the id
     * and type is known (type is the objects instance class)
     */
    protected void setIncomplete(boolean incomplete) {
        updateFlags(FLAG_INCOMPLETE, incomplete);
    }

    @Override
    public boolean isIncomplete() {
        return (flags & FLAG_INCOMPLETE) != 0;
    }

    protected String getFlagsAsString() {
        StringBuilder builder = new StringBuilder();

        if (isIncomplete()) {
            builder.append("I");
        }
        if (isModified()) {
            builder.append("M");
        }
        if (isVisible()) {
            builder.append("V");
        }
        if (isDeleted()) {
            builder.append("D");
        }
        return builder.toString();
    }

    /*------------
     * Keys handling
     ------------*/

    // Note that all methods that read keys first make local copy of keys array reference. This is to ensure thread safety - reading
    // doesn't have to be locked so it's possible that keys array will be modified. But all write methods make copy of keys array so
    // the array itself will be never modified - only reference will be changed

    /**
     * The key/value list for this primitive.
     *
     */
    protected String[] keys;

    /**
     * Replies the map of key/value pairs. Never replies null. The map can be empty, though.
     *
     * @return tags of this primitive. Changes made in returned map are not mapped
     * back to the primitive, use setKeys() to modify the keys
     */
    @Override
    public Map<String, String> getKeys() {
        Map<String, String> result = new HashMap<String, String>();
        String[] keys = this.keys;
        if (keys != null) {
            for (int i=0; i<keys.length ; i+=2) {
                result.put(keys[i], keys[i + 1]);
            }
        }
        return result;
    }

    /**
     * Sets the keys of this primitives to the key/value pairs in <code>keys</code>.
     * Old key/value pairs are removed.
     * If <code>keys</code> is null, clears existing key/value pairs.
     *
     * @param keys the key/value pairs to set. If null, removes all existing key/value pairs.
     */
    @Override
    public void setKeys(Map<String, String> keys) {
        Map<String, String> originalKeys = getKeys();
        if (keys == null || keys.isEmpty()) {
            this.keys = null;
            keysChangedImpl(originalKeys);
            return;
        }
        String[] newKeys = new String[keys.size() * 2];
        int index = 0;
        for (Entry<String, String> entry:keys.entrySet()) {
            newKeys[index++] = entry.getKey();
            newKeys[index++] = entry.getValue();
        }
        this.keys = newKeys;
        keysChangedImpl(originalKeys);
    }

    /**
     * Set the given value to the given key. If key is null, does nothing. If value is null,
     * removes the key and behaves like {@link #remove(String)}.
     *
     * @param key  The key, for which the value is to be set. Can be null, does nothing in this case.
     * @param value The value for the key. If null, removes the respective key/value pair.
     *
     * @see #remove(String)
     */
    @Override
    public void put(String key, String value) {
        Map<String, String> originalKeys = getKeys();
        if (key == null)
            return;
        else if (value == null) {
            remove(key);
        } else if (keys == null){
            keys = new String[] {key, value};
            keysChangedImpl(originalKeys);
        } else {
            for (int i=0; i<keys.length;i+=2) {
                if (keys[i].equals(key)) {
                    keys[i+1] = value;  // This modifies the keys array but it doesn't make it invalidate for any time so its ok (see note no top)
                    keysChangedImpl(originalKeys);
                    return;
                }
            }
            String[] newKeys = new String[keys.length + 2];
            for (int i=0; i< keys.length;i+=2) {
                newKeys[i] = keys[i];
                newKeys[i+1] = keys[i+1];
            }
            newKeys[keys.length] = key;
            newKeys[keys.length + 1] = value;
            keys = newKeys;
            keysChangedImpl(originalKeys);
        }
    }

    /**
     * Remove the given key from the list
     *
     * @param key  the key to be removed. Ignored, if key is null.
     */
    @Override
    public void remove(String key) {
        if (key == null || keys == null) return;
        if (!hasKey(key))
            return;
        Map<String, String> originalKeys = getKeys();
        if (keys.length == 2) {
            keys = null;
            keysChangedImpl(originalKeys);
            return;
        }
        String[] newKeys = new String[keys.length - 2];
        int j=0;
        for (int i=0; i < keys.length; i+=2) {
            if (!keys[i].equals(key)) {
                newKeys[j++] = keys[i];
                newKeys[j++] = keys[i+1];
            }
        }
        keys = newKeys;
        keysChangedImpl(originalKeys);
    }

    /**
     * Removes all keys from this primitive.
     */
    @Override
    public void removeAll() {
        if (keys != null) {
            Map<String, String> originalKeys = getKeys();
            keys = null;
            keysChangedImpl(originalKeys);
        }
    }

    /**
     * Replies the value for key <code>key</code>. Replies null, if <code>key</code> is null.
     * Replies null, if there is no value for the given key.
     *
     * @param key the key. Can be null, replies null in this case.
     * @return the value for key <code>key</code>.
     */
    @Override
    public final String get(String key) {
        String[] keys = this.keys;
        if (key == null)
            return null;
        if (keys == null)
            return null;
        for (int i=0; i<keys.length;i+=2) {
            if (keys[i].equals(key)) return keys[i+1];
        }
        return null;
    }

    /**
     * Returns true if the {@code key} corresponds to an OSM true value.
     * @see OsmUtils#isTrue(String)
     */
    public final boolean isKeyTrue(String key) {
        return OsmUtils.isTrue(get(key));
    }

    /**
     * Returns true if the {@code key} corresponds to an OSM false value.
     * @see OsmUtils#isFalse(String)
     */
    public final boolean isKeyFalse(String key) {
        return OsmUtils.isFalse(get(key));
    }

    public final String getIgnoreCase(String key) {
        String[] keys = this.keys;
        if (key == null)
            return null;
        if (keys == null)
            return null;
        for (int i=0; i<keys.length;i+=2) {
            if (keys[i].equalsIgnoreCase(key)) return keys[i+1];
        }
        return null;
    }

    @Override
    public final Collection<String> keySet() {
        String[] keys = this.keys;
        if (keys == null)
            return Collections.emptySet();
        Set<String> result = new HashSet<String>(keys.length / 2);
        for (int i=0; i<keys.length; i+=2) {
            result.add(keys[i]);
        }
        return result;
    }

    /**
     * Replies true, if the map of key/value pairs of this primitive is not empty.
     *
     * @return true, if the map of key/value pairs of this primitive is not empty; false
     *   otherwise
     */
    @Override
    public final boolean hasKeys() {
        return keys != null;
    }

    /**
     * Replies true if this primitive has a tag with key <code>key</code>.
     *
     * @param key the key
     * @return true, if his primitive has a tag with key <code>key</code>
     */
    public boolean hasKey(String key) {
        String[] keys = this.keys;
        if (key == null) return false;
        if (keys == null) return false;
        for (int i=0; i< keys.length;i+=2) {
            if (keys[i].equals(key)) return true;
        }
        return false;
    }

    /**
     * What to do, when the tags have changed by one of the tag-changing methods.
     */
    abstract protected void keysChangedImpl(Map<String, String> originalKeys);

    /**
     * Replies the name of this primitive. The default implementation replies the value
     * of the tag <tt>name</tt> or null, if this tag is not present.
     *
     * @return the name of this primitive
     */
    @Override
    public String getName() {
        return get("name");
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
    @Override
    public String getLocalName() {
        final Locale locale = Locale.getDefault();
        String key = "name:" + locale.toString();
        String val = get(key);
        if (val != null)
            return val;

        final String language = locale.getLanguage();
        key = "name:" + language + "_" + locale.getCountry();
        val = get(key);
        if (val != null)
            return val;

        key = "name:" + language;
        val = get(key);
        if (val != null)
            return val;

        return getName();
    }

    /**
     * Tests whether this primitive contains a tag consisting of {@code key} and {@code values}.
     * @param key the key forming the tag.
     * @param value value forming the tag.
     * @return true iff primitive contains a tag consisting of {@code key} and {@code value}.
     */
    public boolean hasTag(String key, String value) {
        return Utils.equal(value, get(key));
    }

    /**
     * Tests whether this primitive contains a tag consisting of {@code key} and any of {@code values}.
     * @param key the key forming the tag.
     * @param values one or many values forming the tag.
     * @return true if primitive contains a tag consisting of {@code key} and any of {@code values}.
     */
    public boolean hasTag(String key, String... values) {
        return hasTag(key, Arrays.asList(values));
    }

    /**
     * Tests whether this primitive contains a tag consisting of {@code key} and any of {@code values}.
     * @param key the key forming the tag.
     * @param values one or many values forming the tag.
     * @return true iff primitive contains a tag consisting of {@code key} and any of {@code values}.
     */
    public boolean hasTag(String key, Collection<String> values) {
        return values.contains(get(key));
    }
}
