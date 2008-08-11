// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import org.openstreetmap.josm.Main;

public final class DeleteAction extends JosmAction {

	public DeleteAction() {
		super(tr("Delete"), "dialogs/delete", tr("Delete selected objects."),
		        KeyEvent.VK_DELETE, 0, true);
		setEnabled(true);
	}

	public void actionPerformed(ActionEvent e) {
		new org.openstreetmap.josm.actions.mapmode.DeleteAction(Main.map)
		        .doActionPerformed(e);
	}
}
