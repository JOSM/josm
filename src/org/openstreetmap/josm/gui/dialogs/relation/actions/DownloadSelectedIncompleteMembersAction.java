// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Dialog;
import java.awt.event.ActionEvent;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.dialogs.relation.DownloadRelationMemberTask;
import org.openstreetmap.josm.gui.dialogs.relation.IRelationEditor;
import org.openstreetmap.josm.gui.dialogs.relation.MemberTable;
import org.openstreetmap.josm.gui.dialogs.relation.MemberTableModel;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.io.OnlineResource;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Download selected incomplete members.
 * @since 9496
 */
public class DownloadSelectedIncompleteMembersAction extends AbstractRelationEditorAction {

    /**
     * Constructs a new {@code DownloadSelectedIncompleteMembersAction}.
     * @param memberTable member table
     * @param memberTableModel member table model
     * @param actionMapKey action map key
     * @param layer OSM data layer
     * @param editor relation editor
     */
    public DownloadSelectedIncompleteMembersAction(MemberTable memberTable, MemberTableModel memberTableModel, String actionMapKey,
            OsmDataLayer layer, IRelationEditor editor) {
        super(memberTable, memberTableModel, actionMapKey, layer, editor);
        putValue(SHORT_DESCRIPTION, tr("Download selected incomplete members"));
        putValue(SMALL_ICON, ImageProvider.get("dialogs/relation", "downloadincompleteselected"));
        putValue(NAME, tr("Download Members"));
        updateEnabledState();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!isEnabled())
            return;
        MainApplication.worker.submit(new DownloadRelationMemberTask(
                editor.getRelation(),
                memberTableModel.getSelectedIncompleteMemberPrimitives(),
                layer,
                (Dialog) editor)
        );
    }

    @Override
    protected void updateEnabledState() {
        setEnabled(memberTableModel.hasIncompleteSelectedMembers() && !Main.isOffline(OnlineResource.OSM_API));
    }
}
