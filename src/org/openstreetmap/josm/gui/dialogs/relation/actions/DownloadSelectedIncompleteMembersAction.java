// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Dialog;
import java.awt.event.ActionEvent;

import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.dialogs.relation.DownloadRelationMemberTask;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Download selected incomplete members.
 * @since 9496
 */
public class DownloadSelectedIncompleteMembersAction extends AbstractRelationEditorAction {
	private static final long serialVersionUID = 1L;

    /**
     * Constructs a new {@code DownloadSelectedIncompleteMembersAction}.
     * @param memberTable member table
     * @param memberTableModel member table model
     * @param actionMapKey action map key
     * @param layer OSM data layer
     * @param editor relation editor
     */
    public DownloadSelectedIncompleteMembersAction(IRelationEditorActionAccess editorAccess) {
        super(editorAccess, IRelationEditorUpdateOn.MEMBER_TABLE_CHANGE, IRelationEditorUpdateOn.MEMBER_TABLE_SELECTION);
        putValue(SHORT_DESCRIPTION, tr("Download selected incomplete members"));
        new ImageProvider("dialogs/relation", "downloadincompleteselected").getResource().attachImageIcon(this, true);
        putValue(NAME, tr("Download members"));
        updateEnabledState();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!isEnabled())
            return;
        MainApplication.worker.submit(new DownloadRelationMemberTask(
        		getEditor().getRelation(),
                getMemberTableModel().getSelectedIncompleteMemberPrimitives(),
                getLayer(),
                (Dialog) getEditor())
        );
    }

    @Override
    protected void updateEnabledState() {
        setEnabled(getMemberTableModel().hasIncompleteSelectedMembers() && canDownload());
    }
}
