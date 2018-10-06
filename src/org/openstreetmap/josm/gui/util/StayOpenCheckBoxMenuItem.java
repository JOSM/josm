// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.util;

import javax.swing.Action;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.MenuElement;
import javax.swing.MenuSelectionManager;

/**
 * An extension of JCheckBoxMenuItem that doesn't close the menu when selected.
 *
 * @author Darryl https://tips4java.wordpress.com/2010/09/12/keeping-menus-open/
 */
public class StayOpenCheckBoxMenuItem extends JCheckBoxMenuItem {

  private static volatile MenuElement[] path;

  {
    getModel().addChangeListener(e -> {
        if (getModel().isArmed() && isShowing()) {
          path = MenuSelectionManager.defaultManager().getSelectedPath();
        }
      });
  }

  /**
   * Constructs a new {@code StayOpenCheckBoxMenuItem} whose properties are taken from the Action supplied.
   * @param a action
   */
  public StayOpenCheckBoxMenuItem(Action a) {
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
