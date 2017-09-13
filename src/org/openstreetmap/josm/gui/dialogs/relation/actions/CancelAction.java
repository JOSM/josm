// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;

import javax.swing.JOptionPane;
import javax.swing.RootPaneContainer;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.gui.HelpAwareOptionPane;
import org.openstreetmap.josm.gui.HelpAwareOptionPane.ButtonSpec;
import org.openstreetmap.josm.gui.dialogs.relation.IRelationEditor;
import org.openstreetmap.josm.gui.dialogs.relation.MemberTable;
import org.openstreetmap.josm.gui.dialogs.relation.MemberTableModel;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.tagging.TagEditorModel;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompletingTextField;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.InputMapUtils;

/**
 * Cancel the updates and close the dialog
 * @since 9496
 */
public class CancelAction extends SavingAction {

    /**
     * Constructs a new {@code CancelAction}.
     * @param memberTable member table
     * @param memberTableModel member table model
     * @param tagModel tag editor model
     * @param layer OSM data layer
     * @param editor relation editor
     * @param tfRole role text field
     */
    public CancelAction(MemberTable memberTable, MemberTableModel memberTableModel, TagEditorModel tagModel, OsmDataLayer layer,
            IRelationEditor editor, AutoCompletingTextField tfRole) {
        super(memberTable, memberTableModel, tagModel, layer, editor, tfRole);
        putValue(SHORT_DESCRIPTION, tr("Cancel the updates and close the dialog"));
        new ImageProvider("cancel").getResource().attachImageIcon(this);
        putValue(NAME, tr("Cancel"));

        if (editor instanceof RootPaneContainer) {
            InputMapUtils.addEscapeAction(((RootPaneContainer) editor).getRootPane(), this);
        }
        setEnabled(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        memberTable.stopHighlighting();
        Relation snapshot = editor.getRelationSnapshot();
        if ((!memberTableModel.hasSameMembersAs(snapshot) || tagModel.isDirty())
         && !(snapshot == null && tagModel.getTags().isEmpty())) {
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
        ButtonSpec[] options = new ButtonSpec[] {
                new ButtonSpec(
                        tr("Yes, save the changes and close"),
                        ImageProvider.get("ok"),
                        tr("Click to save the changes and close this relation editor"),
                        null /* no specific help topic */
                ),
                new ButtonSpec(
                        tr("No, discard the changes and close"),
                        ImageProvider.get("cancel"),
                        tr("Click to discard the changes and close this relation editor"),
                        null /* no specific help topic */
                ),
                new ButtonSpec(
                        tr("Cancel, continue editing"),
                        ImageProvider.get("cancel"),
                        tr("Click to return to the relation editor and to resume relation editing"),
                        null /* no specific help topic */
                )
        };

        return HelpAwareOptionPane.showOptionDialog(
                Main.parent,
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
