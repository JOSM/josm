// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.openstreetmap.josm.tools.Logging;

/**
 * TagCollection is a collection of tags which can be used to manipulate
 * tags managed by {@link org.openstreetmap.josm.data.osm.OsmPrimitive}s.
 *
 * A TagCollection can be created:
 * <ul>
 *  <li>from the tags managed by a specific {@link org.openstreetmap.josm.data.osm.OsmPrimitive}
 *  with {@link #from(org.openstreetmap.josm.data.osm.Tagged)}</li>
 *  <li>from the union of all tags managed by a collection of {@link org.openstreetmap.josm.data.osm.OsmPrimitive}s
 *  with {@link #unionOfAllPrimitives(java.util.Collection)}</li>
 *  <li>from the union of all tags managed by a {@link org.openstreetmap.josm.data.osm.DataSet}
 *  with {@link #unionOfAllPrimitives(org.openstreetmap.josm.data.osm.DataSet)}</li>
 *  <li>from the intersection of all tags managed by a collection of primitives
 *  with {@link #commonToAllPrimitives(java.util.Collection)}</li>
 * </ul>
 *
 * It  provides methods to query the collection, like {@link #size()}, {@link #hasTagsFor(String)}, etc.
 *
 * Basic set operations allow to create the union, the intersection and  the difference
 * of tag collections, see {@link #union(org.openstreetmap.josm.data.osm.TagCollection)},
 * {@link #intersect(org.openstreetmap.josm.data.osm.TagCollection)}, and {@link #minus(org.openstreetmap.josm.data.osm.TagCollection)}.
 *
 * @since 2008
 */
public class TagCollection implements Iterable<Tag>, Serializable {

    private static final long serialVersionUID = 1;

    /**
     * Creates a tag collection from the tags managed by a specific
     * {@link org.openstreetmap.josm.data.osm.OsmPrimitive}. If <code>primitive</code> is null, replies
     * an empty tag collection.
     *
     * @param primitive  the primitive
     * @return a tag collection with the tags managed by a specific
     * {@link org.openstreetmap.josm.data.osm.OsmPrimitive}
     */
    public static TagCollection from(Tagged primitive) {
        TagCollection tags = new TagCollection();
        if (primitive != null) {
            for (String key: primitive.keySet()) {
                tags.add(new Tag(key, primitive.get(key)));
            }
        }
        return tags;
    }

    /**
     * Creates a tag collection from a map of key/value-pairs. Replies
     * an empty tag collection if {@code tags} is null.
     *
     * @param tags  the key/value-pairs
     * @return the tag collection
     */
    public static TagCollection from(Map<String, String> tags) {
        TagCollection ret = new TagCollection();
        if (tags == null) return ret;
        for (Entry<String, String> entry: tags.entrySet()) {
            String key = entry.getKey() == null ? "" : entry.getKey();
            String value = entry.getValue() == null ? "" : entry.getValue();
            ret.add(new Tag(key, value));
        }
        return ret;
    }

    /**
     * Creates a tag collection from the union of the tags managed by
     * a collection of primitives. Replies an empty tag collection,
     * if <code>primitives</code> is null.
     *
     * @param primitives the primitives
     * @return  a tag collection with the union of the tags managed by
     * a collection of primitives
     */
    public static TagCollection unionOfAllPrimitives(Collection<? extends Tagged> primitives) {
        TagCollection tags = new TagCollection();
        if (primitives == null) return tags;
        for (Tagged primitive: primitives) {
            if (primitive == null) {
                continue;
            }
            tags.add(TagCollection.from(primitive));
        }
        return tags;
    }

    /**
     * Replies a tag collection with the tags which are common to all primitives in in
     * <code>primitives</code>. Replies an empty tag collection of <code>primitives</code>
     * is null.
     *
     * @param primitives the primitives
     * @return  a tag collection with the tags which are common to all primitives
     */
    public static TagCollection commonToAllPrimitives(Collection<? extends Tagged> primitives) {
        TagCollection tags = new TagCollection();
        if (primitives == null || primitives.isEmpty()) return tags;
        // initialize with the first
        tags.add(TagCollection.from(primitives.iterator().next()));

        // intersect with the others
        //
        for (Tagged primitive: primitives) {
            if (primitive == null) {
                continue;
            }
            tags = tags.intersect(TagCollection.from(primitive));
            if (tags.isEmpty())
                break;
        }
        return tags;
    }

