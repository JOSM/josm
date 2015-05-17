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

    private static final int NO_USER_INPUT = Integer.MAX_VALUE;

    private final int userInput;
    private final boolean inDataSet;
    private final boolean inStandard;
    private final boolean selected;


    /**
     * Create new AutoCompletionItemPriority object.
     *
     * @param inDataSet true, if the item is found in the currently active data layer
     * @param inStandard true, if the item is a standard tag, e.g. from the presets.
     * @param selected true, if it is found on an object that is currently selected
     * @param userInput null, if the user hasn't entered this tag so far. A number when
     * the tag key / value has been entered by the user before. A lower number means
     * this happened more recently and beats a higher number in priority.
     */
    public AutoCompletionItemPriority(boolean inDataSet, boolean inStandard, boolean selected, Integer userInput) {
        this.inDataSet = inDataSet;
        this.inStandard = inStandard;
        this.selected = selected;
        this.userInput = userInput == null ? NO_USER_INPUT : userInput;
    }

    public AutoCompletionItemPriority(boolean inDataSet, boolean inStandard, boolean selected) {
        this(inDataSet, inStandard, selected, NO_USER_INPUT);
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

    public Integer getUserInput() {
        return userInput == NO_USER_INPUT ? null : userInput;
    }

    /**
     * Imposes an ordering on the priorities.
     * Currently, being in the current DataSet is worth more than being in the Presets.
     */
    @Override
    public int compareTo(AutoCompletionItemPriority other) {
        int ui = Integer.compare(other.userInput, userInput);
        if (ui != 0) return ui;

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
                selected || other.selected,
                Math.min(userInput, other.userInput));
    }

    @Override
    public String toString() {
        return String.format("<Priority; userInput: %s, inDataSet: %b, inStandard: %b, selected: %b>",
                userInput == NO_USER_INPUT ? "no" : Integer.toString(userInput), inDataSet, inStandard, selected);
    }
}
