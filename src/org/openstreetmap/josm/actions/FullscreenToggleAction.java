// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ButtonModel;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.KeyStroke;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.tools.PlatformHookWindows;
import org.openstreetmap.josm.tools.Shortcut;

public class FullscreenToggleAction extends JosmAction {
    private final List<ButtonModel> buttonModels = new ArrayList<ButtonModel>();
    private GraphicsDevice gd;
    private Rectangle prevBounds;

    public FullscreenToggleAction() {
        super(
                tr("Fullscreen view"),
                null, /* no icon */
                tr("Toggle fullscreen view"),
                Shortcut.registerShortcut("menu:view:fullscreen", tr("Toggle fullscreen view"),KeyEvent.VK_F11, Shortcut.DIRECT),
                false /* register */
        );
        putValue("help", ht("/Action/FullscreenView"));
        putValue("toolbar", "fullscreen");
        Main.toolbar.register(this);
        gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        putValue(SELECTED_KEY, Main.pref.getBoolean("draw.fullscreen", false));
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
            if (model.isSelected() != isSelected()) {
                model.setSelected(isSelected());
            }
        }
    }

    protected void toggleSelectedState() {
        putValue(SELECTED_KEY, !isSelected());
        Main.pref.put("draw.fullscreen", isSelected());
        notifySelectedState();
        setMode();
    }

    public void initial() {
        if (isSelected()) {
            setMode();
        }
    }

    protected void setMode() {
        JFrame frame = (JFrame) Main.parent;

        List<Window> visibleWindows = new ArrayList<Window>();
        visibleWindows.add(frame);
        for (Window w : Frame.getWindows()) {
            if (w.isVisible() && w != frame) {
                visibleWindows.add(w);
            }
        }

        frame.dispose();
        frame.setUndecorated(isSelected());

        if (isSelected()) {
            prevBounds = frame.getBounds();
            frame.setBounds(new Rectangle(Toolkit.getDefaultToolkit().getScreenSize()));
        }

        // we cannot use hw-exclusive fullscreen mode in MS-Win, as long
        // as josm throws out modal dialogs, see here:
        // http://forums.sun.com/thread.jspa?threadID=5351882 FIXME this url does not work anymore
        //
        // the good thing is: fullscreen works without exclusive mode,
        // since windows (or java?) draws the undecorated window full-
        // screen by default (it's a simulated mode, but should be ok)
        String exclusive = Main.pref.get("draw.fullscreen.exclusive-mode", "auto");
        if ("true".equals(exclusive) || ("auto".equals(exclusive) && !(Main.platform instanceof PlatformHookWindows))) {
            gd.setFullScreenWindow(isSelected() ? frame : null);
        }

        if (!isSelected() && prevBounds != null) {
            frame.setBounds(prevBounds);
        }

        for (Window wind : visibleWindows) {
            wind.setVisible(true);
        }
        
        // Free F10 key to allow it to be used by plugins, even after full screen (see #7502)
        frame.getJMenuBar().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0), "none"); 
    }

    public void actionPerformed(ActionEvent e) {
        toggleSelectedState();
    }
    
    public final boolean isSelected() {
        return (Boolean)getValue(SELECTED_KEY);
    }
}
