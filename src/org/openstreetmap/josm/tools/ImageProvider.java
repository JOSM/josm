// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageFilter;
import java.awt.image.ImageProducer;
import java.awt.image.RGBImageFilter;
import java.awt.image.WritableRaster;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import javax.swing.ImageIcon;
import javax.xml.bind.DatatypeConverter;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.io.CachedFile;
import org.openstreetmap.josm.plugins.PluginHandler;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.Attributes;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import com.kitfox.svg.SVGDiagram;
import com.kitfox.svg.SVGUniverse;

/**
 * Helper class to support the application with images.
 *
 * How to use:
 *
 * <code>ImageIcon icon = new ImageProvider(name).setMaxSize(ImageSizes.MAP).get();</code>
 * (there are more options, see below)
 *
 * short form:
 * <code>ImageIcon icon = ImageProvider.get(name);</code>
 *
 * @author imi
 */
public class ImageProvider {

    private static final String HTTP_PROTOCOL  = "http://";
    private static final String HTTPS_PROTOCOL = "https://";
    private static final String WIKI_PROTOCOL  = "wiki://";

    /**
     * Position of an overlay icon
     */
    public static enum OverlayPosition {
        /** North west */
        NORTHWEST,
        /** North east */
        NORTHEAST,
        /** South west */
        SOUTHWEST,
        /** South east */
        SOUTHEAST
    }

    /**
     * Supported image types
     */
    public static enum ImageType {
        /** Scalable vector graphics */
        SVG,
        /** Everything else, e.g. png, gif (must be supported by Java) */
        OTHER
    }

    /**
     * Supported image sizes
     * @since 7687
     */
    public static enum ImageSizes {
        /** SMALL_ICON value of on Action */
        SMALLICON,
        /** LARGE_ICON_KEY value of on Action */
        LARGEICON,
        /** map icon */
        MAP,
        /** map icon maximum size */
        MAPMAX,
        /** cursor icon size */
        CURSOR,
        /** cursor overlay icon size */
        CURSOROVERLAY,
        /** menu icon size */
        MENU,
        /** menu icon size in popup menus
         * @since 8323
         */
        POPUPMENU,
        /** Layer list icon size
         * @since 8323
         */
        LAYER
    }

    /**
     * Property set on {@code BufferedImage} returned by {@link #makeImageTransparent}.
     * @since 7132
     */
    public static final String PROP_TRANSPARENCY_FORCED = "josm.transparency.forced";

    /**
     * Property set on {@code BufferedImage} returned by {@link #read} if metadata is required.
     * @since 7132
     */
    public static final String PROP_TRANSPARENCY_COLOR = "josm.transparency.color";

    /** directories in which images are searched */
    protected Collection<String> dirs;
    /** caching identifier */
    protected String id;
    /** sub directory the image can be found in */
    protected String subdir;
    /** image file name */
    protected String name;
    /** archive file to take image from */
    protected File archive;
    /** directory inside the archive */
    protected String inArchiveDir;
    /** width of the resulting image, -1 when original image data should be used */
    protected int width = -1;
    /** height of the resulting image, -1 when original image data should be used */
    protected int height = -1;
    /** maximum width of the resulting image, -1 for no restriction */
    protected int maxWidth = -1;
    /** maximum height of the resulting image, -1 for no restriction */
    protected int maxHeight = -1;
    /** In case of errors do not throw exception but return <code>null</code> for missing image */
    protected boolean optional;
    /** <code>true</code> if warnings should be suppressed */
    protected boolean suppressWarnings;
    /** list of class loaders to take images from */
    protected Collection<ClassLoader> additionalClassLoaders;
    /** ordered list of overlay images */
    protected List<ImageOverlay> overlayInfo = null;

    private static SVGUniverse svgUniverse;

    /**
     * The icon cache
     */
    private static final Map<String, ImageResource> cache = new HashMap<>();

    /**
     * Caches the image data for rotated versions of the same image.
     */
    private static final Map<Image, Map<Long, ImageResource>> ROTATE_CACHE = new HashMap<>();

    private static final ExecutorService IMAGE_FETCHER = Executors.newSingleThreadExecutor();

    /**
     * Callback interface for asynchronous image loading.
     */
    public interface ImageCallback {
        /**
         * Called when image loading has finished.
         * @param result the loaded image icon
         */
        void finished(ImageIcon result);
    }

    /**
     * Callback interface for asynchronous image loading (with delayed scaling possibility).
     * @since 7693
     */
    public interface ImageResourceCallback {
        /**
         * Called when image loading has finished.
         * @param result the loaded image resource
         */
        void finished(ImageResource result);
    }

    /**
     * Constructs a new {@code ImageProvider} from a filename in a given directory.
     * @param subdir subdirectory the image lies in
     * @param name the name of the image. If it does not end with '.png' or '.svg',
     * both extensions are tried.
     */
    public ImageProvider(String subdir, String name) {
        this.subdir = subdir;
        this.name = name;
    }

    /**
     * Constructs a new {@code ImageProvider} from a filename.
     * @param name the name of the image. If it does not end with '.png' or '.svg',
     * both extensions are tried.
     */
    public ImageProvider(String name) {
        this.name = name;
    }

    /**
     * Constructs a new {@code ImageProvider} from an existing one.
     * @param image the existing image provider to be copied
     * @since 8095
     */
    public ImageProvider(ImageProvider image) {
        this.dirs = image.dirs;
        this.id = image.id;
        this.subdir = image.subdir;
        this.name = image.name;
        this.archive = image.archive;
        this.inArchiveDir = image.inArchiveDir;
        this.width = image.width;
        this.height = image.height;
        this.maxWidth = image.maxWidth;
        this.maxHeight = image.maxHeight;
        this.optional = image.optional;
        this.suppressWarnings = image.suppressWarnings;
        this.additionalClassLoaders = image.additionalClassLoaders;
        this.overlayInfo = image.overlayInfo;
    }

    /**
     * Directories to look for the image.
     * @param dirs The directories to look for.
     * @return the current object, for convenience
     */
    public ImageProvider setDirs(Collection<String> dirs) {
        this.dirs = dirs;
        return this;
    }

    /**
     * Set an id used for caching.
     * If name starts with <tt>http://</tt> Id is not used for the cache.
     * (A URL is unique anyway.)
     * @param id the id for the cached image
     * @return the current object, for convenience
     */
    public ImageProvider setId(String id) {
        this.id = id;
        return this;
    }

