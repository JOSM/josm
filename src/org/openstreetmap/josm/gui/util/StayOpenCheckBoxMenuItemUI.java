// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.util;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.lang.reflect.Method;

import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.MenuSelectionManager;
import javax.swing.UIManager;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicCheckBoxMenuItemUI;
import javax.swing.plaf.basic.BasicMenuItemUI;

import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.ReflectionUtils;

/**
 * A CheckBoxMenuItem UI delegate that doesn't close the menu when selected.
 * @author Darryl Burke https://stackoverflow.com/a/3759675/2257172
 * @since 15288
 */
public class StayOpenCheckBoxMenuItemUI extends BasicCheckBoxMenuItemUI {

    @Override
    protected void doClick(MenuSelectionManager msm) {
        menuItem.doClick(0);
    }

    @Override
    protected void paintBackground(Graphics g, JMenuItem menuItem, Color bgColor) {
        ComponentUI ui = UIManager.getUI(menuItem);
        if (ui instanceof BasicMenuItemUI) {
            try {
                Method paintBackground = BasicMenuItemUI.class.getDeclaredMethod(
                        "paintBackground", Graphics.class, JMenuItem.class, Color.class);
                ReflectionUtils.setObjectsAccessible(paintBackground);
                paintBackground.invoke(ui, g, menuItem, bgColor);
            } catch (ReflectiveOperationException | SecurityException | IllegalArgumentException e) {
                Logging.error(e);
                super.paintBackground(g, menuItem, bgColor);
            }
        } else {
            super.paintBackground(g, menuItem, bgColor);
        }
    }

    @Override
    protected void paintText(Graphics g, JMenuItem menuItem, Rectangle textRect, String text) {
        ComponentUI ui = UIManager.getUI(menuItem);
        if (ui instanceof BasicMenuItemUI) {
            try {
                Method paintText = BasicMenuItemUI.class.getDeclaredMethod(
                        "paintText", Graphics.class, JMenuItem.class, Rectangle.class, String.class);
                ReflectionUtils.setObjectsAccessible(paintText);
                paintText.invoke(ui, g, menuItem, textRect, text);
            } catch (ReflectiveOperationException | SecurityException | IllegalArgumentException e) {
                Logging.error(e);
                super.paintText(g, menuItem, textRect, text);
            }
        } else {
            super.paintText(g, menuItem, textRect, text);
        }
    }

    /**
     * Creates a new {@code StayOpenCheckBoxMenuItemUI}.
     * @param c not used
     * @return newly created {@code StayOpenCheckBoxMenuItemUI}
     */
    public static ComponentUI createUI(JComponent c) {
        return new StayOpenCheckBoxMenuItemUI();
    }
}
