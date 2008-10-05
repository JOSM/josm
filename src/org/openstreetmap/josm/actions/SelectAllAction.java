// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.tools.ShortCut;

public class SelectAllAction extends JosmAction {

	public SelectAllAction() {
		super(tr("Select All"),"selectall", tr("Select all undeleted objects in the data layer. This selects incomplete objects too."),
		ShortCut.registerShortCut("system:selectall", tr("Edit: Select all"), KeyEvent.VK_A, ShortCut.GROUP_MENU), true);
	}

	public void actionPerformed(ActionEvent e) {
		Main.ds.setSelected(Main.ds.allNonDeletedPhysicalPrimitives());
	}
}