    /**
     * Specify a zip file where the image is located.
     *
     * (optional)
     * @param archive zip file where the image is located
     * @return the current object, for convenience
     */
    public ImageProvider setArchive(File archive) {
        this.archive = archive;
        return this;
    }

    /**
     * Specify a base path inside the zip file.
     *
     * The subdir and name will be relative to this path.
     *
     * (optional)
     * @param inArchiveDir path inside the archive
     * @return the current object, for convenience
     */
    public ImageProvider setInArchiveDir(String inArchiveDir) {
        this.inArchiveDir = inArchiveDir;
        return this;
    }

    /**
     * Add an overlay over the image. Multiple overlays are possible.
     *
     * @param overlay overlay image and placement specification
     * @return the current object, for convenience
     * @since 8095
     */
    public ImageProvider addOverlay(ImageOverlay overlay) {
        if (overlayInfo == null) {
            overlayInfo = new LinkedList<ImageOverlay>();
        }
        overlayInfo.add(overlay);
        return this;
    }

    /**
     * Convert enumerated size values to real numbers
     * @param size the size enumeration
     * @return dimension of image in pixels
     * @since 7687
     */
    public static Dimension getImageSizes(ImageSizes size) {
        int sizeval;
        switch(size) {
        case MAPMAX: sizeval = Main.pref.getInteger("iconsize.mapmax", 48); break;
        case MAP: sizeval = Main.pref.getInteger("iconsize.mapmax", 16); break;
        case POPUPMENU: /* POPUPMENU is LARGELICON - only provided in case of future changes */
        case LARGEICON: sizeval = Main.pref.getInteger("iconsize.largeicon", 24); break;
        case MENU: /* MENU is SMALLICON - only provided in case of future changes */
        case SMALLICON: sizeval = Main.pref.getInteger("iconsize.smallicon", 16); break;
        case CURSOROVERLAY: /* same as cursor - only provided in case of future changes */
        case CURSOR: sizeval = Main.pref.getInteger("iconsize.cursor", 32); break;
        case LAYER: sizeval = Main.pref.getInteger("iconsize.layer", 16); break;
        default: sizeval = Main.pref.getInteger("iconsize.default", 24); break;
        }
        return new Dimension(sizeval, sizeval);
    }

    /**
     * Set the dimensions of the image.
     *
     * If not specified, the original size of the image is used.
     * The width part of the dimension can be -1. Then it will only set the height but
     * keep the aspect ratio. (And the other way around.)
     * @param size final dimensions of the image
     * @return the current object, for convenience
     */
    public ImageProvider setSize(Dimension size) {
        this.width = size.width;
        this.height = size.height;
        return this;
    }

    /**
     * Set the dimensions of the image.
     *
     * If not specified, the original size of the image is used.
     * @param size final dimensions of the image
     * @return the current object, for convenience
     * @since 7687
     */
    public ImageProvider setSize(ImageSizes size) {
        return setSize(getImageSizes(size));
    }

    /**
     * Set image width
     * @param width final width of the image
     * @see #setSize
     * @return the current object, for convenience
     */
    public ImageProvider setWidth(int width) {
        this.width = width;
        return this;
    }

    /**
     * Set image height
     * @param height final height of the image
     * @see #setSize
     * @return the current object, for convenience
     */
    public ImageProvider setHeight(int height) {
        this.height = height;
        return this;
    }

    /**
     * Limit the maximum size of the image.
     *
     * It will shrink the image if necessary, but keep the aspect ratio.
     * The given width or height can be -1 which means this direction is not bounded.
     *
     * 'size' and 'maxSize' are not compatible, you should set only one of them.
     * @param maxSize maximum image size
     * @return the current object, for convenience
     */
    public ImageProvider setMaxSize(Dimension maxSize) {
        this.maxWidth = maxSize.width;
        this.maxHeight = maxSize.height;
        return this;
    }

    /**
     * Limit the maximum size of the image.
     *
     * It will shrink the image if necessary, but keep the aspect ratio.
     * The given width or height can be -1 which means this direction is not bounded.
     *
     * This function sets value using the most restrictive of the new or existing set of
     * values.
     *
     * @param maxSize maximum image size
     * @return the current object, for convenience
     * @see #setMaxSize(Dimension)
     */
    public ImageProvider resetMaxSize(Dimension maxSize) {
        if (this.maxWidth == -1 || maxSize.width < this.maxWidth) {
            this.maxWidth = maxSize.width;
        }
        if (this.maxHeight == -1 || maxSize.height < this.maxHeight) {
            this.maxHeight = maxSize.height;
        }
        return this;
    }

    /**
     * Limit the maximum size of the image.
     *
     * It will shrink the image if necessary, but keep the aspect ratio.
     * The given width or height can be -1 which means this direction is not bounded.
     *
     * 'size' and 'maxSize' are not compatible, you should set only one of them.
     * @param size maximum image size
     * @return the current object, for convenience
     * @since 7687
     */
    public ImageProvider setMaxSize(ImageSizes size) {
        return setMaxSize(getImageSizes(size));
    }

    /**
     * Convenience method, see {@link #setMaxSize(Dimension)}.
     * @param maxSize maximum image size
     * @return the current object, for convenience
     */
    public ImageProvider setMaxSize(int maxSize) {
        return this.setMaxSize(new Dimension(maxSize, maxSize));
    }

    /**
     * Limit the maximum width of the image.
     * @param maxWidth maximum image width
     * @return the current object, for convenience
     * @see #setMaxSize
     */
    public ImageProvider setMaxWidth(int maxWidth) {
        this.maxWidth = maxWidth;
        return this;
    }

    /**
     * Limit the maximum height of the image.
     * @param maxHeight maximum image height
     * @return the current object, for convenience
     * @see #setMaxSize
     */
    public ImageProvider setMaxHeight(int maxHeight) {
        this.maxHeight = maxHeight;
        return this;
    }

    /**
     * Decide, if an exception should be thrown, when the image cannot be located.
     *
     * Set to true, when the image URL comes from user data and the image may be missing.
     *
     * @param optional true, if JOSM should <b>not</b> throw a RuntimeException
     * in case the image cannot be located.
     * @return the current object, for convenience
     */
    public ImageProvider setOptional(boolean optional) {
        this.optional = optional;
        return this;
    }

    /**
     * Suppresses warning on the command line in case the image cannot be found.
     *
     * In combination with setOptional(true);
     * @param suppressWarnings if <code>true</code> warnings are suppressed
     * @return the current object, for convenience
     */
    public ImageProvider setSuppressWarnings(boolean suppressWarnings) {
        this.suppressWarnings = suppressWarnings;
        return this;
    }

