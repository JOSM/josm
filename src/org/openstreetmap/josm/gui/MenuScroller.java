/**
 * MenuScroller.java    1.5.0 04/02/12
 * License: use / modify without restrictions (see https://tips4java.wordpress.com/about/)
 * Heavily modified for JOSM needs => drop unused features and replace static scrollcount approach by dynamic behaviour
 */
package org.openstreetmap.josm.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.Arrays;

import javax.swing.Icon;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.openstreetmap.josm.gui.util.WindowGeometry;
import org.openstreetmap.josm.tools.Logging;

/**
 * A class that provides scrolling capabilities to a long menu dropdown or
 * popup menu. A number of items can optionally be frozen at the top of the menu.
 * <p>
 * <b>Implementation note:</B>  The default scrolling interval is 150 milliseconds.
 * <p>
 * @author Darryl, https://tips4java.wordpress.com/2009/02/01/menu-scroller/
 * @since 4593
 */
public class MenuScroller {

    private JPopupMenu menu;
    private Component[] menuItems;
    private MenuScrollItem upItem;
    private MenuScrollItem downItem;
    private final MenuScrollListener menuListener = new MenuScrollListener();
    private final MouseWheelListener mouseWheelListener = new MouseScrollListener();
    private int topFixedCount;
    private int firstIndex;

    private static final int ARROW_ICON_HEIGHT = 10;

    private int computeScrollCount(int startIndex) {
        int result = 15;
        if (menu != null) {
            // Compute max height of current screen
            int maxHeight = WindowGeometry.getMaxDimensionOnScreen(menu).height - MainApplication.getMainFrame().getInsets().top;

            // Remove top fixed part height
            if (topFixedCount > 0) {
                for (int i = 0; i < topFixedCount; i++) {
                    maxHeight -= menuItems[i].getPreferredSize().height;
                }
                maxHeight -= new JSeparator().getPreferredSize().height;
            }

            // Remove height of our two arrow items + insets
            maxHeight -= menu.getInsets().top;
            maxHeight -= upItem.getPreferredSize().height;
            maxHeight -= downItem.getPreferredSize().height;
            maxHeight -= menu.getInsets().bottom;

            // Compute scroll count
            result = 0;
            int height = 0;
            for (int i = startIndex; i < menuItems.length && height <= maxHeight; i++, result++) {
                height += menuItems[i].getPreferredSize().height;
            }

            if (height > maxHeight) {
                // Remove extra item from count
                result--;
            } else {
                // Increase scroll count to take into account upper items that will be displayed
                // after firstIndex is updated
                for (int i = startIndex-1; i >= 0 && height <= maxHeight; i--, result++) {
                    height += menuItems[i].getPreferredSize().height;
                }
                if (height > maxHeight) {
                    result--;
                }
            }
        }
        return result;
    }

    /**
     * Registers a menu to be scrolled with the default scrolling interval.
     *
     * @param menu the menu
     * @return the MenuScroller
     */
    public static MenuScroller setScrollerFor(JMenu menu) {
        return new MenuScroller(menu);
    }

    /**
     * Registers a popup menu to be scrolled with the default scrolling interval.
     *
     * @param menu the popup menu
     * @return the MenuScroller
     */
    public static MenuScroller setScrollerFor(JPopupMenu menu) {
        return new MenuScroller(menu);
    }

    /**
     * Registers a menu to be scrolled, with the specified scrolling interval.
     *
     * @param menu the menu
     * @param interval the scroll interval, in milliseconds
     * @return the MenuScroller
     * @throws IllegalArgumentException if scrollCount or interval is 0 or negative
     * @since 7463
     */
    public static MenuScroller setScrollerFor(JMenu menu, int interval) {
        return new MenuScroller(menu, interval);
    }

    /**
     * Registers a popup menu to be scrolled, with the specified scrolling interval.
     *
     * @param menu the popup menu
     * @param interval the scroll interval, in milliseconds
     * @return the MenuScroller
     * @throws IllegalArgumentException if scrollCount or interval is 0 or negative
     * @since 7463
     */
    public static MenuScroller setScrollerFor(JPopupMenu menu, int interval) {
        return new MenuScroller(menu, interval);
    }

    /**
     * Registers a menu to be scrolled, with the specified scrolling interval,
     * and the specified numbers of items fixed at the top of the menu.
     *
     * @param menu the menu
     * @param interval the scroll interval, in milliseconds
     * @param topFixedCount the number of items to fix at the top.  May be 0.
     * @return the MenuScroller
     * @throws IllegalArgumentException if scrollCount or interval is 0 or
     * negative or if topFixedCount is negative
     * @since 7463
     */
    public static MenuScroller setScrollerFor(JMenu menu, int interval, int topFixedCount) {
        return new MenuScroller(menu, interval, topFixedCount);
    }

