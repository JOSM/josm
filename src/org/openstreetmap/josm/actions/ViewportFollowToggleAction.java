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

public class ViewportFollowToggleAction extends JosmAction {
    private final List<ButtonModel> buttonModels = new ArrayList<ButtonModel>();
    private boolean selected;
    public ViewportFollowToggleAction() {
        super(
                tr("Viewport Following"),
                "viewport-follow",
                tr("Enable/disable automatic moving of the map view to last placed node"),
                Shortcut.registerShortcut("menu:view:viewportfollow", tr("Toggle Viewport Following"),KeyEvent.VK_F, Shortcut.GROUP_MENU, Shortcut.SHIFT_DEFAULT),
                true /* register shortcut */
        );
        selected = false; 
        notifySelectedState();
    }

    public void addButtonModel(ButtonModel model) {
        if (model != null && !buttonModels.contains(model)) {
            buttonModels.add(model);
            model.setSelected(selected);
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
        Main.map.mapView.viewportFollowing = selected;
        notifySelectedState();
    }
    public void actionPerformed(ActionEvent e) {
        toggleSelectedState();
    }

    @Override
    protected void updateEnabledState() {
        setEnabled(Main.map != null && Main.main.getEditLayer() != null);
    }
}
