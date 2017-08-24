// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Zoom in map.
 * @since 770
 */
public final class ZoomInAction extends JosmAction {

    /**
     * Constructs a new {@code ZoomInAction}.
     */
    public ZoomInAction() {
        super(
                tr("Zoom In"),
                "dialogs/zoomin",
                tr("Zoom In"),
                // Although it might be possible on few custom keyboards, the vast majority of layouts do not have a direct '+' key, see below
                Shortcut.registerShortcut("view:zoomin", tr("View: {0}", tr("Zoom In")), KeyEvent.VK_PLUS, Shortcut.DIRECT),
                true
        );
        putValue("help", ht("/Action/ZoomIn"));
        // On standard QWERTY, AZERTY and other common layouts the '+' key is obtained with Shift+EQUALS
        Main.registerActionShortcut(this,
                Shortcut.registerShortcut("view:zoominbis", tr("View: {0}", tr("Zoom In")),
                    KeyEvent.VK_EQUALS, Shortcut.SHIFT));
        // But on some systems (Belgian keyboard under Ubuntu) it seems not to work, so use also EQUALS
        Main.registerActionShortcut(this,
                Shortcut.registerShortcut("view:zoominter", tr("View: {0}", tr("Zoom In")),
                    KeyEvent.VK_EQUALS, Shortcut.DIRECT));
        // make numpad + behave like +
        Main.registerActionShortcut(this,
            Shortcut.registerShortcut("view:zoominkeypad", tr("View: {0}", tr("Zoom In (Keypad)")),
                KeyEvent.VK_ADD, Shortcut.DIRECT));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!MainApplication.isDisplayingMapView()) return;
        MainApplication.getMap().mapView.zoomIn();
    }

    @Override
    protected void updateEnabledState() {
        setEnabled(!getLayerManager().getLayers().isEmpty());
    }

}
