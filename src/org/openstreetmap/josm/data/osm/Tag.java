// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.Utils;

/**
 * Tag represents an immutable key/value-pair. Both the key and the value may be empty, but not null.
 * <p>
 * It implements the {@link Tagged} interface. However, since instances of this class are immutable,
 * the modifying methods throw an {@link UnsupportedOperationException}.
 */
public class Tag implements Tagged, Entry<String, String>, Serializable {

    private static final long serialVersionUID = 1;

    private final String key;
    private final String value;

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
    @Override
    public String getKey() {
        return key;
    }

    /**
     * Replies the value of the tag. This is never null.
     *
     * @return the value of the tag
     */
    @Override
    public String getValue() {
        return value;
    }

    /**
     * This is not supported by this implementation.
     * @param value ignored
     * @return (Does not return)
     * @throws UnsupportedOperationException always
     */
    @Override
    public String setValue(String value) {
        throw new UnsupportedOperationException();
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
        return Objects.hash(key, value);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Tag tag = (Tag) obj;
        return Objects.equals(key, tag.key) &&
                Objects.equals(value, tag.value);
    }

    /**
     * This constructs a {@link Tag} by splitting {@code s} on the first equality sign.
     * @param s the string to convert
     * @return the constructed tag
     * @see org.openstreetmap.josm.tools.TextTagParser
     */
    public static Tag ofString(String s) {
        CheckParameterUtil.ensureParameterNotNull(s, "s");
        final String[] x = s.split("=", 2);
        if (x.length == 2) {
            return new Tag(x[0], x[1]);
        } else {
            throw new IllegalArgumentException('\'' + s + "' does not contain '='");
        }
    }

    @Override
    public String toString() {
        return key + '=' + value;
    }

    /**
     * Removes leading, trailing, and multiple inner whitespaces from the given string, to be used as a key or value.
     * @param s The string
     * @return The string without leading, trailing or multiple inner whitespaces
     * @since 6699
     * @deprecated since 13597. Use {@link Utils#removeWhiteSpaces(String)} instead
     */
    @Deprecated
    public static String removeWhiteSpaces(String s) {
        return Utils.removeWhiteSpaces(s);
    }

    /**
     * Unsupported.
     * @param keys ignored
     * @throws UnsupportedOperationException always
     */
    @Override
    public void setKeys(Map<String, String> keys) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, String> getKeys() {
        return Collections.singletonMap(key, value);
    }

    /**
     * Unsupported.
     * @param key ignored
     * @param value ignored
     * @throws UnsupportedOperationException always
     */
    @Override
    public void put(String key, String value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String get(String k) {
        return key.equals(k) ? value : null;
    }

    /**
     * Unsupported.
     * @param key ignored
     * @throws UnsupportedOperationException always
     */
    @Override
    public void remove(String key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasKeys() {
        return true;
    }

    @Override
    public Collection<String> keySet() {
        return Collections.singleton(key);
    }

    @Override
    public final int getNumKeys() {
        return 1;
    }

    /**
     * Unsupported.
     * @throws UnsupportedOperationException always
     */
    @Override
    public void removeAll() {
        throw new UnsupportedOperationException();
    }

    /**
     * true if this is a direction dependent tag (e.g. oneway)
     *
     * @return {@code true} if this is is a direction dependent tag
     * @since 10716
     */
    public boolean isDirectionKey() {
        return OsmPrimitive.directionKeys.match(this);
    }

}
