// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.widgets;

import javax.swing.Action;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.event.ListSelectionListener;

/**
 * A popup menu for one or more lists. If actions are added to this menu, a ListSelectionListener is registered automatically.
 * @author Vincent
 */
public class ListPopupMenu extends JPopupMenu {

    private final JList<?>[] lists;

    /**
     * Create a new ListPopupMenu
     * @param lists The lists to which listeners should be appended
     */
    public ListPopupMenu(JList<?>... lists) {
        this.lists = lists;
    }

    @Override
    public JMenuItem add(Action a) {
        if (lists != null && a instanceof ListSelectionListener) {
            for (JList<?> list : lists) {
                list.addListSelectionListener((ListSelectionListener) a);
            }
        }
        return super.add(a);
    }
}
