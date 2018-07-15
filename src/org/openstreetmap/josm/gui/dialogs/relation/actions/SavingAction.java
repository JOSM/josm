// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.AddCommand;
import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.command.conflict.ConflictAddCommand;
import org.openstreetmap.josm.data.conflict.Conflict;
import org.openstreetmap.josm.data.osm.DefaultNameFormatter;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.gui.HelpAwareOptionPane;
import org.openstreetmap.josm.gui.HelpAwareOptionPane.ButtonSpec;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.dialogs.relation.RelationDialogManager;
import org.openstreetmap.josm.gui.dialogs.relation.RelationEditor;
import org.openstreetmap.josm.gui.tagging.TagEditorModel;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompletingTextField;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Utils;

/**
 * Abstract superclass of relation saving actions (OK, Apply, Cancel).
 * @since 9496
 */
abstract class SavingAction extends AbstractRelationEditorAction {
    private static final long serialVersionUID = 1L;

    protected final AutoCompletingTextField tfRole;

    protected SavingAction(IRelationEditorActionAccess editorAccess, IRelationEditorUpdateOn... updateOn) {
        super(editorAccess, updateOn);
        this.tfRole = editorAccess.getTextFieldRole();
    }

    /**
     * apply updates to a new relation
     * @param tagEditorModel tag editor model
     */
    protected void applyNewRelation(TagEditorModel tagEditorModel) {
        final Relation newRelation = new Relation();
        tagEditorModel.applyToPrimitive(newRelation);
        getMemberTableModel().applyToRelation(newRelation);
        List<RelationMember> newMembers = new ArrayList<>();
        for (RelationMember rm: newRelation.getMembers()) {
            if (!rm.getMember().isDeleted()) {
                newMembers.add(rm);
            }
        }
        if (newRelation.getMembersCount() != newMembers.size()) {
            newRelation.setMembers(newMembers);
            String msg = tr("One or more members of this new relation have been deleted while the relation editor\n" +
            "was open. They have been removed from the relation members list.");
            JOptionPane.showMessageDialog(Main.parent, msg, tr("Warning"), JOptionPane.WARNING_MESSAGE);
        }
        // If the user wanted to create a new relation, but hasn't added any members or
        // tags, don't add an empty relation
        if (newRelation.getMembersCount() == 0 && !newRelation.hasKeys())
            return;
        MainApplication.undoRedo.add(new AddCommand(getLayer().getDataSet(), newRelation));

        // make sure everybody is notified about the changes
        //
        getEditor().setRelation(newRelation);
        if (getEditor() instanceof RelationEditor) {
            RelationDialogManager.getRelationDialogManager().updateContext(
                    getLayer(), getEditor().getRelation(), (RelationEditor) getEditor());
        }
        // Relation list gets update in EDT so selecting my be postponed to following EDT run
        SwingUtilities.invokeLater(() -> MainApplication.getMap().relationListDialog.selectRelation(newRelation));
    }

    /**
     * Apply the updates for an existing relation which has been changed outside of the relation editor.
     * @param tagEditorModel tag editor model
     */
    protected void applyExistingConflictingRelation(TagEditorModel tagEditorModel) {
        Relation editedRelation = new Relation(editorAccess.getEditor().getRelation());
        tagEditorModel.applyToPrimitive(editedRelation);
        editorAccess.getMemberTableModel().applyToRelation(editedRelation);
        Conflict<Relation> conflict = new Conflict<>(editorAccess.getEditor().getRelation(), editedRelation);
        MainApplication.undoRedo.add(new ConflictAddCommand(getLayer().getDataSet(), conflict));
    }

    /**
     * Apply the updates for an existing relation which has not been changed outside of the relation editor.
     * @param tagEditorModel tag editor model
     */
    protected void applyExistingNonConflictingRelation(TagEditorModel tagEditorModel) {
        Relation originRelation = editorAccess.getEditor().getRelation();
        Relation editedRelation = new Relation(originRelation);
        tagEditorModel.applyToPrimitive(editedRelation);
        getMemberTableModel().applyToRelation(editedRelation);
        if (!editedRelation.hasEqualSemanticAttributes(originRelation, false)) {
            MainApplication.undoRedo.add(new ChangeCommand(originRelation, editedRelation));
        }
    }

    protected boolean confirmClosingBecauseOfDirtyState() {
        ButtonSpec[] options = new ButtonSpec[] {
                new ButtonSpec(
                        tr("Yes, create a conflict and close"),
                        new ImageProvider("ok"),
                        tr("Click to create a conflict and close this relation editor"),
                        null /* no specific help topic */
                ),
                new ButtonSpec(
                        tr("No, continue editing"),
                        new ImageProvider("cancel"),
                        tr("Click to return to the relation editor and to resume relation editing"),
                        null /* no specific help topic */
                )
        };

        int ret = HelpAwareOptionPane.showOptionDialog(
                Main.parent,
                tr("<html>This relation has been changed outside of the editor.<br>"
                        + "You cannot apply your changes and continue editing.<br>"
                        + "<br>"
                        + "Do you want to create a conflict and close the editor?</html>"),
                        tr("Conflict in data"),
                        JOptionPane.WARNING_MESSAGE,
                        null,
                        options,
                        options[0], // OK is default
                        "/Dialog/RelationEditor#RelationChangedOutsideOfEditor"
        );
        if (ret == 0) {
            MainApplication.getMap().conflictDialog.unfurlDialog();
        }
        return ret == 0;
    }

    protected void warnDoubleConflict() {
        JOptionPane.showMessageDialog(
                Main.parent,
                tr("<html>Layer ''{0}'' already has a conflict for object<br>"
                        + "''{1}''.<br>"
                        + "Please resolve this conflict first, then try again.</html>",
                        Utils.escapeReservedCharactersHTML(getLayer().getName()),
                        Utils.escapeReservedCharactersHTML(getEditor().getRelation().getDisplayName(DefaultNameFormatter.getInstance()))
                ),
                tr("Double conflict"),
                JOptionPane.WARNING_MESSAGE
        );
    }

    @Override
    protected void updateEnabledState() {
        // Do nothing
    }

    protected boolean applyChanges() {
        if (editorAccess.getEditor().getRelation() == null) {
            applyNewRelation(getTagModel());
        } else if (isEditorDirty()) {
            if (editorAccess.getEditor().isDirtyRelation()) {
                if (confirmClosingBecauseOfDirtyState()) {
                    if (getLayer().getConflicts().hasConflictForMy(editorAccess.getEditor().getRelation())) {
                        warnDoubleConflict();
                        return false;
                    }
                    applyExistingConflictingRelation(getTagModel());
                    hideEditor();
                } else
                    return false;
            } else {
                applyExistingNonConflictingRelation(getTagModel());
            }
        }
        editorAccess.getEditor().setRelation(editorAccess.getEditor().getRelation());
        return true;
    }

    protected void hideEditor() {
        if (editorAccess.getEditor() instanceof Component) {
            ((Component) editorAccess.getEditor()).setVisible(false);
        }
    }

    protected boolean isEditorDirty() {
        Relation snapshot = editorAccess.getEditor().getRelationSnapshot();
        return (snapshot != null && !getMemberTableModel().hasSameMembersAs(snapshot)) || getTagModel().isDirty();
    }
}
