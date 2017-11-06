// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Shortcut;

/**
* This action toggles visibility of dialogs panel and other panels to free more space for drawing (GIMP-like)
* @author cmuelle8
* @since 5965
*/
public class DialogsToggleAction extends ToggleAction {

    private boolean toolbarPreviouslyVisible;
    private boolean sideToolbarPreviouslyVisible;

    /**
     * Constructs a new {@code DialogsToggleAction}.
     */
    public DialogsToggleAction() {
        super(tr("Dialogs panel"),
              (ImageProvider) null, /* no icon */
              tr("Toggle dialogs panel, maximize mapview"),
              Shortcut.registerShortcut("menu:view:dialogspanel", tr("Toggle dialogs panel"), KeyEvent.VK_TAB, Shortcut.DIRECT),
              true, "dialogspanel", /* register in toolbar */
              false
        );
        putValue("help", ht("/ToggleDialogs"));
        setSelected(Config.getPref().getBoolean("draw.dialogspanel", true));
        notifySelectedState();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        toggleSelectedState(e);
        Config.getPref().putBoolean("draw.dialogspanel", isSelected());
        notifySelectedState();
        setMode();
    }

    protected void setMode() {
        if (MainApplication.isDisplayingMapView()) {
            boolean selected = isSelected();
            if (!selected) {
                toolbarPreviouslyVisible = Config.getPref().getBoolean("toolbar.visible", true);
                sideToolbarPreviouslyVisible = Config.getPref().getBoolean("sidetoolbar.visible", true);
            }
            MapFrame map = MainApplication.getMap();
            map.setDialogsPanelVisible(selected);
            map.statusLine.setVisible(selected || Config.getPref().getBoolean("statusbar.always-visible", true));
            MainApplication.getMenu().setVisible(selected || Config.getPref().getBoolean("menu.always-visible", true));
            // Toolbars listen to preference changes, use it here
            if (!Config.getPref().getBoolean("toolbar.always-visible", true) && (!selected || toolbarPreviouslyVisible)) {
                Config.getPref().putBoolean("toolbar.visible", selected);
            }
            if (!Config.getPref().getBoolean("sidetoolbar.always-visible", true) && (!selected || sideToolbarPreviouslyVisible)) {
                Config.getPref().putBoolean("sidetoolbar.visible", selected);
            }
            map.mapView.rememberLastPositionOnScreen();
        }
    }
}