    /**
     * Replies a tag collection with the union of the tags which are common to all primitives in
     * the dataset <code>ds</code>. Returns an empty tag collection of <code>ds</code> is null.
     *
     * @param ds the dataset
     * @return a tag collection with the union of the tags which are common to all primitives in
     * the dataset <code>ds</code>
     */
    public static TagCollection unionOfAllPrimitives(DataSet ds) {
        TagCollection tags = new TagCollection();
        if (ds == null) return tags;
        tags.add(TagCollection.unionOfAllPrimitives(ds.allPrimitives()));
        return tags;
    }

    private final Map<Tag, Integer> tags = new HashMap<>();

    /**
     * Creates an empty tag collection.
     */
    public TagCollection() {
        // contents can be set later with add()
    }

    /**
     * Creates a clone of the tag collection <code>other</code>. Creats an empty
     * tag collection if <code>other</code> is null.
     *
     * @param other the other collection
     */
    public TagCollection(TagCollection other) {
        if (other != null) {
            tags.putAll(other.tags);
        }
    }

    /**
     * Creates a tag collection from <code>tags</code>.
     * @param tags the collection of tags
     * @since 5724
     */
    public TagCollection(Collection<Tag> tags) {
        add(tags);
    }

    /**
     * Replies the number of tags in this tag collection
     *
     * @return the number of tags in this tag collection
     */
    public int size() {
        return tags.size();
    }

    /**
     * Replies true if this tag collection is empty
     *
     * @return true if this tag collection is empty; false, otherwise
     */
    public boolean isEmpty() {
        return size() == 0;
    }

    /**
     * Adds a tag to the tag collection. If <code>tag</code> is null, nothing is added.
     *
     * @param tag the tag to add
     */
    public final void add(Tag tag) {
        if (tag != null) {
            tags.merge(tag, 1, (i, j) -> i + j);
        }
    }

    /**
     * Gets the number of times this tag was added to the collection.
     * @param tag The tag
     * @return The number of times this tag is used in this collection.
     * @since 14302
     */
    public int getTagOccurrence(Tag tag) {
        return tags.getOrDefault(tag, 0);
    }

    /**
     * Adds a collection of tags to the tag collection. If <code>tags</code> is null, nothing
     * is added. null values in the collection are ignored.
     *
     * @param tags the collection of tags
     */
    public final void add(Collection<Tag> tags) {
        if (tags == null) return;
        for (Tag tag: tags) {
            add(tag);
        }
    }

    /**
     * Adds the tags of another tag collection to this collection. Adds nothing, if
     * <code>tags</code> is null.
     *
     * @param tags the other tag collection
     */
    public final void add(TagCollection tags) {
        if (tags != null) {
            for (Entry<Tag, Integer> entry : tags.tags.entrySet()) {
                this.tags.merge(entry.getKey(), entry.getValue(), (i, j) -> i + j);
            }
        }
    }

    /**
     * Removes a specific tag from the tag collection. Does nothing if <code>tag</code> is
     * null.
     *
     * @param tag the tag to be removed
     */
    public void remove(Tag tag) {
        if (tag == null) return;
        tags.remove(tag);
    }

    /**
     * Removes a collection of tags from the tag collection. Does nothing if <code>tags</code> is
     * null.
     *
     * @param tags the tags to be removed
     */
    public void remove(Collection<Tag> tags) {
        if (tags != null) {
            tags.stream().forEach(this::remove);
        }
    }

    /**
     * Removes all tags in the tag collection <code>tags</code> from the current tag collection.
     * Does nothing if <code>tags</code> is null.
     *
     * @param tags the tag collection to be removed.
     */
    public void remove(TagCollection tags) {
        if (tags != null) {
            tags.tags.keySet().stream().forEach(this::remove);
        }
    }

    /**
     * Removes all tags whose keys are equal to  <code>key</code>. Does nothing if <code>key</code>
     * is null.
     *
     * @param key the key to be removed
     */
    public void removeByKey(String key) {
        if (key != null) {
            tags.keySet().removeIf(tag -> tag.matchesKey(key));
        }
    }

    /**
     * Removes all tags whose key is in the collection <code>keys</code>. Does nothing if
     * <code>keys</code> is null.
     *
     * @param keys the collection of keys to be removed
     */
    public void removeByKey(Collection<String> keys) {
        if (keys == null) return;
        for (String key: keys) {
            removeByKey(key);
        }
    }

