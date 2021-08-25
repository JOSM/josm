// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.openstreetmap.josm.command.AddCommand;
import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.command.ChangeMembersCommand;
import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.conflict.ConflictAddCommand;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.conflict.Conflict;
import org.openstreetmap.josm.data.osm.DefaultNameFormatter;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.gui.HelpAwareOptionPane;
import org.openstreetmap.josm.gui.HelpAwareOptionPane.ButtonSpec;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.dialogs.relation.IRelationEditor;
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
        // If the user wanted to create a new relation, but hasn't added any members or
        // tags, don't add an empty relation
        if (newRelation.isEmpty() && !newRelation.hasKeys())
            return;
        UndoRedoHandler.getInstance().add(new AddCommand(getLayer().getDataSet(), newRelation));

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
        UndoRedoHandler.getInstance().add(new ConflictAddCommand(getLayer().getDataSet(), conflict));
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
        List<Command> cmds = new ArrayList<>();
        if (!originRelation.getMembers().equals(editedRelation.getMembers())) {
            cmds.add(new ChangeMembersCommand(originRelation, editedRelation.getMembers()));
        }
        Command cmdProps = ChangePropertyCommand.build(originRelation, editedRelation);
        if (cmdProps != null)
            cmds.add(cmdProps);
        if (cmds.size() >= 2) {
            UndoRedoHandler.getInstance().add(new ChangeCommand(originRelation, editedRelation));
        } else if (!cmds.isEmpty()) {
            UndoRedoHandler.getInstance().add(cmds.get(0));
            editedRelation.setMembers(null); // see #19885
        }
    }

    protected boolean confirmClosingBecauseOfDirtyState() {
        ButtonSpec[] options = {
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
                MainApplication.getMainFrame(),
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
                MainApplication.getMainFrame(),
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
        IRelationEditor editor = editorAccess.getEditor();
        if (editor.getRelation() == null) {
            applyNewRelation(getTagModel());
        } else if (isEditorDirty()) {
            if (editor.isDirtyRelation()) {
                if (confirmClosingBecauseOfDirtyState()) {
                    if (getLayer().getConflicts().hasConflictForMy(editor.getRelation())) {
                        warnDoubleConflict();
                        return false;
                    }
                    applyExistingConflictingRelation(getTagModel());
                    hideEditor();
                    return false;
                } else
                    return false;
            } else {
                applyExistingNonConflictingRelation(getTagModel());
            }
        }
        editor.setRelation(editor.getRelation());
        return true;
    }

    protected void hideEditor() {
        if (editorAccess.getEditor() instanceof Component) {
            ((Component) editorAccess.getEditor()).setVisible(false);
            editorAccess.getEditor().setRelation(null);
        }
    }

    protected boolean isEditorDirty() {
        Relation snapshot = editorAccess.getEditor().getRelationSnapshot();
        return (snapshot != null && !getMemberTableModel().hasSameMembersAs(snapshot)) || getTagModel().isDirty()
                || getEditor().getRelation() == null || getEditor().getRelation().getDataSet() == null;
    }
}
