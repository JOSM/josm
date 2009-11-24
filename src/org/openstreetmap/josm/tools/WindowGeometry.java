// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.Window;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;

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
     * Replies a window geometry object for a window which a specific size which is centered
     * relative to a parent window
     *
     * @param parent the parent window
     * @param extent the size
     * @return the geometry object
     */
    static public WindowGeometry centerInWindow(Component parent, Dimension extent) {
        Frame parentWindow = JOptionPane.getFrameForComponent(parent);
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
            Pattern p = Pattern.compile(field + "=(\\d+)",Pattern.CASE_INSENSITIVE);
            Matcher m = p.matcher(preferenceValue);
            if (!m.find())
                throw new WindowGeometryException(tr("Preference with key ''{0}'' does not include ''{1}''. Can''t restore window geometry from preferences.", preferenceKey, field));
            v = m.group(1);
            return Integer.parseInt(v);
        } catch(WindowGeometryException e) {
            throw e;
        } catch(NumberFormatException e) {
            throw new WindowGeometryException(tr("Preference with key ''{0}'' does not provide an int value for ''{1}''. Got {2}. Can''t restore window geometry from preferences.", preferenceKey, field, v));
        } catch(Exception e) {
            throw new WindowGeometryException(tr("Failed to parse field ''{1}'' in preference with key ''{0}''. Exception was: {2}. Can''t restore window geometry from preferences.", preferenceKey, field, e.toString()), e);
        }
    }

    protected void initFromPreferences(String preferenceKey) throws WindowGeometryException {
        String value = Main.pref.get(preferenceKey);
        if (value == null || value.equals(""))
            throw new WindowGeometryException(tr("Preference with key ''{0}'' does not exist. Can''t restore window geometry from preferences.", preferenceKey));
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
//            System.out.println(tr("Warning: Failed to restore window geometry from key ''{0}''. Falling back to default geometry. Details: {1}", preferenceKey, e.getMessage()));
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
     * Applies this geometry to a window
     *
     * @param window the window
     */
    public void apply(Window window) {
        window.setLocation(topLeft);
        window.setSize(extent);
    }

    /**
     * Applies this geometry to a window. Makes sure that the window is not placed outside
     * of the coordinate range of the current screen.
     *
     * @param window the window
     */
    public void applySafe(Window window) {
        Point p = new Point(topLeft);
        if (p.x > Toolkit.getDefaultToolkit().getScreenSize().width - 10) {
            p.x  = 0;
        }
        if (p.y >  Toolkit.getDefaultToolkit().getScreenSize().height - 10) {
            p.y = 0;
        }
        window.setLocation(p);
        window.setSize(extent);
    }
}
