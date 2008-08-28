// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.tools;

import java.awt.Color;

/**
 * Helper to convert from color to html string and back
 */
public class ColorHelper {
	
	public static Color html2color(String html) {
		if (html.length() > 0 && html.charAt(0) == '#')
			html = html.substring(1);
		else if (html.length() != 6 && html.length() != 8)
			return null;
		try {
			return new Color(
					Integer.parseInt(html.substring(0,2),16),
					Integer.parseInt(html.substring(2,4),16),
					Integer.parseInt(html.substring(4,6),16),
					(html.length() == 8 ? Integer.parseInt(html.substring(6,8),16) : 255));
		} catch (NumberFormatException e) {
			return null;
		}		
	}

	private static String int2hex(int i) {
		String s = Integer.toHexString(i / 16) + Integer.toHexString(i % 16);
		return s.toUpperCase();
	}
	
	public static String color2html(Color col) {
		return "#"+int2hex(col.getRed())+int2hex(col.getGreen())+int2hex(col.getBlue());
	}
}
