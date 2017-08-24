// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.LinkedList;

import javax.swing.Action;

import org.openstreetmap.josm.actions.SelectByInternalPointAction;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.paint.PaintColors;
import org.openstreetmap.josm.gui.layer.AbstractMapViewPaintable;
import org.openstreetmap.josm.tools.Utils;

/**
 * Manages the selection of a rectangle or a lasso loop. Listening to left and right mouse button
 * presses and to mouse motions and draw the rectangle accordingly.
 *
 * Left mouse button selects a rectangle from the press until release. Pressing
 * right mouse button while left is still pressed enable the selection area to move
 * around. Releasing the left button fires an action event to the listener given
 * at constructor, except if the right is still pressed, which just remove the
 * selection rectangle and does nothing.
 *
 * It is possible to switch between lasso selection and rectangle selection by using {@link #setLassoMode(boolean)}.
 *
 * The point where the left mouse button was pressed and the current mouse
 * position are two opposite corners of the selection rectangle.
 *
 * For rectangle mode, it is possible to specify an aspect ratio (width per height) which the
 * selection rectangle always must have. In this case, the selection rectangle
 * will be the largest window with this aspect ratio, where the position the left
 * mouse button was pressed and the corner of the current mouse position are at
 * opposite sites (the mouse position corner is the corner nearest to the mouse
 * cursor).
 *
 * When the left mouse button was released, an ActionEvent is send to the
 * ActionListener given at constructor. The source of this event is this manager.
 *
 * @author imi
 */
public class SelectionManager implements MouseListener, MouseMotionListener, PropertyChangeListener {

    /**
     * This is the interface that an user of SelectionManager has to implement
     * to get informed when a selection closes.
     * @author imi
     */
    public interface SelectionEnded extends Action {
        /**
         * Called, when the left mouse button was released.
         * @param r The rectangle that encloses the current selection.
         * @param e The mouse event.
         * @see InputEvent#getModifiersEx()
         * @see SelectionManager#getSelectedObjects(boolean)
         */
        void selectionEnded(Rectangle r, MouseEvent e);
    }

