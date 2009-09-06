// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.tags;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Font;

import javax.swing.JLabel;

public enum RelationMemberConflictDecisionType {
    /**
     * replace the respective relation member with a member referring
     * to the new primitive
     */
    REPLACE,

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
            case REPLACE:
                label.setText(tr("Replace"));
                label.setToolTipText(tr("Replace the way this member refers with the combined way"));
                break;
            case UNDECIDED:
                label.setText(tr("Undecided"));
                label.setToolTipText(tr("Not decided yet"));
                break;
        }
    }
}
