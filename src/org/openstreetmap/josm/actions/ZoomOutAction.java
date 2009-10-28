// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.gui.help.HelpUtil.ht;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.tools.Shortcut;

public final class ZoomOutAction extends JosmAction {

    public ZoomOutAction() {
        super(tr("Zoom Out"), "dialogs/zoomout", tr("Zoom Out"),
                Shortcut.registerShortcut("view:zoomout", tr("View: {0}", tr("Zoom Out")), KeyEvent.VK_MINUS, Shortcut.GROUP_DIRECT), true);
        putValue("help", ht("/Action/ZoomOut"));
    }

    public void actionPerformed(ActionEvent e) {
        if (!Main.isDisplayingMapView()) return;
        Main.map.mapView.zoomToFactor(1/0.9);
    }

    @Override
    protected void updateEnabledState() {
        setEnabled(
                Main.isDisplayingMapView()
                && Main.map.mapView.hasLayers()
        );
    }
}
