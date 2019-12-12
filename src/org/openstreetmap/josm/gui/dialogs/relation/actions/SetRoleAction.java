// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation.actions;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.event.ActionEvent;

import javax.swing.JOptionPane;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.openstreetmap.josm.gui.ConditionalOptionPaneUtil;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompletingTextField;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Sets a role for the selected members
 * @since 9496
 */
public class SetRoleAction extends AbstractRelationEditorAction implements DocumentListener {
    private static final long serialVersionUID = 1L;

    private final transient AutoCompletingTextField tfRole;

    /**
     * Constructs a new {@code SetRoleAction}.
     * @param editorAccess An interface to access the relation editor contents.
     */
    public SetRoleAction(IRelationEditorActionAccess editorAccess) {
        super(editorAccess);
        this.tfRole = editorAccess.getTextFieldRole();
        putValue(SHORT_DESCRIPTION, tr("Sets a role for the selected members"));
        new ImageProvider("apply").getResource().attachImageIcon(this);
        putValue(NAME, tr("Apply Role"));
        updateEnabledState();
    }

    @Override
    protected void updateEnabledState() {
        setEnabled(editorAccess.getMemberTable().getSelectedRowCount() > 0);
    }

    protected boolean isEmptyRole() {
        return tfRole.getText() == null || tfRole.getText().trim().isEmpty();
    }

    protected boolean confirmSettingEmptyRole(int onNumMembers) {
        String message = "<html>"
            + trn("You are setting an empty role on {0} object.",
                    "You are setting an empty role on {0} objects.", onNumMembers, onNumMembers)
                    + "<br>"
                    + tr("This is equal to deleting the roles of these objects.") +
                    "<br>"
                    + tr("Do you really want to apply the new role?") + "</html>";
        String[] options = {
                tr("Yes, apply it"),
                tr("No, do not apply")
        };
        int ret = ConditionalOptionPaneUtil.showOptionDialog(
                "relation_editor.confirm_applying_empty_role",
                MainApplication.getMainFrame(),
                message,
                tr("Confirm empty role"),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE,
                options,
                options[0]
        );
        switch(ret) {
        case JOptionPane.YES_OPTION:
        case ConditionalOptionPaneUtil.DIALOG_DISABLED_OPTION:
            return true;
        default:
            return false;
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (isEmptyRole() && !confirmSettingEmptyRole(editorAccess.getMemberTable().getSelectedRowCount())) {
            return;
        }
        editorAccess.getMemberTableModel().updateRole(editorAccess.getMemberTable().getSelectedRows(), tfRole.getText());
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
        updateEnabledState();
    }

    @Override
    public void insertUpdate(DocumentEvent e) {
        updateEnabledState();
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        updateEnabledState();
    }
}
