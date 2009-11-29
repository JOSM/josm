// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.JComponent;
import javax.swing.KeyStroke;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.tools.Shortcut;

public final class ZoomInAction extends JosmAction {

    public ZoomInAction() {
        super(
                tr("Zoom In"),
                "dialogs/zoomin",
                tr("Zoom In"),
                Shortcut.registerShortcut("view:zoomin", tr("View: {0}", tr("Zoom In")),KeyEvent.VK_PLUS, Shortcut.GROUP_DIRECT),
                true
        );
        putValue("help", ht("/Action/ZoomIn"));
        // make numpad + behave like + (action is already registred)
        Main.contentPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ADD,0), tr("Zoom In"));
    }

    public void actionPerformed(ActionEvent e) {
        Object name = Main.contentPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, 0));
        Main.contentPane.getActionMap().put(name, this);

        if (!Main.isDisplayingMapView()) return;
        Main.map.mapView.zoomToFactor(0.9);
    }

    @Override
    protected void updateEnabledState() {
        setEnabled(
                Main.isDisplayingMapView()
                && Main.map.mapView.hasLayers()
        );
    }

}
