// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import org.openstreetmap.josm.tools.Shortcut;

/**
 * User action to clear the current selection.
 */
public class UnselectAllAction extends JosmAction {

    /**
     * Constructs a new {@code UnselectAllAction}.
     */
    public UnselectAllAction() {
        super(tr("Unselect All"), "unselectall", tr("Unselect all objects."),
            Shortcut.registerShortcut("edit:unselectall", tr("Edit: {0}",
            tr("Unselect All")), KeyEvent.VK_ESCAPE, Shortcut.DIRECT), true);

        setHelpId(ht("/Action/UnselectAll"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!isEnabled())
            return;
        getLayerManager().getActiveData().setSelected();
    }

    /**
     * Refreshes the enabled state
     */
    @Override
    protected void updateEnabledState() {
        setEnabled(getLayerManager().getActiveData() != null);
    }
}
