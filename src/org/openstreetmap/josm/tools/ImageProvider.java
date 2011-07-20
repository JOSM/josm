// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.tools;

import static org.openstreetmap.josm.tools.I18n.tr;

import com.kitfox.svg.SVGDiagram;
import com.kitfox.svg.SVGException;
import com.kitfox.svg.SVGUniverse;

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
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.io.MirroredInputStream;
import org.openstreetmap.josm.plugins.PluginHandler;

/**
 * Helperclass to support the application with images.
 * @author imi
 */
public class ImageProvider {

    private static SVGUniverse svgUniverse;

    /**
     * Position of an overlay icon
     * @author imi
     */
    public static enum OverlayPosition {
        NORTHWEST, NORTHEAST, SOUTHWEST, SOUTHEAST
    }

    public static enum ImageType {
        SVG,    // scalable vector graphics
        OTHER   // everything else, e.g. png, gif
                // must be supported by Java
    }

    /**
     * remember whether the image has been sanitized
     */
    private static class ImageWrapper {
        Image img;
        boolean sanitized;

        public ImageWrapper(Image img, boolean sanitized) {
            this.img = img;
            this.sanitized = sanitized;
        }
    }

    /**
     * The icon cache
     */
    private static Map<String, ImageWrapper> cache = new HashMap<String, ImageWrapper>();

    /**
     * Return an image from the specified location.
     *
     * @param subdir The position of the directory, e.g. 'layer'
     * @param name The icons name (with or without '.png' or '.svg' extension)
     * @return The requested Image.
     */
    public static ImageIcon get(String subdir, String name) {
        ImageIcon icon = getIfAvailable(subdir, name);
        if (icon == null) {
            String ext = name.indexOf('.') != -1 ? "" : ".???";
            throw new NullPointerException(tr(
            "Fatal: failed to locate image ''{0}''. This is a serious configuration problem. JOSM will stop working.",
            name+ext));
        }
        return icon;
    }

    /**
     * Shortcut for get("", name);
     */
    public static ImageIcon get(String name) {
        return get("", name);
    }

    public static ImageIcon getIfAvailable(String subdir, String name) {
        return getIfAvailable((Collection<String>) null, null, subdir, name);
    }

    public static ImageIcon getIfAvailable(String[] dirs, String id, String subdir, String name) {
        return getIfAvailable(Arrays.asList(dirs), id, subdir, name);
    }

    /**
     * Like {@link #get(String)}, but does not throw and return <code>null</code> in case of nothing
     * is found. Use this, if the image to retrieve is optional. Nevertheless a warning will
     * be printed on the console if the image could not be found.
     */
    public static ImageIcon getIfAvailable(Collection<String> dirs, String id, String subdir, String name) {
        return getIfAvailable(dirs, id, subdir, name, null);
    }

    public static ImageIcon getIfAvailable(Collection<String> dirs, String id, String subdir, String name, File archive) {
        return getIfAvailable(dirs, id, subdir, name, archive, false);
    }

    /**
     * The full path of the image is either a url (starting with http://)
     * or something like
     *   dirs.get(i)+"/"+subdir+"/"+name+".png".
     * @param dirs      Directories to look (may be null).
     * @param id        An id used for caching. Id is not used for cache if name starts with http://. (URL is unique anyway.)
     * @param subdir    Subdirectory the image lies in.
     * @param name      The name of the image. If it does not end with '.png' or '.svg',
     *                  it will try both extensions.
     * @param archive   A zip file where the image is located (may be null).
     * @param sanitize  If the image should be repainted to a new BufferedImage to work
     *                  around certain issues.
     */
    public static ImageIcon getIfAvailable(Collection<String> dirs, String id, String subdir, String name, File archive, boolean sanitize) {
        ImageWrapper iw = getIfAvailableImpl(dirs, id, subdir, name, archive);
        if (iw == null)
            return null;
        if (sanitize && !iw.sanitized) {
            iw.img = sanitize(iw.img);
            iw.sanitized = true;
        }
        return new ImageIcon(iw.img);
    }

    private static ImageWrapper getIfAvailableImpl(Collection<String> dirs, String id, String subdir, String name, File archive) {
        if (name == null)
            return null;
        ImageType type = name.toLowerCase().endsWith(".svg") ? ImageType.SVG : ImageType.OTHER;

        if (name.startsWith("http://")) {
            String url = name;
            ImageWrapper iw = cache.get(url);
            if (iw != null) return iw;
            iw = getIfAvailableHttp(url, type);
            if (iw != null) {
                cache.put(url, iw);
            }
            return iw;
        }

        if (subdir == null) {
            subdir = "";
        } else if (!subdir.equals("")) {
            subdir += "/";
        }
        String[] extensions;
        if (name.indexOf('.') != -1) {
            extensions = new String[] { "" };
        } else {
            extensions = new String[] { ".png", ".svg"};
        }
        final int ARCHIVE = 0, LOCAL = 1;
        for (int place : new Integer[] { ARCHIVE, LOCAL }) {
            for (String ext : extensions) {

                if (".svg".equals(ext)) {
                    type = ImageType.SVG;
                } else if (".png".equals(ext)) {
                    type = ImageType.OTHER;
                }

                String full_name = subdir + name + ext;
                String cache_name = full_name;
                /* cache separately */
                if (dirs != null && dirs.size() > 0) {
                    cache_name = "id:" + id + ":" + full_name;
                    if(archive != null) {
                        cache_name += ":" + archive.getName();
                    }
                }

                ImageWrapper iw = cache.get(cache_name);
                if (iw != null) return iw;

                switch (place) {
                    case ARCHIVE:
                        if (archive != null) {
                            iw = getIfAvailableZip(full_name, archive, type);
                            if (iw != null) {
                                cache.put(cache_name, iw);
                                return iw;
                            }
                        }
                        break;
                    case LOCAL:
                        // getImageUrl() does a ton of "stat()" calls and gets expensive
                        // and redundant when you have a whole ton of objects. So,
                        // index the cache by the name of the icon we're looking for
                        // and don't bother to create a URL unless we're actually
                        // creating the image.
                        URL path = getImageUrl(full_name, dirs);
                        if (path == null)
                            continue;
                        iw = getIfAvailableLocalURL(path, type);
                        if (iw != null) {
                            cache.put(cache_name, iw);
                            return iw;
                        }
                        break;
                }
            }
        }
        return null;
    }

