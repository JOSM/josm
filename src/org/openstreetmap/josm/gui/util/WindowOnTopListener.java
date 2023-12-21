// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.util;

import java.awt.Container;
import java.awt.Dialog;
import java.awt.Window;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;

import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;

/**
 * A listener for windows that block other inputs, to ensure they are always on top
 * @since 18923
 */
public class WindowOnTopListener implements AncestorListener, WindowFocusListener {

    /**
     * {@code true} indicates that the window was always on top prior to the change
     */
    private boolean wasAlwaysOnTop;
    @Override
    public void windowGainedFocus(WindowEvent e) {
        final Window window = e.getWindow();
        if (window != null && window.isAlwaysOnTop() != wasAlwaysOnTop) {
            window.setAlwaysOnTop(wasAlwaysOnTop);
        }
    }

    @Override
    public void windowLostFocus(WindowEvent e) {
        final Window window = e.getWindow();
        if (window != null) {
            wasAlwaysOnTop = window.isAlwaysOnTop();
        }
    }

    @Override
    public void ancestorAdded(AncestorEvent event) {
        final Container ancestor = event.getAncestor();
        if (ancestor instanceof Dialog) {
            Dialog dialog = (Dialog) ancestor;
            wasAlwaysOnTop = dialog.isAlwaysOnTop();
            if (dialog.isVisible() && dialog.isModal()) {
                dialog.setAlwaysOnTop(true);
            }
        }
        if (ancestor instanceof Window) {
            Window window = (Window) ancestor;
            window.addWindowFocusListener(this);
        }
    }

    @Override
    public void ancestorRemoved(AncestorEvent event) {
        final Container ancestor = event.getAncestor();
        if (ancestor instanceof Dialog) {
            Dialog dialog = (Dialog) ancestor;
            if (dialog.isVisible() && dialog.isModal()) {
                dialog.setAlwaysOnTop(wasAlwaysOnTop);
            }
        }
        if (ancestor instanceof Window) {
            Window window = (Window) ancestor;
            window.removeWindowFocusListener(this);
        }
    }

    @Override
    public void ancestorMoved(AncestorEvent event) {
        // Do nothing
    }
}
