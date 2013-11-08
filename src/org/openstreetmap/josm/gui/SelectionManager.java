// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui;

import java.awt.Component;
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

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;

/**
 * Manages the selection of a rectangle. Listening to left and right mouse button
 * presses and to mouse motions and draw the rectangle accordingly.
 *
 * Left mouse button selects a rectangle from the press until release. Pressing
 * right mouse button while left is still pressed enable the rectangle to move
 * around. Releasing the left button fires an action event to the listener given
 * at constructor, except if the right is still pressed, which just remove the
 * selection rectangle and does nothing.
 *
 * The point where the left mouse button was pressed and the current mouse
 * position are two opposite corners of the selection rectangle.
 *
 * It is possible to specify an aspect ratio (width per height) which the
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
    public interface SelectionEnded {
        /**
         * Called, when the left mouse button was released.
         * @param r The rectangle that is currently the selection.
         * @param e The mouse event.
         * @see InputEvent#getModifiersEx()
         */
        public void selectionEnded(Rectangle r, MouseEvent e);
        /**
         * Called to register the selection manager for "active" property.
         * @param listener The listener to register
         */
        public void addPropertyChangeListener(PropertyChangeListener listener);
        /**
         * Called to remove the selection manager from the listener list
         * for "active" property.
         * @param listener The listener to register
         */
        public void removePropertyChangeListener(PropertyChangeListener listener);
    }
    /**
     * The listener that receives the events after left mouse button is released.
     */
    private final SelectionEnded selectionEndedListener;
    /**
     * Position of the map when the mouse button was pressed.
     * If this is not <code>null</code>, a rectangle is drawn on screen.
     */
    private Point mousePosStart;
    /**
     * Position of the map when the selection rectangle was last drawn.
     */
    private Point mousePos;
    /**
     * The Component, the selection rectangle is drawn onto.
     */
    private final NavigatableComponent nc;
    /**
     * Whether the selection rectangle must obtain the aspect ratio of the
     * drawComponent.
     */
    private boolean aspectRatio;

    private boolean lassoMode;
    private Polygon lasso = new Polygon();

    /**
     * Create a new SelectionManager.
     *
     * @param selectionEndedListener The action listener that receives the event when
     *      the left button is released.
     * @param aspectRatio If true, the selection window must obtain the aspect
     *      ratio of the drawComponent.
     * @param navComp The component, the rectangle is drawn onto.
     */
    public SelectionManager(SelectionEnded selectionEndedListener, boolean aspectRatio, NavigatableComponent navComp) {
        this.selectionEndedListener = selectionEndedListener;
        this.aspectRatio = aspectRatio;
        this.nc = navComp;
    }

    /**
     * Register itself at the given event source.
     * @param eventSource The emitter of the mouse events.
     * @param lassoMode {@code true} to enable lasso mode, {@code false} to disable it.
     */
    public void register(NavigatableComponent eventSource, boolean lassoMode) {
       this.lassoMode = lassoMode;
        eventSource.addMouseListener(this);
        eventSource.addMouseMotionListener(this);
        selectionEndedListener.addPropertyChangeListener(this);
        eventSource.addPropertyChangeListener("scale", new PropertyChangeListener(){
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (mousePosStart != null) {
                    paintRect();
                    mousePos = mousePosStart = null;
                }
            }
        });
    }
    /**
     * Unregister itself from the given event source. If a selection rectangle is
     * shown, hide it first.
     *
     * @param eventSource The emitter of the mouse events.
     */
    public void unregister(Component eventSource) {
        eventSource.removeMouseListener(this);
        eventSource.removeMouseMotionListener(this);
        selectionEndedListener.removePropertyChangeListener(this);
    }

    /**
     * If the correct button, from the "drawing rectangle" mode
     */
    @Override
    public void mousePressed(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
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
            if (!lassoMode) {
                paintRect();
            }
        }

        if (buttonPressed == MouseEvent.BUTTON1_DOWN_MASK) {
            mousePos = e.getPoint();
            if (lassoMode) {
                paintLasso();
            } else {
                paintRect();
            }
        } else if (buttonPressed == (MouseEvent.BUTTON1_DOWN_MASK | MouseEvent.BUTTON3_DOWN_MASK)) {
            mousePosStart.x += e.getX()-mousePos.x;
            mousePosStart.y += e.getY()-mousePos.y;
            mousePos = e.getPoint();
            paintRect();
        }
    }

    /**
     * Check the state of the keys and buttons and set the selection accordingly.
     */
    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.getButton() != MouseEvent.BUTTON1)
            return;
        if (mousePos == null || mousePosStart == null)
            return; // injected release from outside
        // disable the selection rect
        Rectangle r;
        if (!lassoMode) {
            nc.requestClearRect();
            r = getSelectionRectangle();

            lasso = rectToPolygon(r);
        } else {
            nc.requestClearPoly();
            lasso.addPoint(mousePos.x, mousePos.y);
            r = lasso.getBounds();
        }
        mousePosStart = null;
        mousePos = null;

        if ((e.getModifiersEx() & MouseEvent.BUTTON3_DOWN_MASK) == 0) {
            selectionEndedListener.selectionEnded(r, e);
        }
    }

    /**
     * Draws a selection rectangle on screen.
     */
    private void paintRect() {
        if (mousePos == null || mousePosStart == null || mousePos == mousePosStart)
            return;
        nc.requestPaintRect(getSelectionRectangle());
    }

    private void paintLasso() {
        if (mousePos == null || mousePosStart == null || mousePos == mousePosStart) {
            return;
        }
        lasso.addPoint(mousePos.x, mousePos.y);
        nc.requestPaintPoly(lasso);
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
            double aspectRatio = (double)nc.getWidth()/nc.getHeight();
            if ((double)w/h < aspectRatio) {
                int neww = (int)(h*aspectRatio);
                if (mousePos.x < mousePosStart.x) {
                    x += w - neww;
                }
                w = neww;
            } else {
                int newh = (int)(w/aspectRatio);
                if (mousePos.y < mousePosStart.y) {
                    y += h - newh;
                }
                h = newh;
            }
        }

        return new Rectangle(x,y,w,h);
    }

    /**
     * If the action goes inactive, remove the selection rectangle from screen
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals("active") && !(Boolean)evt.getNewValue() && mousePosStart != null) {
            paintRect();
            mousePosStart = null;
            mousePos = null;
        }
    }

    /**
     * Return a list of all objects in the selection, respecting the different
     * modifier.
     *
     * @param alt Whether the alt key was pressed, which means select all
     * objects that are touched, instead those which are completely covered.
     * @return The collection of selected objects.
     */
    public Collection<OsmPrimitive> getSelectedObjects(boolean alt) {

        Collection<OsmPrimitive> selection = new LinkedList<OsmPrimitive>();

        // whether user only clicked, not dragged.
        boolean clicked = false;
        Rectangle bounding = lasso.getBounds();
        if (bounding.height <= 2 && bounding.width <= 2) {
            clicked = true;
        }

        if (clicked) {
            Point center = new Point(lasso.xpoints[0], lasso.ypoints[0]);
            OsmPrimitive osm = nc.getNearestNodeOrWay(center, OsmPrimitive.isSelectablePredicate, false);
            if (osm != null) {
                selection.add(osm);
            }
        } else {
            // nodes
            for (Node n : nc.getCurrentDataSet().getNodes()) {
                if (n.isSelectable() && lasso.contains(nc.getPoint2D(n))) {
                    selection.add(n);
                }
            }

            // ways
            for (Way w : nc.getCurrentDataSet().getWays()) {
                if (!w.isSelectable() || w.getNodesCount() == 0) {
                    continue;
                }
                if (alt) {
                    for (Node n : w.getNodes()) {
                        if (!n.isIncomplete() && lasso.contains(nc.getPoint2D(n))) {
                            selection.add(w);
                            break;
                        }
                    }
                } else {
                    boolean allIn = true;
                    for (Node n : w.getNodes()) {
                        if (!n.isIncomplete() && !lasso.contains(nc.getPoint(n))) {
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

    private Polygon rectToPolygon(Rectangle r) {
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
    public void mouseClicked(MouseEvent e) {}
    @Override
    public void mouseEntered(MouseEvent e) {}
    @Override
    public void mouseExited(MouseEvent e) {}
    @Override
    public void mouseMoved(MouseEvent e) {}
}
