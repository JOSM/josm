// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * A special version of the {@link PasteAction} that pastes at the exact source location the item was copied from.
 * @author Michael Zangl
 * @since 10765
 */
public class PasteAtSourcePositionAction extends AbstractPasteAction {

    /**
     * Constructs a new {@link PasteAtSourcePositionAction}.
     */
    public PasteAtSourcePositionAction() {
        super(tr("Paste at source position"), "paste", tr("Paste contents of clipboard at the position they were copied from."),
                Shortcut.registerShortcut("menu:edit:pasteAtSource", tr("Edit: {0}", tr("Paste at source position")),
                        KeyEvent.VK_V, Shortcut.ALT_CTRL), true, "pasteatsource");
        setHelpId(ht("/Action/Paste"));
    }

    @Override
    protected EastNorth computePastePosition(ActionEvent e) {
        // null means use old position
        return null;
    }
}
