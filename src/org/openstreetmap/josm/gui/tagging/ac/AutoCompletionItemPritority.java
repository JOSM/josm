// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.ac;

public enum AutoCompletionItemPritority implements Comparable<AutoCompletionItemPritority> {

    /** Indicates that a value is in the current selection. */
    IS_IN_SELECTION,

    /**
     * Indicates, that the value is standard and it is found in the data.
     * This has higher priority than some arbitrary standard value that is
     * usually not used by the user.
     */
    IS_IN_STANDARD_AND_IN_DATASET,

    /**
     * Indicates that this is a standard value, i.e. a standard tag name
     * or a standard value for a given tag name (from the presets).
     */
    IS_IN_STANDARD,

    /**
     * Indicates that this is an arbitrary value from the data set, i.e.
     * the value of a tag name=*.
     */
    IS_IN_DATASET,

    /** Unknown priority. This is the lowest priority. */
    UNKNOWN
}
