// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.util;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.IllegalComponentStateException;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JComponent;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.JosmRuntimeException;
import org.openstreetmap.josm.tools.Logging;

/**
 * This is a helper class for persisting the geometry of a JOSM window to the preference store
 * and for restoring it from the preference store.
 * @since 12678 (moved from {@code tools} package
 * @since 2008
 */
public class WindowGeometry {

    /** the top left point */
    private Point topLeft;
    /** the size */
    private Dimension extent;

    /**
     * Creates a window geometry from a position and dimension
     *
     * @param topLeft the top left point
     * @param extent the extent
     */
    public WindowGeometry(Point topLeft, Dimension extent) {
        this.topLeft = topLeft;
        this.extent = extent;
    }

    /**
     * Creates a window geometry from a rectangle
     *
     * @param rect the position
     */
    public WindowGeometry(Rectangle rect) {
        this(rect.getLocation(), rect.getSize());
    }

    /**
     * Creates a window geometry from the position and the size of a window.
     *
     * @param window the window
     * @throws IllegalComponentStateException if the window is not showing on the screen
     */
    public WindowGeometry(Window window) {
        this(window.getLocationOnScreen(), window.getSize());
    }

    /**
     * Creates a window geometry from the values kept in the preference store under the
     * key <code>preferenceKey</code>
     *
     * @param preferenceKey the preference key
     * @throws WindowGeometryException if no such key exist or if the preference value has
     * an illegal format
     */
    public WindowGeometry(String preferenceKey) throws WindowGeometryException {
        initFromPreferences(preferenceKey);
    }

    /**
     * Creates a window geometry from the values kept in the preference store under the
     * key <code>preferenceKey</code>. Falls back to the <code>defaultGeometry</code> if
     * something goes wrong.
     *
     * @param preferenceKey the preference key
     * @param defaultGeometry the default geometry
     *
     */
    public WindowGeometry(String preferenceKey, WindowGeometry defaultGeometry) {
        try {
            initFromPreferences(preferenceKey);
        } catch (WindowGeometryException e) {
            Logging.debug(e);
            initFromWindowGeometry(defaultGeometry);
        }
    }

    /**
     * Replies a window geometry object for a window with a specific size which is
     * centered on screen, where main window is
     *
     * @param extent  the size
     * @return the geometry object
     */
    public static WindowGeometry centerOnScreen(Dimension extent) {
        return centerOnScreen(extent, "gui.geometry");
    }

    /**
     * Replies a window geometry object for a window with a specific size which is
     * centered on screen where the corresponding window is.
     *
     * @param extent  the size
     * @param preferenceKey the key to get window size and position from, null value format
     * for whole virtual screen
     * @return the geometry object
     */
    public static WindowGeometry centerOnScreen(Dimension extent, String preferenceKey) {
        Rectangle size = preferenceKey != null ? getScreenInfo(preferenceKey) : getFullScreenInfo();
        Point topLeft = new Point(
                size.x + Math.max(0, (size.width - extent.width) /2),
                size.y + Math.max(0, (size.height - extent.height) /2)
        );
        return new WindowGeometry(topLeft, extent);
    }

    /**
     * Replies a window geometry object for a window with a specific size which is centered
     * relative to the parent window of a reference component.
     *
     * @param reference the reference component.
     * @param extent the size
     * @return the geometry object
     */
    public static WindowGeometry centerInWindow(Component reference, Dimension extent) {
        while (reference != null && !(reference instanceof Window)) {
            reference = reference.getParent();
        }
        if (reference == null)
            return new WindowGeometry(new Point(0, 0), extent);
        Window parentWindow = (Window) reference;
        Point topLeft = new Point(
                Math.max(0, (parentWindow.getSize().width - extent.width) /2),
                Math.max(0, (parentWindow.getSize().height - extent.height) /2)
        );
        topLeft.x += parentWindow.getLocation().x;
        topLeft.y += parentWindow.getLocation().y;
        return new WindowGeometry(topLeft, extent);
    }