    /**
     * Registers a popup menu to be scrolled, with the specified scrolling interval,
     * and the specified numbers of items fixed at the top of the popup menu.
     *
     * @param menu the popup menu
     * @param interval the scroll interval, in milliseconds
     * @param topFixedCount the number of items to fix at the top. May be 0
     * @return the MenuScroller
     * @throws IllegalArgumentException if scrollCount or interval is 0 or
     * negative or if topFixedCount is negative
     * @since 7463
     */
    public static MenuScroller setScrollerFor(JPopupMenu menu, int interval, int topFixedCount) {
        return new MenuScroller(menu, interval, topFixedCount);
    }

    /**
     * Constructs a <code>MenuScroller</code> that scrolls a menu with the
     * default scrolling interval.
     *
     * @param menu the menu
     * @throws IllegalArgumentException if scrollCount is 0 or negative
     */
    public MenuScroller(JMenu menu) {
        this(menu, 150);
    }

    /**
     * Constructs a <code>MenuScroller</code> that scrolls a popup menu with the
     * default scrolling interval.
     *
     * @param menu the popup menu
     * @throws IllegalArgumentException if scrollCount is 0 or negative
     */
    public MenuScroller(JPopupMenu menu) {
        this(menu, 150);
    }

    /**
     * Constructs a <code>MenuScroller</code> that scrolls a menu with the
     * specified scrolling interval.
     *
     * @param menu the menu
     * @param interval the scroll interval, in milliseconds
     * @throws IllegalArgumentException if scrollCount or interval is 0 or negative
     * @since 7463
     */
    public MenuScroller(JMenu menu, int interval) {
        this(menu, interval, 0);
    }

    /**
     * Constructs a <code>MenuScroller</code> that scrolls a popup menu with the
     * specified scrolling interval.
     *
     * @param menu the popup menu
     * @param interval the scroll interval, in milliseconds
     * @throws IllegalArgumentException if scrollCount or interval is 0 or negative
     * @since 7463
     */
    public MenuScroller(JPopupMenu menu, int interval) {
        this(menu, interval, 0);
    }

    /**
     * Constructs a <code>MenuScroller</code> that scrolls a menu with the
     * specified scrolling interval, and the specified numbers of items fixed at
     * the top of the menu.
     *
     * @param menu the menu
     * @param interval the scroll interval, in milliseconds
     * @param topFixedCount the number of items to fix at the top.  May be 0
     * @throws IllegalArgumentException if scrollCount or interval is 0 or
     * negative or if topFixedCount is negative
     * @since 7463
     */
    public MenuScroller(JMenu menu, int interval, int topFixedCount) {
        this(menu.getPopupMenu(), interval, topFixedCount);
    }

    /**
     * Constructs a <code>MenuScroller</code> that scrolls a popup menu with the
     * specified scrolling interval, and the specified numbers of items fixed at
     * the top of the popup menu.
     *
     * @param menu the popup menu
     * @param interval the scroll interval, in milliseconds
     * @param topFixedCount the number of items to fix at the top.  May be 0
     * @throws IllegalArgumentException if scrollCount or interval is 0 or
     * negative or if topFixedCount is negative
     * @since 7463
     */
    public MenuScroller(JPopupMenu menu, int interval, int topFixedCount) {
        if (interval <= 0) {
            throw new IllegalArgumentException("interval must be greater than 0");
        }
        if (topFixedCount < 0) {
            throw new IllegalArgumentException("topFixedCount cannot be negative");
        }

        upItem = new MenuScrollItem(MenuIcon.UP, -1, interval);
        downItem = new MenuScrollItem(MenuIcon.DOWN, +1, interval);
        setTopFixedCount(topFixedCount);

        this.menu = menu;
        menu.addPopupMenuListener(menuListener);
        menu.addMouseWheelListener(mouseWheelListener);
    }

    /**
     * Returns the number of items fixed at the top of the menu or popup menu.
     *
     * @return the number of items
     */
    public int getTopFixedCount() {
        return topFixedCount;
    }

    /**
     * Sets the number of items to fix at the top of the menu or popup menu.
     *
     * @param topFixedCount the number of items
     */
    public void setTopFixedCount(int topFixedCount) {
        if (firstIndex <= topFixedCount) {
            firstIndex = topFixedCount;
        } else {
            firstIndex += (topFixedCount - this.topFixedCount);
        }
        this.topFixedCount = topFixedCount;
    }

