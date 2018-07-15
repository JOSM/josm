// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation.actions;

import java.awt.datatransfer.FlavorEvent;
import java.awt.datatransfer.FlavorListener;
import java.awt.event.ActionEvent;
import java.util.Optional;

import javax.swing.TransferHandler.TransferSupport;

import org.openstreetmap.josm.gui.datatransfer.ClipboardUtils;
import org.openstreetmap.josm.gui.dialogs.relation.MemberTransferHandler;
import org.openstreetmap.josm.tools.Logging;

/**
 * Paste members.
 * @since 9496
 */
public class PasteMembersAction extends AddFromSelectionAction implements FlavorListener {
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new {@code PasteMembersAction}.
     * @param editorAccess An interface to access the relation editor contents.
     */
    public PasteMembersAction(IRelationEditorActionAccess editorAccess) {
        super(editorAccess);
        updateEnabledState();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            new MemberTransferHandler().importData(getSupport());
        } catch (IllegalStateException ex) {
            Logging.error(ex);
        }
    }

    private TransferSupport getSupport() {
        return new TransferSupport(editorAccess.getMemberTable(), Optional.ofNullable(ClipboardUtils.getClipboardContent())
                .orElseThrow(() -> new IllegalStateException("Failed to retrieve clipboard content")));
    }

    @Override
    protected void updateEnabledState() {
        try {
            setEnabled(new MemberTransferHandler().canImport(getSupport()));
        } catch (IllegalStateException ex) {
            Logging.error(ex);
        }
    }

    @Override
    public void flavorsChanged(FlavorEvent e) {
        updateEnabledState();
    }
}
