// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

/**
 * This class provides a read/write map that uses the same format as {@link AbstractPrimitive#keys}.
 * It offers good performance for few keys.
 * It uses copy on write, so there cannot be a {@link ConcurrentModificationException} while iterating through it.
 *
 * @author Michael Zangl
 */
public class TagMap extends AbstractMap<String, String> implements Serializable {
    static final long serialVersionUID = 1;
    /**
     * We use this array every time we want to represent an empty map.
     * This saves us the burden of checking for null every time but saves some object allocations.
     */
    private static final String[] EMPTY_TAGS = new String[0];

    /**
     * An iterator that iterates over the tags in this map. The iterator always represents the state of the map when it was created.
     * Further changes to the map won't change the tags that we iterate over but they also won't raise any exceptions.
     * @author Michael Zangl
     */
    private static class TagEntryIterator implements Iterator<Entry<String, String>> {
        /**
         * The current state of the tags we iterate over.
         */
        private final String[] tags;
        /**
         * Current tag index. Always a multiple of 2.
         */
        private int currentIndex;

        /**
         * Create a new {@link TagEntryIterator}
         * @param tags The tags array. It is never changed but should also not be changed by you.
         */
        TagEntryIterator(String... tags) {
            super();
            this.tags = tags;
        }

        @Override
        public boolean hasNext() {
            return currentIndex < tags.length;
        }

        @Override
        public Entry<String, String> next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }

            Tag tag = new Tag(tags[currentIndex], tags[currentIndex + 1]);
            currentIndex += 2;
            return tag;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

    }

    /**
     * This is the entry set of this map. It represents the state when it was created.
     * @author Michael Zangl
     */
    private static class TagEntrySet extends AbstractSet<Entry<String, String>> {
        private final String[] tags;

        /**
         * Create a new {@link TagEntrySet}
         * @param tags The tags array. It is never changed but should also not be changed by you.
         */
        TagEntrySet(String... tags) {
            super();
            this.tags = tags;
        }

        @Override
        public Iterator<Entry<String, String>> iterator() {
            return new TagEntryIterator(tags);
        }

        @Override
        public int size() {
            return tags.length / 2;
        }

    }

    /**
     * The tags field. This field is guarded using RCU.
     */
    private volatile String[] tags;

    /**
     * Creates a new, empty tag map.
     */
    public TagMap() {
        this((String[]) null);
    }

    /**
     * Create a new tag map and load it from the other map.
     * @param tags The map to load from.
     * @since 10604
     */
    public TagMap(Map<String, String> tags) {
        putAll(tags);
    }

    /**
     * Copy constructor.
     * @param tagMap The map to copy from.
     * @since 10604
     */
    public TagMap(TagMap tagMap) {
        this(tagMap.tags);
    }

    /**
     * Creates a new read only tag map using a key/value/key/value/... array.
     * <p>
     * The array that is passed as parameter may not be modified after passing it to this map.
     * @param tags The tags array. It is not modified by this map.
     */
    public TagMap(String... tags) {
        if (tags == null || tags.length == 0) {
            this.tags = EMPTY_TAGS;
        } else {
            if (tags.length % 2 != 0) {
                throw new IllegalArgumentException("tags array length needs to be multiple of two.");
            }
            this.tags = tags;
        }
    }

    /**
     * Creates a new map using the given list of tags. For duplicate keys the last value found is used.
     * @param tags The tags
     * @since 10736
     */
    public TagMap(Collection<Tag> tags) {
        for (Tag tag : tags) {
            put(tag.getKey(), tag.getValue());
        }
    }

    @Override
    public Set<Entry<String, String>> entrySet() {
        return new TagEntrySet(tags);
    }

    @Override
    public boolean containsKey(Object key) {
        return indexOfKey(tags, key) >= 0;
    }

    @Override
    public String get(Object key) {
        int index = indexOfKey(tags, key);
        return index < 0 ? null : tags[index + 1];
    }

    @Override
    public boolean containsValue(Object value) {
        for (int i = 1; i < tags.length; i += 2) {
            if (value.equals(tags[i])) {
                return true;
            }
        }
        return false;
    }

    @Override
    public synchronized String put(String key, String value) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(value);
        int index = indexOfKey(tags, key);
        int newTagArrayLength = tags.length;
        if (index < 0) {
            index = newTagArrayLength;
            newTagArrayLength += 2;
        }

        String[] newTags = Arrays.copyOf(tags, newTagArrayLength);
        String old = newTags[index + 1];
        newTags[index] = key;
        newTags[index + 1] = value;
        tags = newTags;
        return old;
    }

    @Override
    public synchronized String remove(Object key) {
        int index = indexOfKey(tags, key);
        if (index < 0) {
            return null;
        }
        String old = tags[index + 1];
        int newLength = tags.length - 2;
        if (newLength == 0) {
            tags = EMPTY_TAGS;
        } else {
            String[] newTags = new String[newLength];
            System.arraycopy(tags, 0, newTags, 0, index);
            System.arraycopy(tags, index + 2, newTags, index, newLength - index);
            tags = newTags;
        }

        return old;
    }

    @Override
    public synchronized void clear() {
        tags = EMPTY_TAGS;
    }

    @Override
    public int size() {
        return tags.length / 2;
    }

    /**
     * Gets a list of all tags contained in this map.
     * @return The list of tags in the order they were added.
     * @since 10604
     */
    public List<Tag> getTags() {
        List<Tag> tagList = new ArrayList<>();
        for (int i = 0; i < tags.length; i += 2) {
            tagList.add(new Tag(tags[i], tags[i+1]));
        }
        return tagList;
    }

    /**
     * Finds a key in an array that is structured like the {@link #tags} array and returns the position.
     * <p>
     * We allow the parameter to be passed to allow for better synchronization.
     *
     * @param tags The tags array to search through.
     * @param key The key to search.
     * @return The index of the key (a multiple of two) or -1 if it was not found.
     */
    private static int indexOfKey(String[] tags, Object key) {
        for (int i = 0; i < tags.length; i += 2) {
            if (tags[i].equals(key)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("TagMap[");
        boolean first = true;
        for (Map.Entry<String, String> e : entrySet()) {
            if (!first) {
                stringBuilder.append(',');
            }
            stringBuilder.append(e.getKey());
            stringBuilder.append('=');
            stringBuilder.append(e.getValue());
            first = false;
        }
        stringBuilder.append(']');
        return stringBuilder.toString();
    }

    /**
     * Gets the backing tags array. Do not modify this array.
     * @return The tags array.
     */
    String[] getTagsArray() {
        return tags;
    }
}
