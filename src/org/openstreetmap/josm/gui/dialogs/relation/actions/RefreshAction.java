// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.JComponent;
import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.UndoRedoHandler.CommandQueueListener;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.gui.HelpAwareOptionPane;
import org.openstreetmap.josm.gui.HelpAwareOptionPane.ButtonSpec;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.dialogs.relation.IRelationEditor;
import org.openstreetmap.josm.gui.dialogs.relation.MemberTable;
import org.openstreetmap.josm.gui.dialogs.relation.MemberTableModel;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.tagging.TagEditorModel;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Refresh relation.
 * @since 9657
 */
public class RefreshAction extends SavingAction implements CommandQueueListener {

    /**
     * Constructs a new {@code RefreshAction}.
     * @param memberTable member table
     * @param memberTableModel member table model
     * @param layer OSM data layer
     * @param editor relation editor
     * @param tagModel tag editor model
     */
    public RefreshAction(MemberTable memberTable, MemberTableModel memberTableModel, TagEditorModel tagModel, OsmDataLayer layer,
            IRelationEditor editor) {
        super(memberTable, memberTableModel, tagModel, layer, editor, null);
        // CHECKSTYLE.OFF: LineLength
        Shortcut sc = Shortcut.registerShortcut("relationeditor:refresh", tr("Relation Editor: Refresh"), KeyEvent.CHAR_UNDEFINED, Shortcut.NONE);
        // CHECKSTYLE.ON: LineLength
        putValue(SHORT_DESCRIPTION, Main.platform.makeTooltip(tr("Refresh relation from data layer"), sc));
        new ImageProvider("dialogs/refresh").getResource().attachImageIcon(this, true);
        putValue(NAME, tr("Refresh"));
        if (editor instanceof JComponent) {
            ((JComponent) editor).getRootPane().getActionMap().put("refresh", this);
            ((JComponent) editor).getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(sc.getKeyStroke(), "refresh");
        }
        MainApplication.undoRedo.addCommandQueueListener(this);
        updateEnabledState();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Relation relation = editor.getRelation();
        if (relation == null)
            return;
        if (relation.isDeleted()) {
            if (confirmCloseDeletedRelation() == 0) {
                hideEditor();
            }
            return;
        }
        if (isEditorDirty() && confirmDiscardDirtyData() != 0)
            return;
        editor.reloadDataFromRelation();
    }

    @Override
    public void updateEnabledState() {
        Relation relation = editor.getRelation();
        Relation snapshot = editor.getRelationSnapshot();
        setEnabled(snapshot != null && (
            !relation.hasEqualTechnicalAttributes(snapshot) ||
            !relation.hasEqualSemanticAttributes(snapshot)
        ));
    }

    protected int confirmDiscardDirtyData() {
        ButtonSpec[] options = new ButtonSpec[] {
                new ButtonSpec(
                        tr("Yes, discard changes and reload"),
                        new ImageProvider("ok"),
                        tr("Click to discard the changes and reload data from layer"),
                        null /* no specific help topic */
                ),
                new ButtonSpec(
                        tr("Cancel, continue editing"),
                        new ImageProvider("cancel"),
                        tr("Click to return to the relation editor and to resume relation editing"),
                        null /* no specific help topic */
                )
        };

        return HelpAwareOptionPane.showOptionDialog(
                Main.parent,
                tr("<html>You have unsaved changes in this editor window.<br>"+
                   "<br>Do you want to discard these changes and reload data from layer?</html>"),
                        tr("Unsaved changes"),
                        JOptionPane.WARNING_MESSAGE,
                        null,
                        options,
                        options[1], // Cancel is default
                        "/Dialog/RelationEditor#Reload"
        );
    }

    protected int confirmCloseDeletedRelation() {
        ButtonSpec[] options = new ButtonSpec[] {
                new ButtonSpec(
                        tr("Yes"),
                        new ImageProvider("ok"),
                        tr("Click to close window"),
                        null /* no specific help topic */
                ),
                new ButtonSpec(
                        tr("No, continue editing"),
                        new ImageProvider("cancel"),
                        tr("Click to return to the relation editor and to resume relation editing"),
                        null /* no specific help topic */
                )
        };

        return HelpAwareOptionPane.showOptionDialog(
                Main.parent,
                tr("<html>Relation has been deleted outside editor.<br><br>Do you want to close this window?</html>"),
                        tr("Deleted relation"),
                        JOptionPane.WARNING_MESSAGE,
                        null,
                        options,
                        options[0], // Yes is default
                        "/Dialog/RelationEditor#Reload"
        );
    }

    @Override
    public void commandChanged(int queueSize, int redoSize) {
        updateEnabledState();
    }
}
