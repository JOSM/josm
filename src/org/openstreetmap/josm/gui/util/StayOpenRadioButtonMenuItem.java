// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.util;

import javax.swing.Action;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.MenuElement;
import javax.swing.MenuSelectionManager;

/**
 * An extension of JRadioButtonMenuItem that doesn't close the menu when selected.
 *
 * @author Darryl Burke https://tips4java.wordpress.com/2010/09/12/keeping-menus-open/
 */
public class StayOpenRadioButtonMenuItem extends JRadioButtonMenuItem {

    private static volatile MenuElement[] path;

    {
        getModel().addChangeListener(e -> {
            if (getModel().isArmed() && isShowing()) {
                path = MenuSelectionManager.defaultManager().getSelectedPath();
            }
        });
    }

    /**
     * Constructs a new {@code StayOpenRadioButtonMenuItem} with no set text or icon.
     * @see JRadioButtonMenuItem#JRadioButtonMenuItem()
     */
    public StayOpenRadioButtonMenuItem() {
        super();
    }

    /**
     * Constructs a new {@code StayOpenRadioButtonMenuItem} whose properties are taken from the Action supplied.
     * @see JRadioButtonMenuItem#JRadioButtonMenuItem(Action)
     */
    public StayOpenRadioButtonMenuItem(Action a) {
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
