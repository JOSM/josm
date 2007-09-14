// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import org.openstreetmap.josm.Main;


/**
 * Redoes the last command.
 * 
 * @author imi
 */
public class RedoAction extends JosmAction {

	/**
	 * Construct the action with "Undo" as label.
	 */
	public RedoAction() {
		super(tr("Redo"), "redo", tr("Redo the last undone action."), KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK, true);
		setEnabled(false);
	}

	public void actionPerformed(ActionEvent e) {
		if (Main.map == null)
			return;
		Main.map.repaint();
		Main.main.undoRedo.redo();
	}
}
