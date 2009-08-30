// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui.tagging;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;

import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;

import org.openstreetmap.josm.tools.PresetTextComparator;

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
    /**
     * Sorts the menu items using the translated item text
     */
    public void sortMenu(){
        TaggingPresetMenu.sortMenu(this.menu);
    }

    /**
     * Sorts the menu items using the translated item text
     */
    public static void sortMenu(JMenu menu){
        Component[] items = menu.getMenuComponents();
        PresetTextComparator comp = new PresetTextComparator();
        ArrayList<JMenuItem> sortarray = new ArrayList<JMenuItem>();
        int lastSeperator = 0;
        for (int i = 0; i < items.length; i++) {
            Object item = items[i];
            if (item instanceof JMenu){
                sortMenu((JMenu)item);
            }
            if (item instanceof JMenuItem){
                sortarray.add((JMenuItem)item);
                if (i == items.length-1){
                    Collections.sort(sortarray, comp);
                    int pos = 0;
                    for (JMenuItem menuItem : sortarray) {
                        int oldPos;
                        if(lastSeperator == 0){
                            oldPos=pos;
                        }else {
                            oldPos = pos+lastSeperator+1;
                        }
                        menu.add(menuItem, oldPos);
                        pos++;
                    }
                    sortarray = new ArrayList<JMenuItem>();
                    lastSeperator = 0;
                }
            }else if (item instanceof JSeparator){
                Collections.sort(sortarray, comp);
                int pos = 0;
                for (JMenuItem menuItem : sortarray) {
                    int oldPos;
                    if(lastSeperator == 0){
                        oldPos=pos;
                    }else {
                        oldPos = pos+lastSeperator+1;
                    }
                    menu.add(menuItem, oldPos);
                    pos++;
                }
                sortarray = new ArrayList<JMenuItem>();
                lastSeperator = i;
            }
        }
    }
}
