// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.tools.Shortcut;

public final class ZoomInAction extends JosmAction {

    public ZoomInAction() {
        super(tr("Zoom In"), "dialogs/zoomin", tr("Zoom In"),
        Shortcut.registerShortcut("view:zoomin", tr("View: {0}", tr("Zoom In")), KeyEvent.VK_PLUS, Shortcut.GROUP_DIRECT), true);
        setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
        if (Main.map == null) return;
        Main.map.mapView.zoomToFactor(0.9);
    }
}