    /**
     * Exception thrown by the WindowGeometry class if something goes wrong
     */
    public static class WindowGeometryException extends Exception {
        WindowGeometryException(String message, Throwable cause) {
            super(message, cause);
        }

        WindowGeometryException(String message) {
            super(message);
        }
    }

    /**
     * Fixes a window geometry to shift to the correct screen.
     *
     * @param window the window
     */
    public void fixScreen(Window window) {
        Rectangle oldScreen = getScreenInfo(getRectangle());
        Rectangle newScreen = getScreenInfo(new Rectangle(window.getLocationOnScreen(), window.getSize()));
        if (oldScreen.x != newScreen.x) {
            this.topLeft.x += newScreen.x - oldScreen.x;
        }
        if (oldScreen.y != newScreen.y) {
            this.topLeft.y += newScreen.y - oldScreen.y;
        }
    }

    protected int parseField(String preferenceKey, String preferenceValue, String field) throws WindowGeometryException {
        String v = "";
        try {
            Pattern p = Pattern.compile(field + "=(-?\\d+)", Pattern.CASE_INSENSITIVE);
            Matcher m = p.matcher(preferenceValue);
            if (!m.find())
                throw new WindowGeometryException(
                        tr("Preference with key ''{0}'' does not include ''{1}''. Cannot restore window geometry from preferences.",
                                preferenceKey, field));
            v = m.group(1);
            return Integer.parseInt(v);
        } catch (WindowGeometryException e) {
            throw e;
        } catch (NumberFormatException e) {
            throw new WindowGeometryException(
                    tr("Preference with key ''{0}'' does not provide an int value for ''{1}''. Got {2}. " +
                       "Cannot restore window geometry from preferences.",
                            preferenceKey, field, v), e);
        } catch (JosmRuntimeException | IllegalArgumentException | IllegalStateException e) {
            throw new WindowGeometryException(
                    tr("Failed to parse field ''{1}'' in preference with key ''{0}''. Exception was: {2}. " +
                       "Cannot restore window geometry from preferences.",
                            preferenceKey, field, e.toString()), e);
        }
    }

    protected final void initFromPreferences(String preferenceKey) throws WindowGeometryException {
        String value = Main.pref.get(preferenceKey);
        if (value.isEmpty())
            throw new WindowGeometryException(
                    tr("Preference with key ''{0}'' does not exist. Cannot restore window geometry from preferences.", preferenceKey));
        topLeft = new Point();
        extent = new Dimension();
        topLeft.x = parseField(preferenceKey, value, "x");
        topLeft.y = parseField(preferenceKey, value, "y");
        extent.width = parseField(preferenceKey, value, "width");
        extent.height = parseField(preferenceKey, value, "height");
    }

    protected final void initFromWindowGeometry(WindowGeometry other) {
        this.topLeft = other.topLeft;
        this.extent = other.extent;
    }

    /**
     * Gets the geometry of the main window
     * @param preferenceKey The preference key to use
     * @param arg The command line geometry arguments
     * @param maximize If the user requested to maximize the window
     * @return The geometry for the main window
     */
    public static WindowGeometry mainWindow(String preferenceKey, String arg, boolean maximize) {
        Rectangle screenDimension = getScreenInfo("gui.geometry");
        if (arg != null) {
            final Matcher m = Pattern.compile("(\\d+)x(\\d+)(([+-])(\\d+)([+-])(\\d+))?").matcher(arg);
            if (m.matches()) {
                int w = Integer.parseInt(m.group(1));
                int h = Integer.parseInt(m.group(2));
                int x = screenDimension.x;
                int y = screenDimension.y;
                if (m.group(3) != null) {
                    x = Integer.parseInt(m.group(5));
                    y = Integer.parseInt(m.group(7));
                    if ("-".equals(m.group(4))) {
                        x = screenDimension.x + screenDimension.width - x - w;
                    }
                    if ("-".equals(m.group(6))) {
                        y = screenDimension.y + screenDimension.height - y - h;
                    }
                }
                return new WindowGeometry(new Point(x, y), new Dimension(w, h));
            } else {
                Logging.warn(tr("Ignoring malformed geometry: {0}", arg));
            }
        }
        WindowGeometry def;
        if (maximize) {
            def = new WindowGeometry(screenDimension);
        } else {
            Point p = screenDimension.getLocation();
            p.x += (screenDimension.width-1000)/2;
            p.y += (screenDimension.height-740)/2;
            def = new WindowGeometry(p, new Dimension(1000, 740));
        }
        return new WindowGeometry(preferenceKey, def);
    }