    /**
     * Replies true if the this tag collection contains <code>tag</code>.
     *
     * @param tag the tag to look up
     * @return true if the this tag collection contains <code>tag</code>; false, otherwise
     */
    public boolean contains(Tag tag) {
        return tags.containsKey(tag);
    }

    /**
     * Replies true if this tag collection contains all tags in <code>tags</code>. Replies
     * false, if tags is null.
     *
     * @param tags the tags to look up
     * @return true if this tag collection contains all tags in <code>tags</code>. Replies
     * false, if tags is null.
     */
    public boolean containsAll(Collection<Tag> tags) {
        if (tags == null) {
            return false;
        } else {
            return this.tags.keySet().containsAll(tags);
        }
    }

    /**
     * Replies true if this tag collection at least one tag for every key in <code>keys</code>.
     * Replies false, if <code>keys</code> is null. null values in <code>keys</code> are ignored.
     *
     * @param keys the keys to lookup
     * @return true if this tag collection at least one tag for every key in <code>keys</code>.
     */
    public boolean containsAllKeys(Collection<String> keys) {
        if (keys == null) {
            return false;
        } else {
            return keys.stream().filter(Objects::nonNull).allMatch(this::hasTagsFor);
        }
    }

    /**
     * Replies the number of tags with key <code>key</code>
     *
     * @param key the key to look up
     * @return the number of tags with key <code>key</code>, including the empty "" value. 0, if key is null.
     */
    public int getNumTagsFor(String key) {
        return (int) generateStreamForKey(key).count();
    }

    /**
     * Replies true if there is at least one tag for the given key.
     *
     * @param key the key to look up
     * @return true if there is at least one tag for the given key. false, if key is null.
     */
    public boolean hasTagsFor(String key) {
        return getNumTagsFor(key) > 0;
    }

    /**
     * Replies true it there is at least one tag with a non empty value for key.
     * Replies false if key is null.
     *
     * @param key the key
     * @return true it there is at least one tag with a non empty value for key.
     */
    public boolean hasValuesFor(String key) {
        return generateStreamForKey(key).anyMatch(t -> !t.getValue().isEmpty());
    }

    /**
     * Replies true if there is exactly one tag for <code>key</code> and
     * if the value of this tag is not empty. Replies false if key is
     * null.
     *
     * @param key the key
     * @return true if there is exactly one tag for <code>key</code> and
     * if the value of this tag is not empty
     */
    public boolean hasUniqueNonEmptyValue(String key) {
        return generateStreamForKey(key).filter(t -> !t.getValue().isEmpty()).count() == 1;
    }

    /**
     * Replies true if there is a tag with an empty value for <code>key</code>.
     * Replies false, if key is null.
     *
     * @param key the key
     * @return true if there is a tag with an empty value for <code>key</code>
     */
    public boolean hasEmptyValue(String key) {
        return generateStreamForKey(key).anyMatch(t -> t.getValue().isEmpty());
    }

    /**
     * Replies true if there is exactly one tag for <code>key</code> and if
     * the value for this tag is empty. Replies false if key is null.
     *
     * @param key the key
     * @return  true if there is exactly one tag for <code>key</code> and if
     * the value for this tag is empty
     */
    public boolean hasUniqueEmptyValue(String key) {
        Set<String> values = getValues(key);
        return values.size() == 1 && values.contains("");
    }

    /**
     * Replies a tag collection with the tags for a given key. Replies an empty collection
     * if key is null.
     *
     * @param key the key to look up
     * @return a tag collection with the tags for a given key. Replies an empty collection
     * if key is null.
     */
    public TagCollection getTagsFor(String key) {
        TagCollection ret = new TagCollection();
        generateStreamForKey(key).forEach(ret::add);
        return ret;
    }

    /**
     * Replies a tag collection with all tags whose key is equal to one of the keys in
     * <code>keys</code>. Replies an empty collection if keys is null.
     *
     * @param keys the keys to look up
     * @return a tag collection with all tags whose key is equal to one of the keys in
     * <code>keys</code>
     */
    public TagCollection getTagsFor(Collection<String> keys) {
        TagCollection ret = new TagCollection();
        if (keys == null)
            return ret;
        for (String key : keys) {
            if (key != null) {
                ret.add(getTagsFor(key));
            }
        }
        return ret;
    }

