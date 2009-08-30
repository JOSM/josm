// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.tools;

import java.awt.event.KeyEvent;
import java.io.IOException;

import org.openstreetmap.josm.Main;

/**
  * see PlatformHook.java
  *
  * BTW: THIS IS A STUB. See comments below for details.
  */
public class PlatformHookUnixoid implements PlatformHook {
    public void preStartupHook(){
    }
    public void startupHook() {
    }
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
    public void initShortcutGroups() {
        // This is the Windows list. Someone should look over it and make it more "*nix"...
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
        // TODO: Insert system shortcuts here. See Windows and espacially OSX to see how to.
    }
    /**
      * This should work for all platforms. Yeah, should.
      * See PlatformHook.java for a list of reasons why
      * this is implemented here...
      */
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
}