    private static ImageWrapper getIfAvailableHttp(String url, ImageType type) {
        Image img = null;
        try {
            MirroredInputStream is = new MirroredInputStream(url,
                    new File(Main.pref.getPreferencesDir(), "images").toString());
            switch (type) {
                case SVG:
                    URI uri = getSvgUniverse().loadSVG(is, is.getFile().toURI().toURL().toString());
                    img = createImageFromSvgUri(uri);
                    break;
                case OTHER:
                    img = Toolkit.getDefaultToolkit().createImage(is.getFile().toURI().toURL());
                    break;
            }
        } catch (IOException e) {
        }
        return img == null ? null : new ImageWrapper(img, false);
    }

    private static ImageWrapper getIfAvailableZip(String full_name, File archive, ImageType type) {
        ZipFile zipFile = null;
        Image img = null;
        try
        {
            zipFile = new ZipFile(archive);
            ZipEntry entry = zipFile.getEntry(full_name);
            if(entry != null)
            {
                int size = (int)entry.getSize();
                int offs = 0;
                byte[] buf = new byte[size];
                InputStream is = null;
                try {
                    is = zipFile.getInputStream(entry);
                    switch (type) {
                        case SVG:
                            URI uri = getSvgUniverse().loadSVG(is, full_name);
                            img = createImageFromSvgUri(uri);
                            break;
                        case OTHER:
                            while(size > 0)
                            {
                                int l = is.read(buf, offs, size);
                                offs += l;
                                size -= l;
                            }
                            img = Toolkit.getDefaultToolkit().createImage(buf);
                            break;
                    }
                } finally {
                    if (is != null) {
                        is.close();
                    }
                }
            }
        } catch (Exception e) {
            System.err.println(tr("Warning: failed to handle zip file ''{0}''. Exception was: {1}", archive.getName(), e.toString()));
        } finally {
            if (zipFile != null) {
                try {
                    zipFile.close();
                } catch (IOException ex) {
                }
            }
        }
        return img == null ? null : new ImageWrapper(img, false);
    }

    private static ImageWrapper getIfAvailableLocalURL(URL path, ImageType type) {
        Image img = null;
        switch (type) {
            case SVG:
                URI uri = getSvgUniverse().loadSVG(path);
                img = createImageFromSvgUri(uri);
                break;
            case OTHER:
                img = Toolkit.getDefaultToolkit().createImage(path);
                break;
        }
        return img == null ? null : new ImageWrapper(img, false);
    }

    private static URL getImageUrl(String path, String name) {
        if (path != null && path.startsWith("resource://")) {
            String p = path.substring("resource://".length());
            for (ClassLoader source : PluginHandler.getResourceClassLoaders()) {
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
                            "Warning: failed to access directory ''{0}'' for security reasons. Exception was: {1}",
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
                    "Warning: failed to access directory ''{0}'' for security reasons. Exception was: {1}", dir, e
                    .toString()));
        }

        // Absolute path?
        u = getImageUrl(null, imageName);
        if (u != null)
            return u;

        // Try plugins and josm classloader
        u = getImageUrl("resource://images/", imageName);
        if (u != null)
            return u;

        // Try all other resource directories
        for (String location : Main.pref.getAllPossiblePreferenceDirs()) {
            u = getImageUrl(location + "images", imageName);
            if (u != null)
                return u;
            u = getImageUrl(location, imageName);
            if (u != null)
                return u;
        }

        return null;
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
        return overlay(ground, ImageProvider.get(overlayImage), pos);
    }

    public static ImageIcon overlay(Icon ground, Icon overlay, OverlayPosition pos) {
        GraphicsConfiguration conf = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice()
        .getDefaultConfiguration();
        int w = ground.getIconWidth();
        int h = ground.getIconHeight();
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
        CheckParameterUtil.ensureParameterNotNull(type, "type");
        return get("data", type.getAPIName());
    }

    public static BufferedImage sanitize(Image img) {
        (new ImageIcon(img)).getImage(); // load competely
        int width = img.getWidth(null);
        int height = img.getHeight(null);
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        result.getGraphics().drawImage(img, 0, 0, null);
        return result;
    }

    private static Image createImageFromSvgUri(URI uri) {
        SVGDiagram dia = getSvgUniverse().getDiagram(uri);
        int w = (int)dia.getWidth();
        int h = (int)dia.getHeight();
        Image img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = ((BufferedImage) img).createGraphics();
        g.setClip(0, 0, w, h);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        try {
            dia.render(g);
        } catch (SVGException ex) {
            return null;
        }
        return img;
    }

    private static SVGUniverse getSvgUniverse() {
        if (svgUniverse == null) {
            svgUniverse = new SVGUniverse();
        }
        return svgUniverse;
    }
}
