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
              new ImageProvider("dialogs/dialogs_panel"),
              tr("Toggle dialogs panel, maximize mapview"),
              Shortcut.registerShortcut("menu:view:dialogspanel", tr("Toggle dialogs panel"), KeyEvent.VK_TAB, Shortcut.DIRECT),
              true, "dialogspanel", /* register in toolbar */
              false
        );
        setHelpId(ht("/ToggleDialogs"));
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
                toolbarPreviouslyVisible = MapFrame.TOOLBAR_VISIBLE.get();
                sideToolbarPreviouslyVisible = MapFrame.SIDE_TOOLBAR_VISIBLE.get();
            }
            MapFrame map = MainApplication.getMap();
            map.setDialogsPanelVisible(selected);
            map.statusLine.setVisible(selected || Config.getPref().getBoolean("statusbar.always-visible", true));
            MainApplication.getMenu().setVisible(selected || Config.getPref().getBoolean("menu.always-visible", true));
            // Toolbars listen to preference changes, use it here
            if (!Config.getPref().getBoolean("toolbar.always-visible", true) && (!selected || toolbarPreviouslyVisible)) {
                MapFrame.TOOLBAR_VISIBLE.put(selected);
            }
            if (!Config.getPref().getBoolean("sidetoolbar.always-visible", true) && (!selected || sideToolbarPreviouslyVisible)) {
                MapFrame.SIDE_TOOLBAR_VISIBLE.put(selected);
            }
            map.mapView.rememberLastPositionOnScreen();
        }
    }
}
