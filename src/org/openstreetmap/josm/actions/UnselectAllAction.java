// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.KeyStroke;
import javax.swing.JComponent;

import org.openstreetmap.josm.Main;

public class UnselectAllAction extends JosmAction {

	public UnselectAllAction() {
		super(tr("Unselect All"), "unselectall", tr("Unselect all objects."),
		        KeyEvent.VK_U, 0, true);

		// Add extra shortcut C-S-a
		Main.contentPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
		        KeyStroke.getKeyStroke(KeyEvent.VK_A, KeyEvent.CTRL_DOWN_MASK
		                | KeyEvent.SHIFT_DOWN_MASK), tr("Unselect All"));

		// Add extra shortcut ESCAPE
		/*
		 * FIXME: this isn't optimal. In a better world the mapmode actions
		 * would be able to capture keyboard events and react accordingly. But
		 * for now this is a reasonable approximation.
		 */
		Main.contentPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
		        KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
		        tr("Unselect All"));
	}

	public void actionPerformed(ActionEvent e) {
		Main.ds.setSelected();
	}
}
