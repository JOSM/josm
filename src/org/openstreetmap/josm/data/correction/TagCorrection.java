// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.correction;

import java.util.Objects;

/**
 * Represents a change of a single tag.
 * Both key and value can be subject of this change.
 * @since 729
 */
public class TagCorrection implements Correction {

    /** Old key */
    public final String oldKey;
    /** New key */
    public final String newKey;
    /** Old value */
    public final String oldValue;
    /** New value */
    public final String newValue;

    /**
     * Constructs a new {@code TagCorrection}.
     * @param oldKey old key
     * @param oldValue old value
     * @param newKey new key
     * @param newValue new value
     */
    public TagCorrection(String oldKey, String oldValue, String newKey, String newValue) {
        this.oldKey = oldKey;
        this.oldValue = oldValue;
        this.newKey = newKey;
        this.newValue = newValue;
    }

    /**
     * Determines if the key has changed.
     * @return {@code true} if the key has changed
     */
    public boolean isKeyChanged() {
        return !newKey.equals(oldKey);
    }

    /**
     * Determines if the value has changed.
     * @return {@code true} if the value has changed
     */
    public boolean isValueChanged() {
        return !newValue.equals(oldValue);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass())
            return false;
        TagCorrection that = (TagCorrection) o;
        return Objects.equals(oldKey, that.oldKey) &&
               Objects.equals(newKey, that.newKey) &&
               Objects.equals(oldValue, that.oldValue) &&
               Objects.equals(newValue, that.newValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(oldKey, newKey, oldValue, newValue);
    }
}
