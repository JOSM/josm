// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.ArrayList;
import java.util.Optional;

import javax.swing.AbstractAction;

import org.openstreetmap.gui.jmapviewer.JMapViewer;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.mapmode.SelectAction;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.preferences.BooleanProperty;
import org.openstreetmap.josm.gui.MapViewState.MapViewPoint;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.spi.preferences.PreferenceChangeEvent;
import org.openstreetmap.josm.spi.preferences.PreferenceChangedListener;
import org.openstreetmap.josm.tools.Destroyable;
import org.openstreetmap.josm.tools.Pair;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Enables moving of the map by holding down the right mouse button and drag
 * the mouse. Also, enables zooming by the mouse wheel.
 *
 * @author imi
 */
public class MapMover extends MouseAdapter implements Destroyable {

    /**
     * Zoom wheel is reversed.
     */
    public static final BooleanProperty PROP_ZOOM_REVERSE_WHEEL = new BooleanProperty("zoom.reverse-wheel", false);

    static {
        new JMapViewerUpdater();
    }

    private static class JMapViewerUpdater implements PreferenceChangedListener {

        JMapViewerUpdater() {
            Config.getPref().addPreferenceChangeListener(this);
            updateJMapViewer();
        }

        @Override
        public void preferenceChanged(PreferenceChangeEvent e) {
            if (MapMover.PROP_ZOOM_REVERSE_WHEEL.getKey().equals(e.getKey())) {
                updateJMapViewer();
            }
        }

        private static void updateJMapViewer() {
            JMapViewer.zoomReverseWheel = MapMover.PROP_ZOOM_REVERSE_WHEEL.get();
        }
    }

    private final class ZoomerAction extends AbstractAction {
        private final String action;

        ZoomerAction(String action) {
            this(action, "MapMover.Zoomer." + action);
        }

        ZoomerAction(String action, String name) {
            this.action = action;
            putValue(NAME, name);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (".".equals(action) || ",".equals(action)) {
                Point mouse = Optional.ofNullable(nc.getMousePosition()).orElseGet(
                    () -> new Point((int) nc.getBounds().getCenterX(), (int) nc.getBounds().getCenterY()));
                mouseWheelMoved(new MouseWheelEvent(nc, e.getID(), e.getWhen(), e.getModifiers(), mouse.x, mouse.y, 0, false,
                        MouseWheelEvent.WHEEL_UNIT_SCROLL, 1, ",".equals(action) ? -1 : 1));
            } else {
                EastNorth center = nc.getCenter();
                EastNorth newcenter = nc.getEastNorth(nc.getWidth()/2+nc.getWidth()/5, nc.getHeight()/2+nc.getHeight()/5);
                switch(action) {
                case "left":
                    nc.zoomTo(new EastNorth(2*center.east()-newcenter.east(), center.north()));
                    break;
                case "right":
                    nc.zoomTo(new EastNorth(newcenter.east(), center.north()));
                    break;
                case "up":
                    nc.zoomTo(new EastNorth(center.east(), 2*center.north()-newcenter.north()));
                    break;
                case "down":
                    nc.zoomTo(new EastNorth(center.east(), newcenter.north()));
                    break;
                default: // Do nothing
                }
            }
        }
    }

    /**
     * The point in the map that was the under the mouse point
     * when moving around started.
     *
     * This is <code>null</code> if movement is not active
     */
    private MapViewPoint mousePosMoveStart;

    /**
     * The map to move around.
     */
    private final NavigatableComponent nc;

    private final ArrayList<Pair<ZoomerAction, Shortcut>> registeredShortcuts = new ArrayList<>();

    /**
     * Constructs a new {@code MapMover}.
     * @param navComp the navigatable component
     * @since 11713
     */
    public MapMover(NavigatableComponent navComp) {
        this.nc = navComp;
        nc.addMouseListener(this);
        nc.addMouseMotionListener(this);
        nc.addMouseWheelListener(this);

        registerActionShortcut(new ZoomerAction("right"),
                Shortcut.registerShortcut("system:movefocusright", tr("Map: {0}", tr("Move right")), KeyEvent.VK_RIGHT, Shortcut.CTRL));

        registerActionShortcut(new ZoomerAction("left"),
                Shortcut.registerShortcut("system:movefocusleft", tr("Map: {0}", tr("Move left")), KeyEvent.VK_LEFT, Shortcut.CTRL));

        registerActionShortcut(new ZoomerAction("up"),
                Shortcut.registerShortcut("system:movefocusup", tr("Map: {0}", tr("Move up")), KeyEvent.VK_UP, Shortcut.CTRL));
        registerActionShortcut(new ZoomerAction("down"),
                Shortcut.registerShortcut("system:movefocusdown", tr("Map: {0}", tr("Move down")), KeyEvent.VK_DOWN, Shortcut.CTRL));

        // see #10592 - Disable these alternate shortcuts on OS X because of conflict with system shortcut
        if (!Main.isPlatformOsx()) {
            registerActionShortcut(new ZoomerAction(",", "MapMover.Zoomer.in"),
                    Shortcut.registerShortcut("view:zoominalternate", tr("Map: {0}", tr("Zoom In")), KeyEvent.VK_COMMA, Shortcut.CTRL));

            registerActionShortcut(new ZoomerAction(".", "MapMover.Zoomer.out"),
                    Shortcut.registerShortcut("view:zoomoutalternate", tr("Map: {0}", tr("Zoom Out")), KeyEvent.VK_PERIOD, Shortcut.CTRL));
        }
    }

