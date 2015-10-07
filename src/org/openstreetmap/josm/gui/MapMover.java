// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.mapmode.SelectAction;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.tools.Destroyable;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Enables moving of the map by holding down the right mouse button and drag
 * the mouse. Also, enables zooming by the mouse wheel.
 *
 * @author imi
 */
public class MapMover extends MouseAdapter implements MouseMotionListener, MouseWheelListener, Destroyable {

    private final class ZoomerAction extends AbstractAction {
        private final String action;

        ZoomerAction(String action) {
            this.action = action;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (".".equals(action) || ",".equals(action)) {
                Point mouse = nc.getMousePosition();
                if (mouse == null)
                    mouse = new Point((int) nc.getBounds().getCenterX(), (int) nc.getBounds().getCenterY());
                MouseWheelEvent we = new MouseWheelEvent(nc, e.getID(), e.getWhen(), e.getModifiers(), mouse.x, mouse.y, 0, false,
                        MouseWheelEvent.WHEEL_UNIT_SCROLL, 1, ",".equals(action) ? -1 : 1);
                mouseWheelMoved(we);
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
                }
            }
        }
    }

    /**
     * The point in the map that was the under the mouse point
     * when moving around started.
     */
    private EastNorth mousePosMove;
    /**
     * The map to move around.
     */
    private final NavigatableComponent nc;
    private final JPanel contentPane;

    private boolean movementInPlace = false;

    /**
     * Constructs a new {@code MapMover}.
     * @param navComp the navigatable component
     * @param contentPane the content pane
     */
    public MapMover(NavigatableComponent navComp, JPanel contentPane) {
        this.nc = navComp;
        this.contentPane = contentPane;
        nc.addMouseListener(this);
        nc.addMouseMotionListener(this);
        nc.addMouseWheelListener(this);

        if (contentPane != null) {
            // CHECKSTYLE.OFF: LineLength
            contentPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                Shortcut.registerShortcut("system:movefocusright", tr("Map: {0}", tr("Move right")), KeyEvent.VK_RIGHT, Shortcut.CTRL).getKeyStroke(),
                "MapMover.Zoomer.right");
            contentPane.getActionMap().put("MapMover.Zoomer.right", new ZoomerAction("right"));

            contentPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                Shortcut.registerShortcut("system:movefocusleft", tr("Map: {0}", tr("Move left")), KeyEvent.VK_LEFT, Shortcut.CTRL).getKeyStroke(),
                "MapMover.Zoomer.left");
            contentPane.getActionMap().put("MapMover.Zoomer.left", new ZoomerAction("left"));

            contentPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                Shortcut.registerShortcut("system:movefocusup", tr("Map: {0}", tr("Move up")), KeyEvent.VK_UP, Shortcut.CTRL).getKeyStroke(),
                "MapMover.Zoomer.up");
            contentPane.getActionMap().put("MapMover.Zoomer.up", new ZoomerAction("up"));

            contentPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                Shortcut.registerShortcut("system:movefocusdown", tr("Map: {0}", tr("Move down")), KeyEvent.VK_DOWN, Shortcut.CTRL).getKeyStroke(),
                "MapMover.Zoomer.down");
            contentPane.getActionMap().put("MapMover.Zoomer.down", new ZoomerAction("down"));
            // CHECKSTYLE.ON: LineLength

            // see #10592 - Disable these alternate shortcuts on OS X because of conflict with system shortcut
            if (!Main.isPlatformOsx()) {
                contentPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                    Shortcut.registerShortcut("view:zoominalternate",
                            tr("Map: {0}", tr("Zoom in")), KeyEvent.VK_COMMA, Shortcut.CTRL).getKeyStroke(),
                    "MapMover.Zoomer.in");
                contentPane.getActionMap().put("MapMover.Zoomer.in", new ZoomerAction(","));

                contentPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                    Shortcut.registerShortcut("view:zoomoutalternate",
                            tr("Map: {0}", tr("Zoom out")), KeyEvent.VK_PERIOD, Shortcut.CTRL).getKeyStroke(),
                    "MapMover.Zoomer.out");
                contentPane.getActionMap().put("MapMover.Zoomer.out", new ZoomerAction("."));
            }
        }
    }

    /**
     * If the right (and only the right) mouse button is pressed, move the map.
     */
    @Override
    public void mouseDragged(MouseEvent e) {
        int offMask = MouseEvent.BUTTON1_DOWN_MASK | MouseEvent.BUTTON2_DOWN_MASK;
        int macMouseMask = MouseEvent.CTRL_DOWN_MASK | MouseEvent.BUTTON1_DOWN_MASK;
        boolean stdMovement = (e.getModifiersEx() & (MouseEvent.BUTTON3_DOWN_MASK | offMask)) == MouseEvent.BUTTON3_DOWN_MASK;
        boolean macMovement = Main.isPlatformOsx() && e.getModifiersEx() == macMouseMask;
        boolean allowedMode = !Main.map.mapModeSelect.equals(Main.map.mapMode)
                          || SelectAction.Mode.SELECT.equals(Main.map.mapModeSelect.getMode());
        if (stdMovement || (macMovement && allowedMode)) {
            if (mousePosMove == null)
                startMovement(e);
            EastNorth center = nc.getCenter();
            EastNorth mouseCenter = nc.getEastNorth(e.getX(), e.getY());
            nc.zoomTo(new EastNorth(
                    mousePosMove.east() + center.east() - mouseCenter.east(),
                    mousePosMove.north() + center.north() - mouseCenter.north()));
        } else {
            endMovement();
        }
    }

    /**
     * Start the movement, if it was the 3rd button (right button).
     */
    @Override
    public void mousePressed(MouseEvent e) {
        int offMask = MouseEvent.BUTTON1_DOWN_MASK | MouseEvent.BUTTON2_DOWN_MASK;
        int macMouseMask = MouseEvent.CTRL_DOWN_MASK | MouseEvent.BUTTON1_DOWN_MASK;
        if (e.getButton() == MouseEvent.BUTTON3 && (e.getModifiersEx() & offMask) == 0 ||
                Main.isPlatformOsx() && e.getModifiersEx() == macMouseMask) {
            startMovement(e);
        }
    }

    /**
     * Change the cursor back to it's pre-move cursor.
     */
    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON3 || Main.isPlatformOsx() && e.getButton() == MouseEvent.BUTTON1) {
            endMovement();
        }
    }

    /**
     * Start movement by setting a new cursor and remember the current mouse
     * position.
     * @param e The mouse event that leat to the movement from.
     */
    private void startMovement(MouseEvent e) {
        if (movementInPlace)
            return;
        movementInPlace = true;
        mousePosMove = nc.getEastNorth(e.getX(), e.getY());
        nc.setNewCursor(Cursor.MOVE_CURSOR, this);
    }

    /**
     * End the movement. Setting back the cursor and clear the movement variables
     */
    private void endMovement() {
        if (!movementInPlace)
            return;
        movementInPlace = false;
        nc.resetCursor(this);
        mousePosMove = null;
    }

    /**
     * Zoom the map by 1/5th of current zoom per wheel-delta.
     * @param e The wheel event.
     */
    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        nc.zoomToFactor(e.getX(), e.getY(), Math.pow(Math.sqrt(2), e.getWheelRotation()));
    }

    /**
     * Emulates dragging on Mac OSX.
     */
    @Override
    public void mouseMoved(MouseEvent e) {
        if (!movementInPlace)
            return;
        // Mac OSX simulates with  ctrl + mouse 1  the second mouse button hence no dragging events get fired.
        // Is only the selected mouse button pressed?
        if (Main.isPlatformOsx()) {
            if (e.getModifiersEx() == MouseEvent.CTRL_DOWN_MASK) {
                if (mousePosMove == null) {
                    startMovement(e);
                }
                EastNorth center = nc.getCenter();
                EastNorth mouseCenter = nc.getEastNorth(e.getX(), e.getY());
                nc.zoomTo(new EastNorth(mousePosMove.east() + center.east() - mouseCenter.east(), mousePosMove.north()
                        + center.north() - mouseCenter.north()));
            } else {
                endMovement();
            }
        }
    }

    @Override
    public void destroy() {
        if (this.contentPane != null) {
            InputMap inputMap = contentPane.getInputMap();
            KeyStroke[] inputKeys = inputMap.keys();
            if (inputKeys != null) {
                for (KeyStroke key : inputKeys) {
                    Object binding = inputMap.get(key);
                    if (binding instanceof String && ((String) binding).startsWith("MapMover.")) {
                        inputMap.remove(key);
                    }
                }
            }
            ActionMap actionMap = contentPane.getActionMap();
            Object[] actionsKeys = actionMap.keys();
            if (actionsKeys != null) {
                for (Object key : actionsKeys) {
                    if (key instanceof String && ((String) key).startsWith("MapMover.")) {
                        actionMap.remove(key);
                    }
                }
            }
        }
    }
}
