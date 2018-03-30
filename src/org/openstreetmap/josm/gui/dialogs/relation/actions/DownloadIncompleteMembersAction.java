// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.dialogs.relation.DownloadRelationMemberTask;
import org.openstreetmap.josm.gui.dialogs.relation.IRelationEditor;
import org.openstreetmap.josm.gui.dialogs.relation.MemberTable;
import org.openstreetmap.josm.gui.dialogs.relation.MemberTableModel;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Download all incomplete members.
 * @since 9496
 */
public class DownloadIncompleteMembersAction extends AbstractRelationEditorAction {

    /**
     * Constructs a new {@code DownloadIncompleteMembersAction}.
     * @param memberTable member table
     * @param memberTableModel member table model
     * @param actionMapKey action map key
     * @param layer OSM data layer
     * @param editor relation editor
     */
    public DownloadIncompleteMembersAction(MemberTable memberTable, MemberTableModel memberTableModel, String actionMapKey,
            OsmDataLayer layer, IRelationEditor editor) {
        super(memberTable, memberTableModel, actionMapKey, layer, editor);
        Shortcut sc = Shortcut.registerShortcut("relationeditor:downloadincomplete", tr("Relation Editor: Download Members"),
            KeyEvent.VK_HOME, Shortcut.ALT);
        sc.setAccelerator(this);
        putValue(SHORT_DESCRIPTION, Main.platform.makeTooltip(tr("Download all incomplete members"), sc));
        new ImageProvider("dialogs/relation", "downloadincomplete").getResource().attachImageIcon(this, true);
        putValue(NAME, tr("Download Members"));
        updateEnabledState();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!isEnabled())
            return;
        MainApplication.worker.submit(new DownloadRelationMemberTask(
                editor.getRelation(),
                memberTableModel.getIncompleteMemberPrimitives(),
                layer,
                (Dialog) editor)
        );
    }

    @Override
    protected void updateEnabledState() {
        setEnabled(memberTableModel.hasIncompleteMembers() && canDownload());
    }
}
