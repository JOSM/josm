// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ButtonModel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.tools.PlatformHookUnixoid;
import org.openstreetmap.josm.tools.Shortcut;

public class FullscreenToggleAction extends JosmAction {
    private final List<ButtonModel> buttonModels = new ArrayList<ButtonModel>();
    //FIXME: replace with property Action.SELECTED_KEY when migrating to
    // Java 6
    private boolean selected;
    private GraphicsDevice gd;
    public FullscreenToggleAction() {
        super(
                tr("Fullscreen View"),
                null, /* no icon */
                tr("Toggle fullscreen view"),
                Shortcut.registerShortcut("menu:view:fullscreen", tr("Toggle Fullscreen view"),KeyEvent.VK_F11, Shortcut.GROUP_DIRECT),
                true /* register shortcut */
        );
        gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        selected = Main.pref.getBoolean("draw.fullscreen", false);
        notifySelectedState();
    }

    public boolean canFullscreen() {
        /* We only support fullscreen, see
         * http://lists.openstreetmap.org/pipermail/josm-dev/2009-March/002659.html
         * for why
         */
        return Main.platform instanceof PlatformHookUnixoid && gd.isFullScreenSupported();
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
        Main.pref.put("draw.fullscreen", selected);
        notifySelectedState();

        Frame frame = (Frame)Main.parent;
        frame.dispose();
        frame.setUndecorated(selected);
        gd.setFullScreenWindow(selected ? frame : null);
        frame.setVisible(true);
    }

    public void actionPerformed(ActionEvent e) {
        toggleSelectedState();
    }
}
