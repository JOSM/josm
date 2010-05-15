// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.tools.Shortcut;

public class UnselectAllAction extends JosmAction {

    public UnselectAllAction() {
        super(tr("Unselect All"), "unselectall", tr("Unselect all objects."),
                Shortcut.registerShortcut("edit:unselectall", tr("Edit: {0}", tr("Unselect All")), KeyEvent.VK_U, Shortcut.GROUP_EDIT), true);
        // this is not really GROUP_EDIT, but users really would complain if the yhad to reconfigure because we put
        // the correct group in

        // Add extra shortcut C-S-a
        Main.registerActionShortcut(this, Shortcut.registerShortcut("edit:unselectallfocus", tr("Edit: {0}", tr("Unselect All (Focus)")),
                KeyEvent.VK_A, Shortcut.GROUP_MENU, Shortcut.SHIFT_DEFAULT));



        // Add extra shortcut ESCAPE
        /*
         * FIXME: this isn't optimal. In a better world the mapmode actions
         * would be able to capture keyboard events and react accordingly. But
         * for now this is a reasonable approximation.
         */
        Main.registerActionShortcut(this, Shortcut.registerShortcut("edit:unselectallescape", tr("Edit: {0}", tr("Unselect All (Escape)")),
                KeyEvent.VK_ESCAPE, Shortcut.GROUP_DIRECT));

        putValue("help", ht("/Action/UnselectAll"));
    }

    public void actionPerformed(ActionEvent e) {
        if (!isEnabled())
            return;
        getCurrentDataSet().setSelected();
    }
    /**
     * Refreshes the enabled state
     *
     */
    @Override
    protected void updateEnabledState() {
        setEnabled(getEditLayer() != null);
    }
}
