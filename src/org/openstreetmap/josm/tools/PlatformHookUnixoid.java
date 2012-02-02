// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.tools;

import java.awt.GraphicsEnvironment;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.openstreetmap.josm.Main;

/**
 * see PlatformHook.java
 *
 * BTW: THIS IS A STUB. See comments below for details.
 *
 * Don't write (Main.platform instanceof PlatformHookUnixoid) because other platform
 * hooks are subclasses of this class.
 */
public class PlatformHookUnixoid implements PlatformHook {
    @Override
    public void preStartupHook(){
    }

    @Override
    public void startupHook() {
    }

    @Override
    public void openUrl(String url) throws IOException {
        String[] programs = {"gnome-open", "kfmclient openURL", "firefox"};
        for (String program : programs) {
            try {
                Runtime.getRuntime().exec(program+" "+url);
                return;
            } catch (IOException e) {
            }
        }
    }

    protected void setupGroup(HashMap<Integer, Integer> groups, boolean load, int group, int value) {
        if(load)
            groups.put(group, Main.pref.getInteger("shortcut.groups."+group, value));
        else
            groups.put(group, value);
    }

    @Override
    public HashMap<Integer, Integer>  initShortcutGroups(boolean load) {
        HashMap<Integer, Integer> groups = new HashMap<Integer, Integer>();

        setupGroup(groups, load, Shortcut.GROUPS_DEFAULT+Shortcut.GROUP_NONE,    -1);
        setupGroup(groups, load, Shortcut.GROUPS_DEFAULT+Shortcut.GROUP_HOTKEY,  KeyEvent.CTRL_DOWN_MASK);
        setupGroup(groups, load, Shortcut.GROUPS_DEFAULT+Shortcut.GROUP_MENU,    KeyEvent.CTRL_DOWN_MASK);
        setupGroup(groups, load, Shortcut.GROUPS_DEFAULT+Shortcut.GROUP_EDIT,    0);
        setupGroup(groups, load, Shortcut.GROUPS_DEFAULT+Shortcut.GROUP_LAYER,   KeyEvent.ALT_DOWN_MASK);
        setupGroup(groups, load, Shortcut.GROUPS_DEFAULT+Shortcut.GROUP_DIRECT,  0);
        setupGroup(groups, load, Shortcut.GROUPS_DEFAULT+Shortcut.GROUP_MNEMONIC,KeyEvent.ALT_DOWN_MASK);

        setupGroup(groups, load, Shortcut.GROUPS_ALT1+Shortcut.GROUP_NONE,       -1);
        setupGroup(groups, load, Shortcut.GROUPS_ALT1+Shortcut.GROUP_HOTKEY,     KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK);
        setupGroup(groups, load, Shortcut.GROUPS_ALT1+Shortcut.GROUP_MENU,       KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK);
        setupGroup(groups, load, Shortcut.GROUPS_ALT1+Shortcut.GROUP_EDIT,       KeyEvent.SHIFT_DOWN_MASK);
        setupGroup(groups, load, Shortcut.GROUPS_ALT1+Shortcut.GROUP_LAYER,      KeyEvent.ALT_DOWN_MASK  | KeyEvent.SHIFT_DOWN_MASK);
        setupGroup(groups, load, Shortcut.GROUPS_ALT1+Shortcut.GROUP_DIRECT,     KeyEvent.SHIFT_DOWN_MASK);
        setupGroup(groups, load, Shortcut.GROUPS_ALT1+Shortcut.GROUP_MNEMONIC,   KeyEvent.ALT_DOWN_MASK);

        setupGroup(groups, load, Shortcut.GROUPS_ALT2+Shortcut.GROUP_NONE,       -1);
        setupGroup(groups, load, Shortcut.GROUPS_ALT2+Shortcut.GROUP_HOTKEY,     KeyEvent.CTRL_DOWN_MASK | KeyEvent.ALT_DOWN_MASK);
        setupGroup(groups, load, Shortcut.GROUPS_ALT2+Shortcut.GROUP_MENU,       KeyEvent.CTRL_DOWN_MASK | KeyEvent.ALT_DOWN_MASK);
        setupGroup(groups, load, Shortcut.GROUPS_ALT2+Shortcut.GROUP_EDIT,       KeyEvent.ALT_DOWN_MASK  | KeyEvent.SHIFT_DOWN_MASK);
        setupGroup(groups, load, Shortcut.GROUPS_ALT2+Shortcut.GROUP_LAYER,      KeyEvent.SHIFT_DOWN_MASK);
        setupGroup(groups, load, Shortcut.GROUPS_ALT2+Shortcut.GROUP_DIRECT,     KeyEvent.CTRL_DOWN_MASK);
        setupGroup(groups, load, Shortcut.GROUPS_ALT2+Shortcut.GROUP_MNEMONIC,   KeyEvent.ALT_DOWN_MASK);

        return groups;
    }

    @Override
    public void initSystemShortcuts() {
        // TODO: Insert system shortcuts here. See Windows and especially OSX to see how to.
    }
    /**
     * This should work for all platforms. Yeah, should.
     * See PlatformHook.java for a list of reasons why
     * this is implemented here...
     */
    @Override
    public String makeTooltip(String name, Shortcut sc) {
        String result = "";
        result += "<html>";
        result += name;
        if (sc != null && sc.getKeyText().length() != 0) {
            result += " ";
            result += "<font size='-2'>";
            result += "("+sc.getKeyText()+")";
            result += "</font>";
        }
        result += "&nbsp;</html>";
        return result;
    }

    @Override
    public String getDefaultStyle() {
        return "javax.swing.plaf.metal.MetalLookAndFeel";
    }

    @Override
    public boolean canFullscreen()
    {
        return GraphicsEnvironment.getLocalGraphicsEnvironment()
        .getDefaultScreenDevice().isFullScreenSupported();
    }

    @Override
    public boolean rename(File from, File to)
    {
        return from.renameTo(to);
    }
}
