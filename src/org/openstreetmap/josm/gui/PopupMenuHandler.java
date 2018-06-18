// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.swing.Action;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.MenuElement;
import javax.swing.event.PopupMenuListener;

import org.openstreetmap.josm.actions.IPrimitiveAction;
import org.openstreetmap.josm.data.osm.IPrimitive;

/**
 * Handler to ease management of actions in different popup menus.
 * @since 5821
 */
public class PopupMenuHandler {

    // Set of enabled osm primitives actions
    private final Set<IPrimitiveAction> primitiveActions = new HashSet<>();
    // Managed menu
    private final JPopupMenu menu;

    /**
     * Constructs a new {@code RelationActionMenuHandler} for the specified popup menu.
     *
     * @param menu The menu to be managed
     */
    public PopupMenuHandler(JPopupMenu menu) {
        this.menu = menu;
    }

    /**
     * Appends a new separator at the end of the menu.
     * @see JPopupMenu#addSeparator
     */
    public void addSeparator() {
        menu.addSeparator();
    }

    /**
     * Appends a new menu item to the end of the menu which dispatches the specified <code>Action</code> object.
     *
     * @param a the <code>Action</code> to add to the menu
     * @return the new menu item
     * @see JPopupMenu#add(Action)
     */
    public JMenuItem addAction(Action a) {
        if (a != null) {
            if (a instanceof IPrimitiveAction) {
                primitiveActions.add((IPrimitiveAction) a);
            }
            return menu.add(a);
        }
        return null;
    }

    /**
     * Removes the menu item which dispatches the specified <code>Action</code> object.
     *
     * @param a the <code>Action</code> to remove from the menu
     * @see JPopupMenu#remove(int)
     */
    public void removeAction(Action a) {
        if (a != null) {
            if (a instanceof IPrimitiveAction) {
                primitiveActions.remove(a);
            }
            MenuElement[] elements = menu.getSubElements();
            for (int i = 0; i < elements.length; i++) {
                if (elements[i] instanceof JMenuItem && ((JMenuItem) elements[i]).getAction() == a) {
                    menu.remove(i);
                    return;
                }
            }
        }
    }

    /**
     *  Adds a <code>PopupMenu</code> listener.
     *
     *  @param l the <code>PopupMenuListener</code> to add
     *  @see JPopupMenu#addPopupMenuListener
     */
    public void addListener(PopupMenuListener l) {
        menu.addPopupMenuListener(l);
    }

    /**
     * Removes a <code>PopupMenu</code> listener.
     *
     * @param l the <code>PopupMenuListener</code> to remove
     *  @see JPopupMenu#removePopupMenuListener
     */
    public void removeListener(PopupMenuListener l) {
        menu.removePopupMenuListener(l);
    }

    /**
     * Returns all enabled primitive actions.
     * @return All primitive actions that have been added.
     * @see #addAction(Action)
     * @since 13957 (signature)
     */
    public Collection<IPrimitiveAction> getPrimitiveActions() {
        return Collections.unmodifiableCollection(primitiveActions);
    }

    /**
     * Specifies the working set of primitives for all primitive actions.
     * @param primitives The new working set of primitives. Can be null or empty
     * @see IPrimitiveAction#setPrimitives
     * @since 13957 (signature)
     */
    public void setPrimitives(Collection<? extends IPrimitive> primitives) {
        for (IPrimitiveAction action : primitiveActions) {
            action.setPrimitives(primitives);
        }
    }
}
