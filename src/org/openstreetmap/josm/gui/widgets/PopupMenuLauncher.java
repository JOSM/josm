// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.widgets;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JPopupMenu;

public class PopupMenuLauncher extends MouseAdapter {
    private JPopupMenu menu;

    public PopupMenuLauncher() {
        menu = null;
    }
    public PopupMenuLauncher(JPopupMenu menu) {
        this.menu = menu;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.isPopupTrigger()) {
            launch(e);
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (e.isPopupTrigger()) {
            launch(e);
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.isPopupTrigger()) {
            launch(e);
        }
    }

    public void launch(MouseEvent evt) {
        if (menu != null) {
            menu.show(evt.getComponent(), evt.getX(),evt.getY());
        }
    }
}
