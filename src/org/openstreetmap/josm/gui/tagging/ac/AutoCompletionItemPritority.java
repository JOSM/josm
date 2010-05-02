// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.ac;

/**
 * Describes the priority of an item in an autocompletion list.
 * The selected flag is currently only used in plugins.
 *
 * Instances of this class are not modifiable.
 */
public class AutoCompletionItemPritority implements Comparable<AutoCompletionItemPritority> {

    /**
     * Indicates, that the value is standard and it is found in the data.
     * This has higher priority than some arbitrary standard value that is
     * usually not used by the user.
     */
    public static AutoCompletionItemPritority IS_IN_STANDARD_AND_IN_DATASET = new AutoCompletionItemPritority(true, true, false);

    /**
     * Indicates that this is an arbitrary value from the data set, i.e.
     * the value of a tag name=*.
     */
    public static AutoCompletionItemPritority IS_IN_DATASET = new AutoCompletionItemPritority(true, false, false);

    /**
     * Indicates that this is a standard value, i.e. a standard tag name
     * or a standard value for a given tag name (from the presets).
     */
    public static AutoCompletionItemPritority IS_IN_STANDARD = new AutoCompletionItemPritority(false, true, false);
    
    /**
     * Indicates that this is a value from a selected object.
     */
    public static AutoCompletionItemPritority  IS_IN_SELECTION  = new AutoCompletionItemPritority(false, false, true);

    /** Unknown priority. This is the lowest priority. */
    public static AutoCompletionItemPritority UNKNOWN = new AutoCompletionItemPritority(false, false, false);

    private final boolean inDataSet;
    private final boolean inStandard;
    private final boolean selected;

    public AutoCompletionItemPritority(boolean inDataSet, boolean inStandard, boolean selected) {
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
    public int compareTo(AutoCompletionItemPritority other) {
        int sel = new Boolean(selected).compareTo(other.selected);
        if (sel != 0) return sel;

        int ds = new Boolean(inDataSet).compareTo(other.inDataSet);
        if (ds != 0) return ds;

        int std = new Boolean(inStandard).compareTo(other.inStandard);
        if (std != 0) return std;

        return 0;
    }

    /**
     * Merges two priorities.
     * The resulting priority is always >= the original ones.
     */
    public AutoCompletionItemPritority mergeWith(AutoCompletionItemPritority other) {
        return new AutoCompletionItemPritority(
                        inDataSet || other.inDataSet,
                        inStandard || other.inStandard,
                        selected || other.selected);
    }

    @Override public String toString() {
        return String.format("<Priority; inDataSet: %b, inStandard: %b, selected: %b>", inDataSet, inStandard, selected);
    }
}