    /**
     * Removes this MenuScroller from the associated menu and restores the
     * default behavior of the menu.
     */
    public void dispose() {
        if (menu != null) {
            menu.removePopupMenuListener(menuListener);
            menu.removeMouseWheelListener(mouseWheelListener);
            menu.setPreferredSize(null);
            menu = null;
        }
    }

    private void refreshMenu() {
        if (menuItems != null && menuItems.length > 0) {

            int allItemsHeight = 0;
            for (Component item : menuItems) {
                allItemsHeight += item.getPreferredSize().height;
            }

            int allowedHeight = WindowGeometry.getMaxDimensionOnScreen(menu).height - MainApplication.getMainFrame().getInsets().top;

            boolean mustSCroll = allItemsHeight > allowedHeight;

            if (mustSCroll) {
                firstIndex = Math.min(menuItems.length-1, Math.max(topFixedCount, firstIndex));
                int scrollCount = computeScrollCount(firstIndex);
                firstIndex = Math.min(menuItems.length - scrollCount, firstIndex);

                upItem.setEnabled(firstIndex > topFixedCount);
                downItem.setEnabled(firstIndex + scrollCount < menuItems.length);

                menu.removeAll();
                for (int i = 0; i < topFixedCount; i++) {
                    menu.add(menuItems[i]);
                }
                if (topFixedCount > 0) {
                    menu.addSeparator();
                }

                menu.add(upItem);
                for (int i = firstIndex; i < scrollCount + firstIndex; i++) {
                    menu.add(menuItems[i]);
                }
                menu.add(downItem);

                int preferredWidth = 0;
                for (Component item : menuItems) {
                    preferredWidth = Math.max(preferredWidth, item.getPreferredSize().width);
                }
                menu.setPreferredSize(new Dimension(preferredWidth, menu.getPreferredSize().height));

            } else if (!Arrays.equals(menu.getComponents(), menuItems)) {
                // Scroll is not needed but menu is not up to date
                menu.removeAll();
                for (Component item : menuItems) {
                    menu.add(item);
                }
            }

            menu.revalidate();
            menu.repaint();
        }
    }

    private class MenuScrollListener implements PopupMenuListener {

        @Override
        public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
            setMenuItems();
        }

        @Override
        public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
            restoreMenuItems();
        }

        @Override
        public void popupMenuCanceled(PopupMenuEvent e) {
            restoreMenuItems();
        }

        private void setMenuItems() {
            menuItems = menu.getComponents();
            refreshMenu();
        }

        private void restoreMenuItems() {
            menu.removeAll();
            for (Component component : menuItems) {
                menu.add(component);
            }
        }
    }

    private class MenuScrollTimer extends Timer {

        MenuScrollTimer(final int increment, int interval) {
            super(interval, new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    firstIndex += increment;
                    refreshMenu();
                }
            });
        }
    }

    private class MenuScrollItem extends JMenuItem
            implements ChangeListener {

        private final MenuScrollTimer timer;

        MenuScrollItem(MenuIcon icon, int increment, int interval) {
            setIcon(icon);
            setDisabledIcon(icon);
            timer = new MenuScrollTimer(increment, interval);
            addChangeListener(this);
        }

        @Override
        public void stateChanged(ChangeEvent e) {
            if (isArmed() && !timer.isRunning()) {
                timer.start();
            }
            if (!isArmed() && timer.isRunning()) {
                timer.stop();
            }
        }
    }

    private enum MenuIcon implements Icon {

        UP(9, 1, 9),
        DOWN(1, 9, 1);
        private static final int[] XPOINTS = {1, 5, 9};
        private final int[] yPoints;

        MenuIcon(int... yPoints) {
            this.yPoints = yPoints;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Dimension size = c.getSize();
            Graphics g2 = g.create(size.width / 2 - 5, size.height / 2 - 5, 10, 10);
            g2.setColor(Color.GRAY);
            g2.drawPolygon(XPOINTS, yPoints, 3);
            if (c.isEnabled()) {
                g2.setColor(Color.BLACK);
                g2.fillPolygon(XPOINTS, yPoints, 3);
            }
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return 0;
        }

        @Override
        public int getIconHeight() {
            return ARROW_ICON_HEIGHT;
        }
    }

    private class MouseScrollListener implements MouseWheelListener {
        @Override
        public void mouseWheelMoved(MouseWheelEvent mwe) {
            firstIndex += mwe.getWheelRotation();
            refreshMenu();
            if (Logging.isDebugEnabled()) {
                Logging.debug("{0} consuming event {1}", getClass().getName(), mwe);
            }
            mwe.consume();
        }
    }
}
