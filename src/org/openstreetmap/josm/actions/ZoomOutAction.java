// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.tools.ShortCut;

public final class ZoomOutAction extends JosmAction {

	public ZoomOutAction() {
		super(tr("Zoom out"), "dialogs/zoomout", tr("Zoom out"),
		ShortCut.registerShortCut("view:zoomout", tr("View: {0}", tr("Zoom out")), KeyEvent.VK_MINUS, ShortCut.GROUP_DIRECT), true);
		setEnabled(true);
	}

	public void actionPerformed(ActionEvent e) {
		double zoom = Main.map.mapView.getScale();
		Main.map.mapView.zoomTo(Main.map.mapView.getCenter(), zoom /.9);
	}
}
