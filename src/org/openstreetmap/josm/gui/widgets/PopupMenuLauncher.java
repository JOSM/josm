// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.widgets;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JPopupMenu;

/**
 * Utility class that helps to display popup menus on mouse events.
 * @since 2688
 */
public class PopupMenuLauncher extends MouseAdapter {
    private final JPopupMenu menu;

    /**
     * Creates a new {@link PopupMenuLauncher} with no defined menu. 
     * It is then needed to override the {@link #launch} method.
     * @see #launch(MouseEvent)
     */
    public PopupMenuLauncher() {
        this(null);
    }

    /**
     * Creates a new {@link PopupMenuLauncher} with the given menu.
     * @param menu The popup menu to display
     */
    public PopupMenuLauncher(JPopupMenu menu) {
        this.menu = menu;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.isPopupTrigger()) {
            launch(e);
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (e.isPopupTrigger()) {
            launch(e);
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.isPopupTrigger()) {
            launch(e);
        }
    }

    /**
     * Launches the popup menu according to the given mouse event.
     * This method needs to be overriden if the default constructor has been called.
     * @param evt A mouse event
     */
    public void launch(MouseEvent evt) {
        if (menu != null) {
            menu.show(evt.getComponent(), evt.getX(), evt.getY());
        }
    }

    /**
     * @return the popup menu if defined, {@code null} otherwise.
     * @since 5884
     */
    public final JPopupMenu getMenu() {
        return menu;
    }
}
