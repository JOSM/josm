// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.corrector;

/**
 * TagCorrection reprepresents a change of a single
 * tag. Both key and value can be subject of this change.
 */
public class TagCorrection implements Correction {

    public final String oldKey;
    public final String newKey;
    public final String oldValue;
    public final String newValue;

    public TagCorrection(String oldKey, String oldValue, String newKey,
            String newValue) {
        this.oldKey = oldKey;
        this.oldValue = oldValue;
        this.newKey = newKey;
        this.newValue = newValue;
    }

    public boolean isKeyChanged() {
        return !newKey.equals(oldKey);
    }

    public boolean isValueChanged() {
        return !newValue.equals(oldValue);
    }
}
