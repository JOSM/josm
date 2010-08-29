// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

/**
 * Tag represents an immutable key/value-pair. Both the key and the value may
 * be empty, but not null.
 *
 */
public class Tag {

    private String key;
    private String value;

    /**
     * Create an empty tag whose key and value are empty.
     */
    public Tag(){
        this("", "");
    }

    /**
     * Create a tag whose key is <code>key</code> and whose value is
     * empty.
     *
     * @param key the key. If null, it is set to the empty key.
     */
    public Tag(String key) {
        this(key, "");
    }

    /**
     * Creates a tag for a key and a value. If key and/or value are null,
     * the empty value "" is assumed.
     *
     * @param key the key
     * @param value  the value
     */
    public Tag(String key, String value) {
        this.key = key == null ? "" : key;
        this.value = value == null ? "" : value;
    }

    /**
     * Creates clone of the tag <code>tag</code>.
     *
     * @param tag the tag.
     */
    public Tag(Tag tag) {
        this(tag.getKey(), tag.getValue());
    }

    /**
     * Replies the key of the tag. This is never null.
     *
     * @return the key of the tag
     */
    public String getKey() {
        return key;
    }

    /**
     * Replies the value of the tag. This is never null.
     *
     * @return the value of the tag
     */
    public String getValue() {
        return value;
    }

    /**
     * Replies true if the key of this tag is equal to <code>key</code>.
     * If <code>key</code> is null, assumes the empty key.
     *
     * @param key the key
     * @return true if the key of this tag is equal to <code>key</code>
     */
    public boolean matchesKey(String key) {
        return this.key.equals(key);
    }

    /**
     * Normalizes the key and the value of the tag by
     * <ul>
     *   <li>removing leading and trailing white space</li>
     * <ul>
     *
     */
    public void normalize() {
        key = key.trim();
        value = value.trim();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + key.hashCode();
        result = prime * result + value.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Tag) {
            Tag other = (Tag) obj;
            return key.equals(other.getKey()) && value.equals(other.getValue());
        } else
            return false;
    }

    @Override
    public String toString() {
        return key + "=" + value;
    }
}
