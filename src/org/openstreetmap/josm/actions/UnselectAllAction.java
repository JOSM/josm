// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.JComponent;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.tools.ShortCut;

public class UnselectAllAction extends JosmAction {

	public UnselectAllAction() {
		super(tr("Unselect All"), "unselectall", tr("Unselect all objects."),
		ShortCut.registerShortCut("edit:unselectall", tr("Edit: Unselect all"), KeyEvent.VK_U, ShortCut.GROUP_EDIT), true);
		// this is not really GROUP_EDIT, but users really would complain if the yhad to reconfigure because we put
		// the correct group in

		// Add extra shortcut C-S-a
		Main.contentPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
		ShortCut.registerShortCut("edit:unselectall2", tr("Edit: Unselect all (2)"), KeyEvent.VK_A, ShortCut.GROUP_MENU).getKeyStroke(),
		tr("Unselect All"));

		// Add extra shortcut ESCAPE
		/*
		 * FIXME: this isn't optimal. In a better world the mapmode actions
		 * would be able to capture keyboard events and react accordingly. But
		 * for now this is a reasonable approximation.
		 */
		Main.contentPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
		ShortCut.registerShortCut("edit:unselectall3", tr("Edit: Unselect all (3)"), KeyEvent.VK_ESCAPE, ShortCut.GROUP_DIRECT).getKeyStroke(),
		tr("Unselect All"));
	}

	public void actionPerformed(ActionEvent e) {
		Main.ds.setSelected();
	}
}