    /**
     * Add a collection of additional class loaders to search image for.
     * @param additionalClassLoaders class loaders to add to the internal list
     * @return the current object, for convenience
     */
    public ImageProvider setAdditionalClassLoaders(Collection<ClassLoader> additionalClassLoaders) {
        this.additionalClassLoaders = additionalClassLoaders;
        return this;
    }

    /**
     * Execute the image request and scale result.
     * @return the requested image or null if the request failed
     */
    public ImageIcon get() {
        ImageResource ir = getResource();
        if (ir == null)
            return null;
        if (maxWidth != -1 || maxHeight != -1)
            return ir.getImageIconBounded(new Dimension(maxWidth, maxHeight));
        else
            return ir.getImageIcon(new Dimension(width, height));
    }

    /**
     * Execute the image request.
     *
     * @return the requested image or null if the request failed
     * @since 7693
     */
    public ImageResource getResource() {
        ImageResource ir = getIfAvailableImpl(additionalClassLoaders);
        if (ir == null) {
            if (!optional) {
                String ext = name.indexOf('.') != -1 ? "" : ".???";
                throw new RuntimeException(tr("Fatal: failed to locate image ''{0}''. This is a serious configuration problem. JOSM will stop working.", name + ext));
            } else {
                if (!suppressWarnings) {
                    Main.error(tr("Failed to locate image ''{0}''", name));
                }
                return null;
            }
        }
        if (overlayInfo != null) {
            ir = new ImageResource(ir, overlayInfo);
        }
        return ir;
    }

    /**
     * Load the image in a background thread.
     *
     * This method returns immediately and runs the image request
     * asynchronously.
     *
     * @param callback a callback. It is called, when the image is ready.
     * This can happen before the call to this method returns or it may be
     * invoked some time (seconds) later. If no image is available, a null
     * value is returned to callback (just like {@link #get}).
     */
    public void getInBackground(final ImageCallback callback) {
        if (name.startsWith(HTTP_PROTOCOL) || name.startsWith(WIKI_PROTOCOL)) {
            Runnable fetch = new Runnable() {
                @Override
                public void run() {
                    ImageIcon result = get();
                    callback.finished(result);
                }
            };
            IMAGE_FETCHER.submit(fetch);
        } else {
            ImageIcon result = get();
            callback.finished(result);
        }
    }

    /**
     * Load the image in a background thread.
     *
     * This method returns immediately and runs the image request
     * asynchronously.
     *
     * @param callback a callback. It is called, when the image is ready.
     * This can happen before the call to this method returns or it may be
     * invoked some time (seconds) later. If no image is available, a null
     * value is returned to callback (just like {@link #get}).
     * @since 7693
     */
    public void getInBackground(final ImageResourceCallback callback) {
        if (name.startsWith(HTTP_PROTOCOL) || name.startsWith(WIKI_PROTOCOL)) {
            Runnable fetch = new Runnable() {
                @Override
                public void run() {
                    callback.finished(getResource());
                }
            };
            IMAGE_FETCHER.submit(fetch);
        } else {
            callback.finished(getResource());
        }
    }

    /**
     * Load an image with a given file name.
     *
     * @param subdir subdirectory the image lies in
     * @param name The icon name (base name with or without '.png' or '.svg' extension)
     * @return The requested Image.
     * @throws RuntimeException if the image cannot be located
     */
    public static ImageIcon get(String subdir, String name) {
        return new ImageProvider(subdir, name).get();
    }

    /**
     * Load an image with a given file name.
     *
     * @param name The icon name (base name with or without '.png' or '.svg' extension)
     * @return the requested image or null if the request failed
     * @see #get(String, String)
     */
    public static ImageIcon get(String name) {
        return new ImageProvider(name).get();
    }

    /**
     * Load an image with a given file name, but do not throw an exception
     * when the image cannot be found.
     *
     * @param subdir subdirectory the image lies in
     * @param name The icon name (base name with or without '.png' or '.svg' extension)
     * @return the requested image or null if the request failed
     * @see #get(String, String)
     */
    public static ImageIcon getIfAvailable(String subdir, String name) {
        return new ImageProvider(subdir, name).setOptional(true).get();
    }

    /**
     * Load an image with a given file name, but do not throw an exception
     * when the image cannot be found.
     *
     * @param name The icon name (base name with or without '.png' or '.svg' extension)
     * @return the requested image or null if the request failed
     * @see #getIfAvailable(String, String)
     */
    public static ImageIcon getIfAvailable(String name) {
        return new ImageProvider(name).setOptional(true).get();
    }

    /**
     * {@code data:[<mediatype>][;base64],<data>}
     * @see <a href="http://tools.ietf.org/html/rfc2397">RFC2397</a>
     */
    private static final Pattern dataUrlPattern = Pattern.compile(
            "^data:([a-zA-Z]+/[a-zA-Z+]+)?(;base64)?,(.+)$");

