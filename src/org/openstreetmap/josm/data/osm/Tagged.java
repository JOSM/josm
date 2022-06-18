// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.openstreetmap.josm.tools.Utils;

/**
 * Objects implement Tagged if they provide a map of key/value pairs.
 *
 * @since 2115
 */
// FIXME: better naming? setTags(), getTags(), getKeys() instead of keySet() ?
//
public interface Tagged {

    /**
     * The maximum tag length allowed by OSM API
     * @since 13414
     */
    int MAX_TAG_LENGTH = 255;

    /**
     * Sets the map of key/value pairs
     *
     * @param keys the map of key value pairs. If null, reset to the empty map.
     */
    void setKeys(Map<String, String> keys);

    /**
     * Replies the map of key/value pairs. Never null, but may be the empty map.
     *
     * @return the map of key/value pairs
     */
    Map<String, String> getKeys();

    /**
     * Calls the visitor for every key/value pair.
     *
     * @param visitor The visitor to call.
     * @see #getKeys()
     * @since 13668
     */
    default void visitKeys(KeyValueVisitor visitor) {
        getKeys().forEach((k, v) -> visitor.visitKeyValue(this, k, v));
    }

    /**
     * Sets a key/value pairs
     *
     * @param key the key
     * @param value the value. If null, removes the key/value pair.
     */
    void put(String key, String value);

    /**
     * Sets a key/value pairs
     *
     * @param tag The tag to set.
     * @since 10736
     */
    default void put(Tag tag) {
        put(tag.getKey(), tag.getValue());
    }

