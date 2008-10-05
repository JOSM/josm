// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.tools;

import static org.openstreetmap.josm.tools.I18n.tr;
import org.openstreetmap.josm.tools.ShortCut;
import org.openstreetmap.josm.Main;

import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;
import java.io.IOException;
import javax.swing.KeyStroke;

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
	public void initShortCutGroups() {
		// This is the Windows list. Someone should look over it and make it more "*nix"...
		Main.pref.put("shortcut.groups."+(ShortCut.GROUPS_DEFAULT+ShortCut.GROUP_NONE),    Integer.toString(-1));
		Main.pref.put("shortcut.groups."+(ShortCut.GROUPS_DEFAULT+ShortCut.GROUP_HOTKEY),  Integer.toString(KeyEvent.CTRL_DOWN_MASK));
		Main.pref.put("shortcut.groups."+(ShortCut.GROUPS_DEFAULT+ShortCut.GROUP_MENU),    Integer.toString(KeyEvent.CTRL_DOWN_MASK));
		Main.pref.put("shortcut.groups."+(ShortCut.GROUPS_DEFAULT+ShortCut.GROUP_EDIT),    Integer.toString(0));
		Main.pref.put("shortcut.groups."+(ShortCut.GROUPS_DEFAULT+ShortCut.GROUP_LAYER),   Integer.toString(KeyEvent.ALT_DOWN_MASK));
		Main.pref.put("shortcut.groups."+(ShortCut.GROUPS_DEFAULT+ShortCut.GROUP_DIRECT),  Integer.toString(0));
		Main.pref.put("shortcut.groups."+(ShortCut.GROUPS_DEFAULT+ShortCut.GROUP_MNEMONIC),Integer.toString(KeyEvent.ALT_DOWN_MASK));

		Main.pref.put("shortcut.groups."+(ShortCut.GROUPS_ALT1+ShortCut.GROUP_NONE),       Integer.toString(-1));
		Main.pref.put("shortcut.groups."+(ShortCut.GROUPS_ALT1+ShortCut.GROUP_HOTKEY),     Integer.toString(KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK));
		Main.pref.put("shortcut.groups."+(ShortCut.GROUPS_ALT1+ShortCut.GROUP_MENU),       Integer.toString(KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK));
		Main.pref.put("shortcut.groups."+(ShortCut.GROUPS_ALT1+ShortCut.GROUP_EDIT),       Integer.toString(KeyEvent.SHIFT_DOWN_MASK));
		Main.pref.put("shortcut.groups."+(ShortCut.GROUPS_ALT1+ShortCut.GROUP_LAYER),      Integer.toString(KeyEvent.ALT_DOWN_MASK  | KeyEvent.SHIFT_DOWN_MASK));
		Main.pref.put("shortcut.groups."+(ShortCut.GROUPS_ALT1+ShortCut.GROUP_DIRECT),     Integer.toString(KeyEvent.SHIFT_DOWN_MASK));
		Main.pref.put("shortcut.groups."+(ShortCut.GROUPS_ALT1+ShortCut.GROUP_MNEMONIC),   Integer.toString(KeyEvent.ALT_DOWN_MASK));

		Main.pref.put("shortcut.groups."+(ShortCut.GROUPS_ALT2+ShortCut.GROUP_NONE),       Integer.toString(-1));
		Main.pref.put("shortcut.groups."+(ShortCut.GROUPS_ALT2+ShortCut.GROUP_HOTKEY),     Integer.toString(KeyEvent.CTRL_DOWN_MASK | KeyEvent.ALT_DOWN_MASK));
		Main.pref.put("shortcut.groups."+(ShortCut.GROUPS_ALT2+ShortCut.GROUP_MENU),       Integer.toString(KeyEvent.CTRL_DOWN_MASK | KeyEvent.ALT_DOWN_MASK));
		Main.pref.put("shortcut.groups."+(ShortCut.GROUPS_ALT2+ShortCut.GROUP_EDIT),       Integer.toString(KeyEvent.ALT_DOWN_MASK  | KeyEvent.SHIFT_DOWN_MASK));
		Main.pref.put("shortcut.groups."+(ShortCut.GROUPS_ALT2+ShortCut.GROUP_LAYER),      Integer.toString(KeyEvent.ALT_DOWN_MASK));
		Main.pref.put("shortcut.groups."+(ShortCut.GROUPS_ALT2+ShortCut.GROUP_DIRECT),     Integer.toString(KeyEvent.CTRL_DOWN_MASK));
		Main.pref.put("shortcut.groups."+(ShortCut.GROUPS_ALT2+ShortCut.GROUP_MNEMONIC),   Integer.toString(KeyEvent.ALT_DOWN_MASK));
	}
	public void initSystemShortCuts() {
		// TODO: Insert system shortcuts here. See Windows and espacially OSX to see how to.
	}
	/**
	  * This should work for all platforms. Yeah, should.
	  * See PlatformHook.java for a list of reasons why
	  * this is implemented here...
	  */
	public String makeTooltip(String name, ShortCut sc) {
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
