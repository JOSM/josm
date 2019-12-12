// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;

import javax.swing.JOptionPane;
import javax.swing.RootPaneContainer;

import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.gui.HelpAwareOptionPane;
import org.openstreetmap.josm.gui.HelpAwareOptionPane.ButtonSpec;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.InputMapUtils;

/**
 * Cancel the updates and close the dialog
 * @since 9496
 */
public class CancelAction extends SavingAction {
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new {@code CancelAction}.
     * @param editorAccess An interface to access the relation editor contents.
     */
    public CancelAction(IRelationEditorActionAccess editorAccess) {
        super(editorAccess);
        putValue(SHORT_DESCRIPTION, tr("Cancel the updates and close the dialog"));
        new ImageProvider("cancel").getResource().attachImageIcon(this);
        putValue(NAME, tr("Cancel"));

        if (getEditor() instanceof RootPaneContainer) {
            InputMapUtils.addEscapeAction(((RootPaneContainer) getEditor()).getRootPane(), this);
        }
        setEnabled(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        getMemberTable().stopHighlighting();
        Relation snapshot = getEditor().getRelationSnapshot();
        if ((!getMemberTableModel().hasSameMembersAs(snapshot) || getTagModel().isDirty())
         && !(snapshot == null && getTagModel().getTags().isEmpty())) {
            //give the user a chance to save the changes
            int ret = confirmClosingByCancel();
            if (ret == 0) { //Yes, save the changes
                //copied from OKAction.run()
                Config.getPref().put("relation.editor.generic.lastrole", tfRole.getText());
                if (!applyChanges())
                    return;
            } else if (ret == 2 || ret == JOptionPane.CLOSED_OPTION) //Cancel, continue editing
                return;
            //in case of "No, discard", there is no extra action to be performed here.
        }
        hideEditor();
    }

    protected int confirmClosingByCancel() {
        ButtonSpec[] options = {
                new ButtonSpec(
                        tr("Yes, save the changes and close"),
                        new ImageProvider("ok"),
                        tr("Click to save the changes and close this relation editor"),
                        null /* no specific help topic */
                ),
                new ButtonSpec(
                        tr("No, discard the changes and close"),
                        new ImageProvider("cancel"),
                        tr("Click to discard the changes and close this relation editor"),
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
                tr("<html>The relation has been changed.<br><br>Do you want to save your changes?</html>"),
                        tr("Unsaved changes"),
                        JOptionPane.WARNING_MESSAGE,
                        null,
                        options,
                        options[0], // OK is default,
                        "/Dialog/RelationEditor#DiscardChanges"
        );
    }
}