    /**
     * Add all key/value pairs. This <i>may</i> be more performant than {@link #put}, depending upon the implementation.
     * By default, this calls {@link #put} for each map entry.
     * @param tags The tag map to add
     * @since 18473
     */
    default void putAll(Map<String, String> tags) {
        for (Map.Entry<String, String> entry : tags.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Replies the value of the given key; null, if there is no value for this key
     *
     * @param key the key
     * @return the value
     */
    String get(String key);

    /**
     * Removes a given key/value pair
     *
     * @param key the key
     */
    void remove(String key);

    /**
     * Replies true, if there is at least one key/value pair; false, otherwise
     *
     * @return true, if there is at least one key/value pair; false, otherwise
     */
    boolean hasKeys();

    /**
     * Replies true if there is a tag with key <code>key</code>.
     * The value could however be empty. See {@link #hasTag(String)} to check for non-empty tags.
     *
     * @param key the key
     * @return true, if there is a tag with key <code>key</code>
     * @see #hasTag(String)
     * @since 11608
     */
    default boolean hasKey(String key) {
        return get(key) != null;
    }

    /**
     * Replies true if there is a non-empty tag with key <code>key</code>.
     *
     * @param key the key
     * @return true, if there is a non-empty tag with key <code>key</code>
     * @see Tagged#hasKey(String)
     * @since 13430
     */
    default boolean hasTag(String key) {
        return !Utils.isEmpty(get(key));
    }

    /**
     * Tests whether this primitive contains a tag consisting of {@code key} and {@code value}.
     * @param key the key forming the tag.
     * @param value value forming the tag.
     * @return true if primitive contains a tag consisting of {@code key} and {@code value}.
     * @since 13668
     */
    default boolean hasTag(String key, String value) {
        return Objects.equals(value, get(key));
    }

    /**
     * Tests whether this primitive contains a tag consisting of {@code key} and any of {@code values}.
     * @param key the key forming the tag.
     * @param values one or many values forming the tag.
     * @return true if primitive contains a tag consisting of {@code key} and any of {@code values}.
     * @since 13668
     */
    default boolean hasTag(String key, String... values) {
        return hasTag(key, Arrays.asList(values));
    }

    /**
     * Tests whether this primitive contains a tag consisting of {@code key} and any of {@code values}.
     * @param key the key forming the tag.
     * @param values one or many values forming the tag.
     * @return true if primitive contains a tag consisting of {@code key} and any of {@code values}.
     * @since 13668
     */
    default boolean hasTag(String key, Collection<String> values) {
        String v = get(key);
        return v != null && values.contains(v);
    }

    /**
     * Tests whether this primitive contains a tag consisting of {@code key} and a value different from {@code value}.
     * @param key the key forming the tag.
     * @param value value not forming the tag.
     * @return true if primitive contains a tag consisting of {@code key} and a value different from {@code value}.
     * @since 13668
     */
    default boolean hasTagDifferent(String key, String value) {
        String v = get(key);
        return v != null && !v.equals(value);
    }

    /**
     * Tests whether this primitive contains a tag consisting of {@code key} and none of {@code values}.
     * @param key the key forming the tag.
     * @param values one or many values forming the tag.
     * @return true if primitive contains a tag consisting of {@code key} and none of {@code values}.
     * @since 13668
     */
    default boolean hasTagDifferent(String key, String... values) {
        return hasTagDifferent(key, Arrays.asList(values));
    }

    /**
     * Tests whether this primitive contains a tag consisting of {@code key} and none of {@code values}.
     * @param key the key forming the tag.
     * @param values one or many values forming the tag.
     * @return true if primitive contains a tag consisting of {@code key} and none of {@code values}.
     * @since 13668
     */
    default boolean hasTagDifferent(String key, Collection<String> values) {
        String v = get(key);
        return v != null && !values.contains(v);
    }

    /**
     * Replies the set of keys
     *
     * @return the set of keys
     * @see #keys()
     */
    Collection<String> keySet();

    /**
     * Replies the keys as stream
     *
     * @return the keys as stream
     * @see #keySet()
     * @since 17584
     */
    default Stream<String> keys() {
        return keySet().stream();
    }

    /**
     * Gets the number of keys
     * @return The number of keys set for this tagged object.
     * @since 13625
     */
    int getNumKeys();

    /**
     * Removes all tags
     */
    void removeAll();

    /**
     * Returns true if the {@code key} corresponds to an OSM true value.
     * @param key OSM key
     * @return {@code true} if the {@code key} corresponds to an OSM true value
     * @see OsmUtils#isTrue(String)
     */
    default boolean isKeyTrue(String key) {
        return OsmUtils.isTrue(get(key));
    }

    /**
     * Returns true if the {@code key} corresponds to an OSM false value.
     * @param key OSM key
     * @return {@code true} if the {@code key} corresponds to an OSM false value
     * @see OsmUtils#isFalse(String)
     */
    default boolean isKeyFalse(String key) {
        return OsmUtils.isFalse(get(key));
    }

    /**
     * Returns a Tagged instance for the given tag collection
     * @param tags the tag collection
     * @return a Tagged instance for the given tag collection
     */
    static Tagged ofTags(Collection<Tag> tags) {
        return ofMap(tags.stream().collect(Collectors.toMap(Tag::getKey, Tag::getValue, (a, b) -> a)));
    }

    /**
     * Returns a Tagged instance for the given tag map
     * @param tags the tag map
     * @return a Tagged instance for the given tag map
     */
    static Tagged ofMap(Map<String, String> tags) {
        return new Tagged() {

            @Override
            public String get(String key) {
                return tags.get(key);
            }

            @Override
            public Map<String, String> getKeys() {
                return tags;
            }

            @Override
            public Collection<String> keySet() {
                return tags.keySet();
            }

            @Override
            public void put(String key, String value) {
                tags.put(key, value);
            }

            @Override
            public void setKeys(Map<String, String> keys) {
                tags.putAll(keys);
            }

            @Override
            public boolean hasKeys() {
                return !tags.isEmpty();
            }

            @Override
            public int getNumKeys() {
                return tags.size();
            }

            @Override
            public void remove(String key) {
                tags.remove(key);
            }

            @Override
            public void removeAll() {
                tags.clear();
            }
        };
    }
}
