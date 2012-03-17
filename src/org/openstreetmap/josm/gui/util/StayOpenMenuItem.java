// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.util;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JMenuItem;
import javax.swing.MenuElement;
import javax.swing.MenuSelectionManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * An extension of JMenuItem that doesn't close the menu when selected.
 *
 * @author Darryl http://tips4java.wordpress.com/2010/09/12/keeping-menus-open/
 */
public class StayOpenMenuItem extends JMenuItem {

  private static MenuElement[] path;

  {
    getModel().addChangeListener(new ChangeListener() {

      @Override
      public void stateChanged(ChangeEvent e) {
        if (getModel().isArmed() && isShowing()) {
          path = MenuSelectionManager.defaultManager().getSelectedPath();
        }
      }
    });
  }

  /**
   * @see JMenuItem#JMenuItem()
   */
  public StayOpenMenuItem() {
    super();
  }

  /**
   * @see JMenuItem#JMenuItem(javax.swing.Action)
   */
  public StayOpenMenuItem(Action a) {
    super(a);
  }

  /**
   * @see JMenuItem#JMenuItem(javax.swing.Icon)
   */
  public StayOpenMenuItem(Icon icon) {
    super(icon);
  }

  /**
   * @see JMenuItem#JMenuItem(java.lang.String)
   */
  public StayOpenMenuItem(String text) {
    super(text);
  }

  /**
   * @see JMenuItem#JMenuItem(java.lang.String, javax.swing.Icon)
   */
  public StayOpenMenuItem(String text, Icon icon) {
    super(text, icon);
  }

  /**
   * @see JMenuItem#JMenuItem(java.lang.String, int)
   */
  public StayOpenMenuItem(String text, int mnemonic) {
    super(text, mnemonic);
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
