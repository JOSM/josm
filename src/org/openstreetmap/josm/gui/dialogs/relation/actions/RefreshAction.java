// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JRootPane;

import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.UndoRedoHandler.CommandQueueListener;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.gui.HelpAwareOptionPane;
import org.openstreetmap.josm.gui.HelpAwareOptionPane.ButtonSpec;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.dialogs.relation.IRelationEditor;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.PlatformManager;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Refresh relation.
 * @since 9657
 */
public class RefreshAction extends SavingAction implements CommandQueueListener {
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new {@code RefreshAction}.
     * @param editorAccess An interface to access the relation editor contents.
     */
    public RefreshAction(IRelationEditorActionAccess editorAccess) {
        super(editorAccess);
        // CHECKSTYLE.OFF: LineLength
        Shortcut sc = Shortcut.registerShortcut("relationeditor:refresh", tr("Relation Editor: Refresh"), KeyEvent.CHAR_UNDEFINED, Shortcut.NONE);
        // CHECKSTYLE.ON: LineLength
        putValue(SHORT_DESCRIPTION, PlatformManager.getPlatform().makeTooltip(tr("Refresh relation from data layer"), sc));
        new ImageProvider("dialogs/refresh").getResource().attachImageIcon(this, true);
        putValue(NAME, tr("Refresh"));
        IRelationEditor editor = editorAccess.getEditor();
        if (editor instanceof JComponent) {
            JRootPane rootPane = ((JComponent) editor).getRootPane();
            rootPane.getActionMap().put("refresh", this);
            rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(sc.getKeyStroke(), "refresh");
        }
        UndoRedoHandler.getInstance().addCommandQueueListener(this);
        updateEnabledState();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Relation relation = editorAccess.getEditor().getRelation();
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
        editorAccess.getEditor().reloadDataFromRelation();
    }

    @Override
    public void updateEnabledState() {
        Relation relation = editorAccess.getEditor().getRelation();
        Relation snapshot = editorAccess.getEditor().getRelationSnapshot();
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
                MainApplication.getMainFrame(),
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
                MainApplication.getMainFrame(),
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

    /**
     * Allow GC to do its work
     */
    public void destroy() {
        UndoRedoHandler.getInstance().removeCommandQueueListener(this);
    }
}
