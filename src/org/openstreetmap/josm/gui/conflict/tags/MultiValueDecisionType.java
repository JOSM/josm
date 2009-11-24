// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.tags;

/**
 * Represents a decision for a tag conflict due to multiple possible values.
 *
 *
 */
public enum MultiValueDecisionType {
    /** not yet decided */
    UNDECIDED,
    /** keep exactly one values */
    KEEP_ONE,
    /** keep no value, delete the tag */
    KEEP_NONE,
    /** keep all values; concatenate them with ; */
    KEEP_ALL,
}
