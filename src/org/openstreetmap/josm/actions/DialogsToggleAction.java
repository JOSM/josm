// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ButtonModel;
import javax.swing.Icon;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.tools.Shortcut;

/*
* Action that hides or shows toggle dialogs panel and other panels
* to free more space for drawing (GIMP-like)
* @author cmuelle8
*/
public class DialogsToggleAction extends JosmAction {
    private final List<ButtonModel> buttonModels = new ArrayList<ButtonModel>();
    private boolean selected;

    public DialogsToggleAction() {
        super(
                tr("Toggle dialogs panel"),
                (Icon) null, /* no icon */
                tr("Toggle dialogs panel, maximize mapview"),
                Shortcut.registerShortcut("menu:view:dialogspanel", tr("Toggle dialogs panel"),KeyEvent.VK_TAB, Shortcut.DIRECT),
                true, "dialogspanel", /* register in toolbar */
                false
        );
        putValue("help", ht("/Action/ToggleDialogsPanel"));
        selected = Main.pref.getBoolean("draw.dialogspanel", true);
        notifySelectedState();
    }

    public void addButtonModel(ButtonModel model) {
        if (model != null && !buttonModels.contains(model)) {
            buttonModels.add(model);
        }
    }

    public void removeButtonModel(ButtonModel model) {
        if (model != null && buttonModels.contains(model)) {
            buttonModels.remove(model);
        }
    }

    protected void notifySelectedState() {
        for (ButtonModel model: buttonModels) {
            if (model.isSelected() != selected) {
                model.setSelected(selected);
            }
        }
    }

    protected void toggleSelectedState() {
        selected = !selected;
        Main.pref.put("draw.dialogspanel", selected);
        notifySelectedState();
        setMode();
    }

    public void initial() {
        if(selected) {
            setMode();
        }
    }

    protected void setMode() {
        if (Main.isDisplayingMapView()) {
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

    @Override
    public void actionPerformed(ActionEvent e) {
        toggleSelectedState();
    }
}
