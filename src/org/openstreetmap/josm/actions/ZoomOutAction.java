// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Zoom out map.
 * @since 770
 */
public final class ZoomOutAction extends JosmAction {

    /**
     * Constructs a new {@code ZoomOutAction}.
     */
    public ZoomOutAction() {
        super(tr("Zoom Out"), "dialogs/zoomout", tr("Zoom Out"),
                Shortcut.registerShortcut("view:zoomout", tr("View: {0}", tr("Zoom Out")), KeyEvent.VK_MINUS, Shortcut.DIRECT), true);
        setHelpId(ht("/Action/ZoomOut"));
        // make numpad - behave like -
        MainApplication.registerActionShortcut(this,
            Shortcut.registerShortcut("view:zoomoutkeypad", tr("View: {0}", tr("Zoom Out (Keypad)")),
                KeyEvent.VK_SUBTRACT, Shortcut.DIRECT));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!MainApplication.isDisplayingMapView()) return;
        MainApplication.getMap().mapView.zoomOut();
    }

    @Override
    protected void updateEnabledState() {
        setEnabled(!getLayerManager().getLayers().isEmpty());
    }
}
