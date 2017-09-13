// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;

import org.openstreetmap.josm.gui.dialogs.relation.IRelationEditor;
import org.openstreetmap.josm.gui.dialogs.relation.MemberTable;
import org.openstreetmap.josm.gui.dialogs.relation.MemberTableModel;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.tagging.TagEditorModel;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompletingTextField;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Apply the updates and close the dialog.
 */
public class OKAction extends SavingAction {

    /**
     * Constructs a new {@code OKAction}.
     * @param memberTable member table
     * @param memberTableModel member table model
     * @param tagModel tag editor model
     * @param layer OSM data layer
     * @param editor relation editor
     * @param tfRole role text field
     */
    public OKAction(MemberTable memberTable, MemberTableModel memberTableModel, TagEditorModel tagModel, OsmDataLayer layer,
            IRelationEditor editor, AutoCompletingTextField tfRole) {
        super(memberTable, memberTableModel, tagModel, layer, editor, tfRole);
        putValue(SHORT_DESCRIPTION, tr("Apply the updates and close the dialog"));
        new ImageProvider("ok").getResource().attachImageIcon(this);
        putValue(NAME, tr("OK"));
        setEnabled(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Config.getPref().put("relation.editor.generic.lastrole", tfRole.getText());
        memberTable.stopHighlighting();
        if (!applyChanges())
            return;
        hideEditor();
    }
}
