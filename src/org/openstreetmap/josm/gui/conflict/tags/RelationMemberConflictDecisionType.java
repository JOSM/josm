// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.tags;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Font;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
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
            label.setToolTipText(tr("Replace the way this member refers to with the combined way"));
            break;
        case UNDECIDED:
            label.setText(tr("Undecided"));
            label.setToolTipText(tr("Not decided yet"));
            break;
        }
    }

    static public void prepareComboBox(RelationMemberConflictDecisionType decision, JComboBox comboBox) {
        DefaultComboBoxModel model = (DefaultComboBoxModel)comboBox.getModel();
        model.removeAllElements();
        switch(decision) {
        case REMOVE:
            model.addElement(tr("Remove"));
            comboBox.setToolTipText(tr("Remove this relation member from the relation"));
            comboBox.setSelectedIndex(0);
            break;
        case REPLACE:
            model.addElement(tr("Replace"));
            comboBox.setToolTipText(tr("Replace the way this member refers to with the combined way"));
            comboBox.setSelectedIndex(0);
            break;
        case UNDECIDED:
            model.addElement(tr("Undecided"));
            comboBox.setToolTipText(tr("Not decided yet"));
            comboBox.setSelectedIndex(0);
            break;
        }
    }
}
