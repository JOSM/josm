// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openstreetmap.josm.Main;

/**
 * This is a helper class for persisting the geometry of a JOSM window to the preference store
 * and for restoring it from the preference store.
 *
 */
public class WindowGeometry {

    /**
     * Replies a window geometry object for a window with a specific size which is
     * centered on screen
     *
     * @param extent  the size
     * @return the geometry object
     */
    static public WindowGeometry centerOnScreen(Dimension extent) {
        Point topLeft = new Point(
                Math.max(0, (Toolkit.getDefaultToolkit().getScreenSize().width - extent.width) /2),
                Math.max(0, (Toolkit.getDefaultToolkit().getScreenSize().height - extent.height) /2)
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
    static public WindowGeometry centerInWindow(Component reference, Dimension extent) {
        Window parentWindow = null;
        while(reference != null && ! (reference instanceof Window) ) {
            reference = reference.getParent();
        }
        if (reference == null)
            return new WindowGeometry(new Point(0,0), extent);
        parentWindow = (Window)reference;
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
     *
     */
    static public class WindowGeometryException extends Exception {
        public WindowGeometryException(String message, Throwable cause) {
            super(message, cause);
        }

        public WindowGeometryException(String message) {
            super(message);
        }
    }

    /** the top left point */
    private Point topLeft;
    /** the size */
    private Dimension extent;

    /**
     *
     * @param topLeft the top left point
     * @param extent the extent
     */
    public WindowGeometry(Point topLeft, Dimension extent) {
        this.topLeft = topLeft;
        this.extent = extent;
    }

    /**
     * Creates a window geometry from the position and the size of a window.
     *
     * @param window the window
     */
    public WindowGeometry(Window window)  {
        this(window.getLocationOnScreen(), window.getSize());
    }

    protected int parseField(String preferenceKey, String preferenceValue, String field) throws WindowGeometryException {
        String v = "";
        try {
            Pattern p = Pattern.compile(field + "=(-?\\d+)",Pattern.CASE_INSENSITIVE);
            Matcher m = p.matcher(preferenceValue);
            if (!m.find())
                throw new WindowGeometryException(tr("Preference with key ''{0}'' does not include ''{1}''. Cannot restore window geometry from preferences.", preferenceKey, field));
            v = m.group(1);
            return Integer.parseInt(v);
        } catch(WindowGeometryException e) {
            throw e;
        } catch(NumberFormatException e) {
            throw new WindowGeometryException(tr("Preference with key ''{0}'' does not provide an int value for ''{1}''. Got {2}. Cannot restore window geometry from preferences.", preferenceKey, field, v));
        } catch(Exception e) {
            throw new WindowGeometryException(tr("Failed to parse field ''{1}'' in preference with key ''{0}''. Exception was: {2}. Cannot restore window geometry from preferences.", preferenceKey, field, e.toString()), e);
        }
    }

    protected void initFromPreferences(String preferenceKey) throws WindowGeometryException {
        String value = Main.pref.get(preferenceKey);
        if (value == null || value.equals(""))
            throw new WindowGeometryException(tr("Preference with key ''{0}'' does not exist. Cannot restore window geometry from preferences.", preferenceKey));
        topLeft = new Point();
        extent = new Dimension();
        topLeft.x = parseField(preferenceKey, value, "x");
        topLeft.y = parseField(preferenceKey, value, "y");
        extent.width = parseField(preferenceKey, value, "width");
        extent.height = parseField(preferenceKey, value, "height");
    }

    protected void initFromWindowGeometry(WindowGeometry other) {
        this.topLeft = other.topLeft;
        this.extent = other.extent;
    }

    static public WindowGeometry mainWindow(String preferenceKey, String arg, boolean maximize) {
        Dimension screenDimension = getScreenSize(null);
        if (arg != null) {
            final Matcher m = Pattern.compile("(\\d+)x(\\d+)(([+-])(\\d+)([+-])(\\d+))?").matcher(arg);
            if (m.matches()) {
                int w = Integer.valueOf(m.group(1));
                int h = Integer.valueOf(m.group(2));
                int x = 0, y = 0;
                if (m.group(3) != null) {
                    x = Integer.valueOf(m.group(5));
                    y = Integer.valueOf(m.group(7));
                    if (m.group(4).equals("-")) {
                        x = screenDimension.width - x - w;
                    }
                    if (m.group(6).equals("-")) {
                        y = screenDimension.height - y - h;
                    }
                }
                return new WindowGeometry(new Point(x,y), new Dimension(w,h));
            } else {
                System.out.println(tr("Ignoring malformed geometry: {0}", arg));
            }
        }
        WindowGeometry def;
        if(maximize)
            def = new WindowGeometry(new Point(0,0), screenDimension);
        else
            def = new WindowGeometry(new Point(0,0), new Dimension(1000, 740));
        return new WindowGeometry(preferenceKey, def);
    }

    /**
     * Creates a window geometry from the values kept in the preference store under the
     * key <code>preferenceKey</code>
     *
     * @param preferenceKey the preference key
     * @throws WindowGeometryException thrown if no such key exist or if the preference value has
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
        } catch(WindowGeometryException e) {
            initFromWindowGeometry(defaultGeometry);
        }
    }

    /**
     * Remembers a window geometry under a specific preference key
     *
     * @param preferenceKey the preference key
     */
    public void remember(String preferenceKey) {
        StringBuffer value = new StringBuffer();
        value.append("x=").append(topLeft.x).append(",")
        .append("y=").append(topLeft.y).append(",")
        .append("width=").append(extent.width).append(",")
        .append("height=").append(extent.height);
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
     * Replies the size spezified by the geometry
     *
     * @return the size spezified by the geometry
     */
    public Dimension getSize() {
        return extent;
    }

    /**
     * Center window on screen. When preferenceKey is given, the window is centered
     * on the screen where the corresponding window is.
     * 
     * @param window the window
     * @param preferenceKey the key to get size and position from
     */
    public static void centerOnScreen(Window window, String preferenceKey) {
        Dimension dim = getScreenSize(preferenceKey);
        Dimension size = window.getSize();
        window.setLocation(new Point((dim.width-size.width)/2,(dim.height-size.height)/2));
    }

    /**
     * Applies this geometry to a window. Makes sure that the window is not
     * placed outside of the coordinate range of all available screens.
     * 
     * @param window the window
     */
    public void applySafe(Window window) {
        Point p = new Point(topLeft);

        Rectangle virtualBounds = new Rectangle();
        GraphicsEnvironment ge = GraphicsEnvironment
                .getLocalGraphicsEnvironment();
        GraphicsDevice[] gs = ge.getScreenDevices();
        for (int j = 0; j < gs.length; j++) {
            GraphicsDevice gd = gs[j];
            GraphicsConfiguration[] gc = gd.getConfigurations();
            for (int i = 0; i < gc.length; i++) {
                virtualBounds = virtualBounds.union(gc[i].getBounds());
            }
        }

        if (p.x < virtualBounds.x) {
            p.x = virtualBounds.x;
        } else if (p.x > virtualBounds.x + virtualBounds.width - extent.width) {
            p.x = virtualBounds.x + virtualBounds.width - extent.width;
        }

        if (p.y < virtualBounds.y) {
            p.y = virtualBounds.y;
        } else if (p.y > virtualBounds.y + virtualBounds.height - extent.height) {
            p.y = virtualBounds.y + virtualBounds.height - extent.height;
        }

        window.setLocation(p);
        window.setSize(extent);
    }

    /**
     * Find the size of the screen for given coordinates. Use first screen,
     * when no coordinates are stored or null is passed.
     * 
     * @param preferenceKey the key to get size and position from
     */
    public static Dimension getScreenSize(String preferenceKey) {
        GraphicsEnvironment ge = GraphicsEnvironment
                .getLocalGraphicsEnvironment();
        GraphicsDevice[] gs = ge.getScreenDevices();
        for (int j = 0; j < gs.length; j++) {
            GraphicsDevice gd = gs[j];
            GraphicsConfiguration[] gc = gd.getConfigurations();
            for (int i = 0; i < gc.length; i++) {
//System.out.println("-- " + j + " " + i + " " + gc[i].getBounds());
            }
        }
        /* TODO: implement this function properly */
        return Toolkit.getDefaultToolkit().getScreenSize();
    }

    public String toString() {
        return "WindowGeometry{topLeft="+topLeft+",extent="+extent+"}";
    }
}
