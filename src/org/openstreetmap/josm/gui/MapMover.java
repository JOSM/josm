// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui;

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
import javax.swing.JComponent;
import javax.swing.JPanel;
import org.openstreetmap.josm.tools.Shortcut;
import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.data.coor.EastNorth;

/**
 * Enables moving of the map by holding down the right mouse button and drag
 * the mouse. Also, enables zooming by the mouse wheel.
 *
 * @author imi
 */
public class MapMover extends MouseAdapter implements MouseMotionListener, MouseWheelListener {

    private final class ZoomerAction extends AbstractAction {
        private final String action;
        public ZoomerAction(String action) {
            this.action = action;
        }
        public void actionPerformed(ActionEvent e) {
            if (action.equals(".") || action.equals(",")) {
                Point mouse = nc.getMousePosition();
                if (mouse == null)
                    mouse = new Point((int)nc.getBounds().getCenterX(), (int)nc.getBounds().getCenterY());
                MouseWheelEvent we = new MouseWheelEvent(nc, e.getID(), e.getWhen(), e.getModifiers(), mouse.x, mouse.y, 0, false, MouseWheelEvent.WHEEL_UNIT_SCROLL, 1, action.equals(",") ? -1 : 1);
                mouseWheelMoved(we);
            } else {
                EastNorth center = nc.getCenter();
                EastNorth newcenter = nc.getEastNorth(nc.getWidth()/2+nc.getWidth()/5, nc.getHeight()/2+nc.getHeight()/5);
                if (action.equals("left"))
                    nc.zoomTo(new EastNorth(2*center.east()-newcenter.east(), center.north()), nc.getScale());
                else if (action.equals("right"))
                    nc.zoomTo(new EastNorth(newcenter.east(), center.north()), nc.getScale());
                else if (action.equals("up"))
                    nc.zoomTo(new EastNorth(center.east(), 2*center.north()-newcenter.north()), nc.getScale());
                else if (action.equals("down"))
                    nc.zoomTo(new EastNorth(center.east(), newcenter.north()), nc.getScale());
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
    /**
     * The old cursor when we changed it to movement cursor.
     */
    private Cursor oldCursor;

    private boolean movementInPlace = false;

    /**
     * Create a new MapMover
     */
    public MapMover(NavigatableComponent navComp, JPanel contentPane) {
        this.nc = navComp;
        nc.addMouseListener(this);
        nc.addMouseMotionListener(this);
        nc.addMouseWheelListener(this);

        if (contentPane != null) {
            contentPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                Shortcut.registerShortcut("system:movefocusright", tr("Map: {0}", tr("Move right")), KeyEvent.VK_RIGHT, Shortcut.GROUP_HOTKEY).getKeyStroke(),
                "MapMover.Zoomer.right");
            contentPane.getActionMap().put("MapMover.Zoomer.right", new ZoomerAction("right"));

            contentPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                Shortcut.registerShortcut("system:movefocusleft", tr("Map: {0}", tr("Move left")), KeyEvent.VK_LEFT, Shortcut.GROUP_HOTKEY).getKeyStroke(),
                "MapMover.Zoomer.left");
            contentPane.getActionMap().put("MapMover.Zoomer.left", new ZoomerAction("left"));

            contentPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                Shortcut.registerShortcut("system:movefocusup", tr("Map: {0}", tr("Move up")), KeyEvent.VK_UP, Shortcut.GROUP_HOTKEY).getKeyStroke(),
                "MapMover.Zoomer.up");
            contentPane.getActionMap().put("MapMover.Zoomer.up", new ZoomerAction("up"));

            contentPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                Shortcut.registerShortcut("system:movefocusdown", tr("Map: {0}", tr("Move down")), KeyEvent.VK_DOWN, Shortcut.GROUP_HOTKEY).getKeyStroke(),
                "MapMover.Zoomer.down");
            contentPane.getActionMap().put("MapMover.Zoomer.down", new ZoomerAction("down"));

            contentPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                Shortcut.registerShortcut("view:zoominalternate", tr("Map: {0}", tr("Zoom in")), KeyEvent.VK_COMMA, Shortcut.GROUP_HOTKEY).getKeyStroke(),
                "MapMover.Zoomer.in");
            contentPane.getActionMap().put("MapMover.Zoomer.in", new ZoomerAction(","));

            contentPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                Shortcut.registerShortcut("view:zoomoutalternate", tr("Map: {0}", tr("Zoom out")), KeyEvent.VK_PERIOD, Shortcut.GROUP_HOTKEY).getKeyStroke(),
                "MapMover.Zoomer.out");
            contentPane.getActionMap().put("MapMover.Zoomer.out", new ZoomerAction("."));
        }
    }

    /**
     * If the right (and only the right) mouse button is pressed, move the map
     */
    public void mouseDragged(MouseEvent e) {
        int offMask = MouseEvent.BUTTON1_DOWN_MASK | MouseEvent.BUTTON2_DOWN_MASK;
        if ((e.getModifiersEx() & (MouseEvent.BUTTON3_DOWN_MASK | offMask)) == MouseEvent.BUTTON3_DOWN_MASK) {
            if (mousePosMove == null)
                startMovement(e);
            EastNorth center = nc.getCenter();
            EastNorth mouseCenter = nc.getEastNorth(e.getX(), e.getY());
            EastNorth p = new EastNorth(
                    mousePosMove.east() + center.east() - mouseCenter.east(),
                    mousePosMove.north() + center.north() - mouseCenter.north());
            nc.zoomTo(p, nc.getScale());
        } else
            endMovement();
    }

    /**
     * Start the movement, if it was the 3rd button (right button).
     */
    @Override public void mousePressed(MouseEvent e) {
        int offMask = MouseEvent.BUTTON1_DOWN_MASK | MouseEvent.BUTTON2_DOWN_MASK;
        if (e.getButton() == MouseEvent.BUTTON3 && (e.getModifiersEx() & offMask) == 0)
            startMovement(e);
    }

    /**
     * Change the cursor back to it's pre-move cursor.
     */
    @Override public void mouseReleased(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON3)
            endMovement();
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
        oldCursor = nc.getCursor();
        nc.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
    }

    /**
     * End the movement. Setting back the cursor and clear the movement variables
     */
    private void endMovement() {
        if (!movementInPlace)
            return;
        movementInPlace = false;
        if (oldCursor != null)
            nc.setCursor(oldCursor);
        else
            nc.setCursor(Cursor.getDefaultCursor());
        mousePosMove = null;
        oldCursor = null;
    }

    /**
     * Zoom the map by 1/5th of current zoom per wheel-delta.
     * @param e The wheel event.
     */
    public void mouseWheelMoved(MouseWheelEvent e) {
        double newScale = nc.getScale() * Math.pow(0.8, - e.getWheelRotation());

        // New center position so that point under the mouse pointer stays the same place as it was before zooming
        // You will get the formula by simplifying this expression: newCenter = oldCenter + mouseCoordinatesInNewZoom - mouseCoordinatesInOldZoom
        double newX = nc.center.east() - (e.getX() - nc.getWidth()/2.0) * (newScale - nc.scale);
        double newY = nc.center.north() + (e.getY() - nc.getHeight()/2.0) * (newScale - nc.scale);

        nc.zoomTo(new EastNorth(newX, newY), newScale);
    }

    /**
     * Does nothing. Only to satisfy MouseMotionListener
     */
    public void mouseMoved(MouseEvent e) {}
}
