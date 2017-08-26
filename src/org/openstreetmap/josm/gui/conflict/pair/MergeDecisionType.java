// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.pair;

import org.openstreetmap.josm.gui.conflict.tags.RelationMemberConflictDecisionType;

/**
 * This is the merge decision for a primitive.
 *
 * @see RelationMemberConflictDecisionType the same for relation members
 */
public enum MergeDecisionType {
    KEEP_MINE,
    KEEP_THEIR,
    UNDECIDED,
}
