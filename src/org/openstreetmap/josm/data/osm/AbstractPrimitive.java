// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.openstreetmap.josm.tools.LanguageInfo;
import org.openstreetmap.josm.tools.Utils;

/**
* Abstract class to represent common features of the datatypes primitives.
*
* @since 4099
*/
public abstract class AbstractPrimitive implements IPrimitive {

    private static final AtomicLong idCounter = new AtomicLong(0);

    /**
     * Generates a new primitive unique id.
     * @return new primitive unique (negative) id
     */
    static long generateUniqueId() {
        return idCounter.decrementAndGet();
    }

    /**
     * Returns the current primitive unique id.
     * @return the current primitive unique (negative) id (last generated)
     * @since 12536
     */
    public static long currentUniqueId() {
        return idCounter.get();
    }

    /**
     * Advances the current primitive unique id to skip a range of values.
     * @param newId new unique id
     * @throws IllegalArgumentException if newId is greater than current unique id
     * @since 12536
     */
    public static void advanceUniqueId(long newId) {
        if (newId > currentUniqueId()) {
            throw new IllegalArgumentException("Cannot modify the id counter backwards");
        }
        idCounter.set(newId);
    }

    /**
     * This flag shows, that the properties have been changed by the user
     * and on upload the object will be send to the server.
     */
    protected static final short FLAG_MODIFIED = 1 << 0;

    /**
     * This flag is false, if the object is marked
     * as deleted on the server.
     */
    protected static final short FLAG_VISIBLE = 1 << 1;

    /**
     * An object that was deleted by the user.
     * Deleted objects are usually hidden on the map and a request
     * for deletion will be send to the server on upload.
     * An object usually cannot be deleted if it has non-deleted
     * objects still referring to it.
     */
    protected static final short FLAG_DELETED = 1 << 2;

    /**
     * A primitive is incomplete if we know its id and type, but nothing more.
     * Typically some members of a relation are incomplete until they are
     * fetched from the server.
     */
    protected static final short FLAG_INCOMPLETE = 1 << 3;

    /**
     * An object can be disabled by the filter mechanism.
     * Then it will show in a shade of gray on the map or it is completely
     * hidden from the view.
     * Disabled objects usually cannot be selected or modified
     * while the filter is active.
     */
    protected static final short FLAG_DISABLED = 1 << 4;

    /**
     * This flag is only relevant if an object is disabled by the
     * filter mechanism (i.e.&nbsp;FLAG_DISABLED is set).
     * Then it indicates, whether it is completely hidden or
     * just shown in gray color.
     *
     * When the primitive is not disabled, this flag should be
     * unset as well (for efficient access).
     */
    protected static final short FLAG_HIDE_IF_DISABLED = 1 << 5;

    /**
     * Flag used internally by the filter mechanism.
     */
    protected static final short FLAG_DISABLED_TYPE = 1 << 6;

    /**
     * Flag used internally by the filter mechanism.
     */
    protected static final short FLAG_HIDDEN_TYPE = 1 << 7;

    /**
     * This flag is set if the primitive is a way and
     * according to the tags, the direction of the way is important.
     * (e.g. one way street.)
     */
    protected static final short FLAG_HAS_DIRECTIONS = 1 << 8;

    /**
     * If the primitive is tagged.
     * Some trivial tags like source=* are ignored here.
     */
    protected static final short FLAG_TAGGED = 1 << 9;

    /**
     * This flag is only relevant if FLAG_HAS_DIRECTIONS is set.
     * It shows, that direction of the arrows should be reversed.
     * (E.g. oneway=-1.)
     */
    protected static final short FLAG_DIRECTION_REVERSED = 1 << 10;

    /**
     * When hovering over ways and nodes in add mode, the
     * "target" objects are visually highlighted. This flag indicates
     * that the primitive is currently highlighted.
     */
    protected static final short FLAG_HIGHLIGHTED = 1 << 11;

    /**
     * If the primitive is annotated with a tag such as note, fixme, etc.
     * Match the "work in progress" tags in default map style.
     */
    protected static final short FLAG_ANNOTATED = 1 << 12;

    /**
     * Determines if the primitive is preserved from the filter mechanism.
     */
    protected static final short FLAG_PRESERVED = 1 << 13;

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
    protected long id;

    /**
     * User that last modified this primitive, as specified by the server.
     * Never changed by JOSM.
     */
    protected User user;

