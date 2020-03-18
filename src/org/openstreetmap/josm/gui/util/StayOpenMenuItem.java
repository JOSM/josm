// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.util;

import javax.swing.Action;
import javax.swing.JMenuItem;
import javax.swing.MenuElement;
import javax.swing.MenuSelectionManager;

/**
 * An extension of JMenuItem that doesn't close the menu when selected.
 *
 * @author Darryl Burke https://tips4java.wordpress.com/2010/09/12/keeping-menus-open/
 */
public class StayOpenMenuItem extends JMenuItem {

    private MenuElement[] path;

    {
        getModel().addChangeListener(e -> {
            if (getModel().isArmed() && isShowing()) {
                path = MenuSelectionManager.defaultManager().getSelectedPath();
            }
        });
    }

    /**
     * Constructs a new {@code StayOpenMenuItem} with no set text or icon.
     * @see JMenuItem#JMenuItem()
     */
    public StayOpenMenuItem() {
        super();
    }

    /**
     * Constructs a new {@code StayOpenMenuItem} whose properties are taken from the Action supplied.
     * @param a associated action
     * @see JMenuItem#JMenuItem(javax.swing.Action)
     */
    public StayOpenMenuItem(Action a) {
        super(a);
    }

    /**
     * Overridden to reopen the menu.
     *
     * @param pressTime the time to "hold down" the button, in milliseconds
     */
    @Override
    public void doClick(int pressTime) {
        super.doClick(pressTime);
        MenuSelectionManager.defaultManager().setSelectedPath(path);
    }
}