    /**
     * Remembers a window geometry under a specific preference key
     *
     * @param preferenceKey the preference key
     */
    public void remember(String preferenceKey) {
        StringBuilder value = new StringBuilder(32);
        value.append("x=").append(topLeft.x).append(",y=").append(topLeft.y)
             .append(",width=").append(extent.width).append(",height=").append(extent.height);
        Main.pref.put(preferenceKey, value.toString());
    }

    /**
     * Replies the top left point for the geometry
     *
     * @return  the top left point for the geometry
     */
    public Point getTopLeft() {
        return topLeft;
    }

    /**
     * Replies the size specified by the geometry
     *
     * @return the size specified by the geometry
     */
    public Dimension getSize() {
        return extent;
    }

    /**
     * Replies the size and position specified by the geometry
     *
     * @return the size and position specified by the geometry
     */
    private Rectangle getRectangle() {
        return new Rectangle(topLeft, extent);
    }

    /**
     * Applies this geometry to a window. Makes sure that the window is not
     * placed outside of the coordinate range of all available screens.
     *
     * @param window the window
     */
    public void applySafe(Window window) {
        Point p = new Point(topLeft);
        Dimension size = new Dimension(extent);

        Rectangle virtualBounds = getVirtualScreenBounds();

        // Ensure window fit on screen

        if (p.x < virtualBounds.x) {
            p.x = virtualBounds.x;
        } else if (p.x > virtualBounds.x + virtualBounds.width - size.width) {
            p.x = virtualBounds.x + virtualBounds.width - size.width;
        }

        if (p.y < virtualBounds.y) {
            p.y = virtualBounds.y;
        } else if (p.y > virtualBounds.y + virtualBounds.height - size.height) {
            p.y = virtualBounds.y + virtualBounds.height - size.height;
        }

        int deltax = (p.x + size.width) - (virtualBounds.x + virtualBounds.width);
        if (deltax > 0) {
            size.width -= deltax;
        }

        int deltay = (p.y + size.height) - (virtualBounds.y + virtualBounds.height);
        if (deltay > 0) {
            size.height -= deltay;
        }

        // Ensure window does not hide taskbar

        Rectangle maxbounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();

        if (!isBugInMaximumWindowBounds(maxbounds)) {
            deltax = size.width - maxbounds.width;
            if (deltax > 0) {
                size.width -= deltax;
            }

            deltay = size.height - maxbounds.height;
            if (deltay > 0) {
                size.height -= deltay;
            }
        }
        window.setLocation(p);
        window.setSize(size);
    }

    /**
     * Determines if the bug affecting getMaximumWindowBounds() occured.
     *
     * @param maxbounds result of getMaximumWindowBounds()
     * @return {@code true} if the bug happened, {@code false otherwise}
     *
     * @see <a href="https://josm.openstreetmap.de/ticket/9699">JOSM-9699</a>
     * @see <a href="https://bugs.launchpad.net/ubuntu/+source/openjdk-7/+bug/1171563">Ubuntu-1171563</a>
     * @see <a href="http://icedtea.classpath.org/bugzilla/show_bug.cgi?id=1669">IcedTea-1669</a>
     * @see <a href="https://bugs.openjdk.java.net/browse/JDK-8034224">JDK-8034224</a>
     */
    protected static boolean isBugInMaximumWindowBounds(Rectangle maxbounds) {
        return maxbounds.width <= 0 || maxbounds.height <= 0;
    }

