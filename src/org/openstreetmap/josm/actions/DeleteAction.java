// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Collection;

import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.openstreetmap.josm.command.DeleteCommand.DeletionCallback;
import org.openstreetmap.josm.data.osm.DefaultNameFormatter;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationToChildReference;
import org.openstreetmap.josm.gui.ConditionalOptionPaneUtil;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.dialogs.DeleteFromRelationConfirmationDialog;
import org.openstreetmap.josm.gui.widgets.JMultilineLabel;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Action that deletes selected objects.
 * @since 770
 */
public final class DeleteAction extends JosmAction {

    /**
     * The default {@link DeletionCallback} for {@code DeleteCommand}.
     * @since 12760
     */
    public static final DeletionCallback defaultDeletionCallback = new DeletionCallback() {
        @Override
        public boolean checkAndConfirmOutlyingDelete(Collection<? extends OsmPrimitive> primitives,
                Collection<? extends OsmPrimitive> ignore) {
            return DeleteAction.checkAndConfirmOutlyingDelete(primitives, ignore);
        }

        @Override
        public boolean confirmRelationDeletion(Collection<Relation> relations) {
            return DeleteAction.confirmRelationDeletion(relations);
        }

        @Override
        public boolean confirmDeletionFromRelation(Collection<RelationToChildReference> references) {
            DeleteFromRelationConfirmationDialog dialog = DeleteFromRelationConfirmationDialog.getInstance();
            dialog.getModel().populate(references);
            dialog.setVisible(true);
            return !dialog.isCanceled();
        }
    };

    /**
     * Constructs a new {@code DeleteAction}.
     */
    public DeleteAction() {
        super(tr("Delete"), "dialogs/delete", tr("Delete selected objects."),
                Shortcut.registerShortcut("system:delete", tr("Edit: {0}", tr("Delete")), KeyEvent.VK_DELETE, Shortcut.DIRECT), true);
        setHelpId(ht("/Action/Delete"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        MapFrame map = MainApplication.getMap();
        if (!isEnabled() || !map.mapView.isActiveLayerVisible())
            return;
        map.mapModeDelete.doActionPerformed(e);
    }

    @Override
    protected void updateEnabledState() {
        updateEnabledStateOnCurrentSelection();
    }

    @Override
    protected void updateEnabledState(Collection<? extends OsmPrimitive> selection) {
        updateEnabledStateOnModifiableSelection(selection);
    }

    /**
     * Check whether user is about to delete data outside of the download area.
     * Request confirmation if he is.
     * @param primitives the primitives to operate on
     * @param ignore {@code null} or a primitive to be ignored
     * @return true, if operating on outlying primitives is OK; false, otherwise
     * @since 12749 (moved from DeleteCommand)
     */
    public static boolean checkAndConfirmOutlyingDelete(Collection<? extends OsmPrimitive> primitives,
            Collection<? extends OsmPrimitive> ignore) {
        return checkAndConfirmOutlyingOperation("delete",
                tr("Delete confirmation"),
                tr("You are about to delete nodes outside of the area you have downloaded."
                        + "<br>"
                        + "This can cause problems because other objects (that you do not see) might use them."
                        + "<br>"
                        + "Do you really want to delete?"),
                tr("You are about to delete incomplete objects."
                        + "<br>"
                        + "This will cause problems because you don''t see the real object."
                        + "<br>" + "Do you really want to delete?"),
                primitives, ignore);
    }

    /**
     * Confirm before deleting a relation, as it is a common newbie error.
     * @param relations relation to check for deletion
     * @return {@code true} if user confirms the deletion
     * @since 12760
     */
    public static boolean confirmRelationDeletion(Collection<Relation> relations) {
        JPanel msg = new JPanel(new GridBagLayout());
        msg.add(new JMultilineLabel("<html>" + trn(
                "You are about to delete {0} relation: {1}"
                + "<br/>"
                + "This step is rarely necessary and cannot be undone easily after being uploaded to the server."
                + "<br/>"
                + "Do you really want to delete?",
                "You are about to delete {0} relations: {1}"
                + "<br/>"
                + "This step is rarely necessary and cannot be undone easily after being uploaded to the server."
                + "<br/>"
                + "Do you really want to delete?",
                relations.size(), relations.size(), DefaultNameFormatter.getInstance().formatAsHtmlUnorderedList(relations, 20))
                + "</html>"));
        return ConditionalOptionPaneUtil.showConfirmationDialog(
                "delete_relations",
                MainApplication.getMainFrame(),
                msg,
                tr("Delete relation?"),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                JOptionPane.YES_OPTION);
    }
}
