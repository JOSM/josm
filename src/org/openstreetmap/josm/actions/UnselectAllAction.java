// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import org.openstreetmap.josm.Main;

public class UnselectAllAction extends JosmAction {

	public UnselectAllAction() {
		super(tr("Unselect All"),"unselectall", tr("Unselect all objects."), KeyEvent.VK_A, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK, true);
    }

	public void actionPerformed(ActionEvent e) {
		Main.ds.setSelected();
	}
}
