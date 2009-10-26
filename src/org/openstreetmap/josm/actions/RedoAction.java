// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.gui.help.HelpUtil.ht;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Redoes the last command.
 *
 * @author imi
 */
public class RedoAction extends JosmAction {

    /**
     * Construct the action with "Redo" as label.
     */
    public RedoAction() {
        super(tr("Redo"), "redo", tr("Redo the last undone action."),
                Shortcut.registerShortcut("system:redo", tr("Edit: {0}", tr("Redo")), KeyEvent.VK_Y, Shortcut.GROUP_MENU), true);
        setEnabled(false);
        putValue("help", ht("/Action/Redo"));
    }

    public void actionPerformed(ActionEvent e) {
        if (Main.map == null)
            return;
        Main.map.repaint();
        Main.main.undoRedo.redo();
    }

    @Override
    protected void updateEnabledState() {
        setEnabled(Main.map != null);
    }
}