    /**
     * Computes the virtual bounds of graphics environment, as an union of all screen bounds.
     * @return The virtual bounds of graphics environment, as an union of all screen bounds.
     * @since 6522
     */
    public static Rectangle getVirtualScreenBounds() {
        Rectangle virtualBounds = new Rectangle();
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        if (!GraphicsEnvironment.isHeadless()) {
            for (GraphicsDevice gd : ge.getScreenDevices()) {
                if (gd.getType() == GraphicsDevice.TYPE_RASTER_SCREEN) {
                    virtualBounds = virtualBounds.union(gd.getDefaultConfiguration().getBounds());
                }
            }
        }
        return virtualBounds;
    }

    /**
     * Computes the maximum dimension for a component to fit in screen displaying {@code component}.
     * @param component The component to get current screen info from. Must not be {@code null}
     * @return the maximum dimension for a component to fit in current screen
     * @throws IllegalArgumentException if {@code component} is null
     * @since 7463
     */
    public static Dimension getMaxDimensionOnScreen(JComponent component) {
        CheckParameterUtil.ensureParameterNotNull(component, "component");
        // Compute max dimension of current screen
        Dimension result = new Dimension();
        GraphicsConfiguration gc = component.getGraphicsConfiguration();
        if (gc == null && Main.parent != null) {
            gc = Main.parent.getGraphicsConfiguration();
        }
        if (gc != null) {
            // Max displayable dimension (max screen dimension - insets)
            Rectangle bounds = gc.getBounds();
            Insets insets = component.getToolkit().getScreenInsets(gc);
            result.width = bounds.width - insets.left - insets.right;
            result.height = bounds.height - insets.top - insets.bottom;
        }
        return result;
    }

    /**
     * Find the size and position of the screen for given coordinates. Use first screen,
     * when no coordinates are stored or null is passed.
     *
     * @param preferenceKey the key to get size and position from
     * @return bounds of the screen
     */
    public static Rectangle getScreenInfo(String preferenceKey) {
        Rectangle g = new WindowGeometry(preferenceKey,
            /* default: something on screen 1 */
            new WindowGeometry(new Point(0, 0), new Dimension(10, 10))).getRectangle();
        return getScreenInfo(g);
    }

    /**
     * Find the size and position of the screen for given coordinates. Use first screen,
     * when no coordinates are stored or null is passed.
     *
     * @param g coordinates to check
     * @return bounds of the screen
     */
    private static Rectangle getScreenInfo(Rectangle g) {
        Rectangle bounds = null;
        if (!GraphicsEnvironment.isHeadless()) {
            int intersect = 0;
            for (GraphicsDevice gd : GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()) {
                if (gd.getType() == GraphicsDevice.TYPE_RASTER_SCREEN) {
                    Rectangle b = gd.getDefaultConfiguration().getBounds();
                    if (b.height > 0 && b.width / b.height >= 3) /* multiscreen with wrong definition */ {
                        b.width /= 2;
                        Rectangle is = b.intersection(g);
                        int s = is.width * is.height;
                        if (bounds == null || intersect < s) {
                            intersect = s;
                            bounds = b;
                        }
                        b = new Rectangle(b);
                        b.x += b.width;
                        is = b.intersection(g);
                        s = is.width * is.height;
                        if (intersect < s) {
                            intersect = s;
                            bounds = b;
                        }
                    } else {
                        Rectangle is = b.intersection(g);
                        int s = is.width * is.height;
                        if (bounds == null || intersect < s) {
                            intersect = s;
                            bounds = b;
                        }
                    }
                }
            }
        }
        return bounds != null ? bounds : g;
    }

    /**
     * Find the size of the full virtual screen.
     * @return size of the full virtual screen
     */
    public static Rectangle getFullScreenInfo() {
        return new Rectangle(new Point(0, 0), GuiHelper.getScreenSize());
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((extent == null) ? 0 : extent.hashCode());
        result = prime * result + ((topLeft == null) ? 0 : topLeft.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        WindowGeometry other = (WindowGeometry) obj;
        if (extent == null) {
            if (other.extent != null)
                return false;
        } else if (!extent.equals(other.extent))
            return false;
        if (topLeft == null) {
            if (other.topLeft != null)
                return false;
        } else if (!topLeft.equals(other.topLeft))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "WindowGeometry{topLeft="+topLeft+",extent="+extent+'}';
    }
}
