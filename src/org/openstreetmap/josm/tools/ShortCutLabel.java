// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.tools;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.KeyEvent;


public class ShortCutLabel {
	public static String name(int shortCut, int modifiers) {
		if (shortCut == 0 && modifiers == 0)
			return "";
		String s = "";
		if ((modifiers & KeyEvent.CTRL_MASK) != 0 || (modifiers & KeyEvent.CTRL_DOWN_MASK) != 0)
			s += tr("Ctrl-");
		if ((modifiers & KeyEvent.ALT_MASK) != 0 || (modifiers & KeyEvent.ALT_DOWN_MASK) != 0)
			s += tr("Alt-");
		if ((modifiers & KeyEvent.ALT_GRAPH_MASK) != 0 || (modifiers & KeyEvent.ALT_GRAPH_DOWN_MASK) != 0)
			s += tr("AltGr-");
		if ((modifiers & KeyEvent.SHIFT_MASK) != 0 || (modifiers & KeyEvent.SHIFT_DOWN_MASK) != 0)
			s += tr("Shift-");
		if (shortCut >= KeyEvent.VK_F1 && shortCut <= KeyEvent.VK_F12)
			s += "F"+(shortCut-KeyEvent.VK_F1+1);
		else
			s += Character.toUpperCase((char)shortCut);
		return s;
	}
}
