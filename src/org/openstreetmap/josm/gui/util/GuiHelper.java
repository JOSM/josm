// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.util;

import java.awt.Component;
import java.awt.Container;

import javax.swing.SwingUtilities;

/**
 * basic gui utils
 */
public class GuiHelper {
    /**
     * disable / enable a component and all its child components
     */
    public static void setEnabledRec(Container root, boolean enabled) {
        root.setEnabled(enabled);
        Component[] children = root.getComponents();
        for (Component child : children) {
            if(child instanceof Container) {
                setEnabledRec((Container) child, enabled);
            } else {
                child.setEnabled(enabled);
            }
        }
    }

    public static void runInEDT(Runnable task) {
        if (SwingUtilities.isEventDispatchThread()) {
            task.run();
        } else {
            SwingUtilities.invokeLater(task);
        }
    }

}