    private void registerActionShortcut(ZoomerAction action, Shortcut shortcut) {
        MainApplication.registerActionShortcut(action, shortcut);
        registeredShortcuts.add(new Pair<>(action, shortcut));
    }

    private boolean movementInProgress() {
        return mousePosMoveStart != null;
    }

    /**
     * If the right (and only the right) mouse button is pressed, move the map.
     */
    @Override
    public void mouseDragged(MouseEvent e) {
        int offMask = MouseEvent.BUTTON1_DOWN_MASK | MouseEvent.BUTTON2_DOWN_MASK;
        boolean allowMovement = (e.getModifiersEx() & (MouseEvent.BUTTON3_DOWN_MASK | offMask)) == MouseEvent.BUTTON3_DOWN_MASK;
        if (Main.isPlatformOsx()) {
            MapFrame map = MainApplication.getMap();
            int macMouseMask = MouseEvent.CTRL_DOWN_MASK | MouseEvent.BUTTON1_DOWN_MASK;
            boolean macMovement = e.getModifiersEx() == macMouseMask;
            boolean allowedMode = !map.mapModeSelect.equals(map.mapMode)
                              || SelectAction.Mode.SELECT.equals(map.mapModeSelect.getMode());
            allowMovement |= macMovement && allowedMode;
        }
        if (allowMovement) {
            doMoveForDrag(e);
        } else {
            endMovement();
        }
    }

    private void doMoveForDrag(MouseEvent e) {
        if (!movementInProgress()) {
            startMovement(e);
        }
        EastNorth center = nc.getCenter();
        EastNorth mouseCenter = nc.getEastNorth(e.getX(), e.getY());
        nc.zoomTo(mousePosMoveStart.getEastNorth().add(center).subtract(mouseCenter));
    }

    /**
     * Start the movement, if it was the 3rd button (right button).
     */
    @Override
    public void mousePressed(MouseEvent e) {
        int offMask = MouseEvent.BUTTON1_DOWN_MASK | MouseEvent.BUTTON2_DOWN_MASK;
        int macMouseMask = MouseEvent.CTRL_DOWN_MASK | MouseEvent.BUTTON1_DOWN_MASK;
        if ((e.getButton() == MouseEvent.BUTTON3 && (e.getModifiersEx() & offMask) == 0) ||
                (Main.isPlatformOsx() && e.getModifiersEx() == macMouseMask)) {
            startMovement(e);
        }
    }

    /**
     * Change the cursor back to it's pre-move cursor.
     */
    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON3 || (Main.isPlatformOsx() && e.getButton() == MouseEvent.BUTTON1)) {
            endMovement();
        }
    }

    /**
     * Start movement by setting a new cursor and remember the current mouse
     * position.
     * @param e The mouse event that leat to the movement from.
     */
    private void startMovement(MouseEvent e) {
        if (movementInProgress()) {
            return;
        }
        mousePosMoveStart = nc.getState().getForView(e.getX(), e.getY());
        nc.setNewCursor(Cursor.MOVE_CURSOR, this);
    }

    /**
     * End the movement. Setting back the cursor and clear the movement variables
     */
    private void endMovement() {
        if (!movementInProgress()) {
            return;
        }
        nc.resetCursor(this);
        mousePosMoveStart = null;
    }

    /**
     * Zoom the map by 1/5th of current zoom per wheel-delta.
     * @param e The wheel event.
     */
    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        int rotation = PROP_ZOOM_REVERSE_WHEEL.get() ? -e.getWheelRotation() : e.getWheelRotation();
        nc.zoomManyTimes(e.getX(), e.getY(), rotation);
    }

    /**
     * Emulates dragging on Mac OSX.
     */
    @Override
    public void mouseMoved(MouseEvent e) {
        if (!movementInProgress()) {
            return;
        }
        // Mac OSX simulates with  ctrl + mouse 1  the second mouse button hence no dragging events get fired.
        // Is only the selected mouse button pressed?
        if (Main.isPlatformOsx()) {
            if (e.getModifiersEx() == MouseEvent.CTRL_DOWN_MASK) {
                doMoveForDrag(e);
            } else {
                endMovement();
            }
        }
    }

    @Override
    public void destroy() {
        for (Pair<ZoomerAction, Shortcut> shortcut : registeredShortcuts) {
            MainApplication.unregisterActionShortcut(shortcut.a, shortcut.b);
        }
    }
}
