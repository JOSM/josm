// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.gui.help.HelpUtil.ht;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Undoes the last command.
 *
 * @author imi
 */
public class UndoAction extends JosmAction {

    /**
     * Construct the action with "Undo" as label.
     */
    public UndoAction() {
        super(tr("Undo"), "undo", tr("Undo the last action."),
                Shortcut.registerShortcut("system:undo", tr("Edit: {0}", tr("Undo")), KeyEvent.VK_Z, Shortcut.GROUP_MENU), true);
        setEnabled(false);
        putValue("help", ht("/Action/Undo"));
    }

    public void actionPerformed(ActionEvent e) {
        if (Main.map == null)
            return;
        Main.map.repaint();
        Main.main.undoRedo.undo();
    }

    @Override
    protected void updateEnabledState() {
        setEnabled(Main.map != null);
    }
}
