// License: GPL. For details, see LICENSE file.
// Author: David Earl
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.KeyEvent;

import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Paste OSM primitives from clipboard to the current edit layer.
 * @since 404
 */
public final class PasteAction extends AbstractPasteAction {

    /**
     * Constructs a new {@code PasteAction}.
     */
    public PasteAction() {
        super(tr("Paste"), "paste", tr("Paste contents of clipboard."),
                Shortcut.registerShortcut("system:paste", tr("Edit: {0}", tr("Paste")), KeyEvent.VK_V, Shortcut.CTRL), true);
        setHelpId(ht("/Action/Paste"));
        // CUA shortcut for paste (https://en.wikipedia.org/wiki/IBM_Common_User_Access#Description)
        MainApplication.registerActionShortcut(this,
                Shortcut.registerShortcut("system:paste:cua", tr("Edit: {0}", tr("Paste")), KeyEvent.VK_INSERT, Shortcut.SHIFT));
    }
}