    /**
     * Replies the tags of this tag collection as set
     *
     * @return the tags of this tag collection as set
     */
    public Set<Tag> asSet() {
        return new HashSet<>(tags.keySet());
    }

    /**
     * Replies the tags of this tag collection as list.
     * Note that the order of the list is not preserved between method invocations.
     *
     * @return the tags of this tag collection as list. There are no dupplicate values.
     */
    public List<Tag> asList() {
        return new ArrayList<>(tags.keySet());
    }

    /**
     * Replies an iterator to iterate over the tags in this collection
     *
     * @return the iterator
     */
    @Override
    public Iterator<Tag> iterator() {
        return tags.keySet().iterator();
    }

    /**
     * Replies the set of keys of this tag collection.
     *
     * @return the set of keys of this tag collection
     */
    public Set<String> getKeys() {
        return generateKeyStream().collect(Collectors.toCollection(HashSet::new));
    }

    /**
     * Replies the set of keys which have at least 2 matching tags.
     *
     * @return the set of keys which have at least 2 matching tags.
     */
    public Set<String> getKeysWithMultipleValues() {
        HashSet<String> singleKeys = new HashSet<>();
        return generateKeyStream().filter(key -> !singleKeys.add(key)).collect(Collectors.toSet());
    }

    /**
     * Sets a unique tag for the key of this tag. All other tags with the same key are
     * removed from the collection. Does nothing if tag is null.
     *
     * @param tag the tag to set
     */
    public void setUniqueForKey(Tag tag) {
        if (tag == null) return;
        removeByKey(tag.getKey());
        add(tag);
    }

    /**
     * Sets a unique tag for the key of this tag. All other tags with the same key are
     * removed from the collection. Assume the empty string for key and value if either
     * key or value is null.
     *
     * @param key the key
     * @param value the value
     */
    public void setUniqueForKey(String key, String value) {
        Tag tag = new Tag(key, value);
        setUniqueForKey(tag);
    }

    /**
     * Replies the set of values in this tag collection
     *
     * @return the set of values
     */
    public Set<String> getValues() {
        return tags.keySet().stream().map(Tag::getValue).collect(Collectors.toSet());
    }

    /**
     * Replies the set of values for a given key. Replies an empty collection if there
     * are no values for the given key.
     *
     * @param key the key to look up
     * @return the set of values for a given key. Replies an empty collection if there
     * are no values for the given key
     */
    public Set<String> getValues(String key) {
        // null-safe
        return generateStreamForKey(key).map(Tag::getValue).collect(Collectors.toSet());
    }

    /**
     * Replies true if for every key there is one tag only, i.e. exactly one value.
     *
     * @return {@code true} if for every key there is one tag only
     */
    public boolean isApplicableToPrimitive() {
        return getKeysWithMultipleValues().isEmpty();
    }

    /**
     * Applies this tag collection to an {@link org.openstreetmap.josm.data.osm.OsmPrimitive}. Does nothing if
     * primitive is null
     *
     * @param primitive  the primitive
     * @throws IllegalStateException if this tag collection can't be applied
     * because there are keys with multiple values
     */
    public void applyTo(Tagged primitive) {
        if (primitive == null) return;
        ensureApplicableToPrimitive();
        for (Tag tag: tags.keySet()) {
            if (tag.getValue() == null || tag.getValue().isEmpty()) {
                primitive.remove(tag.getKey());
            } else {
                primitive.put(tag.getKey(), tag.getValue());
            }
        }
    }

    /**
     * Applies this tag collection to a collection of {@link org.openstreetmap.josm.data.osm.OsmPrimitive}s. Does nothing if
     * primitives is null
     *
     * @param primitives the collection of primitives
     * @throws IllegalStateException if this tag collection can't be applied
     * because there are keys with multiple values
     */
    public void applyTo(Collection<? extends Tagged> primitives) {
        if (primitives == null) return;
        ensureApplicableToPrimitive();
        for (Tagged primitive: primitives) {
            applyTo(primitive);
        }
    }

    /**
     * Replaces the tags of an {@link org.openstreetmap.josm.data.osm.OsmPrimitive} by the tags in this collection . Does nothing if
     * primitive is null
     *
     * @param primitive  the primitive
     * @throws IllegalStateException if this tag collection can't be applied
     * because there are keys with multiple values
     */
    public void replaceTagsOf(Tagged primitive) {
        if (primitive == null) return;
        ensureApplicableToPrimitive();
        primitive.removeAll();
        for (Tag tag: tags.keySet()) {
            primitive.put(tag.getKey(), tag.getValue());
        }
    }

