// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.util;

import java.awt.Graphics;

import javax.swing.JComponent;
import javax.swing.MenuSelectionManager;
import javax.swing.UIManager;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicCheckBoxMenuItemUI;

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
    public void update(Graphics g, JComponent c) {
        ComponentUI ui = UIManager.getUI(c);
        if (ui != null) {
            this.uninstallUI(c);
            try {
                ui.installUI(c);
                try {
                    ui.update(g, c);
                } finally {
                    ui.uninstallUI(c);
                }
            } finally {
                this.installUI(c);
            }
        } else {
            super.update(g, c);
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
