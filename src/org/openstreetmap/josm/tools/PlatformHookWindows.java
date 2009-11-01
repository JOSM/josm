// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.tools;

import java.awt.event.KeyEvent;
import java.io.IOException;

import org.openstreetmap.josm.Main;

/**
  * see PlatformHook.java
  */
public class PlatformHookWindows extends PlatformHookUnixoid implements PlatformHook {
    public void preStartupHook(){
    }
    public void startupHook() {
    }
    public void openUrl(String url) throws IOException {
        Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + url);
    }
    public void initShortcutGroups() {
        Main.pref.put("shortcut.groups."+(Shortcut.GROUPS_DEFAULT+Shortcut.GROUP_NONE),    Integer.toString(-1));
        Main.pref.put("shortcut.groups."+(Shortcut.GROUPS_DEFAULT+Shortcut.GROUP_HOTKEY),  Integer.toString(KeyEvent.CTRL_DOWN_MASK));
        Main.pref.put("shortcut.groups."+(Shortcut.GROUPS_DEFAULT+Shortcut.GROUP_MENU),    Integer.toString(KeyEvent.CTRL_DOWN_MASK));
        Main.pref.put("shortcut.groups."+(Shortcut.GROUPS_DEFAULT+Shortcut.GROUP_EDIT),    Integer.toString(0));
        Main.pref.put("shortcut.groups."+(Shortcut.GROUPS_DEFAULT+Shortcut.GROUP_LAYER),   Integer.toString(KeyEvent.ALT_DOWN_MASK));
        Main.pref.put("shortcut.groups."+(Shortcut.GROUPS_DEFAULT+Shortcut.GROUP_DIRECT),  Integer.toString(0));
        Main.pref.put("shortcut.groups."+(Shortcut.GROUPS_DEFAULT+Shortcut.GROUP_MNEMONIC),Integer.toString(KeyEvent.ALT_DOWN_MASK));

        Main.pref.put("shortcut.groups."+(Shortcut.GROUPS_ALT1+Shortcut.GROUP_NONE),       Integer.toString(-1));
        Main.pref.put("shortcut.groups."+(Shortcut.GROUPS_ALT1+Shortcut.GROUP_HOTKEY),     Integer.toString(KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK));
        Main.pref.put("shortcut.groups."+(Shortcut.GROUPS_ALT1+Shortcut.GROUP_MENU),       Integer.toString(KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK));
        Main.pref.put("shortcut.groups."+(Shortcut.GROUPS_ALT1+Shortcut.GROUP_EDIT),       Integer.toString(KeyEvent.SHIFT_DOWN_MASK));
        Main.pref.put("shortcut.groups."+(Shortcut.GROUPS_ALT1+Shortcut.GROUP_LAYER),      Integer.toString(KeyEvent.ALT_DOWN_MASK  | KeyEvent.SHIFT_DOWN_MASK));
        Main.pref.put("shortcut.groups."+(Shortcut.GROUPS_ALT1+Shortcut.GROUP_DIRECT),     Integer.toString(KeyEvent.SHIFT_DOWN_MASK));
        Main.pref.put("shortcut.groups."+(Shortcut.GROUPS_ALT1+Shortcut.GROUP_MNEMONIC),   Integer.toString(KeyEvent.ALT_DOWN_MASK));

        Main.pref.put("shortcut.groups."+(Shortcut.GROUPS_ALT2+Shortcut.GROUP_NONE),       Integer.toString(-1));
        Main.pref.put("shortcut.groups."+(Shortcut.GROUPS_ALT2+Shortcut.GROUP_HOTKEY),     Integer.toString(KeyEvent.CTRL_DOWN_MASK | KeyEvent.ALT_DOWN_MASK));
        Main.pref.put("shortcut.groups."+(Shortcut.GROUPS_ALT2+Shortcut.GROUP_MENU),       Integer.toString(KeyEvent.CTRL_DOWN_MASK | KeyEvent.ALT_DOWN_MASK));
        Main.pref.put("shortcut.groups."+(Shortcut.GROUPS_ALT2+Shortcut.GROUP_EDIT),       Integer.toString(KeyEvent.ALT_DOWN_MASK  | KeyEvent.SHIFT_DOWN_MASK));
        Main.pref.put("shortcut.groups."+(Shortcut.GROUPS_ALT2+Shortcut.GROUP_LAYER),      Integer.toString(KeyEvent.ALT_DOWN_MASK));
        Main.pref.put("shortcut.groups."+(Shortcut.GROUPS_ALT2+Shortcut.GROUP_DIRECT),     Integer.toString(KeyEvent.CTRL_DOWN_MASK));
        Main.pref.put("shortcut.groups."+(Shortcut.GROUPS_ALT2+Shortcut.GROUP_MNEMONIC),   Integer.toString(KeyEvent.ALT_DOWN_MASK));
    }
    public void initSystemShortcuts() {
        // This list if far from complete!
        Shortcut.registerSystemShortcut("system:exit", "unused", KeyEvent.VK_F4, KeyEvent.ALT_DOWN_MASK).setAutomatic(); // items with automatic shortcuts will not be added to the menu bar at all
        Shortcut.registerSystemShortcut("system:menuexit", "unused", KeyEvent.VK_Q, KeyEvent.CTRL_DOWN_MASK);
        Shortcut.registerSystemShortcut("system:copy", "unused", KeyEvent.VK_C, KeyEvent.CTRL_DOWN_MASK);
        Shortcut.registerSystemShortcut("system:paste", "unused", KeyEvent.VK_V, KeyEvent.CTRL_DOWN_MASK);
        Shortcut.registerSystemShortcut("system:cut", "unused", KeyEvent.VK_X, KeyEvent.CTRL_DOWN_MASK);
        Shortcut.registerSystemShortcut("system:duplicate", "unused", KeyEvent.VK_D, KeyEvent.CTRL_DOWN_MASK); // not really system, but to avoid odd results
        Shortcut.registerSystemShortcut("system:help", "unused", KeyEvent.VK_F1, 0);
    }

    public String getDefaultStyle()
    {
        return "com.sun.java.swing.plaf.windows.WindowsLookAndFeel";
    }
}