    /**
     * This draws the selection hint (rectangle or lasso polygon) on the screen.
     *
     * @author Michael Zangl
     */
    private class SelectionHintLayer extends AbstractMapViewPaintable {
        @Override
        public void paint(Graphics2D g, MapView mv, Bounds bbox) {
            if (mousePos == null || mousePosStart == null || mousePos == mousePosStart)
                return;
            Color color = Utils.complement(PaintColors.getBackgroundColor());
            g.setColor(color);
            if (lassoMode) {
                g.drawPolygon(lasso);

                g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha() / 8));
                g.fillPolygon(lasso);
            } else {
                Rectangle paintRect = getSelectionRectangle();
                g.drawRect(paintRect.x, paintRect.y, paintRect.width, paintRect.height);
            }
        }
    }

    /**
     * The listener that receives the events after left mouse button is released.
     */
    private final SelectionEnded selectionEndedListener;
    /**
     * Position of the map when the mouse button was pressed.
     * If this is not <code>null</code>, a rectangle/lasso line is drawn on screen.
     * If this is <code>null</code>, no selection is active.
     */
    private Point mousePosStart;
    /**
     * The last position of the mouse while the mouse button was pressed.
     */
    private Point mousePos;
    /**
     * The Component that provides us with OSM data and the aspect is taken from.
     */
    private final NavigatableComponent nc;
    /**
     * Whether the selection rectangle must obtain the aspect ratio of the drawComponent.
     */
    private final boolean aspectRatio;

    /**
     * <code>true</code> if we should paint a lasso instead of a rectangle.
     */
    private boolean lassoMode;
    /**
     * The polygon to store the selection outline if {@link #lassoMode} is used.
     */
    private final Polygon lasso = new Polygon();

    /**
     * The result of the last selection.
     */
    private Polygon selectionResult = new Polygon();

    private final SelectionHintLayer selectionHintLayer = new SelectionHintLayer();

    /**
     * Create a new SelectionManager.
     *
     * @param selectionEndedListener The action listener that receives the event when
     *      the left button is released.
     * @param aspectRatio If true, the selection window must obtain the aspect
     *      ratio of the drawComponent.
     * @param navComp The component that provides us with OSM data and the aspect is taken from.
     */
    public SelectionManager(SelectionEnded selectionEndedListener, boolean aspectRatio, NavigatableComponent navComp) {
        this.selectionEndedListener = selectionEndedListener;
        this.aspectRatio = aspectRatio;
        this.nc = navComp;
    }

    /**
     * Register itself at the given event source and add a hint layer.
     * @param eventSource The emitter of the mouse events.
     * @param lassoMode {@code true} to enable lasso mode, {@code false} to disable it.
     */
    public void register(MapView eventSource, boolean lassoMode) {
       this.lassoMode = lassoMode;
        eventSource.addMouseListener(this);
        eventSource.addMouseMotionListener(this);
        selectionEndedListener.addPropertyChangeListener(this);
        eventSource.addPropertyChangeListener("scale", evt -> abortSelecting());
        eventSource.addTemporaryLayer(selectionHintLayer);
    }

    /**
     * Unregister itself from the given event source and hide the selection hint layer.
     *
     * @param eventSource The emitter of the mouse events.
     */
    public void unregister(MapView eventSource) {
        abortSelecting();
        eventSource.removeTemporaryLayer(selectionHintLayer);
        eventSource.removeMouseListener(this);
        eventSource.removeMouseMotionListener(this);
        selectionEndedListener.removePropertyChangeListener(this);
    }

    /**
     * If the correct button, from the "drawing rectangle" mode
     */
    @Override
    public void mousePressed(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() > 1 && MainApplication.getLayerManager().getEditDataSet() != null) {
            SelectByInternalPointAction.performSelection(MainApplication.getMap().mapView.getEastNorth(e.getX(), e.getY()),
                    (e.getModifiersEx() & MouseEvent.SHIFT_DOWN_MASK) != 0,
                    (e.getModifiersEx() & MouseEvent.CTRL_DOWN_MASK) != 0);
        } else if (e.getButton() == MouseEvent.BUTTON1) {
            mousePosStart = mousePos = e.getPoint();

            lasso.reset();
            lasso.addPoint(mousePosStart.x, mousePosStart.y);
        }
    }

    /**
     * If the correct button is hold, draw the rectangle.
     */
    @Override
    public void mouseDragged(MouseEvent e) {
        int buttonPressed = e.getModifiersEx() & (MouseEvent.BUTTON1_DOWN_MASK | MouseEvent.BUTTON3_DOWN_MASK);

        if (buttonPressed != 0) {
            if (mousePosStart == null) {
                mousePosStart = mousePos = e.getPoint();
            }
            selectionAreaChanged();
        }

        if (buttonPressed == MouseEvent.BUTTON1_DOWN_MASK) {
            mousePos = e.getPoint();
            addLassoPoint(e.getPoint());
            selectionAreaChanged();
        } else if (buttonPressed == (MouseEvent.BUTTON1_DOWN_MASK | MouseEvent.BUTTON3_DOWN_MASK)) {
            moveSelection(e.getX()-mousePos.x, e.getY()-mousePos.y);
            mousePos = e.getPoint();
            selectionAreaChanged();
        }
    }

    /**
     * Moves the current selection by some pixels.
     * @param dx How much to move it in x direction.
     * @param dy How much to move it in y direction.
     */
    private void moveSelection(int dx, int dy) {
        mousePosStart.x += dx;
        mousePosStart.y += dy;
        lasso.translate(dx, dy);
    }

    /**
     * Check the state of the keys and buttons and set the selection accordingly.
     */
    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
            endSelecting(e);
        }
    }

    /**
     * Ends the selection of the current area. This simulates a release of mouse button 1.
     * @param e A mouse event that caused this. Needed for backward compatibility.
     */
    public void endSelecting(MouseEvent e) {
        mousePos = e.getPoint();
        if (lassoMode) {
            addLassoPoint(e.getPoint());
        }

        // Left mouse was released while right is still pressed.
        boolean rightMouseStillPressed = (e.getModifiersEx() & MouseEvent.BUTTON3_DOWN_MASK) != 0;

        if (!rightMouseStillPressed) {
            selectingDone(e);
        }
        abortSelecting();
    }

    private void addLassoPoint(Point point) {
        if (isNoSelection()) {
            return;
        }
        lasso.addPoint(point.x, point.y);
    }

    private boolean isNoSelection() {
        return mousePos == null || mousePosStart == null || mousePos == mousePosStart;
    }

    /**
     * Calculate and return the current selection rectangle
     * @return A rectangle that spans from mousePos to mouseStartPos
     */
    private Rectangle getSelectionRectangle() {
        int x = mousePosStart.x;
        int y = mousePosStart.y;
        int w = mousePos.x - mousePosStart.x;
        int h = mousePos.y - mousePosStart.y;
        if (w < 0) {
            x += w;
            w = -w;
        }
        if (h < 0) {
            y += h;
            h = -h;
        }

        if (aspectRatio) {
            /* Keep the aspect ratio by growing the rectangle; the
             * rectangle is always under the cursor. */
            double aspectRatio = (double) nc.getWidth()/nc.getHeight();
            if ((double) w/h < aspectRatio) {
                int neww = (int) (h*aspectRatio);
                if (mousePos.x < mousePosStart.x) {
                    x += w - neww;
                }
                w = neww;
            } else {
                int newh = (int) (w/aspectRatio);
                if (mousePos.y < mousePosStart.y) {
                    y += h - newh;
                }
                h = newh;
            }
        }

        return new Rectangle(x, y, w, h);
    }

    /**
     * If the action goes inactive, remove the selection rectangle from screen
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if ("active".equals(evt.getPropertyName()) && !(Boolean) evt.getNewValue()) {
            abortSelecting();
        }
    }

    /**
     * Stores the  current selection and stores the result in {@link #selectionResult} to  be retrieved by
     * {@link #getSelectedObjects(boolean)} later.
     * @param e The mouse event that caused the selection to be finished.
     */
    private void selectingDone(MouseEvent e) {
        if (isNoSelection()) {
            // Nothing selected.
            return;
        }
        Rectangle r;
        if (lassoMode) {
            r = lasso.getBounds();

            selectionResult = new Polygon(lasso.xpoints, lasso.ypoints, lasso.npoints);
        } else {
            r = getSelectionRectangle();

            selectionResult = rectToPolygon(r);
        }
        selectionEndedListener.selectionEnded(r, e);
    }

    private void abortSelecting() {
        if (mousePosStart != null) {
            mousePos = mousePosStart = null;
            lasso.reset();
            selectionAreaChanged();
        }
    }

    private void selectionAreaChanged() {
        selectionHintLayer.invalidate();
    }

    /**
     * Return a list of all objects in the active/last selection, respecting the different
     * modifier.
     *
     * @param alt Whether the alt key was pressed, which means select all
     * objects that are touched, instead those which are completely covered.
     * @return The collection of selected objects.
     */
    public Collection<OsmPrimitive> getSelectedObjects(boolean alt) {
        Collection<OsmPrimitive> selection = new LinkedList<>();

        // whether user only clicked, not dragged.
        boolean clicked = false;
        Rectangle bounding = selectionResult.getBounds();
        if (bounding.height <= 2 && bounding.width <= 2) {
            clicked = true;
        }

        if (clicked) {
            Point center = new Point(selectionResult.xpoints[0], selectionResult.ypoints[0]);
            OsmPrimitive osm = nc.getNearestNodeOrWay(center, OsmPrimitive::isSelectable, false);
            if (osm != null) {
                selection.add(osm);
            }
        } else {
            // nodes
            for (Node n : MainApplication.getLayerManager().getEditDataSet().getNodes()) {
                if (n.isSelectable() && selectionResult.contains(nc.getPoint2D(n))) {
                    selection.add(n);
                }
            }

            // ways
            for (Way w : MainApplication.getLayerManager().getEditDataSet().getWays()) {
                if (!w.isSelectable() || w.getNodesCount() == 0) {
                    continue;
                }
                if (alt) {
                    for (Node n : w.getNodes()) {
                        if (!n.isIncomplete() && selectionResult.contains(nc.getPoint2D(n))) {
                            selection.add(w);
                            break;
                        }
                    }
                } else {
                    boolean allIn = true;
                    for (Node n : w.getNodes()) {
                        if (!n.isIncomplete() && !selectionResult.contains(nc.getPoint(n))) {
                            allIn = false;
                            break;
                        }
                    }
                    if (allIn) {
                        selection.add(w);
                    }
                }
            }
        }
        return selection;
    }

    private static Polygon rectToPolygon(Rectangle r) {
        Polygon poly = new Polygon();

        poly.addPoint(r.x, r.y);
        poly.addPoint(r.x, r.y + r.height);
        poly.addPoint(r.x + r.width, r.y + r.height);
        poly.addPoint(r.x + r.width, r.y);

        return poly;
    }

    /**
     * Enables or disables the lasso mode.
     * @param lassoMode {@code true} to enable lasso mode, {@code false} to disable it.
     */
    public void setLassoMode(boolean lassoMode) {
        this.lassoMode = lassoMode;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        // Do nothing
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        // Do nothing
    }

    @Override
    public void mouseExited(MouseEvent e) {
        // Do nothing
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        // Do nothing
    }
}
