// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.Icon;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.tools.Shortcut;

/**
* This action toggles visibility of dialogs panel and other panels to free more space for drawing (GIMP-like)
* @author cmuelle8
* @since 5965
*/
public class DialogsToggleAction extends ToggleAction {

    /**
     * Constructs a new {@code DialogsToggleAction}.
     */
    public DialogsToggleAction() {
        super(tr("Toggle dialogs panel"),
              (Icon) null, /* no icon */
              tr("Toggle dialogs panel, maximize mapview"),
              Shortcut.registerShortcut("menu:view:dialogspanel", tr("Toggle dialogs panel"),KeyEvent.VK_TAB, Shortcut.DIRECT),
              true, "dialogspanel", /* register in toolbar */
              false
        );
        putValue("help", ht("/Action/ToggleDialogsPanel"));
        setSelected(Main.pref.getBoolean("draw.dialogspanel", true));
        notifySelectedState();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        toggleSelectedState(e);
        Main.pref.put("draw.dialogspanel", isSelected());
        notifySelectedState();
        setMode();
    }

    /**
     * To call if this action must be initially run at JOSM startup.
     */
    public void initial() {
        if (isSelected()) {
            setMode();
        }
    }

    protected void setMode() {
        if (Main.isDisplayingMapView()) {
            boolean selected = isSelected();
            Main.map.setDialogsPanelVisible(selected);
            Main.map.statusLine.setVisible(selected || Main.pref.getBoolean("statusbar.always-visible", true));
            Main.toolbar.control.setVisible(selected || Main.pref.getBoolean("toolbar.always-visible", true));
            Main.main.menu.setVisible(selected || Main.pref.getBoolean("menu.always-visible", true));
            // sideToolBar listens to preference changes, use it here
            if (!Main.pref.getBoolean("sidetoolbar.always-visible", true)) {
                Main.pref.put("sidetoolbar.visible", selected);
            }
            Main.map.mapView.rememberLastPositionOnScreen();
        }
    }
}
