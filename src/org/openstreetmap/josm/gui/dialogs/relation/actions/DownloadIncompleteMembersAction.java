// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.dialogs.relation.DownloadRelationMemberTask;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Download all incomplete members.
 * @since 9496
 */
public class DownloadIncompleteMembersAction extends AbstractRelationEditorAction {
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new {@code DownloadIncompleteMembersAction}.
     * @param editorAccess An interface to access the relation editor contents.
     * @param actionMapKey action map key
     */
    public DownloadIncompleteMembersAction(IRelationEditorActionAccess editorAccess, String actionMapKey) {
        super(editorAccess, actionMapKey, IRelationEditorUpdateOn.MEMBER_TABLE_CHANGE);
        Shortcut sc = Shortcut.registerShortcut("relationeditor:downloadincomplete", tr("Relation Editor: Download Members"),
            KeyEvent.VK_HOME, Shortcut.ALT);
        sc.setAccelerator(this);
        putValue(SHORT_DESCRIPTION, Main.platform.makeTooltip(tr("Download all incomplete members"), sc));
        new ImageProvider("dialogs/relation", "downloadincomplete").getResource().attachImageIcon(this, true);
        putValue(NAME, tr("Download members"));
        updateEnabledState();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!isEnabled())
            return;
        MainApplication.worker.submit(new DownloadRelationMemberTask(
                getEditor().getRelation(),
                getMemberTableModel().getIncompleteMemberPrimitives(),
                getLayer(),
                (Dialog) getEditor())
        );
    }

    @Override
    protected void updateEnabledState() {
        setEnabled(getMemberTableModel().hasIncompleteMembers() && canDownload());
    }
}
