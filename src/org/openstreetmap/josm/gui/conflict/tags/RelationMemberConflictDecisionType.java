// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.tags;

import static org.openstreetmap.josm.tools.I18n.tr;

/**
 * This represents the decision a user can make regarding a relation conflict
 */
public enum RelationMemberConflictDecisionType {
    /**
     * keep the respective relation member for the target primitive (the target node
     * in a node merge operation or the target way in a combine way operation)
     */
    KEEP,

    /**
     * remove the respective relation member
     */
    REMOVE,

    /**
     * not yet decided
     */
    UNDECIDED;

    String getLabelText() {
        switch (this) {
            case REMOVE:
                return tr("Remove");
            case KEEP:
                return tr("Keep");
            case UNDECIDED:
            default:
                return tr("Undecided");
        }
    }

    String getLabelToolTipText() {
        switch (this) {
            case REMOVE:
                return tr("Remove this relation member from the relation");
            case KEEP:
                return tr("Keep this relation member for the target object");
            case UNDECIDED:
            default:
                return tr("Not decided yet");
        }
    }
}
