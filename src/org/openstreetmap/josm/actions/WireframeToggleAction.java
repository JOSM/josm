// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ButtonModel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.tools.Shortcut;

public class WireframeToggleAction extends JosmAction {
    private final List<ButtonModel> buttonModels = new ArrayList<ButtonModel>();
    //FIXME: replace with property Action.SELECTED_KEY when migrating to
    // Java 6
    private boolean selected;
    public WireframeToggleAction() {
        super(
                tr("Wireframe View"),
                null, /* no icon */
                tr("Enable/disable rendering the map as wireframe only"),
                Shortcut.registerShortcut("menu:view:wireframe", tr("Toggle Wireframe view"),KeyEvent.VK_W, Shortcut.GROUP_MENU),
                true /* register shortcut */
        );
        selected = Main.pref.getBoolean("draw.wireframe", false);
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
        Main.pref.put("draw.wireframe", selected);
        notifySelectedState();
        if (Main.map != null) {
            Main.map.mapView.repaint();
        }
    }
    public void actionPerformed(ActionEvent e) {
        toggleSelectedState();
    }

    @Override
    protected void updateEnabledState() {
        setEnabled(Main.map != null && Main.main.getEditLayer() != null);
    }
}
