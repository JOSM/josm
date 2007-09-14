// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.tools;

import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.openstreetmap.josm.Main;

/**
 * Helperclass to support the application with images.
 * @author imi
 */
public class ImageProvider {

	/**
	 * Position of an overlay icon
	 * @author imi
	 */
	public static enum OverlayPosition {NORTHWEST, NORTHEAST, SOUTHWEST, SOUTHEAST}

	/**
	 * The icon cache
	 */
	private static Map<URL, Image> cache = new HashMap<URL, Image>();

	/**
	 * Add here all ClassLoader whose ressource should be searched.
	 * Plugin's class loaders are added by main.
	 */
	public static final List<ClassLoader> sources = new LinkedList<ClassLoader>();

	/**
	 * Return an image from the specified location.
	 *
	 * @param subdir	The position of the directory, e.g. "layer"
	 * @param name		The icons name (without the ending of ".png")
	 * @return The requested Image.
	 */
	public static ImageIcon get(String subdir, String name) {
		ImageIcon icon = getIfAvailable(subdir, name);
		if (icon == null) {
			String ext = name.indexOf('.') != -1 ? "" : ".png";
			throw new NullPointerException("/images/"+subdir+name+ext+" not found");
		}
		return icon;
	}

	/**
	 * Like {@link #get(String)}, but does not throw and return <code>null</code>
	 * in case of nothing is found. Use this, if the image to retrieve is optional.
	 */
	public static ImageIcon getIfAvailable(String subdir, String name) {
		if (name == null)
			return null;
		if (subdir == null)
			subdir = "";
		else if (!subdir.equals(""))
			subdir += "/";
		String ext = name.indexOf('.') != -1 ? "" : ".png";

		URL path = getImageUrl(subdir+name+ext);
		if (path == null)
			return null;
		
		Image img = cache.get(path);
		if (img == null) {
			img = Toolkit.getDefaultToolkit().createImage(path);
			cache.put(path, img);
		}
		return new ImageIcon(img);
	}

	private static URL getImageUrl(String imageName) {
	    URL path = null;
	    // Try user-preference directory first
    	try {
    		if (new File(Main.pref.getPreferencesDir()+"images/"+imageName).exists())
    			return new URL("file", "", Main.pref.getPreferencesDir()+"images/"+imageName);
    	} catch (MalformedURLException e) {
    	}

	    // Try plugins and josm classloader
	    for (ClassLoader source : sources)
			if ((path = source.getResource("images/"+imageName)) != null)
				return path;

	    // Try all other ressource directories
	    for (String location : Main.pref.getAllPossiblePreferenceDirs()) {
	    	try {
	    		if (new File(location+"images/"+imageName).exists())
	    			return new URL("file", "", location+"images/"+imageName);
	    	} catch (MalformedURLException e) {
	    	}
	    }
	    return null;
    }

	/**
	 * Shortcut for get("", name);
	 */
	public static ImageIcon get(String name) {
		return get("", name);
	}

	public static Cursor getCursor(String name, String overlay) {
		ImageIcon img = get("cursor",name);
		if (overlay != null)
			img = overlay(img, "cursor/modifier/"+overlay, OverlayPosition.SOUTHEAST);
		Cursor c = Toolkit.getDefaultToolkit().createCustomCursor(img.getImage(),
				name.equals("crosshair") ? new Point(10,10) : new Point(3,2), "Cursor");
		return c;
	}

	/**
	 * @return an icon that represent the overlay of the two given icons. The
	 * second icon is layed on the first relative to the given position.
	 */
	public static ImageIcon overlay(Icon ground, String overlayImage, OverlayPosition pos) {
		GraphicsConfiguration conf = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
		int w = ground.getIconWidth();
		int h = ground.getIconHeight();
		ImageIcon overlay = ImageProvider.get(overlayImage);
		int wo = overlay.getIconWidth();
		int ho = overlay.getIconHeight();
		BufferedImage img = conf.createCompatibleImage(w,h, Transparency.TRANSLUCENT);
		Graphics g = img.createGraphics();
		ground.paintIcon(null, g, 0, 0);
		int x = 0, y = 0;
		switch (pos) {
		case NORTHWEST:
			x = 0;
			y = 0;
			break;
		case NORTHEAST:
			x = w-wo;
			y = 0;
			break;
		case SOUTHWEST:
			x = 0;
			y = h-ho;
			break;
		case SOUTHEAST:
			x = w-wo;
			y = h-ho;
			break;
		}
		overlay.paintIcon(null, g, x, y);
		return new ImageIcon(img);
	}

	static {
		sources.add(ClassLoader.getSystemClassLoader());
	}
}
