// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.tools.ShortCut;

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
		ShortCut.registerShortCut("system:undo", tr("Edit: Undo"), KeyEvent.VK_Z, ShortCut.GROUP_MENU), true);
		setEnabled(false);
	}

	public void actionPerformed(ActionEvent e) {
		if (Main.map == null)
			return;
		Main.map.repaint();
		Main.main.undoRedo.undo();
	}
}
