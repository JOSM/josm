// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui;

import java.awt.Graphics;

import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.metal.MetalToolTipUI;

import org.openstreetmap.josm.tools.Logging;

/**
 * Overrides MetalToolTipUI to workaround <a href="https://bugs.openjdk.java.net/browse/JDK-8262085">JDK-8262085</a>
 * @since 17681
 */
public class JosmMetalToolTipUI extends MetalToolTipUI {

    static final JosmMetalToolTipUI sharedInstance = new JosmMetalToolTipUI();

    /**
     * Returns an instance of the {@code JosmMetalToolTipUI}.
     *
     * @param c a component
     * @return an instance of the {@code JosmMetalToolTipUI}.
     */
    public static ComponentUI createUI(JComponent c) {
        return sharedInstance;
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        try {
            super.paint(g, c);
        } catch (IllegalArgumentException e) {
            if ("Width and height must be >= 0".equals(e.getMessage())) {
                Logging.debug(e);
            } else {
                throw e;
            }
        }
    }
}
