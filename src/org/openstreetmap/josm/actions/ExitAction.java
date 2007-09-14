// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import org.openstreetmap.josm.Main;

/**
 * Exit the application. May ask for permition first (if something has changed).
 *  
 * @author imi
 */
public class ExitAction extends JosmAction {
	/**
	 * Construct the action with "Exit" as label
	 */
	public ExitAction() {
		super(tr("Exit"), "exit", tr("Exit the application."), KeyEvent.VK_Q, KeyEvent.CTRL_DOWN_MASK, true);
	}

	public void actionPerformed(ActionEvent e) {
		if (!Main.breakBecauseUnsavedChanges())
			System.exit(0);
	}
}
