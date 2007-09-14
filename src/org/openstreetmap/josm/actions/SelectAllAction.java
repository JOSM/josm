// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import org.openstreetmap.josm.Main;

public class SelectAllAction extends JosmAction {

	public SelectAllAction() {
		super(tr("Select All"),"selectall", tr("Select all undeleted objects in the data layer. This selects incomplete objects too."), KeyEvent.VK_A, KeyEvent.CTRL_DOWN_MASK, true);
    }

	public void actionPerformed(ActionEvent e) {
		Main.ds.setSelected(Main.ds.allNonDeletedPrimitives());
	}
}
