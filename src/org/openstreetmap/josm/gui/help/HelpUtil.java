// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.help;

import java.awt.event.KeyEvent;

import javax.swing.JComponent;
import javax.swing.KeyStroke;

import org.openstreetmap.josm.Main;

public class HelpUtil {

    /**
     * Makes a component aware of context sensitive help.
     * 
     * @param component the component
     * @param topic the help topic
     */
    static public void setHelpContext(JComponent component, String topic) {
        component.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_F1,0), "help");
        component.getActionMap().put("help", Main.main.menu.help);
        component.putClientProperty("help", topic);
    }

}
