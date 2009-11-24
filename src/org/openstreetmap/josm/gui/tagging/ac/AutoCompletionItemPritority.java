// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.ac;

public enum AutoCompletionItemPritority implements Comparable<AutoCompletionItemPritority> {

    /** indicates that a value is in the current selection */
    IS_IN_SELECTION,

    /** indicates that this is a standard value, i.e. a standard tag name
     *  or a standard value for a given tag name
     */
    IS_IN_STANDARD,

    /**
     * indicates that this is an arbitrary value from the data set, i.e.
     * the value of a tag name=xxx
     */
    IS_IN_DATASET,

    /** unknown priority. This is the lowest priority. */
    UNKNOWN
}
