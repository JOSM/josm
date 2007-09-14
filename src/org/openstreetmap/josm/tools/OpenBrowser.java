// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.tools;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.JApplet;

import org.openstreetmap.josm.Main;

/**
 * Helper to open platform web browser on different platforms
 * @author Imi
 */
public class OpenBrowser {

	/**
	 * @return <code>null</code> for success or a string in case of an error.
	 */
	public static String displayUrl(String url) {
		if (Main.applet) {
			try {
				JApplet applet = (JApplet) Main.parent;
				applet.getAppletContext().showDocument(new URL(url));
				return null;
			} catch (MalformedURLException mue) {
				return mue.getMessage();
			}
		}

		String os = System.getProperty("os.name");
		if (os == null)
			return "unknown operating system";
		try {
			if (os != null && os.startsWith("Windows"))
				windows(url);
			else if (os.equals("Linux") || os.equals("Solaris") || os.equals("SunOS") || os.equals("AIX") || os.equals("FreeBSD"))
				linux(url);
			else if (os.equals("Mac OS") || os.equals("Mac OS X"))
				mac(url);
			else
				return "unknown operating system";
		} catch (IOException e) {
			return e.getMessage();
		}
		return null;
	}

	private static void windows(String url) throws IOException {
		Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + url);
	}

	private static void linux(String url) {
		String[] programs = {"gnome-open", "kfmclient openURL", "firefox"};
		for (String program : programs) {
			try {
				Runtime.getRuntime().exec(program+" "+url);
				return;
			} catch (IOException e) {
            }
		}
	}

	private static void mac(String url) throws IOException {
		Runtime.getRuntime().exec("open " + url);
	}
}
