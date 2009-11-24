// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.tools;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.io.MirroredInputStream;

/**
 * Helperclass to support the application with images.
 * @author imi
 */
public class ImageProvider {

    /**
     * Position of an overlay icon
     * @author imi
     */
    public static enum OverlayPosition {
        NORTHWEST, NORTHEAST, SOUTHWEST, SOUTHEAST
    }

    /**
     * The icon cache
     */
    private static Map<String, Image> cache = new HashMap<String, Image>();

    /**
     * Add here all ClassLoader whose ressource should be searched. Plugin's class loaders are added
     * by main.
     */
    public static final List<ClassLoader> sources = new LinkedList<ClassLoader>();

    /**
     * Return an image from the specified location.
     *
     * @param subdir The position of the directory, e.g. "layer"
     * @param name The icons name (without the ending of ".png")
     * @return The requested Image.
     */
    public static ImageIcon get(String subdir, String name) {
        ImageIcon icon = getIfAvailable(subdir, name);
        if (icon == null) {
            String ext = name.indexOf('.') != -1 ? "" : ".png";
            throw new NullPointerException("/images/" + subdir + "/" + name + ext + " not found");
        }
        return icon;
    }

    public static ImageIcon getIfAvailable(String subdir, String name) {
        return getIfAvailable((Collection<String>) null, null, subdir, name);
    }

    public static final ImageIcon getIfAvailable(String[] dirs, String id, String subdir, String name) {
        return getIfAvailable(Arrays.asList(dirs), id, subdir, name);
    }

    /**
     * Like {@link #get(String)}, but does not throw and return <code>null</code> in case of nothing
     * is found. Use this, if the image to retrieve is optional.
     */
    public static ImageIcon getIfAvailable(Collection<String> dirs, String id, String subdir, String name) {
        if (name == null)
            return null;
        if (name.startsWith("http://")) {
            Image img = cache.get(name);
            if (img == null) {
                try {
                    MirroredInputStream is = new MirroredInputStream(name, new File(Main.pref.getPreferencesDir(),
                    "images").toString());
                    if (is != null) {
                        img = Toolkit.getDefaultToolkit().createImage(is.getFile().toURI().toURL());
                        cache.put(name, img);
                    }
                } catch (IOException e) {
                }
            }
            return img == null ? null : new ImageIcon(img);
        }
        if (subdir == null) {
            subdir = "";
        } else if (!subdir.equals("")) {
            subdir += "/";
        }
        String ext = name.indexOf('.') != -1 ? "" : ".png";
        String full_name = subdir + name + ext;
        String cache_name = full_name;
        /* cache separately */
        if (dirs != null && dirs.size() > 0) {
            cache_name = "id:" + id + ":" + full_name;
        }

        Image img = cache.get(cache_name);
        if (img == null) {
            // getImageUrl() does a ton of "stat()" calls and gets expensive
            // and redundant when you have a whole ton of objects. So,
            // index the cache by the name of the icon we're looking for
            // and don't bother to create a URL unless we're actually
            // creating the image.
            URL path = getImageUrl(full_name, dirs);
            if (path == null)
                return null;
            img = Toolkit.getDefaultToolkit().createImage(path);
            cache.put(cache_name, img);
        }

        return new ImageIcon(img);
    }

    private static URL getImageUrl(String path, String name) {
        if (path.startsWith("resource://")) {
            String p = path.substring("resource://".length());
            for (ClassLoader source : sources) {
                URL res;
                if ((res = source.getResource(p + name)) != null)
                    return res;
            }
        } else {
            try {
                File f = new File(path, name);
                if (f.exists())
                    return f.toURI().toURL();
            } catch (MalformedURLException e) {
            }
        }
        return null;
    }

    private static URL getImageUrl(String imageName, Collection<String> dirs) {
        URL u = null;

        // Try passed directories first
        if (dirs != null) {
            for (String name : dirs) {
                try {
                    u = getImageUrl(name, imageName);
                    if (u != null)
                        return u;
                } catch (SecurityException e) {
                    System.out.println(tr(
                            "Warning: failed to acccess directory ''{0}'' for security reasons. Exception was: {1}",
                            name, e.toString()));
                }

            }
        }
        // Try user-preference directory
        String dir = Main.pref.getPreferencesDir() + "images";
        try {
            u = getImageUrl(dir, imageName);
            if (u != null)
                return u;
        } catch (SecurityException e) {
            System.out.println(tr(
                    "Warning: failed to acccess directory ''{0}'' for security reasons. Exception was: {1}", dir, e
                    .toString()));
        }

        // Try plugins and josm classloader
        u = getImageUrl("resource://images/", imageName);
        if (u != null)
            return u;

        // Try all other ressource directories
        for (String location : Main.pref.getAllPossiblePreferenceDirs()) {
            u = getImageUrl(location + "images", imageName);
            if (u != null)
                return u;
            u = getImageUrl(location, imageName);
            if (u != null)
                return u;
        }
        System.out
        .println(tr(
                "Fatal: failed to locate image ''{0}''. This is a serious configuration problem. JOSM will stop working.",
                imageName));
        return null;
    }

