// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.widgets;

import java.awt.Component;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JPopupMenu;

/**
 * Utility class that helps to display popup menus on mouse events.
 * @since 2688
 */
public class PopupMenuLauncher extends MouseAdapter {
    private final JPopupMenu menu;
    private final boolean checkEnabled;

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
        this(menu, false);
    }

    /**
     * Creates a new {@link PopupMenuLauncher} with the given menu.
     * @param menu The popup menu to display
     * @param checkEnabled if {@code true}, the popup menu will only be displayed if the component triggering the mouse event is enabled
     * @since 5885
     */
    public PopupMenuLauncher(JPopupMenu menu, boolean checkEnabled) {
        this.menu = menu;
        this.checkEnabled = checkEnabled;
    }

    @Override public void mousePressed(MouseEvent e) { processEvent(e); }
    @Override public void mouseClicked(MouseEvent e) { processEvent(e); }
    @Override public void mouseReleased(MouseEvent e) { processEvent(e); }
    
    private void processEvent(MouseEvent e) {
        if (e.isPopupTrigger() && (!checkEnabled || e.getComponent().isEnabled())) {
            launch(e);
        }
    }

    /**
     * Launches the popup menu according to the given mouse event.
     * This method needs to be overriden if the default constructor has been called.
     * @param evt A mouse event
     */
    public void launch(final MouseEvent evt) {
        if (menu != null) {
            final Component component = evt.getComponent();
            if (component != null && component.isFocusable() && !component.hasFocus() && component.requestFocusInWindow()) {
                component.addFocusListener(new FocusListener() {
                    @Override public void focusLost(FocusEvent e) {}
                    @Override public void focusGained(FocusEvent e) {
                        menu.show(component, evt.getX(), evt.getY());
                        component.removeFocusListener(this);
                    }
                });
            } else {
                menu.show(component, evt.getX(), evt.getY());
            }
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
