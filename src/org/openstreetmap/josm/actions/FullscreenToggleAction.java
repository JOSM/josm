// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.KeyStroke;

import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.PlatformManager;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * This class toggles the full-screen mode.
 * @since 2533
 */
public class FullscreenToggleAction extends ToggleAction {
    private final transient GraphicsDevice gd;
    private Rectangle prevBounds;

    /**
     * Constructs a new {@code FullscreenToggleAction}.
     */
    public FullscreenToggleAction() {
        super(tr("Fullscreen view"),
              null, /* no icon */
              tr("Toggle fullscreen view"),
              Shortcut.registerShortcut("menu:view:fullscreen", tr("Toggle fullscreen view"), KeyEvent.VK_F11, Shortcut.DIRECT),
              false /* register */
        );
        setHelpId(ht("/Action/FullscreenView"));
        setToolbarId("fullscreen");
        MainApplication.getToolbar().register(this);
        gd = GraphicsEnvironment.isHeadless() ? null : GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        setSelected(Config.getPref().getBoolean("draw.fullscreen", false));
        notifySelectedState();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        toggleSelectedState(e);
        Config.getPref().putBoolean("draw.fullscreen", isSelected());
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
        JFrame frame = MainApplication.getMainFrame();

        List<Window> visibleWindows = new ArrayList<>();
        visibleWindows.add(frame);
        for (Window w : Frame.getWindows()) {
            if (w.isVisible() && w != frame) {
                visibleWindows.add(w);
            }
        }

        boolean selected = isSelected();

        if (frame != null) {
            frame.dispose();
            frame.setUndecorated(selected);

            if (selected) {
                prevBounds = frame.getBounds();
                frame.setBounds(new Rectangle(GuiHelper.getScreenSize()));
            }
        }

        // we cannot use hw-exclusive fullscreen mode in MS-Win, as long
        // as josm throws out modal dialogs.
        //
        // the good thing is: fullscreen works without exclusive mode,
        // since windows (or java?) draws the undecorated window full-
        // screen by default (it's a simulated mode, but should be ok)
        String exclusive = Config.getPref().get("draw.fullscreen.exclusive-mode", "auto");
        if (("true".equals(exclusive) || ("auto".equals(exclusive) && !PlatformManager.isPlatformWindows())) && gd != null) {
            gd.setFullScreenWindow(selected ? frame : null);
        }

        if (!selected && prevBounds != null && frame != null) {
            frame.setBounds(prevBounds);
        }

        for (Window wind : visibleWindows) {
            if (wind != null) {
                wind.setVisible(true);
            }
        }

        // Free F10 key to allow it to be used by plugins, even after full screen (see #7502)
        if (frame != null) {
            frame.getJMenuBar().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0), "none");
        }
    }
}
