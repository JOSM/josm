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
import java.awt.Rectangle;
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
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
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
import javax.xml.parsers.ParserConfigurationException;

import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.gui.mappaint.MapPaintStyles;
import org.openstreetmap.josm.gui.mappaint.Range;
import org.openstreetmap.josm.gui.mappaint.StyleElementList;
import org.openstreetmap.josm.gui.mappaint.styleelement.MapImage;
import org.openstreetmap.josm.gui.mappaint.styleelement.NodeElement;
import org.openstreetmap.josm.gui.mappaint.styleelement.StyleElement;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPreset;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresets;
import org.openstreetmap.josm.io.CachedFile;
import org.openstreetmap.josm.spi.preferences.Config;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import com.kitfox.svg.SVGDiagram;
import com.kitfox.svg.SVGException;
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

    // CHECKSTYLE.OFF: SingleSpaceSeparator
    private static final String HTTP_PROTOCOL  = "http://";
    private static final String HTTPS_PROTOCOL = "https://";
    private static final String WIKI_PROTOCOL  = "wiki://";
    // CHECKSTYLE.ON: SingleSpaceSeparator

    /**
     * Supported image types
     */
    public enum ImageType {
        /** Scalable vector graphics */
        SVG,
        /** Everything else, e.g. png, gif (must be supported by Java) */
        OTHER
    }

    /**
     * Supported image sizes
     * @since 7687
     */
    public enum ImageSizes {
        /** SMALL_ICON value of an Action */
        SMALLICON(Config.getPref().getInt("iconsize.smallicon", 16)),
        /** LARGE_ICON_KEY value of an Action */
        LARGEICON(Config.getPref().getInt("iconsize.largeicon", 24)),
        /** map icon */
        MAP(Config.getPref().getInt("iconsize.map", 16)),
        /** map icon maximum size */
        MAPMAX(Config.getPref().getInt("iconsize.mapmax", 48)),
        /** cursor icon size */
        CURSOR(Config.getPref().getInt("iconsize.cursor", 32)),
        /** cursor overlay icon size */
        CURSOROVERLAY(CURSOR),
        /** menu icon size */
        MENU(SMALLICON),
        /** menu icon size in popup menus
         * @since 8323
         */
        POPUPMENU(LARGEICON),
        /** Layer list icon size
         * @since 8323
         */
        LAYER(Config.getPref().getInt("iconsize.layer", 16)),
        /** Toolbar button icon size
         * @since 9253
         */
        TOOLBAR(LARGEICON),
        /** Side button maximum height
         * @since 9253
         */
        SIDEBUTTON(Config.getPref().getInt("iconsize.sidebutton", 20)),
        /** Settings tab icon size
         * @since 9253
         */
        SETTINGS_TAB(Config.getPref().getInt("iconsize.settingstab", 48)),
        /**
         * The default image size
         * @since 9705
         */
        DEFAULT(Config.getPref().getInt("iconsize.default", 24)),
        /**
         * Splash dialog logo size
         * @since 10358
         */
        SPLASH_LOGO(128, 128),
        /**
         * About dialog logo size
         * @since 10358
         */
        ABOUT_LOGO(256, 256),
        /**
         * Status line logo size
         * @since 13369
         */
        STATUSLINE(18, 18);

        private final int virtualWidth;
        private final int virtualHeight;

        ImageSizes(int imageSize) {
            this.virtualWidth = imageSize;
            this.virtualHeight = imageSize;
        }

        ImageSizes(int width, int height) {
            this.virtualWidth = width;
            this.virtualHeight = height;
        }

        ImageSizes(ImageSizes that) {
            this.virtualWidth = that.virtualWidth;
            this.virtualHeight = that.virtualHeight;
        }

        /**
         * Returns the image width in virtual pixels
         * @return the image width in virtual pixels
         * @since 9705
         */
        public int getVirtualWidth() {
            return virtualWidth;
        }

        /**
         * Returns the image height in virtual pixels
         * @return the image height in virtual pixels
         * @since 9705
         */
        public int getVirtualHeight() {
            return virtualHeight;
        }

        /**
         * Returns the image width in pixels to use for display
         * @return the image width in pixels to use for display
         * @since 10484
         */
        public int getAdjustedWidth() {
            return GuiSizesHelper.getSizeDpiAdjusted(virtualWidth);
        }

        /**
         * Returns the image height in pixels to use for display
         * @return the image height in pixels to use for display
         * @since 10484
         */
        public int getAdjustedHeight() {
            return GuiSizesHelper.getSizeDpiAdjusted(virtualHeight);
        }

        /**
         * Returns the image size as dimension
         * @return the image size as dimension
         * @since 9705
         */
        public Dimension getImageDimension() {
            return new Dimension(virtualWidth, virtualHeight);
        }
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

    /** set of class loaders to take images from */
    private static final Set<ClassLoader> classLoaders = new HashSet<>();
    static {
        try {
            classLoaders.add(ClassLoader.getSystemClassLoader());
        } catch (SecurityException e) {
            Logging.log(Logging.LEVEL_ERROR, "Unable to get system classloader", e);
        }
        try {
            classLoaders.add(ImageProvider.class.getClassLoader());
        } catch (SecurityException e) {
            Logging.log(Logging.LEVEL_ERROR, "Unable to get application classloader", e);
        }
    }

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
    /** virtual width of the resulting image, -1 when original image data should be used */
    protected int virtualWidth = -1;
    /** virtual height of the resulting image, -1 when original image data should be used */
    protected int virtualHeight = -1;
    /** virtual maximum width of the resulting image, -1 for no restriction */
    protected int virtualMaxWidth = -1;
    /** virtual maximum height of the resulting image, -1 for no restriction */
    protected int virtualMaxHeight = -1;
    /** In case of errors do not throw exception but return <code>null</code> for missing image */
    protected boolean optional;
    /** <code>true</code> if warnings should be suppressed */
    protected boolean suppressWarnings;
    /** ordered list of overlay images */
    protected List<ImageOverlay> overlayInfo;
    /** <code>true</code> if icon must be grayed out */
    protected boolean isDisabled;
    /** <code>true</code> if multi-resolution image is requested */
    protected boolean multiResolution = true;

    private static SVGUniverse svgUniverse;

    /**
     * The icon cache
     */
    private static final Map<String, ImageResource> cache = new HashMap<>();

    /**
     * Caches the image data for rotated versions of the same image.
     */
    private static final Map<Image, Map<Long, Image>> ROTATE_CACHE = new HashMap<>();

    /** small cache of critical images used in many parts of the application */
    private static final Map<OsmPrimitiveType, ImageIcon> osmPrimitiveTypeCache = new HashMap<>();

    /** larger cache of critical padded image icons used in many parts of the application */
    private static final Map<Dimension, Map<MapImage, ImageIcon>> paddedImageCache = new HashMap<>();

    private static final ExecutorService IMAGE_FETCHER =
            Executors.newSingleThreadExecutor(Utils.newThreadFactory("image-fetcher-%d", Thread.NORM_PRIORITY));

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
        this.virtualWidth = image.virtualWidth;
        this.virtualHeight = image.virtualHeight;
        this.virtualMaxWidth = image.virtualMaxWidth;
        this.virtualMaxHeight = image.virtualMaxHeight;
        this.optional = image.optional;
        this.suppressWarnings = image.suppressWarnings;
        this.overlayInfo = image.overlayInfo;
        this.isDisabled = image.isDisabled;
        this.multiResolution = image.multiResolution;
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
     * If name starts with <code>http://</code> Id is not used for the cache.
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
            overlayInfo = new LinkedList<>();
        }
        overlayInfo.add(overlay);
        return this;
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
        this.virtualWidth = size.width;
        this.virtualHeight = size.height;
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
        return setSize(size.getImageDimension());
    }

    /**
     * Set the dimensions of the image.
     *
     * @param width final width of the image
     * @param height final height of the image
     * @return the current object, for convenience
     * @since 10358
     */
    public ImageProvider setSize(int width, int height) {
        this.virtualWidth = width;
        this.virtualHeight = height;
        return this;
    }

    /**
     * Set image width
     * @param width final width of the image
     * @return the current object, for convenience
     * @see #setSize
     */
    public ImageProvider setWidth(int width) {
        this.virtualWidth = width;
        return this;
    }

    /**
     * Set image height
     * @param height final height of the image
     * @return the current object, for convenience
     * @see #setSize
     */
    public ImageProvider setHeight(int height) {
        this.virtualHeight = height;
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
        this.virtualMaxWidth = maxSize.width;
        this.virtualMaxHeight = maxSize.height;
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
        if (this.virtualMaxWidth == -1 || maxSize.width < this.virtualMaxWidth) {
            this.virtualMaxWidth = maxSize.width;
        }
        if (this.virtualMaxHeight == -1 || maxSize.height < this.virtualMaxHeight) {
            this.virtualMaxHeight = maxSize.height;
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
        return setMaxSize(size.getImageDimension());
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
        this.virtualMaxWidth = maxWidth;
        return this;
    }

    /**
     * Limit the maximum height of the image.
     * @param maxHeight maximum image height
     * @return the current object, for convenience
     * @see #setMaxSize
     */
    public ImageProvider setMaxHeight(int maxHeight) {
        this.virtualMaxHeight = maxHeight;
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
     * Add an additional class loader to search image for.
     * @param additionalClassLoader class loader to add to the internal set
     * @return {@code true} if the set changed as a result of the call
     * @since 12870
     */
    public static boolean addAdditionalClassLoader(ClassLoader additionalClassLoader) {
        return classLoaders.add(additionalClassLoader);
    }

    /**
     * Add a collection of additional class loaders to search image for.
     * @param additionalClassLoaders class loaders to add to the internal set
     * @return {@code true} if the set changed as a result of the call
     * @since 12870
     */
    public static boolean addAdditionalClassLoaders(Collection<ClassLoader> additionalClassLoaders) {
        return classLoaders.addAll(additionalClassLoaders);
    }

    /**
     * Set, if image must be filtered to grayscale so it will look like disabled icon.
     *
     * @param disabled true, if image must be grayed out for disabled state
     * @return the current object, for convenience
     * @since 10428
     */
    public ImageProvider setDisabled(boolean disabled) {
        this.isDisabled = disabled;
        return this;
    }

    /**
     * Decide, if multi-resolution image is requested (default <code>true</code>).
     * <p>
     * A <code>java.awt.image.MultiResolutionImage</code> is a Java 9 {@link Image}
     * implementation, which adds support for HiDPI displays. The effect will be
     * that in HiDPI mode, when GUI elements are scaled by a factor 1.5, 2.0, etc.,
     * the images are not just up-scaled, but a higher resolution version of the image is rendered instead.
     * <p>
     * Use {@link HiDPISupport#getBaseImage(java.awt.Image)} to extract the original image from a multi-resolution image.
     * <p>
     * See {@link HiDPISupport#processMRImage} for how to process the image without removing the multi-resolution magic.
     * @param multiResolution true, if multi-resolution image is requested
     * @return the current object, for convenience
     */
    public ImageProvider setMultiResolution(boolean multiResolution) {
        this.multiResolution = multiResolution;
        return this;
    }

    /**
     * Determines if this icon is located on a remote location (http, https, wiki).
     * @return {@code true} if this icon is located on a remote location (http, https, wiki)
     * @since 13250
     */
    public boolean isRemote() {
        return name.startsWith(HTTP_PROTOCOL) || name.startsWith(HTTPS_PROTOCOL) || name.startsWith(WIKI_PROTOCOL);
    }

    /**
     * Execute the image request and scale result.
     * @return the requested image or null if the request failed
     */
    public ImageIcon get() {
        ImageResource ir = getResource();

        if (ir == null) {
            return null;
        } else if (Logging.isTraceEnabled()) {
            Logging.trace("get {0} from {1}", this, Thread.currentThread());
        }
        if (virtualMaxWidth != -1 || virtualMaxHeight != -1)
            return ir.getImageIconBounded(new Dimension(virtualMaxWidth, virtualMaxHeight), multiResolution);
        else
            return ir.getImageIcon(new Dimension(virtualWidth, virtualHeight), multiResolution);
    }

    /**
     * Load the image in a background thread.
     *
     * This method returns immediately and runs the image request asynchronously.
     * @param action the action that will deal with the image
     *
     * @return the future of the requested image
     * @since 13252
     */
    public CompletableFuture<Void> getAsync(Consumer<? super ImageIcon> action) {
        return isRemote()
                ? CompletableFuture.supplyAsync(this::get, IMAGE_FETCHER).thenAcceptAsync(action, IMAGE_FETCHER)
                : CompletableFuture.completedFuture(get()).thenAccept(action);
    }

    /**
     * Execute the image request.
     *
     * @return the requested image or null if the request failed
     * @since 7693
     */
    public ImageResource getResource() {
        ImageResource ir = getIfAvailableImpl();
        if (ir == null) {
            if (!optional) {
                String ext = name.indexOf('.') != -1 ? "" : ".???";
                throw new JosmRuntimeException(
                        tr("Fatal: failed to locate image ''{0}''. This is a serious configuration problem. JOSM will stop working.",
                                name + ext));
            } else {
                if (!suppressWarnings) {
                    Logging.error(tr("Failed to locate image ''{0}''", name));
                }
                return null;
            }
        }
        if (overlayInfo != null) {
            ir = new ImageResource(ir, overlayInfo);
        }
        if (isDisabled) {
            ir.setDisabled(true);
        }
        return ir;
    }

    /**
     * Load the image in a background thread.
     *
     * This method returns immediately and runs the image request asynchronously.
     * @param action the action that will deal with the image
     *
     * @return the future of the requested image
     * @since 13252
     */
    public CompletableFuture<Void> getResourceAsync(Consumer<? super ImageResource> action) {
        return isRemote()
                ? CompletableFuture.supplyAsync(this::getResource, IMAGE_FETCHER).thenAcceptAsync(action, IMAGE_FETCHER)
                : CompletableFuture.completedFuture(getResource()).thenAccept(action);
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
     * Load an image from directory with a given file name and size.
     *
     * @param subdir subdirectory the image lies in
     * @param name The icon name (base name with or without '.png' or '.svg' extension)
     * @param size Target icon size
     * @return The requested Image.
     * @throws RuntimeException if the image cannot be located
     * @since 10428
     */
    public static ImageIcon get(String subdir, String name, ImageSizes size) {
        return new ImageProvider(subdir, name).setSize(size).get();
    }

    /**
     * Load an empty image with a given size.
     *
     * @param size Target icon size
     * @return The requested Image.
     * @since 10358
     */
    public static ImageIcon getEmpty(ImageSizes size) {
        Dimension iconRealSize = GuiSizesHelper.getDimensionDpiAdjusted(size.getImageDimension());
        return new ImageIcon(new BufferedImage(iconRealSize.width, iconRealSize.height,
            BufferedImage.TYPE_INT_ARGB));
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
     * Load an image with a given file name and size.
     *
     * @param name The icon name (base name with or without '.png' or '.svg' extension)
     * @param size Target icon size
     * @return the requested image or null if the request failed
     * @see #get(String, String)
     * @since 10428
     */
    public static ImageIcon get(String name, ImageSizes size) {
        return new ImageProvider(name).setSize(size).get();
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
     * Clears the internal image caches.
     * @since 11021
     */
    public static void clearCache() {
        synchronized (cache) {
            cache.clear();
        }
        synchronized (ROTATE_CACHE) {
            ROTATE_CACHE.clear();
        }
        synchronized (paddedImageCache) {
            paddedImageCache.clear();
        }
        synchronized (osmPrimitiveTypeCache) {
            osmPrimitiveTypeCache.clear();
        }
    }

    /**
     * Internal implementation of the image request.
     *
     * @return the requested image or null if the request failed
     */
    private ImageResource getIfAvailableImpl() {
        synchronized (cache) {
            // This method is called from different thread and modifying HashMap concurrently can result
            // for example in loops in map entries (ie freeze when such entry is retrieved)
            if (name == null)
                return null;

            String prefix = isDisabled ? "dis:" : "";
            if (name.startsWith("data:")) {
                String url = name;
                ImageResource ir = cache.get(prefix+url);
                if (ir != null) return ir;
                ir = getIfAvailableDataUrl(url);
                if (ir != null) {
                    cache.put(prefix+url, ir);
                }
                return ir;
            }

            ImageType type = Utils.hasExtension(name, "svg") ? ImageType.SVG : ImageType.OTHER;

            if (name.startsWith(HTTP_PROTOCOL) || name.startsWith(HTTPS_PROTOCOL)) {
                String url = name;
                ImageResource ir = cache.get(prefix+url);
                if (ir != null) return ir;
                ir = getIfAvailableHttp(url, type);
                if (ir != null) {
                    cache.put(prefix+url, ir);
                }
                return ir;
            } else if (name.startsWith(WIKI_PROTOCOL)) {
                ImageResource ir = cache.get(prefix+name);
                if (ir != null) return ir;
                ir = getIfAvailableWiki(name, type);
                if (ir != null) {
                    cache.put(prefix+name, ir);
                }
                return ir;
            }

            if (subdir == null) {
                subdir = "";
            } else if (!subdir.isEmpty() && !subdir.endsWith("/")) {
                subdir += '/';
            }
            String[] extensions;
            if (name.indexOf('.') != -1) {
                extensions = new String[] {""};
            } else {
                extensions = new String[] {".png", ".svg"};
            }
            final int typeArchive = 0;
            final int typeLocal = 1;
            for (int place : new Integer[] {typeArchive, typeLocal}) {
                for (String ext : extensions) {

                    if (".svg".equals(ext)) {
                        type = ImageType.SVG;
                    } else if (".png".equals(ext)) {
                        type = ImageType.OTHER;
                    }

                    String fullName = subdir + name + ext;
                    String cacheName = prefix + fullName;
                    /* cache separately */
                    if (dirs != null && !dirs.isEmpty()) {
                        cacheName = "id:" + id + ':' + fullName;
                        if (archive != null) {
                            cacheName += ':' + archive.getName();
                        }
                    }

                    switch (place) {
                    case typeArchive:
                        if (archive != null) {
                            cacheName = "zip:"+archive.hashCode()+':'+cacheName;
                            ImageResource ir = cache.get(cacheName);
                            if (ir != null) return ir;

                            ir = getIfAvailableZip(fullName, archive, inArchiveDir, type);
                            if (ir != null) {
                                cache.put(cacheName, ir);
                                return ir;
                            }
                        }
                        break;
                    case typeLocal:
                        ImageResource ir = cache.get(cacheName);
                        if (ir != null) return ir;

                        // getImageUrl() does a ton of "stat()" calls and gets expensive
                        // and redundant when you have a whole ton of objects. So,
                        // index the cache by the name of the icon we're looking for
                        // and don't bother to create a URL unless we're actually creating the image.
                        URL path = getImageUrl(fullName);
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
        try (CachedFile cf = new CachedFile(url).setDestDir(
                new File(Config.getDirs().getCacheDirectory(true), "images").getPath());
             InputStream is = cf.getInputStream()) {
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
                } catch (IOException | UnsatisfiedLinkError e) {
                    Logging.log(Logging.LEVEL_WARN, "Exception while reading HTTP image:", e);
                }
                return img == null ? null : new ImageResource(img);
            default:
                throw new AssertionError("Unsupported type: " + type);
            }
        } catch (IOException e) {
            Logging.debug(e);
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
            String base64 = m.group(2);
            String data = m.group(3);
            byte[] bytes;
            try {
                if (";base64".equals(base64)) {
                    bytes = Base64.getDecoder().decode(data);
                } else {
                    bytes = Utils.decodeUrl(data).getBytes(StandardCharsets.UTF_8);
                }
            } catch (IllegalArgumentException ex) {
                Logging.log(Logging.LEVEL_WARN, "Unable to decode URL data part: "+ex.getMessage() + " (" + data + ')', ex);
                return null;
            }
            String mediatype = m.group(1);
            if ("image/svg+xml".equals(mediatype)) {
                String s = new String(bytes, StandardCharsets.UTF_8);
                SVGDiagram svg;
                synchronized (getSvgUniverse()) {
                    URI uri = getSvgUniverse().loadSVG(new StringReader(s), Utils.encodeUrl(s));
                    svg = getSvgUniverse().getDiagram(uri);
                }
                if (svg == null) {
                    Logging.warn("Unable to process svg: "+s);
                    return null;
                }
                return new ImageResource(svg);
            } else {
                try {
                    // See #10479: for PNG files, always enforce transparency to be sure tNRS chunk is used even not in paletted mode
                    // This can be removed if someday Oracle fixes https://bugs.openjdk.java.net/browse/JDK-6788458
                    // CHECKSTYLE.OFF: LineLength
                    // hg.openjdk.java.net/jdk8u/jdk8u/jdk/file/dc4322602480/src/share/classes/com/sun/imageio/plugins/png/PNGImageReader.java#l656
                    // CHECKSTYLE.ON: LineLength
                    Image img = read(new ByteArrayInputStream(bytes), false, true);
                    return img == null ? null : new ImageResource(img);
                } catch (IOException | UnsatisfiedLinkError e) {
                    Logging.log(Logging.LEVEL_WARN, "Exception while reading image:", e);
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
        final List<String> defaultBaseUrls = Arrays.asList(
                "https://wiki.openstreetmap.org/w/images/",
                "https://upload.wikimedia.org/wikipedia/commons/",
                "https://wiki.openstreetmap.org/wiki/File:"
                );
        final Collection<String> baseUrls = Config.getPref().getList("image-provider.wiki.urls", defaultBaseUrls);

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
                final String fnMD5 = Utils.md5Hex(fn);
                url = b + fnMD5.substring(0, 1) + '/' + fnMD5.substring(0, 2) + '/' + fn;
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
                inArchiveDir += '/';
            }
            String entryName = inArchiveDir + fullName;
            ZipEntry entry = zipFile.getEntry(entryName);
            if (entry != null) {
                int size = (int) entry.getSize();
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
                        while (size > 0) {
                            int l = is.read(buf, offs, size);
                            offs += l;
                            size -= l;
                        }
                        BufferedImage img = null;
                        try {
                            img = read(new ByteArrayInputStream(buf), false, false);
                        } catch (IOException | UnsatisfiedLinkError e) {
                            Logging.warn(e);
                        }
                        return img == null ? null : new ImageResource(img);
                    default:
                        throw new AssertionError("Unknown ImageType: "+type);
                    }
                }
            }
        } catch (IOException | UnsatisfiedLinkError e) {
            Logging.log(Logging.LEVEL_WARN, tr("Failed to handle zip file ''{0}''. Exception was: {1}", archive.getName(), e.toString()), e);
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
                try {
                    URI uri = getSvgUniverse().loadSVG(path);
                    svg = getSvgUniverse().getDiagram(uri);
                } catch (SecurityException e) {
                    Logging.log(Logging.LEVEL_WARN, "Unable to read SVG", e);
                }
            }
            return svg == null ? null : new ImageResource(svg);
        case OTHER:
            BufferedImage img = null;
            try {
                // See #10479: for PNG files, always enforce transparency to be sure tNRS chunk is used even not in paletted mode
                // This can be removed if someday Oracle fixes https://bugs.openjdk.java.net/browse/JDK-6788458
                // hg.openjdk.java.net/jdk8u/jdk8u/jdk/file/dc4322602480/src/share/classes/com/sun/imageio/plugins/png/PNGImageReader.java#l656
                img = read(path, false, true);
                if (Logging.isDebugEnabled() && isTransparencyForced(img)) {
                    Logging.debug("Transparency has been forced for image {0}", path);
                }
            } catch (IOException | UnsatisfiedLinkError e) {
                Logging.log(Logging.LEVEL_WARN, "Unable to read image", e);
                Logging.debug(e);
            }
            return img == null ? null : new ImageResource(img);
        default:
            throw new AssertionError();
        }
    }

    private static URL getImageUrl(String path, String name) {
        if (path != null && path.startsWith("resource://")) {
            String p = path.substring("resource://".length());
            for (ClassLoader source : classLoaders) {
                URL res;
                if ((res = source.getResource(p + name)) != null)
                    return res;
            }
        } else {
            File f = new File(path, name);
            try {
                if ((path != null || f.isAbsolute()) && f.exists())
                    return Utils.fileToURL(f);
            } catch (SecurityException e) {
                Logging.log(Logging.LEVEL_ERROR, "Unable to access image", e);
            }
        }
        return null;
    }

    private URL getImageUrl(String imageName) {
        URL u;

        // Try passed directories first
        if (dirs != null) {
            for (String name : dirs) {
                try {
                    u = getImageUrl(name, imageName);
                    if (u != null)
                        return u;
                } catch (SecurityException e) {
                    Logging.log(Logging.LEVEL_WARN, tr(
                            "Failed to access directory ''{0}'' for security reasons. Exception was: {1}",
                            name, e.toString()), e);
                }

            }
        }
        // Try user-data directory
        if (Config.getDirs() != null) {
            File file = new File(Config.getDirs().getUserDataDirectory(false), "images");
            String dir = file.getPath();
            try {
                dir = file.getAbsolutePath();
            } catch (SecurityException e) {
                Logging.debug(e);
            }
            try {
                u = getImageUrl(dir, imageName);
                if (u != null)
                    return u;
            } catch (SecurityException e) {
                Logging.log(Logging.LEVEL_WARN, tr(
                        "Failed to access directory ''{0}'' for security reasons. Exception was: {1}", dir, e
                        .toString()), e);
            }
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
        for (String location : Preferences.getAllPossiblePreferenceDirs()) {
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
     *
     * @param base base URL for Wiki image
     * @param fn filename of the Wiki image
     * @return image URL for a Wiki image or null in case of error
     */
    private static String getImgUrlFromWikiInfoPage(final String base, final String fn) {
        try {
            final XMLReader parser = XmlUtils.newSafeSAXParser().getXMLReader();
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

            parser.setEntityResolver((publicId, systemId) -> new InputSource(new ByteArrayInputStream(new byte[0])));

            try (CachedFile cf = new CachedFile(base + fn).setDestDir(
                        new File(Config.getDirs().getUserDataDirectory(true), "images").getPath());
                 InputStream is = cf.getInputStream()) {
                parser.parse(new InputSource(is));
            }
        } catch (SAXReturnException e) {
            Logging.trace(e);
            return e.getResult();
        } catch (IOException | SAXException | ParserConfigurationException e) {
            Logging.warn("Parsing " + base + fn + " failed:\n" + e);
            return null;
        }
        Logging.warn("Parsing " + base + fn + " failed: Unexpected content.");
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
            Logging.debug("Cursors are not available in headless mode. Returning null for '{0}'", name);
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
     * Creates a rotated version of the input image.
     *
     * @param img the image to be rotated.
     * @param rotatedAngle the rotated angle, in degree, clockwise. It could be any double but we
     * will mod it with 360 before using it. More over for caching performance, it will be rounded to
     * an entire value between 0 and 360.
     * @param dimension ignored
     * @return the image after rotating and scaling.
     * @since 6172
     */
    public static Image createRotatedImage(Image img, double rotatedAngle, Dimension dimension) {
        CheckParameterUtil.ensureParameterNotNull(img, "img");

        // convert rotatedAngle to an integer value from 0 to 360
        Long angleLong = Math.round(rotatedAngle % 360);
        Long originalAngle = rotatedAngle != 0 && angleLong == 0 ? Long.valueOf(360L) : angleLong;

        synchronized (ROTATE_CACHE) {
            Map<Long, Image> cacheByAngle = ROTATE_CACHE.computeIfAbsent(img, k -> new HashMap<>());
            Image rotatedImg = cacheByAngle.get(originalAngle);

            if (rotatedImg == null) {
                // convert originalAngle to a value from 0 to 90
                double angle = originalAngle % 90;
                if (originalAngle != 0 && angle == 0) {
                    angle = 90.0;
                }
                double radian = Utils.toRadians(angle);

                rotatedImg = HiDPISupport.processMRImage(img, img0 -> {
                    new ImageIcon(img0); // load completely
                    int iw = img0.getWidth(null);
                    int ih = img0.getHeight(null);
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
                    Graphics g = image.getGraphics();
                    Graphics2D g2d = (Graphics2D) g.create();

                    // calculate the center of the icon.
                    int cx = iw / 2;
                    int cy = ih / 2;

                    // move the graphics center point to the center of the icon.
                    g2d.translate(w / 2, h / 2);

                    // rotate the graphics about the center point of the icon
                    g2d.rotate(Utils.toRadians(originalAngle));

                    g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                    g2d.drawImage(img0, -cx, -cy, null);

                    g2d.dispose();
                    new ImageIcon(image); // load completely
                    return image;
                });
                cacheByAngle.put(originalAngle, rotatedImg);
            }
            return rotatedImg;
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
     * Returns a scaled instance of the provided {@code BufferedImage}.
     * This method will use a multi-step scaling technique that provides higher quality than the usual
     * one-step technique (only useful in downscaling cases, where {@code targetWidth} or {@code targetHeight} is
     * smaller than the original dimensions, and generally only when the {@code BILINEAR} hint is specified).
     *
     * From https://community.oracle.com/docs/DOC-983611: "The Perils of Image.getScaledInstance()"
     *
     * @param img the original image to be scaled
     * @param targetWidth the desired width of the scaled instance, in pixels
     * @param targetHeight the desired height of the scaled instance, in pixels
     * @param hint one of the rendering hints that corresponds to
     * {@code RenderingHints.KEY_INTERPOLATION} (e.g.
     * {@code RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR},
     * {@code RenderingHints.VALUE_INTERPOLATION_BILINEAR},
     * {@code RenderingHints.VALUE_INTERPOLATION_BICUBIC})
     * @return a scaled version of the original {@code BufferedImage}
     * @since 13038
     */
    public static BufferedImage createScaledImage(BufferedImage img, int targetWidth, int targetHeight, Object hint) {
        int type = (img.getTransparency() == Transparency.OPAQUE) ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;
        // start with original size, then scale down in multiple passes with drawImage() until the target size is reached
        BufferedImage ret = img;
        int w = img.getWidth(null);
        int h = img.getHeight(null);
        do {
            if (w > targetWidth) {
                w /= 2;
            }
            if (w < targetWidth) {
                w = targetWidth;
            }
            if (h > targetHeight) {
                h /= 2;
            }
            if (h < targetHeight) {
                h = targetHeight;
            }
            BufferedImage tmp = new BufferedImage(w, h, type);
            Graphics2D g2 = tmp.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, hint);
            g2.drawImage(ret, 0, 0, w, h, null);
            g2.dispose();
            ret = tmp;
        } while (w != targetWidth || h != targetHeight);
        return ret;
    }

    /**
     * Replies the icon for an OSM primitive type
     * @param type the type
     * @return the icon
     */
    public static ImageIcon get(OsmPrimitiveType type) {
        CheckParameterUtil.ensureParameterNotNull(type, "type");
        synchronized (osmPrimitiveTypeCache) {
            return osmPrimitiveTypeCache.computeIfAbsent(type, t -> get("data", t.getAPIName()));
        }
    }

    /**
     * @param primitive Object for which an icon shall be fetched. The icon is chosen based on tags.
     * @param iconSize Target size of icon. Icon is padded if required.
     * @return Icon for {@code primitive} that fits in cell.
     * @since 8903
     */
    public static ImageIcon getPadded(OsmPrimitive primitive, Dimension iconSize) {
        // Check if the current styles have special icon for tagged objects.
        if (primitive.isTagged()) {
            ImageIcon icon = getTaggedPadded(primitive, iconSize);
            if (icon != null) {
                return icon;
            }
        }

        // Check if the presets have icons for nodes/relations.
        if (OsmPrimitiveType.WAY != primitive.getType()) {
            final Collection<TaggingPreset> presets = new TreeSet<>((o1, o2) -> {
                final int o1TypesSize = o1.types == null || o1.types.isEmpty() ? Integer.MAX_VALUE : o1.types.size();
                final int o2TypesSize = o2.types == null || o2.types.isEmpty() ? Integer.MAX_VALUE : o2.types.size();
                return Integer.compare(o1TypesSize, o2TypesSize);
            });
            presets.addAll(TaggingPresets.getMatchingPresets(primitive));
            for (final TaggingPreset preset : presets) {
                if (preset.getIcon() != null) {
                    return preset.getIcon();
                }
            }
        }

        // Use generic default icon.
        return ImageProvider.get(primitive.getDisplayType());
    }

    /**
     * Computes a new padded icon for the given tagged primitive, using map paint styles.
     * This is a slow operation.
     * @param primitive tagged OSM primitive
     * @param iconSize icon size in pixels
     * @return a new padded icon for the given tagged primitive, or null
     */
    private static ImageIcon getTaggedPadded(OsmPrimitive primitive, Dimension iconSize) {
        Pair<StyleElementList, Range> nodeStyles;
        DataSet ds = primitive.getDataSet();
        if (ds != null) {
            ds.getReadLock().lock();
        }
        try {
            nodeStyles = MapPaintStyles.getStyles().generateStyles(primitive, 100, false);
        } finally {
            if (ds != null) {
                ds.getReadLock().unlock();
            }
        }
        for (StyleElement style : nodeStyles.a) {
            if (style instanceof NodeElement) {
                NodeElement nodeStyle = (NodeElement) style;
                MapImage icon = nodeStyle.mapImage;
                if (icon != null) {
                    return getPaddedIcon(icon, iconSize);
                }
            }
        }
        return null;
    }

    /**
     * Returns an {@link ImageIcon} for the given map image, at the specified size.
     * Uses a cache to improve performance.
     * @param mapImage map image
     * @param iconSize size in pixels
     * @return an {@code ImageIcon} for the given map image, at the specified size
     * @see #clearCache
     * @since 14284
     */
    public static ImageIcon getPaddedIcon(MapImage mapImage, Dimension iconSize) {
        synchronized (paddedImageCache) {
            return paddedImageCache.computeIfAbsent(iconSize, x -> new HashMap<>()).computeIfAbsent(mapImage, icon -> {
                int backgroundRealWidth = GuiSizesHelper.getSizeDpiAdjusted(iconSize.width);
                int backgroundRealHeight = GuiSizesHelper.getSizeDpiAdjusted(iconSize.height);
                int iconRealWidth = icon.getWidth();
                int iconRealHeight = icon.getHeight();
                BufferedImage image = new BufferedImage(backgroundRealWidth, backgroundRealHeight, BufferedImage.TYPE_INT_ARGB);
                double scaleFactor = Math.min(
                        backgroundRealWidth / (double) iconRealWidth,
                        backgroundRealHeight / (double) iconRealHeight);
                Image iconImage = icon.getImage(false);
                Image scaledIcon;
                final int scaledWidth;
                final int scaledHeight;
                if (scaleFactor < 1) {
                    // Scale icon such that it fits on background.
                    scaledWidth = (int) (iconRealWidth * scaleFactor);
                    scaledHeight = (int) (iconRealHeight * scaleFactor);
                    scaledIcon = iconImage.getScaledInstance(scaledWidth, scaledHeight, Image.SCALE_SMOOTH);
                } else {
                    // Use original size, don't upscale.
                    scaledWidth = iconRealWidth;
                    scaledHeight = iconRealHeight;
                    scaledIcon = iconImage;
                }
                image.getGraphics().drawImage(scaledIcon,
                        (backgroundRealWidth - scaledWidth) / 2,
                        (backgroundRealHeight - scaledHeight) / 2, null);

                return new ImageIcon(image);
            });
        }
    }

    /**
     * Constructs an image from the given SVG data.
     * @param svg the SVG data
     * @param dim the desired image dimension
     * @return an image from the given SVG data at the desired dimension.
     */
    public static BufferedImage createImageFromSvg(SVGDiagram svg, Dimension dim) {
        if (Logging.isTraceEnabled()) {
            Logging.trace("createImageFromSvg: {0} {1}", svg.getXMLBase(), dim);
        }
        final float sourceWidth = svg.getWidth();
        final float sourceHeight = svg.getHeight();
        final float realWidth;
        final float realHeight;
        if (dim.width >= 0) {
            realWidth = dim.width;
            if (dim.height >= 0) {
                realHeight = dim.height;
            } else {
                realHeight = sourceHeight * realWidth / sourceWidth;
            }
        } else if (dim.height >= 0) {
            realHeight = dim.height;
            realWidth = sourceWidth * realHeight / sourceHeight;
        } else {
            realWidth = GuiSizesHelper.getSizeDpiAdjusted(sourceWidth);
            realHeight = GuiSizesHelper.getSizeDpiAdjusted(sourceHeight);
        }

        if (realWidth == 0 || realHeight == 0) {
            return null;
        }
        BufferedImage img = new BufferedImage(Math.round(realWidth), Math.round(realHeight), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setClip(0, 0, img.getWidth(), img.getHeight());
        g.scale(realWidth / sourceWidth, realHeight / sourceHeight);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        try {
            synchronized (getSvgUniverse()) {
                svg.render(g);
            }
        } catch (SVGException ex) {
            Logging.log(Logging.LEVEL_ERROR, "Unable to load svg:", ex);
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
     * @return a <code>BufferedImage</code> containing the decoded contents of the input, or <code>null</code>.
     *
     * @throws IllegalArgumentException if <code>input</code> is <code>null</code>.
     * @throws IOException if an error occurs during reading.
     * @see BufferedImage#getProperty
     * @since 7132
     */
    public static BufferedImage read(File input, boolean readMetadata, boolean enforceTransparency) throws IOException {
        CheckParameterUtil.ensureParameterNotNull(input, "input");
        if (!input.canRead()) {
            throw new IIOException("Can't read input file!");
        }

        ImageInputStream stream = createImageInputStream(input);
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
     * @return a <code>BufferedImage</code> containing the decoded contents of the input, or <code>null</code>.
     *
     * @throws IllegalArgumentException if <code>input</code> is <code>null</code>.
     * @throws IOException if an error occurs during reading.
     * @since 7132
     */
    public static BufferedImage read(InputStream input, boolean readMetadata, boolean enforceTransparency) throws IOException {
        CheckParameterUtil.ensureParameterNotNull(input, "input");

        ImageInputStream stream = createImageInputStream(input);
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
     * @return a <code>BufferedImage</code> containing the decoded contents of the input, or <code>null</code>.
     *
     * @throws IllegalArgumentException if <code>input</code> is <code>null</code>.
     * @throws IOException if an error occurs during reading.
     * @since 7132
     */
    public static BufferedImage read(URL input, boolean readMetadata, boolean enforceTransparency) throws IOException {
        CheckParameterUtil.ensureParameterNotNull(input, "input");

        try (InputStream istream = Utils.openStream(input)) {
            ImageInputStream stream = createImageInputStream(istream);
            BufferedImage bi = read(stream, readMetadata, enforceTransparency);
            if (bi == null) {
                stream.close();
            }
            return bi;
        } catch (SecurityException e) {
            throw new IOException(e);
        }
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
     * has a transparency set to {@code TRANSLUCENT} and uses the correct transparent color. For Java &lt; 11 only.
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
        BufferedImage bi = null;
        try {
            bi = reader.read(0, param);
            if (bi.getTransparency() != Transparency.TRANSLUCENT && (readMetadata || enforceTransparency) && Utils.getJavaVersion() < 11) {
                Color color = getTransparentColor(bi.getColorModel(), reader);
                if (color != null) {
                    Hashtable<String, Object> properties = new Hashtable<>(1);
                    properties.put(PROP_TRANSPARENCY_COLOR, color);
                    bi = new BufferedImage(bi.getColorModel(), bi.getRaster(), bi.isAlphaPremultiplied(), properties);
                    if (enforceTransparency) {
                        Logging.trace("Enforcing image transparency of {0} for {1}", stream, color);
                        bi = makeImageTransparent(bi, color);
                    }
                }
            }
        } catch (LinkageError e) {
            // On Windows, ComponentColorModel.getRGBComponent can fail with "UnsatisfiedLinkError: no awt in java.library.path", see #13973
            // Then it can leads to "NoClassDefFoundError: Could not initialize class sun.awt.image.ShortInterleavedRaster", see #15079
            Logging.error(e);
        } finally {
            reader.dispose();
            stream.close();
        }
        return bi;
    }

    // CHECKSTYLE.OFF: LineLength

    /**
     * Returns the {@code TransparentColor} defined in image reader metadata.
     * @param model The image color model
     * @param reader The image reader
     * @return the {@code TransparentColor} defined in image reader metadata, or {@code null}
     * @throws IOException if an error occurs during reading
     * @see <a href="http://docs.oracle.com/javase/8/docs/api/javax/imageio/metadata/doc-files/standard_metadata.html">javax_imageio_1.0 metadata</a>
     * @since 7499
     */
    public static Color getTransparentColor(ColorModel model, ImageReader reader) throws IOException {
        // CHECKSTYLE.ON: LineLength
        try {
            IIOMetadata metadata = reader.getImageMetadata(0);
            if (metadata != null) {
                String[] formats = metadata.getMetadataFormatNames();
                if (formats != null) {
                    for (String f : formats) {
                        if ("javax_imageio_1.0".equals(f)) {
                            Node root = metadata.getAsTree(f);
                            if (root instanceof Element) {
                                NodeList list = ((Element) root).getElementsByTagName("TransparentColor");
                                if (list.getLength() > 0) {
                                    Node item = list.item(0);
                                    if (item instanceof Element) {
                                        // Handle different color spaces (tested with RGB and grayscale)
                                        String value = ((Element) item).getAttribute("value");
                                        if (!value.isEmpty()) {
                                            String[] s = value.split(" ");
                                            if (s.length == 3) {
                                                return parseRGB(s);
                                            } else if (s.length == 1) {
                                                int pixel = Integer.parseInt(s[0]);
                                                int r = model.getRed(pixel);
                                                int g = model.getGreen(pixel);
                                                int b = model.getBlue(pixel);
                                                return new Color(r, g, b);
                                            } else {
                                                Logging.warn("Unable to translate TransparentColor '"+value+"' with color model "+model);
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
            Logging.warn(e);
        }
        return null;
    }

    private static Color parseRGB(String... s) {
        int[] rgb = new int[3];
        try {
            for (int i = 0; i < 3; i++) {
                rgb[i] = Integer.parseInt(s[i]);
            }
            return new Color(rgb[0], rgb[1], rgb[2]);
        } catch (IllegalArgumentException e) {
            Logging.error(e);
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
     * Determines if the given {@code BufferedImage} has a transparent color determined by a previous call to {@link #read}.
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
        try {
            if (now) {
                IMAGE_FETCHER.shutdownNow();
            } else {
                IMAGE_FETCHER.shutdown();
            }
        } catch (SecurityException ex) {
            Logging.log(Logging.LEVEL_ERROR, "Failed to shutdown background image fetcher.", ex);
        }
    }

    /**
     * Converts an {@link Image} to a {@link BufferedImage} instance.
     * @param image image to convert
     * @return a {@code BufferedImage} instance for the given {@code Image}.
     * @since 13038
     */
    public static BufferedImage toBufferedImage(Image image) {
        if (image instanceof BufferedImage) {
            return (BufferedImage) image;
        } else {
            BufferedImage buffImage = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = buffImage.createGraphics();
            g2.drawImage(image, 0, 0, null);
            g2.dispose();
            return buffImage;
        }
    }

    /**
     * Converts an {@link Rectangle} area of {@link Image} to a {@link BufferedImage} instance.
     * @param image image to convert
     * @param cropArea rectangle to crop image with
     * @return a {@code BufferedImage} instance for the cropped area of {@code Image}.
     * @since 13127
     */
    public static BufferedImage toBufferedImage(Image image, Rectangle cropArea) {
        BufferedImage buffImage = null;
        Rectangle r = new Rectangle(image.getWidth(null), image.getHeight(null));
        if (r.intersection(cropArea).equals(cropArea)) {
            buffImage = new BufferedImage(cropArea.width, cropArea.height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = buffImage.createGraphics();
            g2.drawImage(image, 0, 0, cropArea.width, cropArea.height,
                cropArea.x, cropArea.y, cropArea.x + cropArea.width, cropArea.y + cropArea.height, null);
            g2.dispose();
        }
        return buffImage;
    }

    private static ImageInputStream createImageInputStream(Object input) throws IOException {
        try {
            return ImageIO.createImageInputStream(input);
        } catch (SecurityException e) {
            if (ImageIO.getUseCache()) {
                ImageIO.setUseCache(false);
                return ImageIO.createImageInputStream(input);
            }
            throw new IOException(e);
        }
    }

    /**
     * Creates a blank icon of the given size.
     * @param size image size
     * @return a blank icon of the given size
     * @since 13984
     */
    public static ImageIcon createBlankIcon(ImageSizes size) {
        return new ImageIcon(new BufferedImage(size.getAdjustedWidth(), size.getAdjustedHeight(), BufferedImage.TYPE_INT_ARGB));
    }

    @Override
    public String toString() {
        return ("ImageProvider ["
                + (dirs != null && !dirs.isEmpty() ? "dirs=" + dirs + ", " : "") + (id != null ? "id=" + id + ", " : "")
                + (subdir != null && !subdir.isEmpty() ? "subdir=" + subdir + ", " : "") + (name != null ? "name=" + name + ", " : "")
                + (archive != null ? "archive=" + archive + ", " : "")
                + (inArchiveDir != null && !inArchiveDir.isEmpty() ? "inArchiveDir=" + inArchiveDir : "") + ']').replaceAll(", \\]", "]");
    }
}
