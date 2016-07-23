// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation.actions;

import java.awt.datatransfer.FlavorEvent;
import java.awt.datatransfer.FlavorListener;
import java.awt.event.ActionEvent;

import javax.swing.TransferHandler.TransferSupport;

import org.openstreetmap.josm.gui.datatransfer.ClipboardUtils;
import org.openstreetmap.josm.gui.dialogs.relation.IRelationEditor;
import org.openstreetmap.josm.gui.dialogs.relation.MemberTable;
import org.openstreetmap.josm.gui.dialogs.relation.MemberTransferHandler;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

/**
 * Paste members.
 * @since 9496
 */
public class PasteMembersAction extends AddFromSelectionAction implements FlavorListener {

    /**
     * Constructs a new {@code PasteMembersAction}.
     * @param memberTable member table
     * @param layer OSM data layer
     * @param editor relation editor
     */
    public PasteMembersAction(MemberTable memberTable, OsmDataLayer layer, IRelationEditor editor) {
        super(memberTable, null, null, null, null, layer, editor);
        updateEnabledState();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        new MemberTransferHandler().importData(getSupport());
    }

    private TransferSupport getSupport() {
        return new TransferSupport(memberTable, ClipboardUtils.getClipboard().getContents(null));
    }

    @Override
    protected void updateEnabledState() {
        setEnabled(new MemberTransferHandler().canImport(getSupport()));
    }

    @Override
    public void flavorsChanged(FlavorEvent e) {
        updateEnabledState();
    }
}
