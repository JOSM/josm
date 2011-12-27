// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.tools;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
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
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import org.apache.commons.codec.binary.Base64;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.io.MirroredInputStream;
import org.openstreetmap.josm.plugins.PluginHandler;
import org.xml.sax.Attributes;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;
import com.kitfox.svg.SVGDiagram;
import com.kitfox.svg.SVGException;
import com.kitfox.svg.SVGUniverse;

/**
 * Helper class to support the application with images.
 *
 * How to use:
 *
 * <code>ImageIcon icon = new ImageProvider(name).setMaxWidth(24).setMaxHeight(24).get();</code>
 * (there are more options, see below)
 *
 * short form:
 * <code>ImageIcon icon = ImageProvider.get(name);</code>
 *
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

    public static enum ImageType {
        SVG,    // scalable vector graphics
        OTHER   // everything else, e.g. png, gif (must be supported by Java)
    }

    protected Collection<String> dirs;
    protected String id;
    protected String subdir;
    protected String name;
    protected File archive;
    protected int width = -1;
    protected int height = -1;
    protected int maxWidth = -1;
    protected int maxHeight = -1;
    protected boolean sanitize;
    protected boolean optional;

    private static SVGUniverse svgUniverse;

    /**
     * The icon cache
     */
    private static Map<String, ImageResource> cache = new HashMap<String, ImageResource>();

    /**
     * @param subdir    Subdirectory the image lies in.
     * @param name      The name of the image. If it does not end with '.png' or '.svg',
     *                  both extensions are tried.
     */
    public ImageProvider(String subdir, String name) {
        this.subdir = subdir;
        this.name = name;
    }

    public ImageProvider(String name) {
        this.name = name;
    }

    /**
     * Directories to look for the image.
     */
    public ImageProvider setDirs(Collection<String> dirs) {
        this.dirs = dirs;
        return this;
    }

    /**
     * An id used for caching. Id is not used for cache if name starts with http://. (URL is unique anyway.)
     */
    public ImageProvider setId(String id) {
        this.id = id;
        return this;
    }

    /**
     * A zip file where the image is located.
     */
    public ImageProvider setArchive(File archive) {
        this.archive = archive;
        return this;
    }

    /**
     * The dimensions of the image.
     *
     * If not specified, the original size of the image is used.
     * The width part of the dimension can be -1. Then it will only set the height but
     * keep the aspect ratio. (And the other way around.)
     */
    public ImageProvider setSize(Dimension size) {
        this.width = size.width;
        this.height = size.height;
        return this;
    }

    /**
     * see setSize
     */
    public ImageProvider setWidth(int width) {
        this.width = width;
        return this;
    }

    /**
     * see setSize
     */
    public ImageProvider setHeight(int height) {
        this.height = height;
        return this;
    }

    /**
     * The maximum size of the image.
     *
     * It will shrink the image if necessary, but keep the aspect ratio.
     * The given width or height can be -1 which means this direction is not bounded.
     *
     * 'size' and 'maxSize' are not compatible, you should set only one of them.
     */
    public ImageProvider setMaxSize(Dimension maxSize) {
        this.maxWidth = maxSize.width;
        this.maxHeight = maxSize.height;
        return this;
    }

    /**
     * see setMaxSize
     */
    public ImageProvider setMaxWidth(int maxWidth) {
        this.maxWidth = maxWidth;
        return this;
    }

    /**
     * see setMaxSize
     */
    public ImageProvider setMaxHeight(int maxHeight) {
        this.maxHeight = maxHeight;
        return this;
    }

    /**
     * Set true, if the image should be repainted to a new BufferedImage in order to work around certain issues.
     */
    public ImageProvider setSanitize(boolean sanitize) {
        this.sanitize = sanitize;
        return this;
    }

    /**
     * The image URL comes from user data and the image may be missing.
     *
     * Set true, if JOSM should *not* throw a RuntimeException in case the image cannot be located.
     */
    public ImageProvider setOptional(boolean optional) {
        this.optional = optional;
        return this;
    }

    /**
     * Execute the image request.
     */
    public ImageIcon get() {
        ImageResource ir = getIfAvailableImpl();
        if (ir == null) {
            if (!optional) {
                String ext = name.indexOf('.') != -1 ? "" : ".???";
                throw new RuntimeException(tr("Fatal: failed to locate image ''{0}''. This is a serious configuration problem. JOSM will stop working.", name + ext));
            } else {
                System.out.println(tr("Failed to locate image ''{0}''", name));
                return null;
            }
        }
        if (maxWidth != -1 || maxHeight != -1)
            return ir.getImageIconBounded(new Dimension(maxWidth, maxHeight), sanitize);
        else
            return ir.getImageIcon(new Dimension(width, height), sanitize);
    }

    /**
     * Return an image from the specified location. Throws a RuntimeException if
     * the image cannot be located.
     *
     * @param subdir The position of the directory, e.g. 'layer'
     * @param name The icons name (with or without '.png' or '.svg' extension)
     * @return The requested Image.
     */
    public static ImageIcon get(String subdir, String name) {
        return new ImageProvider(subdir, name).get();
    }

    public static ImageIcon get(String name) {
        return new ImageProvider(name).get();
    }

    public static ImageIcon getIfAvailable(String name) {
        return new ImageProvider(name).setOptional(true).get();
    }

    public static ImageIcon getIfAvailable(String subdir, String name) {
        return new ImageProvider(subdir, name).setOptional(true).get();
    }

    @Deprecated
    public static ImageIcon getIfAvailable(String[] dirs, String id, String subdir, String name) {
        return getIfAvailable(Arrays.asList(dirs), id, subdir, name);
    }

    /**
     * Like {@link #get(String)}, but does not throw and return <code>null</code> in case of nothing
     * is found. Use this, if the image to retrieve is optional. Nevertheless a warning will
     * be printed on the console if the image could not be found.
     */
    @Deprecated
    public static ImageIcon getIfAvailable(Collection<String> dirs, String id, String subdir, String name) {
        return getIfAvailable(dirs, id, subdir, name, null);
    }

    @Deprecated
    public static ImageIcon getIfAvailable(Collection<String> dirs, String id, String subdir, String name, File archive) {
        return getIfAvailable(dirs, id, subdir, name, archive, false);
    }

    @Deprecated
    public static ImageIcon getIfAvailable(Collection<String> dirs, String id, String subdir, String name, File archive, boolean sanitize) {
        return getIfAvailable(dirs, id, subdir, name, archive, null, sanitize);
    }

    @Deprecated
    public static ImageIcon getIfAvailable(Collection<String> dirs, String id, String subdir, String name, File archive, Dimension dim, boolean sanitize) {
        return getIfAvailable(dirs, id, subdir, name, archive, dim, null, sanitize);
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
     * @param dim       The dimensions of the image if it should be scaled. null if the
     *                  original size of the image should be returned. The width
     *                  part of the dimension can be -1. Then it will scale the width
     *                  in the same way as the height. (And the other way around.)
     * @param maxSize   The maximum size of the image. It will shrink the image if necessary, and
     *                  keep the aspect ratio. The given width or height can be -1 which means this
     *                  direction is not bounded.
     *                  If this parameter has a non-null value, the parameter 'dim' will be ignored.
     * @param sanitize  If the image should be repainted to a new BufferedImage to work
     *                  around certain issues.
     */
    @Deprecated
    public static ImageIcon getIfAvailable(Collection<String> dirs, String id, String subdir, String name,
            File archive, Dimension dim, Dimension maxSize, boolean sanitize) {
        ImageProvider p = new ImageProvider(subdir, name).setDirs(dirs).setId(id).setArchive(archive).setSanitize(sanitize).setOptional(true);
        if (dim != null) {
            p.setSize(dim);
        }
        if (maxSize != null) {
            p.setMaxSize(maxSize);
        }
        return p.get();
    }

    /**
     * {@code data:[<mediatype>][;base64],<data>}
     * @see RFC2397
     */
    private static final Pattern dataUrlPattern = Pattern.compile(
            "^data:([a-zA-Z]+/[a-zA-Z+]+)?(;base64)?,(.+)$");

    private ImageResource getIfAvailableImpl() {
        if (name == null)
            return null;

        try {
            if (name.startsWith("data:")) {
                Matcher m = dataUrlPattern.matcher(name);
                if (m.matches()) {
                    String mediatype = m.group(1);
                    String base64 = m.group(2);
                    String data = m.group(3);
                    byte[] bytes = ";base64".equals(base64)
                            ? Base64.decodeBase64(data)
                            : URLDecoder.decode(data, "utf-8").getBytes();
                    if (mediatype != null && mediatype.contains("image/svg+xml")) {
                        URI uri = getSvgUniverse().loadSVG(new StringReader(new String(bytes)), name);
                        SVGDiagram svg = getSvgUniverse().getDiagram(uri);
                        return new ImageResource(svg);
                    } else {
                        return new ImageResource(new ImageIcon(bytes).getImage(), true);
                    }
                }
            }
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        } catch (IOException ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }

        ImageType type = name.toLowerCase().endsWith(".svg") ? ImageType.SVG : ImageType.OTHER;

        if (name.startsWith("http://")) {
            String url = name;
            ImageResource ir = cache.get(url);
            if (ir != null) return ir;
            ir = getIfAvailableHttp(url, type);
            if (ir != null) {
                cache.put(url, ir);
            }
            return ir;
        } else if (name.startsWith("wiki://")) {
            ImageResource ir = cache.get(name);
            if (ir != null) return ir;
            ir = getIfAvailableWiki(name, type);
            if (ir != null) {
                cache.put(name, ir);
            }
            return ir;
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

                ImageResource ir = cache.get(cache_name);
                if (ir != null) return ir;

                switch (place) {
                    case ARCHIVE:
                        if (archive != null) {
                            ir = getIfAvailableZip(full_name, archive, type);
                            if (ir != null) {
                                cache.put(cache_name, ir);
                                return ir;
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
                        if (path == null) {
                            continue;
                        }
                        ir = getIfAvailableLocalURL(path, type);
                        if (ir != null) {
                            cache.put(cache_name, ir);
                            return ir;
                        }
                        break;
                }
            }
        }
        return null;
    }

    private static ImageResource getIfAvailableHttp(String url, ImageType type) {
        try {
            MirroredInputStream is = new MirroredInputStream(url,
                    new File(Main.pref.getPreferencesDir(), "images").toString());
            switch (type) {
                case SVG:
                    URI uri = getSvgUniverse().loadSVG(is, is.getFile().toURI().toURL().toString());
                    SVGDiagram svg = getSvgUniverse().getDiagram(uri);
                    return svg == null ? null : new ImageResource(svg);
                case OTHER:
                    Image img = Toolkit.getDefaultToolkit().createImage(is.getFile().toURI().toURL());
                    return img == null ? null : new ImageResource(img, false);
                default:
                    throw new AssertionError();
            }
        } catch (IOException e) {
            return null;
        }
    }

    private static ImageResource getIfAvailableWiki(String name, ImageType type) {
        final Collection<String> defaultBaseUrls = Arrays.asList(
                "http://wiki.openstreetmap.org/w/images/",
                "http://upload.wikimedia.org/wikipedia/commons/",
                "http://wiki.openstreetmap.org/wiki/File:"
        );
        final Collection<String> baseUrls = Main.pref.getCollection("image-provider.wiki.urls", defaultBaseUrls);

        final String fn = name.substring(name.lastIndexOf('/') + 1);

        ImageResource result = null;
        for (String b : baseUrls) {
            String url;
            if (b.endsWith(":")) {
                url = getImgUrlFromWikiInfoPage(b, fn);
                if (url == null) {
                    continue;
                }
            } else {
                final String fn_md5 = Utils.md5Hex(fn);
                url = b + fn_md5.substring(0,1) + "/" + fn_md5.substring(0,2) + "/" + fn;
            }
            result = getIfAvailableHttp(url, type);
            if (result != null) {
                break;
            }
        }
        return result;
    }

    private static ImageResource getIfAvailableZip(String full_name, File archive, ImageType type) {
        ZipFile zipFile = null;
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
                            SVGDiagram svg = getSvgUniverse().getDiagram(uri);
                            return svg == null ? null : new ImageResource(svg);
                        case OTHER:
                            while(size > 0)
                            {
                                int l = is.read(buf, offs, size);
                                offs += l;
                                size -= l;
                            }
                            Image img = Toolkit.getDefaultToolkit().createImage(buf);
                            return img == null ? null : new ImageResource(img, false);
                        default:
                            throw new AssertionError();
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
        return null;
    }

    private static ImageResource getIfAvailableLocalURL(URL path, ImageType type) {
        switch (type) {
            case SVG:
                URI uri = getSvgUniverse().loadSVG(path);
                SVGDiagram svg = getSvgUniverse().getDiagram(uri);
                return svg == null ? null : new ImageResource(svg);
            case OTHER:
                Image img = Toolkit.getDefaultToolkit().createImage(path);
                return img == null ? null : new ImageResource(img, false);
            default:
                throw new AssertionError();
        }
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

    /**
     * Reads the wiki page on a certain file in html format in order to find the real image URL.
     */
    private static String getImgUrlFromWikiInfoPage(final String base, final String fn) {

        /** Quit parsing, when a certain condition is met */
        class SAXReturnException extends SAXException {
            private String result;

            public SAXReturnException(String result) {
                this.result = result;
            }

            public String getResult() {
                return result;
            }
        }

        try {
            final XMLReader parser = XMLReaderFactory.createXMLReader();
            parser.setContentHandler(new DefaultHandler() {
                @Override
                public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
                    System.out.println();
                    if (localName.equalsIgnoreCase("img")) {
                        String val = atts.getValue("src");
                        if (val.endsWith(fn))
                            throw new SAXReturnException(val);  // parsing done, quit early
                    }
                }
            });

            parser.setEntityResolver(new EntityResolver() {
                public InputSource resolveEntity (String publicId, String systemId) {
                    return new InputSource(new ByteArrayInputStream(new byte[0]));
                }
            });

            parser.parse(new InputSource(new MirroredInputStream(
                    base + fn,
                    new File(Main.pref.getPreferencesDir(), "images").toString()
            )));
        } catch (SAXReturnException r) {
            return r.getResult();
        } catch (Exception e) {
            System.out.println("INFO: parsing " + base + fn + " failed:\n" + e);
            return null;
        }
        System.out.println("INFO: parsing " + base + fn + " failed: Unexpected content.");
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

    public static Image createImageFromSvg(SVGDiagram svg, Dimension dim) {
        float realWidth = svg.getWidth();
        float realHeight = svg.getHeight();
        int width = Math.round(realWidth);
        int height = Math.round(realHeight);
        Double scaleX = null, scaleY = null;
        if (dim.width != -1) {
            width = dim.width;
            scaleX = (double) width / realWidth;
            if (dim.height == -1) {
                scaleY = scaleX;
                height = (int) Math.round(realHeight * scaleY);
            } else {
                height = dim.height;
                scaleY = (double) height / realHeight;
            }
        } else if (dim.height != -1) {
            height = dim.height;
            scaleX = scaleY = (double) height / realHeight;
            width = (int) Math.round(realWidth * scaleX);
        }
        Image img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = ((BufferedImage) img).createGraphics();
        g.setClip(0, 0, width, height);
        if (scaleX != null) {
            g.scale(scaleX, scaleY);
        }
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        try {
            svg.render(g);
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