    /**
     * Shortcut for get("", name);
     */
    public static ImageIcon get(String name) {
        return get("", name);
    }

    public static Cursor getCursor(String name, String overlay) {
        ImageIcon img = get("cursor", name);
        if (overlay != null) {
            img = overlay(img, "cursor/modifier/" + overlay, OverlayPosition.SOUTHEAST);
        }
        Cursor c = Toolkit.getDefaultToolkit().createCustomCursor(img.getImage(),
                name.equals("crosshair") ? new Point(10, 10) : new Point(3, 2), "Cursor");
        return c;
    }

    /**
     * @return an icon that represent the overlay of the two given icons. The second icon is layed
     * on the first relative to the given position.
     */
    public static ImageIcon overlay(Icon ground, String overlayImage, OverlayPosition pos) {
        GraphicsConfiguration conf = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice()
        .getDefaultConfiguration();
        int w = ground.getIconWidth();
        int h = ground.getIconHeight();
        ImageIcon overlay = ImageProvider.get(overlayImage);
        int wo = overlay.getIconWidth();
        int ho = overlay.getIconHeight();
        BufferedImage img = conf.createCompatibleImage(w, h, Transparency.TRANSLUCENT);
        Graphics g = img.createGraphics();
        ground.paintIcon(null, g, 0, 0);
        int x = 0, y = 0;
        switch (pos) {
        case NORTHWEST:
            x = 0;
            y = 0;
            break;
        case NORTHEAST:
            x = w - wo;
            y = 0;
            break;
        case SOUTHWEST:
            x = 0;
            y = h - ho;
            break;
        case SOUTHEAST:
            x = w - wo;
            y = h - ho;
            break;
        }
        overlay.paintIcon(null, g, x, y);
        return new ImageIcon(img);
    }

    static {
        try {
            sources.add(ClassLoader.getSystemClassLoader());
            sources.add(org.openstreetmap.josm.gui.MainApplication.class.getClassLoader());
        } catch (SecurityException ex) {
            sources.add(ImageProvider.class.getClassLoader());
        }
    }

    /*
     * from: http://www.jidesoft.com/blog/2008/02/29/rotate-an-icon-in-java/ License:
     * "feel free to use"
     */
    final static double DEGREE_90 = 90.0 * Math.PI / 180.0;

    /**
     * Creates a rotated version of the input image.
     *
     * @param c The component to get properties useful for painting, e.g. the foreground or
     * background color.
     * @param icon the image to be rotated.
     * @param rotatedAngle the rotated angle, in degree, clockwise. It could be any double but we
     * will mod it with 360 before using it.
     *
     * @return the image after rotating.
     */
    public static ImageIcon createRotatedImage(Component c, Icon icon, double rotatedAngle) {
        // convert rotatedAngle to a value from 0 to 360
        double originalAngle = rotatedAngle % 360;
        if (rotatedAngle != 0 && originalAngle == 0) {
            originalAngle = 360.0;
        }

        // convert originalAngle to a value from 0 to 90
        double angle = originalAngle % 90;
        if (originalAngle != 0.0 && angle == 0.0) {
            angle = 90.0;
        }

        double radian = Math.toRadians(angle);

        int iw = icon.getIconWidth();
        int ih = icon.getIconHeight();
        int w;
        int h;

        if ((originalAngle >= 0 && originalAngle <= 90) || (originalAngle > 180 && originalAngle <= 270)) {
            w = (int) (iw * Math.sin(DEGREE_90 - radian) + ih * Math.sin(radian));
            h = (int) (iw * Math.sin(radian) + ih * Math.sin(DEGREE_90 - radian));
        } else {
            w = (int) (ih * Math.sin(DEGREE_90 - radian) + iw * Math.sin(radian));
            h = (int) (ih * Math.sin(radian) + iw * Math.sin(DEGREE_90 - radian));
        }
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics g = image.getGraphics();
        Graphics2D g2d = (Graphics2D) g.create();

        // calculate the center of the icon.
        int cx = iw / 2;
        int cy = ih / 2;

        // move the graphics center point to the center of the icon.
        g2d.translate(w / 2, h / 2);

        // rotate the graphics about the center point of the icon
        g2d.rotate(Math.toRadians(originalAngle));

        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        icon.paintIcon(c, g2d, -cx, -cy);

        g2d.dispose();
        return new ImageIcon(image);
    }

    /**
     * Replies the icon for an OSM primitive type
     * @param type the type
     * @return the icon
     */
    public static ImageIcon get(OsmPrimitiveType type) throws IllegalArgumentException {
        if (type == null)
            throw new IllegalArgumentException(tr("Parameter ''{0}'' must not be null.", "type"));
        return get("data", type.getAPIName());
    }
}