    /**
     * Internal implementation of the image request.
     *
     * @param additionalClassLoaders the list of class loaders to use
     * @return the requested image or null if the request failed
     */
    private ImageResource getIfAvailableImpl(Collection<ClassLoader> additionalClassLoaders) {
        synchronized (cache) {
            // This method is called from different thread and modifying HashMap concurrently can result
            // for example in loops in map entries (ie freeze when such entry is retrieved)
            // Yes, it did happen to me :-)
            if (name == null)
                return null;

            if (name.startsWith("data:")) {
                String url = name;
                ImageResource ir = cache.get(url);
                if (ir != null) return ir;
                ir = getIfAvailableDataUrl(url);
                if (ir != null) {
                    cache.put(url, ir);
                }
                return ir;
            }

            ImageType type = Utils.hasExtension(name, "svg") ? ImageType.SVG : ImageType.OTHER;

            if (name.startsWith(HTTP_PROTOCOL) || name.startsWith(HTTPS_PROTOCOL)) {
                String url = name;
                ImageResource ir = cache.get(url);
                if (ir != null) return ir;
                ir = getIfAvailableHttp(url, type);
                if (ir != null) {
                    cache.put(url, ir);
                }
                return ir;
            } else if (name.startsWith(WIKI_PROTOCOL)) {
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
            } else if (!subdir.isEmpty() && !subdir.endsWith("/")) {
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

                    String fullName = subdir + name + ext;
                    String cacheName = fullName;
                    /* cache separately */
                    if (dirs != null && !dirs.isEmpty()) {
                        cacheName = "id:" + id + ":" + fullName;
                        if(archive != null) {
                            cacheName += ":" + archive.getName();
                        }
                    }

                    ImageResource ir = cache.get(cacheName);
                    if (ir != null) return ir;

                    switch (place) {
                    case ARCHIVE:
                        if (archive != null) {
                            ir = getIfAvailableZip(fullName, archive, inArchiveDir, type);
                            if (ir != null) {
                                cache.put(cacheName, ir);
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
                        URL path = getImageUrl(fullName, dirs, additionalClassLoaders);
                        if (path == null) {
                            continue;
                        }
                        ir = getIfAvailableLocalURL(path, type);
                        if (ir != null) {
                            cache.put(cacheName, ir);
                            return ir;
                        }
                        break;
                    }
                }
            }
            return null;
        }
    }

    /**
     * Internal implementation of the image request for URL's.
     *
     * @param url URL of the image
     * @param type data type of the image
     * @return the requested image or null if the request failed
     */
    private static ImageResource getIfAvailableHttp(String url, ImageType type) {
        CachedFile cf = new CachedFile(url)
                .setDestDir(new File(Main.pref.getCacheDirectory(), "images").getPath());
        try (InputStream is = cf.getInputStream()) {
            switch (type) {
            case SVG:
                SVGDiagram svg = null;
                synchronized (getSvgUniverse()) {
                    URI uri = getSvgUniverse().loadSVG(is, Utils.fileToURL(cf.getFile()).toString());
                    svg = getSvgUniverse().getDiagram(uri);
                }
                return svg == null ? null : new ImageResource(svg);
            case OTHER:
                BufferedImage img = null;
                try {
                    img = read(Utils.fileToURL(cf.getFile()), false, false);
                } catch (IOException e) {
                    Main.warn("IOException while reading HTTP image: "+e.getMessage());
                }
                return img == null ? null : new ImageResource(img);
            default:
                throw new AssertionError();
            }
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Internal implementation of the image request for inline images (<b>data:</b> urls).
     *
     * @param url the data URL for image extraction
     * @return the requested image or null if the request failed
     */
    private static ImageResource getIfAvailableDataUrl(String url) {
        Matcher m = dataUrlPattern.matcher(url);
        if (m.matches()) {
            String mediatype = m.group(1);
            String base64 = m.group(2);
            String data = m.group(3);
            byte[] bytes;
            if (";base64".equals(base64)) {
                bytes = DatatypeConverter.parseBase64Binary(data);
            } else {
                try {
                    bytes = Utils.decodeUrl(data).getBytes(StandardCharsets.UTF_8);
                } catch (IllegalArgumentException ex) {
                    Main.warn("Unable to decode URL data part: "+ex.getMessage() + " (" + data + ")");
                    return null;
                }
            }
            if ("image/svg+xml".equals(mediatype)) {
                String s = new String(bytes, StandardCharsets.UTF_8);
                SVGDiagram svg = null;
                synchronized (getSvgUniverse()) {
                    URI uri = getSvgUniverse().loadSVG(new StringReader(s), Utils.encodeUrl(s));
                    svg = getSvgUniverse().getDiagram(uri);
                }
                if (svg == null) {
                    Main.warn("Unable to process svg: "+s);
                    return null;
                }
                return new ImageResource(svg);
            } else {
                try {
                    // See #10479: for PNG files, always enforce transparency to be sure tNRS chunk is used even not in paletted mode
                    // This can be removed if someday Oracle fixes https://bugs.openjdk.java.net/browse/JDK-6788458
                    // hg.openjdk.java.net/jdk7u/jdk7u/jdk/file/828c4fedd29f/src/share/classes/com/sun/imageio/plugins/png/PNGImageReader.java#l656
                    Image img = read(new ByteArrayInputStream(bytes), false, true);
                    return img == null ? null : new ImageResource(img);
                } catch (IOException e) {
                    Main.warn("IOException while reading image: "+e.getMessage());
                }
            }
        }
        return null;
    }

    /**
     * Internal implementation of the image request for wiki images.
     *
     * @param name image file name
     * @param type data type of the image
     * @return the requested image or null if the request failed
     */
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

    /**
     * Internal implementation of the image request for images in Zip archives.
     *
     * @param fullName image file name
     * @param archive the archive to get image from
     * @param inArchiveDir directory of the image inside the archive or <code>null</code>
     * @param type data type of the image
     * @return the requested image or null if the request failed
     */
    private static ImageResource getIfAvailableZip(String fullName, File archive, String inArchiveDir, ImageType type) {
        try (ZipFile zipFile = new ZipFile(archive, StandardCharsets.UTF_8)) {
            if (inArchiveDir == null || ".".equals(inArchiveDir)) {
                inArchiveDir = "";
            } else if (!inArchiveDir.isEmpty()) {
                inArchiveDir += "/";
            }
            String entryName = inArchiveDir + fullName;
            ZipEntry entry = zipFile.getEntry(entryName);
            if (entry != null) {
                int size = (int)entry.getSize();
                int offs = 0;
                byte[] buf = new byte[size];
                try (InputStream is = zipFile.getInputStream(entry)) {
                    switch (type) {
                    case SVG:
                        SVGDiagram svg = null;
                        synchronized (getSvgUniverse()) {
                            URI uri = getSvgUniverse().loadSVG(is, entryName);
                            svg = getSvgUniverse().getDiagram(uri);
                        }
                        return svg == null ? null : new ImageResource(svg);
                    case OTHER:
                        while(size > 0) {
                            int l = is.read(buf, offs, size);
                            offs += l;
                            size -= l;
                        }
                        BufferedImage img = null;
                        try {
                            img = read(new ByteArrayInputStream(buf), false, false);
                        } catch (IOException e) {
                            Main.warn(e);
                        }
                        return img == null ? null : new ImageResource(img);
                    default:
                        throw new AssertionError("Unknown ImageType: "+type);
                    }
                }
            }
        } catch (Exception e) {
            Main.warn(tr("Failed to handle zip file ''{0}''. Exception was: {1}", archive.getName(), e.toString()));
        }
        return null;
    }

    /**
     * Internal implementation of the image request for local images.
     *
     * @param path image file path
     * @param type data type of the image
     * @return the requested image or null if the request failed
     */
    private static ImageResource getIfAvailableLocalURL(URL path, ImageType type) {
        switch (type) {
        case SVG:
            SVGDiagram svg = null;
            synchronized (getSvgUniverse()) {
                URI uri = getSvgUniverse().loadSVG(path);
                svg = getSvgUniverse().getDiagram(uri);
            }
            return svg == null ? null : new ImageResource(svg);
        case OTHER:
            BufferedImage img = null;
            try {
                // See #10479: for PNG files, always enforce transparency to be sure tNRS chunk is used even not in paletted mode
                // This can be removed if someday Oracle fixes https://bugs.openjdk.java.net/browse/JDK-6788458
                // hg.openjdk.java.net/jdk7u/jdk7u/jdk/file/828c4fedd29f/src/share/classes/com/sun/imageio/plugins/png/PNGImageReader.java#l656
                img = read(path, false, true);
                if (Main.isDebugEnabled() && isTransparencyForced(img)) {
                    Main.debug("Transparency has been forced for image "+path.toExternalForm());
                }
            } catch (IOException e) {
                Main.warn(e);
            }
            return img == null ? null : new ImageResource(img);
        default:
            throw new AssertionError();
        }
    }

    private static URL getImageUrl(String path, String name, Collection<ClassLoader> additionalClassLoaders) {
        if (path != null && path.startsWith("resource://")) {
            String p = path.substring("resource://".length());
            Collection<ClassLoader> classLoaders = new ArrayList<>(PluginHandler.getResourceClassLoaders());
            if (additionalClassLoaders != null) {
                classLoaders.addAll(additionalClassLoaders);
            }
            for (ClassLoader source : classLoaders) {
                URL res;
                if ((res = source.getResource(p + name)) != null)
                    return res;
            }
        } else {
            File f = new File(path, name);
            if ((path != null || f.isAbsolute()) && f.exists())
                return Utils.fileToURL(f);
        }
        return null;
    }

    private static URL getImageUrl(String imageName, Collection<String> dirs, Collection<ClassLoader> additionalClassLoaders) {
        URL u = null;

        // Try passed directories first
        if (dirs != null) {
            for (String name : dirs) {
                try {
                    u = getImageUrl(name, imageName, additionalClassLoaders);
                    if (u != null)
                        return u;
                } catch (SecurityException e) {
                    Main.warn(tr(
                            "Failed to access directory ''{0}'' for security reasons. Exception was: {1}",
                            name, e.toString()));
                }

            }
        }
        // Try user-data directory
        String dir = new File(Main.pref.getUserDataDirectory(), "images").getAbsolutePath();
        try {
            u = getImageUrl(dir, imageName, additionalClassLoaders);
            if (u != null)
                return u;
        } catch (SecurityException e) {
            Main.warn(tr(
                    "Failed to access directory ''{0}'' for security reasons. Exception was: {1}", dir, e
                    .toString()));
        }

        // Absolute path?
        u = getImageUrl(null, imageName, additionalClassLoaders);
        if (u != null)
            return u;

        // Try plugins and josm classloader
        u = getImageUrl("resource://images/", imageName, additionalClassLoaders);
        if (u != null)
            return u;

        // Try all other resource directories
        for (String location : Main.pref.getAllPossiblePreferenceDirs()) {
            u = getImageUrl(location + "images", imageName, additionalClassLoaders);
            if (u != null)
                return u;
            u = getImageUrl(location, imageName, additionalClassLoaders);
            if (u != null)
                return u;
        }

        return null;
    }

    /** Quit parsing, when a certain condition is met */
    private static class SAXReturnException extends SAXException {
        private final String result;

        public SAXReturnException(String result) {
            this.result = result;
        }

        public String getResult() {
            return result;
        }
    }

    /**
     * Reads the wiki page on a certain file in html format in order to find the real image URL.
     *
     * @param base base URL for Wiki image
     * @param fn filename of the Wiki image
     * @return image URL for a Wiki image or null in case of error
     */
    private static String getImgUrlFromWikiInfoPage(final String base, final String fn) {
        try {
            final XMLReader parser = XMLReaderFactory.createXMLReader();
            parser.setContentHandler(new DefaultHandler() {
                @Override
                public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
                    if ("img".equalsIgnoreCase(localName)) {
                        String val = atts.getValue("src");
                        if (val.endsWith(fn))
                            throw new SAXReturnException(val);  // parsing done, quit early
                    }
                }
            });

            parser.setEntityResolver(new EntityResolver() {
                @Override
                public InputSource resolveEntity(String publicId, String systemId) {
                    return new InputSource(new ByteArrayInputStream(new byte[0]));
                }
            });

            CachedFile cf = new CachedFile(base + fn).setDestDir(
                    new File(Main.pref.getUserDataDirectory(), "images").getPath());
            try (InputStream is = cf.getInputStream()) {
                parser.parse(new InputSource(is));
            }
        } catch (SAXReturnException r) {
            return r.getResult();
        } catch (Exception e) {
            Main.warn("Parsing " + base + fn + " failed:\n" + e);
            return null;
        }
        Main.warn("Parsing " + base + fn + " failed: Unexpected content.");
        return null;
    }

    /**
     * Load a cursor with a given file name, optionally decorated with an overlay image.
     *
     * @param name the cursor image filename in "cursor" directory
     * @param overlay optional overlay image
     * @return cursor with a given file name, optionally decorated with an overlay image
     */
    public static Cursor getCursor(String name, String overlay) {
        ImageIcon img = get("cursor", name);
        if (overlay != null) {
            img = new ImageProvider("cursor", name).setMaxSize(ImageSizes.CURSOR)
                .addOverlay(new ImageOverlay(new ImageProvider("cursor/modifier/" + overlay)
                    .setMaxSize(ImageSizes.CURSOROVERLAY))).get();
        }
        if (GraphicsEnvironment.isHeadless()) {
            Main.warn("Cursors are not available in headless mode. Returning null for '"+name+"'");
            return null;
        }
        return Toolkit.getDefaultToolkit().createCustomCursor(img.getImage(),
                "crosshair".equals(name) ? new Point(10, 10) : new Point(3, 2), "Cursor");
    }

    /** 90 degrees in radians units */
    private static final double DEGREE_90 = 90.0 * Math.PI / 180.0;

    /**
     * Creates a rotated version of the input image.
     *
     * @param img the image to be rotated.
     * @param rotatedAngle the rotated angle, in degree, clockwise. It could be any double but we
     * will mod it with 360 before using it. More over for caching performance, it will be rounded to
     * an entire value between 0 and 360.
     *
     * @return the image after rotating.
     * @since 6172
     */
    public static Image createRotatedImage(Image img, double rotatedAngle) {
        return createRotatedImage(img, rotatedAngle, ImageResource.DEFAULT_DIMENSION);
    }

    /**
     * Creates a rotated version of the input image, scaled to the given dimension.
     *
     * @param img the image to be rotated.
     * @param rotatedAngle the rotated angle, in degree, clockwise. It could be any double but we
     * will mod it with 360 before using it. More over for caching performance, it will be rounded to
     * an entire value between 0 and 360.
     * @param dimension The requested dimensions. Use (-1,-1) for the original size
     * and (width, -1) to set the width, but otherwise scale the image proportionally.
     * @return the image after rotating and scaling.
     * @since 6172
     */
    public static Image createRotatedImage(Image img, double rotatedAngle, Dimension dimension) {
        CheckParameterUtil.ensureParameterNotNull(img, "img");

        // convert rotatedAngle to an integer value from 0 to 360
        Long originalAngle = Math.round(rotatedAngle % 360);
        if (rotatedAngle != 0 && originalAngle == 0) {
            originalAngle = 360L;
        }

        ImageResource imageResource = null;

        synchronized (ROTATE_CACHE) {
            Map<Long, ImageResource> cacheByAngle = ROTATE_CACHE.get(img);
            if (cacheByAngle == null) {
                ROTATE_CACHE.put(img, cacheByAngle = new HashMap<>());
            }

            imageResource = cacheByAngle.get(originalAngle);

            if (imageResource == null) {
                // convert originalAngle to a value from 0 to 90
                double angle = originalAngle % 90;
                if (originalAngle != 0 && angle == 0) {
                    angle = 90.0;
                }

                double radian = Math.toRadians(angle);

                new ImageIcon(img); // load completely
                int iw = img.getWidth(null);
                int ih = img.getHeight(null);
                int w;
                int h;

                if ((originalAngle >= 0 && originalAngle <= 90) || (originalAngle > 180 && originalAngle <= 270)) {
                    w = (int) (iw * Math.sin(DEGREE_90 - radian) + ih * Math.sin(radian));
                    h = (int) (iw * Math.sin(radian) + ih * Math.sin(DEGREE_90 - radian));
                } else {
                    w = (int) (ih * Math.sin(DEGREE_90 - radian) + iw * Math.sin(radian));
                    h = (int) (ih * Math.sin(radian) + iw * Math.sin(DEGREE_90 - radian));
                }
                Image image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                cacheByAngle.put(originalAngle, imageResource = new ImageResource(image));
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
                g2d.drawImage(img, -cx, -cy, null);

                g2d.dispose();
                new ImageIcon(image); // load completely
            }
            return imageResource.getImageIcon(dimension).getImage();
        }
    }

    /**
     * Creates a scaled down version of the input image to fit maximum dimensions. (Keeps aspect ratio)
     *
     * @param img the image to be scaled down.
     * @param maxSize the maximum size in pixels (both for width and height)
     *
     * @return the image after scaling.
     * @since 6172
     */
    public static Image createBoundedImage(Image img, int maxSize) {
        return new ImageResource(img).getImageIconBounded(new Dimension(maxSize, maxSize)).getImage();
    }

    /**
     * Replies the icon for an OSM primitive type
     * @param type the type
     * @return the icon
     */
    public static ImageIcon get(OsmPrimitiveType type) {
        CheckParameterUtil.ensureParameterNotNull(type, "type");
        return get("data", type.getAPIName());
    }

    /**
     * Constructs an image from the given SVG data.
     * @param svg the SVG data
     * @param dim the desired image dimension
     * @return an image from the given SVG data at the desired dimension.
     */
    public static BufferedImage createImageFromSvg(SVGDiagram svg, Dimension dim) {
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
        if (width == 0 || height == 0) {
            return null;
        }
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setClip(0, 0, width, height);
        if (scaleX != null && scaleY != null) {
            g.scale(scaleX, scaleY);
        }
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        try {
            synchronized (getSvgUniverse()) {
                svg.render(g);
            }
        } catch (Exception ex) {
            Main.error("Unable to load svg: {0}", ex.getMessage());
            return null;
        }
        return img;
    }

    private static synchronized SVGUniverse getSvgUniverse() {
        if (svgUniverse == null) {
            svgUniverse = new SVGUniverse();
        }
        return svgUniverse;
    }

    /**
     * Returns a <code>BufferedImage</code> as the result of decoding
     * a supplied <code>File</code> with an <code>ImageReader</code>
     * chosen automatically from among those currently registered.
     * The <code>File</code> is wrapped in an
     * <code>ImageInputStream</code>.  If no registered
     * <code>ImageReader</code> claims to be able to read the
     * resulting stream, <code>null</code> is returned.
     *
     * <p> The current cache settings from <code>getUseCache</code>and
     * <code>getCacheDirectory</code> will be used to control caching in the
     * <code>ImageInputStream</code> that is created.
     *
     * <p> Note that there is no <code>read</code> method that takes a
     * filename as a <code>String</code>; use this method instead after
     * creating a <code>File</code> from the filename.
     *
     * <p> This method does not attempt to locate
     * <code>ImageReader</code>s that can read directly from a
     * <code>File</code>; that may be accomplished using
     * <code>IIORegistry</code> and <code>ImageReaderSpi</code>.
     *
     * @param input a <code>File</code> to read from.
     * @param readMetadata if {@code true}, makes sure to read image metadata to detect transparency color, if any.
     * In that case the color can be retrieved later through {@link #PROP_TRANSPARENCY_COLOR}.
     * Always considered {@code true} if {@code enforceTransparency} is also {@code true}
     * @param enforceTransparency if {@code true}, makes sure to read image metadata and, if the image does not
     * provide an alpha channel but defines a {@code TransparentColor} metadata node, that the resulting image
     * has a transparency set to {@code TRANSLUCENT} and uses the correct transparent color.
     *
     * @return a <code>BufferedImage</code> containing the decoded
     * contents of the input, or <code>null</code>.
     *
     * @throws IllegalArgumentException if <code>input</code> is <code>null</code>.
     * @throws IOException if an error occurs during reading.
     * @since 7132
     * @see BufferedImage#getProperty
     */
    public static BufferedImage read(File input, boolean readMetadata, boolean enforceTransparency) throws IOException {
        CheckParameterUtil.ensureParameterNotNull(input, "input");
        if (!input.canRead()) {
            throw new IIOException("Can't read input file!");
        }

        ImageInputStream stream = ImageIO.createImageInputStream(input);
        if (stream == null) {
            throw new IIOException("Can't create an ImageInputStream!");
        }
        BufferedImage bi = read(stream, readMetadata, enforceTransparency);
        if (bi == null) {
            stream.close();
        }
        return bi;
    }

    /**
     * Returns a <code>BufferedImage</code> as the result of decoding
     * a supplied <code>InputStream</code> with an <code>ImageReader</code>
     * chosen automatically from among those currently registered.
     * The <code>InputStream</code> is wrapped in an
     * <code>ImageInputStream</code>.  If no registered
     * <code>ImageReader</code> claims to be able to read the
     * resulting stream, <code>null</code> is returned.
     *
     * <p> The current cache settings from <code>getUseCache</code>and
     * <code>getCacheDirectory</code> will be used to control caching in the
     * <code>ImageInputStream</code> that is created.
     *
     * <p> This method does not attempt to locate
     * <code>ImageReader</code>s that can read directly from an
     * <code>InputStream</code>; that may be accomplished using
     * <code>IIORegistry</code> and <code>ImageReaderSpi</code>.
     *
     * <p> This method <em>does not</em> close the provided
     * <code>InputStream</code> after the read operation has completed;
     * it is the responsibility of the caller to close the stream, if desired.
     *
     * @param input an <code>InputStream</code> to read from.
     * @param readMetadata if {@code true}, makes sure to read image metadata to detect transparency color for non translucent images, if any.
     * In that case the color can be retrieved later through {@link #PROP_TRANSPARENCY_COLOR}.
     * Always considered {@code true} if {@code enforceTransparency} is also {@code true}
     * @param enforceTransparency if {@code true}, makes sure to read image metadata and, if the image does not
     * provide an alpha channel but defines a {@code TransparentColor} metadata node, that the resulting image
     * has a transparency set to {@code TRANSLUCENT} and uses the correct transparent color.
     *
     * @return a <code>BufferedImage</code> containing the decoded
     * contents of the input, or <code>null</code>.
     *
     * @throws IllegalArgumentException if <code>input</code> is <code>null</code>.
     * @throws IOException if an error occurs during reading.
     * @since 7132
     */
    public static BufferedImage read(InputStream input, boolean readMetadata, boolean enforceTransparency) throws IOException {
        CheckParameterUtil.ensureParameterNotNull(input, "input");

        ImageInputStream stream = ImageIO.createImageInputStream(input);
        BufferedImage bi = read(stream, readMetadata, enforceTransparency);
        if (bi == null) {
            stream.close();
        }
        return bi;
    }

    /**
     * Returns a <code>BufferedImage</code> as the result of decoding
     * a supplied <code>URL</code> with an <code>ImageReader</code>
     * chosen automatically from among those currently registered.  An
     * <code>InputStream</code> is obtained from the <code>URL</code>,
     * which is wrapped in an <code>ImageInputStream</code>.  If no
     * registered <code>ImageReader</code> claims to be able to read
     * the resulting stream, <code>null</code> is returned.
     *
     * <p> The current cache settings from <code>getUseCache</code>and
     * <code>getCacheDirectory</code> will be used to control caching in the
     * <code>ImageInputStream</code> that is created.
     *
     * <p> This method does not attempt to locate
     * <code>ImageReader</code>s that can read directly from a
     * <code>URL</code>; that may be accomplished using
     * <code>IIORegistry</code> and <code>ImageReaderSpi</code>.
     *
     * @param input a <code>URL</code> to read from.
     * @param readMetadata if {@code true}, makes sure to read image metadata to detect transparency color for non translucent images, if any.
     * In that case the color can be retrieved later through {@link #PROP_TRANSPARENCY_COLOR}.
     * Always considered {@code true} if {@code enforceTransparency} is also {@code true}
     * @param enforceTransparency if {@code true}, makes sure to read image metadata and, if the image does not
     * provide an alpha channel but defines a {@code TransparentColor} metadata node, that the resulting image
     * has a transparency set to {@code TRANSLUCENT} and uses the correct transparent color.
     *
     * @return a <code>BufferedImage</code> containing the decoded
     * contents of the input, or <code>null</code>.
     *
     * @throws IllegalArgumentException if <code>input</code> is <code>null</code>.
     * @throws IOException if an error occurs during reading.
     * @since 7132
     */
    public static BufferedImage read(URL input, boolean readMetadata, boolean enforceTransparency) throws IOException {
        CheckParameterUtil.ensureParameterNotNull(input, "input");

        InputStream istream = null;
        try {
            istream = input.openStream();
        } catch (IOException e) {
            throw new IIOException("Can't get input stream from URL!", e);
        }
        ImageInputStream stream = ImageIO.createImageInputStream(istream);
        BufferedImage bi;
        try {
            bi = read(stream, readMetadata, enforceTransparency);
            if (bi == null) {
                stream.close();
            }
        } finally {
            istream.close();
        }
        return bi;
    }

    /**
     * Returns a <code>BufferedImage</code> as the result of decoding
     * a supplied <code>ImageInputStream</code> with an
     * <code>ImageReader</code> chosen automatically from among those
     * currently registered.  If no registered
     * <code>ImageReader</code> claims to be able to read the stream,
     * <code>null</code> is returned.
     *
     * <p> Unlike most other methods in this class, this method <em>does</em>
     * close the provided <code>ImageInputStream</code> after the read
     * operation has completed, unless <code>null</code> is returned,
     * in which case this method <em>does not</em> close the stream.
     *
     * @param stream an <code>ImageInputStream</code> to read from.
     * @param readMetadata if {@code true}, makes sure to read image metadata to detect transparency color for non translucent images, if any.
     * In that case the color can be retrieved later through {@link #PROP_TRANSPARENCY_COLOR}.
     * Always considered {@code true} if {@code enforceTransparency} is also {@code true}
     * @param enforceTransparency if {@code true}, makes sure to read image metadata and, if the image does not
     * provide an alpha channel but defines a {@code TransparentColor} metadata node, that the resulting image
     * has a transparency set to {@code TRANSLUCENT} and uses the correct transparent color.
     *
     * @return a <code>BufferedImage</code> containing the decoded
     * contents of the input, or <code>null</code>.
     *
     * @throws IllegalArgumentException if <code>stream</code> is <code>null</code>.
     * @throws IOException if an error occurs during reading.
     * @since 7132
     */
    public static BufferedImage read(ImageInputStream stream, boolean readMetadata, boolean enforceTransparency) throws IOException {
        CheckParameterUtil.ensureParameterNotNull(stream, "stream");

        Iterator<ImageReader> iter = ImageIO.getImageReaders(stream);
        if (!iter.hasNext()) {
            return null;
        }

        ImageReader reader = iter.next();
        ImageReadParam param = reader.getDefaultReadParam();
        reader.setInput(stream, true, !readMetadata && !enforceTransparency);
        BufferedImage bi;
        try {
            bi = reader.read(0, param);
            if (bi.getTransparency() != Transparency.TRANSLUCENT && (readMetadata || enforceTransparency)) {
                Color color = getTransparentColor(bi.getColorModel(), reader);
                if (color != null) {
                    Hashtable<String, Object> properties = new Hashtable<>(1);
                    properties.put(PROP_TRANSPARENCY_COLOR, color);
                    bi = new BufferedImage(bi.getColorModel(), bi.getRaster(), bi.isAlphaPremultiplied(), properties);
                    if (enforceTransparency) {
                        if (Main.isTraceEnabled()) {
                            Main.trace("Enforcing image transparency of "+stream+" for "+color);
                        }
                        bi = makeImageTransparent(bi, color);
                    }
                }
            }
        } finally {
            reader.dispose();
            stream.close();
        }
        return bi;
    }

    /**
     * Returns the {@code TransparentColor} defined in image reader metadata.
     * @param model The image color model
     * @param reader The image reader
     * @return the {@code TransparentColor} defined in image reader metadata, or {@code null}
     * @throws IOException if an error occurs during reading
     * @see <a href="http://docs.oracle.com/javase/7/docs/api/javax/imageio/metadata/doc-files/standard_metadata.html">javax_imageio_1.0 metadata</a>
     * @since 7499
     */
    public static Color getTransparentColor(ColorModel model, ImageReader reader) throws IOException {
        try {
            IIOMetadata metadata = reader.getImageMetadata(0);
            if (metadata != null) {
                String[] formats = metadata.getMetadataFormatNames();
                if (formats != null) {
                    for (String f : formats) {
                        if ("javax_imageio_1.0".equals(f)) {
                            Node root = metadata.getAsTree(f);
                            if (root instanceof Element) {
                                NodeList list = ((Element)root).getElementsByTagName("TransparentColor");
                                if (list.getLength() > 0) {
                                    Node item = list.item(0);
                                    if (item instanceof Element) {
                                        // Handle different color spaces (tested with RGB and grayscale)
                                        String value = ((Element)item).getAttribute("value");
                                        if (!value.isEmpty()) {
                                            String[] s = value.split(" ");
                                            if (s.length == 3) {
                                                return parseRGB(s);
                                            } else if (s.length == 1) {
                                                int pixel = Integer.parseInt(s[0]);
                                                int r = model.getRed(pixel);
                                                int g = model.getGreen(pixel);
                                                int b = model.getBlue(pixel);
                                                return new Color(r,g,b);
                                            } else {
                                                Main.warn("Unable to translate TransparentColor '"+value+"' with color model "+model);
                                            }
                                        }
                                    }
                                }
                            }
                            break;
                        }
                    }
                }
            }
        } catch (IIOException | NumberFormatException e) {
            // JAI doesn't like some JPEG files with error "Inconsistent metadata read from stream" (see #10267)
            Main.warn(e);
        }
        return null;
    }

    private static Color parseRGB(String[] s) {
        int[] rgb = new int[3];
        try {
            for (int i = 0; i<3; i++) {
                rgb[i] = Integer.parseInt(s[i]);
            }
            return new Color(rgb[0], rgb[1], rgb[2]);
        } catch (IllegalArgumentException e) {
            Main.error(e);
            return null;
        }
    }

    /**
     * Returns a transparent version of the given image, based on the given transparent color.
     * @param bi The image to convert
     * @param color The transparent color
     * @return The same image as {@code bi} where all pixels of the given color are transparent.
     * This resulting image has also the special property {@link #PROP_TRANSPARENCY_FORCED} set to {@code color}
     * @see BufferedImage#getProperty
     * @see #isTransparencyForced
     * @since 7132
     */
    public static BufferedImage makeImageTransparent(BufferedImage bi, Color color) {
        // the color we are looking for. Alpha bits are set to opaque
        final int markerRGB = color.getRGB() | 0xFF000000;
        ImageFilter filter = new RGBImageFilter() {
            @Override
            public int filterRGB(int x, int y, int rgb) {
                if ((rgb | 0xFF000000) == markerRGB) {
                   // Mark the alpha bits as zero - transparent
                   return 0x00FFFFFF & rgb;
                } else {
                   return rgb;
                }
            }
        };
        ImageProducer ip = new FilteredImageSource(bi.getSource(), filter);
        Image img = Toolkit.getDefaultToolkit().createImage(ip);
        ColorModel colorModel = ColorModel.getRGBdefault();
        WritableRaster raster = colorModel.createCompatibleWritableRaster(img.getWidth(null), img.getHeight(null));
        String[] names = bi.getPropertyNames();
        Hashtable<String, Object> properties = new Hashtable<>(1 + (names != null ? names.length : 0));
        if (names != null) {
            for (String name : names) {
                properties.put(name, bi.getProperty(name));
            }
        }
        properties.put(PROP_TRANSPARENCY_FORCED, Boolean.TRUE);
        BufferedImage result = new BufferedImage(colorModel, raster, false, properties);
        Graphics2D g2 = result.createGraphics();
        g2.drawImage(img, 0, 0, null);
        g2.dispose();
        return result;
    }

    /**
     * Determines if the transparency of the given {@code BufferedImage} has been enforced by a previous call to {@link #makeImageTransparent}.
     * @param bi The {@code BufferedImage} to test
     * @return {@code true} if the transparency of {@code bi} has been enforced by a previous call to {@code makeImageTransparent}.
     * @see #makeImageTransparent
     * @since 7132
     */
    public static boolean isTransparencyForced(BufferedImage bi) {
        return bi != null && !bi.getProperty(PROP_TRANSPARENCY_FORCED).equals(Image.UndefinedProperty);
    }

    /**
     * Determines if the given {@code BufferedImage} has a transparent color determiend by a previous call to {@link #read}.
     * @param bi The {@code BufferedImage} to test
     * @return {@code true} if {@code bi} has a transparent color determined by a previous call to {@code read}.
     * @see #read
     * @since 7132
     */
    public static boolean hasTransparentColor(BufferedImage bi) {
        return bi != null && !bi.getProperty(PROP_TRANSPARENCY_COLOR).equals(Image.UndefinedProperty);
    }

    /**
     * Shutdown background image fetcher.
     * @param now if {@code true}, attempts to stop all actively executing tasks, halts the processing of waiting tasks.
     * if {@code false}, initiates an orderly shutdown in which previously submitted tasks are executed, but no new tasks will be accepted
     * @since 8412
     */
    public static void shutdown(boolean now) {
        if (now) {
            IMAGE_FETCHER.shutdownNow();
        } else {
            IMAGE_FETCHER.shutdown();
        }
    }
}
