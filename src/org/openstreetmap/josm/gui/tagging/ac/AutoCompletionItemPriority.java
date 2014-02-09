// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.ac;

/**
 * Describes the priority of an item in an autocompletion list.
 * The selected flag is currently only used in plugins.
 *
 * Instances of this class are not modifiable.
 */
public class AutoCompletionItemPriority implements Comparable<AutoCompletionItemPriority> {

    /**
     * Indicates, that the value is standard and it is found in the data.
     * This has higher priority than some arbitrary standard value that is
     * usually not used by the user.
     */
    public static final AutoCompletionItemPriority IS_IN_STANDARD_AND_IN_DATASET = new AutoCompletionItemPriority(true, true, false);

    /**
     * Indicates that this is an arbitrary value from the data set, i.e.
     * the value of a tag name=*.
     */
    public static final AutoCompletionItemPriority IS_IN_DATASET = new AutoCompletionItemPriority(true, false, false);

    /**
     * Indicates that this is a standard value, i.e. a standard tag name
     * or a standard value for a given tag name (from the presets).
     */
    public static final AutoCompletionItemPriority IS_IN_STANDARD = new AutoCompletionItemPriority(false, true, false);

    /**
     * Indicates that this is a value from a selected object.
     */
    public static final AutoCompletionItemPriority  IS_IN_SELECTION  = new AutoCompletionItemPriority(false, false, true);

    /** Unknown priority. This is the lowest priority. */
    public static final AutoCompletionItemPriority UNKNOWN = new AutoCompletionItemPriority(false, false, false);

    private final boolean inDataSet;
    private final boolean inStandard;
    private final boolean selected;

    public AutoCompletionItemPriority(boolean inDataSet, boolean inStandard, boolean selected) {
        this.inDataSet = inDataSet;
        this.inStandard = inStandard;
        this.selected = selected;
    }

    public boolean isInDataSet() {
        return inDataSet;
    }

    public boolean isInStandard() {
        return inStandard;
    }

    public boolean isSelected() {
        return selected;
    }

    /**
     * Imposes an ordering on the priorities.
     * Currently, being in the current DataSet is worth more than being in the Presets.
     */
    @Override
    public int compareTo(AutoCompletionItemPriority other) {
        int sel = Boolean.valueOf(selected).compareTo(other.selected);
        if (sel != 0) return sel;

        int ds = Boolean.valueOf(inDataSet).compareTo(other.inDataSet);
        if (ds != 0) return ds;

        int std = Boolean.valueOf(inStandard).compareTo(other.inStandard);
        if (std != 0) return std;

        return 0;
    }

    /**
     * Merges two priorities.
     * The resulting priority is always &gt;= the original ones.
     */
    public AutoCompletionItemPriority mergeWith(AutoCompletionItemPriority other) {
        return new AutoCompletionItemPriority(
                inDataSet || other.inDataSet,
                inStandard || other.inStandard,
                selected || other.selected);
    }

    @Override public String toString() {
        return String.format("<Priority; inDataSet: %b, inStandard: %b, selected: %b>", inDataSet, inStandard, selected);
    }
}
