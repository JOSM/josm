// License: GPL. For details, see LICENSE file.
// Author: David Earl
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Collection;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.datatransfer.PrimitiveTransferable;
import org.openstreetmap.josm.gui.datatransfer.data.PrimitiveTransferData;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * An action that duplicates the given nodes. They are not added to the clipboard.
 */
public final class DuplicateAction extends AbstractPasteAction {

    /**
     * Constructs a new {@code DuplicateAction}.
     */
    public DuplicateAction() {
        super(tr("Duplicate"), "duplicate",
                tr("Duplicate selection."),
                Shortcut.registerShortcut("system:duplicate", tr("Edit: {0}", tr("Duplicate")), KeyEvent.VK_D, Shortcut.CTRL), true);
        putValue("help", ht("/Action/Duplicate"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        PrimitiveTransferData data = PrimitiveTransferData.getDataWithReferences(getLayerManager().getEditDataSet().getSelected());
        doPaste(e, new PrimitiveTransferable(data));
    }

    @Override
    protected void updateEnabledState() {
        updateEnabledStateOnCurrentSelection();
    }

    @Override
    protected void updateEnabledState(Collection<? extends OsmPrimitive> selection) {
        updateEnabledStateOnModifiableSelection(selection);
    }
}
