// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.tags;

import static org.openstreetmap.josm.tools.I18n.tr;

import javax.swing.JLabel;

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

    static public void prepareLabel(RelationMemberConflictDecisionType decision, JLabel label) {
        switch(decision) {
        case REMOVE:
            label.setText(tr("Remove"));
            label.setToolTipText(tr("Remove this relation member from the relation"));
            break;
        case KEEP:
            label.setText(tr("Keep"));
            label.setToolTipText(tr("Keep this relation member for the target object"));
            break;
        case UNDECIDED:
            label.setText(tr("Undecided"));
            label.setToolTipText(tr("Not decided yet"));
            break;
        }
    }
}
