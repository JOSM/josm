// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.widgets;

import javax.swing.Action;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.event.ListSelectionListener;

/**
 * @author Vincent
 *
 */
public class ListPopupMenu extends JPopupMenu {

    private final JList<?>[] lists;

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