    /**
     * Replaces the tags of a collection of{@link org.openstreetmap.josm.data.osm.OsmPrimitive}s by the tags in this collection.
     * Does nothing if primitives is null
     *
     * @param primitives the collection of primitives
     * @throws IllegalStateException if this tag collection can't be applied
     * because there are keys with multiple values
     */
    public void replaceTagsOf(Collection<? extends Tagged> primitives) {
        if (primitives == null) return;
        ensureApplicableToPrimitive();
        for (Tagged primitive: primitives) {
            replaceTagsOf(primitive);
        }
    }

    private void ensureApplicableToPrimitive() {
        if (!isApplicableToPrimitive())
            throw new IllegalStateException(tr("Tag collection cannot be applied to a primitive because there are keys with multiple values."));
    }

    /**
     * Builds the intersection of this tag collection and another tag collection
     *
     * @param other the other tag collection. If null, replies an empty tag collection.
     * @return the intersection of this tag collection and another tag collection. All counts are set to 1.
     */
    public TagCollection intersect(TagCollection other) {
        TagCollection ret = new TagCollection();
        if (other != null) {
            tags.keySet().stream().filter(other::contains).forEach(ret::add);
        }
        return ret;
    }

    /**
     * Replies the difference of this tag collection and another tag collection
     *
     * @param other the other tag collection. May be null.
     * @return the difference of this tag collection and another tag collection
     */
    public TagCollection minus(TagCollection other) {
        TagCollection ret = new TagCollection(this);
        if (other != null) {
            ret.remove(other);
        }
        return ret;
    }

    /**
     * Replies the union of this tag collection and another tag collection
     *
     * @param other the other tag collection. May be null.
     * @return the union of this tag collection and another tag collection. The tag count is summed.
     */
    public TagCollection union(TagCollection other) {
        TagCollection ret = new TagCollection(this);
        if (other != null) {
            ret.add(other);
        }
        return ret;
    }

    public TagCollection emptyTagsForKeysMissingIn(TagCollection other) {
        TagCollection ret = new TagCollection();
        for (String key: this.minus(other).getKeys()) {
            ret.add(new Tag(key));
        }
        return ret;
    }

    private static final Pattern SPLIT_VALUES_PATTERN = Pattern.compile(";\\s*");

    /**
     * Replies the concatenation of all tag values (concatenated by a semicolon)
     * @param key the key to look up
     *
     * @return the concatenation of all tag values
     */
    public String getJoinedValues(String key) {

        // See #7201 combining ways screws up the order of ref tags
        Set<String> originalValues = getValues(key);
        if (originalValues.size() == 1) {
            return originalValues.iterator().next();
        }

        Set<String> values = new LinkedHashSet<>();
        Map<String, Collection<String>> originalSplitValues = new LinkedHashMap<>();
        for (String v : originalValues) {
            List<String> vs = Arrays.asList(SPLIT_VALUES_PATTERN.split(v));
            originalSplitValues.put(v, vs);
            values.addAll(vs);
        }
        values.remove("");
        // try to retain an already existing key if it contains all needed values (remove this if it causes performance problems)
        for (Entry<String, Collection<String>> i : originalSplitValues.entrySet()) {
            if (i.getValue().containsAll(values)) {
                return i.getKey();
            }
        }
        return String.join(";", values);
    }

    /**
     * Replies the sum of all numeric tag values. Ignores dupplicates.
     * @param key the key to look up
     *
     * @return the sum of all numeric tag values, as string.
     * @since 7743
     */
    public String getSummedValues(String key) {
        int result = 0;
        for (String value : getValues(key)) {
            try {
                result += Integer.parseInt(value);
            } catch (NumberFormatException e) {
                Logging.trace(e);
            }
        }
        return Integer.toString(result);
    }

    private Stream<String> generateKeyStream() {
        return tags.keySet().stream().map(Tag::getKey);
    }

    /**
     * Get a stram for the given key.
     * @param key The key
     * @return The stream. An empty stream if key is <code>null</code>
     */
    private Stream<Tag> generateStreamForKey(String key) {
        return tags.keySet().stream().filter(e -> e.matchesKey(key));
    }

    @Override
    public String toString() {
        return tags.toString();
    }
}
