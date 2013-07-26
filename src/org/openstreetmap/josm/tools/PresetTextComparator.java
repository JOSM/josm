// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import java.io.Serializable;
import java.util.Comparator;

import javax.swing.JMenuItem;

import org.openstreetmap.josm.Main;

public class PresetTextComparator implements Comparator<JMenuItem>, Serializable {
    @Override
    public int compare(JMenuItem arg0, JMenuItem arg1) {
        if (Main.main.menu.presetSearchAction.equals(arg0.getAction()))
            return -1;
        else if (Main.main.menu.presetSearchAction.equals(arg0.getAction()))
            return 1;
        else if (arg0.getText() == arg1.getText())
            return 0;
        else if (arg0.getText() == null)
            return -1;
        else if (arg1.getText() == null)
            return 1;
        else
            return arg0.getText().compareTo(arg1.getText());
    }

}
