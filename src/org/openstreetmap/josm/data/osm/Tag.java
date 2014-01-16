// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.Utils;

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
    public Tag() {
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

    /**
     * This constructs a {@link Tag} by splitting {@code s} on the first equality sign.
     * @see org.openstreetmap.josm.tools.TextTagParser
     * @param s the string to convert
     * @return the constructed tag
     */
    public static Tag ofString(String s) {
        CheckParameterUtil.ensureParameterNotNull(s, "s");
        final String[] x = s.split("=", 2);
        if (x.length == 2) {
            return new Tag(x[0], x[1]);
        } else {
            throw new IllegalArgumentException("'" + s + "' does not contain '='");
        }
    }

    @Override
    public String toString() {
        return key + "=" + value;
    }

    /**
     * Removes leading, trailing, and multiple inner whitespaces from the given string, to be used as a key or value.
     * @param s The string
     * @return The string without leading, trailing or multiple inner whitespaces
     * @since 6699
     */
    public static String removeWhiteSpaces(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return Utils.strip(s).replaceAll("\\s+", " ");
    }
}
