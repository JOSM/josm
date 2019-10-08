// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui;

import static org.openstreetmap.josm.tools.I18n.trc;

import java.beans.PropertyChangeListener;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

/**
 * Mode menu. Unlike traditional menus, default menu item is based on {@link JCheckBoxMenuItem}.
 * @since 15445
 */
public class ModeMenu extends JMenu {

    /**
     * Constructs a new {@code ModeMenu}.
     */
    public ModeMenu() {
        /* I18N: mnemonic: M */
        super(trc("menu", "Mode"));
    }

    @Override
    protected JMenuItem createActionComponent(Action a) {
        JCheckBoxMenuItem mi = new JCheckBoxMenuItem() {
            @Override
            protected PropertyChangeListener createActionPropertyChangeListener(Action a) {
                PropertyChangeListener pcl = createActionChangeListener(this);
                if (pcl == null) {
                    pcl = super.createActionPropertyChangeListener(a);
                }
                return pcl;
            }
        };
        mi.setHorizontalTextPosition(JButton.TRAILING);
        mi.setVerticalTextPosition(JButton.CENTER);
        return mi;
    }
}
