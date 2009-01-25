// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui.tagging;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.Component;

import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;

import org.openstreetmap.josm.gui.tagging.TaggingPreset;

public class TaggingPresetMenu extends TaggingPreset {
    public JMenu menu = null; // set by TaggingPresetPreferences
    public void setDisplayName() {
        String n = getName();
        putValue(Action.NAME, n);
        putValue(SHORT_DESCRIPTION, tr("Preset group ''{0}''", n));
        putValue("toolbar", "tagginggroup_" + getRawName());
    }
    public void setIcon(String iconName) {
        super.setIcon(iconName);
    }
    public void actionPerformed(ActionEvent e) {
        Object s = e.getSource();
        if(menu != null && s instanceof Component)
        {
            Component co = (Component)s;
            JPopupMenu pm = new JPopupMenu(getName());
            for(Component c : menu.getMenuComponents())
            {
                if(c instanceof JMenuItem)
                {
                    JMenuItem j = new JMenuItem(((JMenuItem)c).getAction());
                    j.setText(((JMenuItem)c).getText());
                    pm.add(j);
                }
                else if(c instanceof JSeparator)
                    pm.addSeparator();
            }
            pm.show(co, co.getWidth()/2, co.getHeight()/2);
        }
    }
}