    /**
     * Contains the version number as returned by the API. Needed to
     * ensure update consistency
     */
    protected int version;

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
        if (id <= 0) {
            // reset version and changeset id
            version = 0;
            changesetId = 0;
        }
        timestamp = other.timestamp;
        if (id > 0) {
            version = other.version;
        }
        flags = other.flags;
        user = other.user;
        if (id > 0 && other.changesetId > 0) {
            // #4208: sometimes we cloned from other with id < 0 *and*
            // an assigned changeset id. Don't know why yet. For primitives
            // with id < 0 we don't propagate the changeset id any more.
            //
            setChangesetId(other.changesetId);
        }
    }

    @Override
    public int getVersion() {
        return version;
    }

    @Override
    public long getId() {
        long id = this.id;
        return id >= 0 ? id : 0;
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
     * Determines if this primitive is new.
     * @return {@code true} if this primitive is new (not yet uploaded the server, id &lt;= 0)
     */
    @Override
    public boolean isNew() {
        return id <= 0;
    }

    @Override
    public boolean isNewOrUndeleted() {
        return isNew() || ((flags & (FLAG_VISIBLE + FLAG_DELETED)) == 0);
    }

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

    @Override
    public User getUser() {
        return user;
    }

    @Override
    public void setUser(User user) {
        this.user = user;
    }

    @Override
    public int getChangesetId() {
        return changesetId;
    }

    @Override
    public void setChangesetId(int changesetId) {
        if (this.changesetId == changesetId)
            return;
        if (changesetId < 0)
            throw new IllegalArgumentException(MessageFormat.format("Parameter ''{0}'' >= 0 expected, got {1}", "changesetId", changesetId));
        if (changesetId > 0 && isNew())
            throw new IllegalStateException(tr("Cannot assign a changesetId > 0 to a new primitive. Value of changesetId is {0}", changesetId));

        this.changesetId = changesetId;
    }

    @Override
    public PrimitiveId getPrimitiveId() {
        return new SimplePrimitiveId(getUniqueId(), getType());
    }

    @Override
    public void setTimestamp(Date timestamp) {
        this.timestamp = (int) TimeUnit.MILLISECONDS.toSeconds(timestamp.getTime());
    }

    @Override
    public void setRawTimestamp(int timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public Date getTimestamp() {
        return new Date(TimeUnit.SECONDS.toMillis(timestamp));
    }

    @Override
    public int getRawTimestamp() {
        return timestamp;
    }

    @Override
    public boolean isTimestampEmpty() {
        return timestamp == 0;
    }

    /* -------
    /* FLAGS
    /* ------*/

    protected void updateFlags(short flag, boolean value) {
        if (value) {
            flags |= flag;
        } else {
            flags &= (short) ~flag;
        }
    }

    @Override
    public void setModified(boolean modified) {
        updateFlags(FLAG_MODIFIED, modified);
    }

    @Override
    public boolean isModified() {
        return (flags & FLAG_MODIFIED) != 0;
    }

    @Override
    public boolean isDeleted() {
        return (flags & FLAG_DELETED) != 0;
    }

    @Override
    public boolean isUndeleted() {
        return (flags & (FLAG_VISIBLE + FLAG_DELETED)) == 0;
    }

    @Override
    public boolean isUsable() {
        return (flags & (FLAG_DELETED + FLAG_INCOMPLETE)) == 0;
    }

    @Override
    public boolean isVisible() {
        return (flags & FLAG_VISIBLE) != 0;
    }

    @Override
    public void setVisible(boolean visible) {
        if (!visible && isNew())
            throw new IllegalStateException(tr("A primitive with ID = 0 cannot be invisible."));
        updateFlags(FLAG_VISIBLE, visible);
    }

    @Override
    public void setDeleted(boolean deleted) {
        updateFlags(FLAG_DELETED, deleted);
        setModified(deleted ^ !isVisible());
    }

    /**
     * If set to true, this object is incomplete, which means only the id
     * and type is known (type is the objects instance class)
     * @param incomplete incomplete flag value
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
            builder.append('I');
        }
        if (isModified()) {
            builder.append('M');
        }
        if (isVisible()) {
            builder.append('V');
        }
        if (isDeleted()) {
            builder.append('D');
        }
        return builder.toString();
    }

    /*------------
     * Keys handling
     ------------*/

    /**
     * The key/value list for this primitive.
     * <p>
     * Note that the keys field is synchronized using RCU.
     * Writes to it are not synchronized by this object, the writers have to synchronize writes themselves.
     * <p>
     * In short this means that you should not rely on this variable being the same value when read again and your should always
     * copy it on writes.
     * <p>
     * Further reading:
     * <ul>
     * <li>{@link java.util.concurrent.CopyOnWriteArrayList}</li>
     * <li> <a href="http://stackoverflow.com/questions/2950871/how-can-copyonwritearraylist-be-thread-safe">
     *     http://stackoverflow.com/questions/2950871/how-can-copyonwritearraylist-be-thread-safe</a></li>
     * <li> <a href="https://en.wikipedia.org/wiki/Read-copy-update">
     *     https://en.wikipedia.org/wiki/Read-copy-update</a> (mind that we have a Garbage collector,
     *     {@code rcu_assign_pointer} and {@code rcu_dereference} are ensured by the {@code volatile} keyword)</li>
     * </ul>
     */
    protected volatile String[] keys;

    /**
     * Replies the map of key/value pairs. Never replies null. The map can be empty, though.
     *
     * @return tags of this primitive. Changes made in returned map are not mapped
     * back to the primitive, use setKeys() to modify the keys
     * @see #visitKeys(KeyValueVisitor)
     */
    @Override
    public TagMap getKeys() {
        return new TagMap(keys);
    }

    /**
     * Calls the visitor for every key/value pair of this primitive.
     *
     * @param visitor The visitor to call.
     * @see #getKeys()
     * @since 8742
     */
    public void visitKeys(KeyValueVisitor visitor) {
        final String[] keys = this.keys;
        if (keys != null) {
            for (int i = 0; i < keys.length; i += 2) {
                visitor.visitKeyValue(this, keys[i], keys[i + 1]);
            }
        }
    }

    /**
     * Sets the keys of this primitives to the key/value pairs in <code>keys</code>.
     * Old key/value pairs are removed.
     * If <code>keys</code> is null, clears existing key/value pairs.
     * <p>
     * Note that this method, like all methods that modify keys, is not synchronized and may lead to data corruption when being used
     * from multiple threads.
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
     * Copy the keys from a TagMap.
     * @param keys The new key map.
     */
    public void setKeys(TagMap keys) {
        Map<String, String> originalKeys = getKeys();
        if (keys == null) {
            this.keys = null;
        } else {
            String[] arr = keys.getTagsArray();
            if (arr.length == 0) {
                this.keys = null;
            } else {
                this.keys = arr;
            }
        }
        keysChangedImpl(originalKeys);
    }

    /**
     * Set the given value to the given key. If key is null, does nothing. If value is null,
     * removes the key and behaves like {@link #remove(String)}.
     * <p>
     * Note that this method, like all methods that modify keys, is not synchronized and may lead to data corruption when being used
     * from multiple threads.
     *
     * @param key  The key, for which the value is to be set. Can be null or empty, does nothing in this case.
     * @param value The value for the key. If null, removes the respective key/value pair.
     *
     * @see #remove(String)
     */
    @Override
    public void put(String key, String value) {
        Map<String, String> originalKeys = getKeys();
        if (key == null || Utils.isStripEmpty(key))
            return;
        else if (value == null) {
            remove(key);
        } else if (keys == null) {
            keys = new String[] {key, value};
            keysChangedImpl(originalKeys);
        } else {
            int keyIndex = indexOfKey(keys, key);
            int tagArrayLength = keys.length;
            if (keyIndex < 0) {
                keyIndex = tagArrayLength;
                tagArrayLength += 2;
            }

            // Do not try to optimize this array creation if the key already exists.
            // We would need to convert the keys array to be an AtomicReferenceArray
            // Or we would at least need a volatile write after the array was modified to
            // ensure that changes are visible by other threads.
            String[] newKeys = Arrays.copyOf(keys, tagArrayLength);
            newKeys[keyIndex] = key;
            newKeys[keyIndex + 1] = value;
            keys = newKeys;
            keysChangedImpl(originalKeys);
        }
    }

    /**
     * Scans a key/value array for a given key.
     * @param keys The key array. It is not modified. It may be null to indicate an emtpy array.
     * @param key The key to search for.
     * @return The position of that key in the keys array - which is always a multiple of 2 - or -1 if it was not found.
     */
    private static int indexOfKey(String[] keys, String key) {
        if (keys == null) {
            return -1;
        }
        for (int i = 0; i < keys.length; i += 2) {
            if (keys[i].equals(key)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Remove the given key from the list
     * <p>
     * Note that this method, like all methods that modify keys, is not synchronized and may lead to data corruption when being used
     * from multiple threads.
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
        int j = 0;
        for (int i = 0; i < keys.length; i += 2) {
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
     * <p>
     * Note that this method, like all methods that modify keys, is not synchronized and may lead to data corruption when being used
     * from multiple threads.
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
        for (int i = 0; i < keys.length; i += 2) {
            if (keys[i].equals(key)) return keys[i+1];
        }
        return null;
    }

    /**
     * Returns true if the {@code key} corresponds to an OSM true value.
     * @param key OSM key
     * @return {@code true} if the {@code key} corresponds to an OSM true value
     * @see OsmUtils#isTrue(String)
     */
    public final boolean isKeyTrue(String key) {
        return OsmUtils.isTrue(get(key));
    }

    /**
     * Returns true if the {@code key} corresponds to an OSM false value.
     * @param key OSM key
     * @return {@code true} if the {@code key} corresponds to an OSM false value
     * @see OsmUtils#isFalse(String)
     */
    public final boolean isKeyFalse(String key) {
        return OsmUtils.isFalse(get(key));
    }

    /**
     * Gets a key ignoring the case of the key
     * @param key The key to get
     * @return The value for a key that matches the given key ignoring case.
     */
    public final String getIgnoreCase(String key) {
        String[] keys = this.keys;
        if (key == null)
            return null;
        if (keys == null)
            return null;
        for (int i = 0; i < keys.length; i += 2) {
            if (keys[i].equalsIgnoreCase(key)) return keys[i+1];
        }
        return null;
    }

    /**
     * Gets the number of keys
     * @return The number of keys set for this primitive.
     */
    public final int getNumKeys() {
        String[] keys = this.keys;
        return keys == null ? 0 : keys.length / 2;
    }

    @Override
    public final Collection<String> keySet() {
        final String[] keys = this.keys;
        if (keys == null) {
            return Collections.emptySet();
        }
        if (keys.length == 1) {
            return Collections.singleton(keys[0]);
        }

        final Set<String> result = new HashSet<>(Utils.hashMapInitialCapacity(keys.length / 2));
        for (int i = 0; i < keys.length; i += 2) {
            result.add(keys[i]);
        }
        return result;
    }

    /**
     * Replies true, if the map of key/value pairs of this primitive is not empty.
     *
     * @return true, if the map of key/value pairs of this primitive is not empty; false otherwise
     */
    @Override
    public final boolean hasKeys() {
        return keys != null;
    }

    /**
     * Replies true if this primitive has a tag with key <code>key</code>.
     *
     * @param key the key
     * @return true, if this primitive has a tag with key <code>key</code>
     */
    @Override
    public boolean hasKey(String key) {
        return key != null && indexOfKey(keys, key) >= 0;
    }

    /**
     * Replies true if this primitive has a tag any of the <code>keys</code>.
     *
     * @param keys the keys
     * @return true, if this primitive has a tag with any of the <code>keys</code>
     * @since 11587
     */
    public boolean hasKey(String... keys) {
        return keys != null && Arrays.stream(keys).anyMatch(this::hasKey);
    }

    /**
     * What to do, when the tags have changed by one of the tag-changing methods.
     * @param originalKeys original tags
     */
    protected abstract void keysChangedImpl(Map<String, String> originalKeys);

    @Override
    public String getName() {
        return get("name");
    }

    @Override
    public String getLocalName() {
        for (String s : LanguageInfo.getLanguageCodes(null)) {
            String val = get("name:" + s);
            if (val != null)
                return val;
        }

        return getName();
    }

    /**
     * Tests whether this primitive contains a tag consisting of {@code key} and {@code value}.
     * @param key the key forming the tag.
     * @param value value forming the tag.
     * @return true if primitive contains a tag consisting of {@code key} and {@code value}.
     */
    public boolean hasTag(String key, String value) {
        return Objects.equals(value, get(key));
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
     * @return true if primitive contains a tag consisting of {@code key} and any of {@code values}.
     */
    public boolean hasTag(String key, Collection<String> values) {
        return values.contains(get(key));
    }

    /**
     * Tests whether this primitive contains a tag consisting of {@code key} and a value different from {@code value}.
     * @param key the key forming the tag.
     * @param value value not forming the tag.
     * @return true if primitive contains a tag consisting of {@code key} and a value different from {@code value}.
     * @since 11608
     */
    public boolean hasTagDifferent(String key, String value) {
        String v = get(key);
        return v != null && !v.equals(value);
    }

    /**
     * Tests whether this primitive contains a tag consisting of {@code key} and none of {@code values}.
     * @param key the key forming the tag.
     * @param values one or many values forming the tag.
     * @return true if primitive contains a tag consisting of {@code key} and none of {@code values}.
     * @since 11608
     */
    public boolean hasTagDifferent(String key, String... values) {
        return hasTagDifferent(key, Arrays.asList(values));
    }

    /**
     * Tests whether this primitive contains a tag consisting of {@code key} and none of {@code values}.
     * @param key the key forming the tag.
     * @param values one or many values forming the tag.
     * @return true if primitive contains a tag consisting of {@code key} and none of {@code values}.
     * @since 11608
     */
    public boolean hasTagDifferent(String key, Collection<String> values) {
        String v = get(key);
        return v != null && !values.contains(v);
    }
}
