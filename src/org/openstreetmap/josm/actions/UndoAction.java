// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.UndoRedoHandler.CommandQueueListener;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Undoes the last command.
 *
 * @author imi
 */
public class UndoAction extends JosmAction implements CommandQueueListener {

    /**
     * Construct the action with "Undo" as label.
     */
    public UndoAction() {
        super(tr("Undo"), "undo", tr("Undo the last action."),
                Shortcut.registerShortcut("system:undo", tr("Edit: {0}", tr("Undo")), KeyEvent.VK_Z, Shortcut.CTRL), true);
        setEnabled(false);
        putValue("help", ht("/Action/Undo"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        MapFrame map = MainApplication.getMap();
        if (map == null)
            return;
        map.repaint();
        MainApplication.undoRedo.undo();
    }

    @Override
    protected void updateEnabledState() {
        setEnabled(Main.main != null && !MainApplication.undoRedo.commands.isEmpty());
    }

    @Override
    public void commandChanged(int queueSize, int redoSize) {
        if (MainApplication.undoRedo.commands.isEmpty()) {
            putValue(NAME, tr("Undo"));
            setTooltip(tr("Undo the last action."));
        } else {
            putValue(NAME, tr("Undo ..."));
            setTooltip(tr("Undo {0}",
                    MainApplication.undoRedo.commands.getLast().getDescriptionText()));
        }
    }
}
