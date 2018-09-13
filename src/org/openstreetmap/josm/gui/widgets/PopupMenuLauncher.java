// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.widgets;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JList;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.TreePath;

/**
 * Utility class that helps to display popup menus on mouse events.
 * @since 2688
 */
public class PopupMenuLauncher extends MouseAdapter {
    protected JPopupMenu menu;
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
     * @since 5886
     */
    public PopupMenuLauncher(JPopupMenu menu, boolean checkEnabled) {
        this.menu = menu;
        this.checkEnabled = checkEnabled;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        processEvent(e);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        processEvent(e);
    }

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
        if (evt != null) {
            final Component component = evt.getComponent();
            if (checkSelection(component, evt.getPoint())) {
                checkFocusAndShowMenu(component, evt);
            }
        }
    }

    protected boolean checkSelection(Component component, Point p) {
        if (component instanceof JList) {
            return checkListSelection((JList<?>) component, p) > -1;
        } else if (component instanceof JTable) {
            return checkTableSelection((JTable) component, p) > -1;
        } else if (component instanceof JTree) {
            return checkTreeSelection((JTree) component, p) != null;
        }
        return true;
    }

    protected void checkFocusAndShowMenu(final Component component, final MouseEvent evt) {
        if (component != null && component.isFocusable() && !component.hasFocus() && component.requestFocusInWindow()) {
            component.addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    showMenu(evt);
                    component.removeFocusListener(this);
                }
            });
        } else {
            showMenu(evt);
        }
    }

    protected void showMenu(MouseEvent evt) {
        if (menu != null && evt != null) {
            Component component = evt.getComponent();
            if (component.isShowing()) {
                menu.show(component, evt.getX(), evt.getY());
            }
        }
    }

    protected int checkListSelection(JList<?> list, Point p) {
        int idx = list.locationToIndex(p);
        if (idx >= 0 && idx < list.getModel().getSize() && list.getSelectedIndices().length < 2 && !list.isSelectedIndex(idx)) {
            list.setSelectedIndex(idx);
        }
        return idx;
    }

    protected int checkTableSelection(JTable table, Point p) {
        int row = table.rowAtPoint(p);
        if (row >= 0 && row < table.getRowCount() && table.getSelectedRowCount() < 2 && table.getSelectedRow() != row) {
            table.getSelectionModel().setSelectionInterval(row, row);
        }
        return row;
    }

    protected TreePath checkTreeSelection(JTree tree, Point p) {
        TreePath path = tree.getPathForLocation(p.x, p.y);
        if (path != null && tree.getSelectionCount() < 2 && !tree.isPathSelected(path)) {
            tree.setSelectionPath(path);
        }
        return path;
    }

    protected static boolean isDoubleClick(MouseEvent e) {
        return e != null && SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2;
    }

    /**
     * @return the popup menu if defined, {@code null} otherwise.
     * @since 5884
     */
    public final JPopupMenu getMenu() {
        return menu;
    }
}
