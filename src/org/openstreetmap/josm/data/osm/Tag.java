// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

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
        this.key = "";
        this.value = "";
    }

    /**
     * Create a tag whose key is <code>key</code> and whose value is
     * empty.
     * 
     * @param key the key. If null, it is set to the empty key.
     */
    public Tag(String key) {
        this();
        this.key = key == null ? "" : key;
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
     * @param tag the tag. If null, creates an empty tag.
     */
    public Tag(Tag tag) {
        if (tag != null) {
            key = tag.getKey();
            value = tag.getValue();
        }
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

    @Override
    public Tag clone() {
        return new Tag(this);
    }

    /**
     * Replies true if the key of this tag is equal to <code>key</code>.
     * If <code>key</code> is null, assumes the empty key.
     * 
     * @param key the key
     * @return true if the key of this tag is equal to <code>key</code>
     */
    public boolean matchesKey(String key) {
        if (key == null) {
            key = "";
        }
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
        result = prime * result + ((key == null) ? 0 : key.hashCode());
        result = prime * result + ((value == null) ? 0 : value.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Tag other = (Tag) obj;
        if (key == null) {
            if (other.key != null)
                return false;
        } else if (!key.equals(other.key))
            return false;
        if (value == null) {
            if (other.value != null)
                return false;
        } else if (!value.equals(other.value))
            return false;
        return true;
    }
}
