// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.tools;

public class Base64 {

	private static String enc = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

	public static String encode(String s) {
		StringBuilder out = new StringBuilder();
		for (int i = 0; i < (s.length()+2)/3; ++i) {
			int l = Math.min(3, s.length()-i*3);
			String buf = s.substring(i*3, i*3+l);
            out.append(enc.charAt(buf.charAt(0)>>2));
            out.append(enc.charAt((buf.charAt(0) & 0x03) << 4 | (l==1?0:(buf.charAt(1) & 0xf0) >> 4)));
            out.append(l>1?enc.charAt((buf.charAt(1) & 0x0f) << 2 | (l==2?0:(buf.charAt(2) & 0xc0) >> 6)):'=');
            out.append(l>2?enc.charAt(buf.charAt(2) & 0x3f):'=');
		}
		return out.toString();
	}
}
