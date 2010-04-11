// License: GPL. See LICENSE file for details.
package org.openstreetmap.josm.actions.mapmode;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.AWTEvent;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.EventQueue;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.AddCommand;
import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.SelectionChangedListener;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.WaySegment;
import org.openstreetmap.josm.data.osm.visitor.paint.PaintColors;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.MapViewPaintable;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Pair;
import org.openstreetmap.josm.tools.Shortcut;

/**
 *
 */
public class DrawAction extends MapMode implements MapViewPaintable, SelectionChangedListener, AWTEventListener {
    //static private final Logger logger = Logger.getLogger(DrawAction.class.getName());

    final private Cursor cursorCrosshair;
    final private Cursor cursorJoinNode;
    final private Cursor cursorJoinWay;
    enum Cursors { crosshair, node, way }
    private Cursors currCursor = Cursors.crosshair;

    private Node lastUsedNode = null;
    private double PHI=Math.toRadians(90);

    private boolean ctrl;
    private boolean alt;
    private boolean shift;
    private Node mouseOnExistingNode;
    private Set<Way> mouseOnExistingWays = new HashSet<Way>();
    private Set<OsmPrimitive> oldHighlights = new HashSet<OsmPrimitive>();
    private boolean drawHelperLine;
    private boolean wayIsFinished = false;
    private boolean drawTargetHighlight;
    private boolean drawTargetCursor;
    private Point mousePos;
    private Point oldMousePos;
    private Color selectedColor;

    private Node currentBaseNode;
    private EastNorth currentMouseEastNorth;

    public DrawAction(MapFrame mapFrame) {
        super(tr("Draw"), "node/autonode", tr("Draw nodes"),
                Shortcut.registerShortcut("mapmode:draw", tr("Mode: {0}", tr("Draw")), KeyEvent.VK_A, Shortcut.GROUP_EDIT),
                mapFrame, getCursor());

        // Add extra shortcut N
        Main.contentPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                Shortcut.registerShortcut("mapmode:drawfocus", tr("Mode: Draw Focus"), KeyEvent.VK_N, Shortcut.GROUP_EDIT).getKeyStroke(), tr("Draw"));

        cursorCrosshair = getCursor();
        cursorJoinNode = ImageProvider.getCursor("crosshair", "joinnode");
        cursorJoinWay = ImageProvider.getCursor("crosshair", "joinway");
    }

    private static Cursor getCursor() {
        try {
            return ImageProvider.getCursor("crosshair", null);
        } catch (Exception e) {
        }
        return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
    }

    /**
     * Displays the given cursor instead of the normal one
     * @param Cursors One of the available cursors
     */
    private void setCursor(final Cursors c) {
        if(currCursor.equals(c) || (!drawTargetCursor && currCursor.equals(Cursors.crosshair)))
            return;
        // We invoke this to prevent strange things from happening
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                // Don't change cursor when mode has changed already
                if(!(Main.map.mapMode instanceof DrawAction))
                    return;
                switch(c) {
                case way:
                    Main.map.mapView.setCursor(cursorJoinWay);
                    break;
                case node:
                    Main.map.mapView.setCursor(cursorJoinNode);
                    break;
                default:
                    Main.map.mapView.setCursor(cursorCrosshair);
                    break;
                }
            }
        });
        currCursor = c;
    }

    /**
     * Checks if a map redraw is required and does so if needed. Also updates the status bar
     */
    private void redrawIfRequired() {
        updateStatusLine();
        if ((!drawHelperLine || wayIsFinished) && !drawTargetHighlight) return;
        Main.map.mapView.repaint();
    }

    /**
     * Takes the data from computeHelperLine to determine which ways/nodes should be highlighted
     * (if feature enabled). Also sets the target cursor if appropriate.
     */
    private void addHighlighting() {
        removeHighlighting();
        // if ctrl key is held ("no join"), don't highlight anything
        if (ctrl) {
            setCursor(Cursors.crosshair);
            return;
        }

        // This happens when nothing is selected, but we still want to highlight the "target node"
        if (mouseOnExistingNode == null && getCurrentDataSet().getSelected().size() == 0
                && mousePos != null) {
            mouseOnExistingNode = Main.map.mapView.getNearestNode(mousePos, OsmPrimitive.isSelectablePredicate);
        }

        if (mouseOnExistingNode != null) {
            setCursor(Cursors.node);
            // We also need this list for the statusbar help text
            oldHighlights.add(mouseOnExistingNode);
            if(drawTargetHighlight) {
                mouseOnExistingNode.setHighlighted(true);
            }
            return;
        }

        // Insert the node into all the nearby way segments
        if (mouseOnExistingWays.size() == 0) {
            setCursor(Cursors.crosshair);
            return;
        }

        setCursor(Cursors.way);

        // We also need this list for the statusbar help text
        oldHighlights.addAll(mouseOnExistingWays);
        if (!drawTargetHighlight) return;
        for (Way w : mouseOnExistingWays) {
            w.setHighlighted(true);
        }
    }

    /**
     * Removes target highlighting from primitives
     */
    private void removeHighlighting() {
        for(OsmPrimitive prim : oldHighlights) {
            prim.setHighlighted(false);
        }
        oldHighlights = new HashSet<OsmPrimitive>();
    }

    @Override public void enterMode() {
        if (!isEnabled())
            return;
        super.enterMode();
        currCursor = Cursors.crosshair;
        selectedColor =PaintColors.SELECTED.get();
        drawHelperLine = Main.pref.getBoolean("draw.helper-line", true);
        drawTargetHighlight = Main.pref.getBoolean("draw.target-highlight", true);
        drawTargetCursor = Main.pref.getBoolean("draw.target-cursor", true);
        wayIsFinished = false;

        Main.map.mapView.addMouseListener(this);
        Main.map.mapView.addMouseMotionListener(this);
        Main.map.mapView.addTemporaryLayer(this);
        DataSet.selListeners.add(this);

        try {
            Toolkit.getDefaultToolkit().addAWTEventListener(this, AWTEvent.KEY_EVENT_MASK);
        } catch (SecurityException ex) {
        }
        // would like to but haven't got mouse position yet:
        // computeHelperLine(false, false, false);
    }

    @Override public void exitMode() {
        super.exitMode();
        Main.map.mapView.removeMouseListener(this);
        Main.map.mapView.removeMouseMotionListener(this);
        Main.map.mapView.removeTemporaryLayer(this);
        DataSet.selListeners.remove(this);
        removeHighlighting();
        try {
            Toolkit.getDefaultToolkit().removeAWTEventListener(this);
        } catch (SecurityException ex) {
        }

        // when exiting we let everybody know about the currently selected
        // primitives
        //
        getCurrentDataSet().fireSelectionChanged();
    }

    /**
     * redraw to (possibly) get rid of helper line if selection changes.
     */
    public void eventDispatched(AWTEvent event) {
        if(Main.map == null || Main.map.mapView == null || !Main.map.mapView.isActiveLayerDrawable())
            return;
        updateKeyModifiers((InputEvent) event);
        computeHelperLine();
        addHighlighting();
        redrawIfRequired();
    }
    /**
     * redraw to (possibly) get rid of helper line if selection changes.
     */
    public void selectionChanged(Collection<? extends OsmPrimitive> newSelection) {
        if(!Main.map.mapView.isActiveLayerDrawable())
            return;
        computeHelperLine();
        addHighlighting();
        redrawIfRequired();
    }

    private void tryAgain(MouseEvent e) {
        getCurrentDataSet().setSelected();
        mouseReleased(e);
    }

    /**
     * This function should be called when the user wishes to finish his current draw action.
     * If Potlatch Style is enabled, it will switch to select tool, otherwise simply disable
     * the helper line until the user chooses to draw something else.
     */
    private void finishDrawing() {
        // let everybody else know about the current selection
        //
        Main.main.getCurrentDataSet().fireSelectionChanged();
        lastUsedNode = null;
        wayIsFinished = true;
        Main.map.selectSelectTool(true);

        // Redraw to remove the helper line stub
        computeHelperLine();
        removeHighlighting();
        redrawIfRequired();
    }

    /**
     * If user clicked with the left button, add a node at the current mouse
     * position.
     *
     * If in nodeway mode, insert the node into the way.
     */
    @Override public void mouseReleased(MouseEvent e) {
        if (e.getButton() != MouseEvent.BUTTON1)
            return;
        if(!Main.map.mapView.isActiveLayerDrawable())
            return;
        // request focus in order to enable the expected keyboard shortcuts
        //
        Main.map.mapView.requestFocus();

        if(e.getClickCount() > 1 && mousePos != null && mousePos.equals(oldMousePos)) {
            // A double click equals "user clicked last node again, finish way"
            // Change draw tool only if mouse position is nearly the same, as
            // otherwise fast clicks will count as a double click
            finishDrawing();
            return;
        }
        oldMousePos = mousePos;

        // we copy ctrl/alt/shift from the event just in case our global
        // AWTEvent didn't make it through the security manager. Unclear
        // if that can ever happen but better be safe.
        updateKeyModifiers(e);
        mousePos = e.getPoint();

        DataSet ds = getCurrentDataSet();
        Collection<OsmPrimitive> selection = new ArrayList<OsmPrimitive>(ds.getSelected());
        Collection<Command> cmds = new LinkedList<Command>();
        Collection<OsmPrimitive> newSelection = new LinkedList<OsmPrimitive>(ds.getSelected());

        ArrayList<Way> reuseWays = new ArrayList<Way>(),
        replacedWays = new ArrayList<Way>();
        boolean newNode = false;
        Node n = null;

        if (!ctrl) {
            n = Main.map.mapView.getNearestNode(mousePos, OsmPrimitive.isSelectablePredicate);
        }

        if (n != null) {
            // user clicked on node
            if (selection.isEmpty() || wayIsFinished) {
                // select the clicked node and do nothing else
                // (this is just a convenience option so that people don't
                // have to switch modes)
                newSelection.clear();
                newSelection.add(n);
                getCurrentDataSet().setSelected(n);
                // The user explicitly selected a node, so let him continue drawing
                wayIsFinished = false;
                return;
            }
        } else {
            // no node found in clicked area
            n = new Node(Main.map.mapView.getLatLon(e.getX(), e.getY()));
            if (n.getCoor().isOutSideWorld()) {
                JOptionPane.showMessageDialog(
                        Main.parent,
                        tr("Cannot add a node outside of the world."),
                        tr("Warning"),
                        JOptionPane.WARNING_MESSAGE
                );
                return;
            }
            newNode = true;

            cmds.add(new AddCommand(n));

            if (!ctrl) {
                // Insert the node into all the nearby way segments
                List<WaySegment> wss = Main.map.mapView.getNearestWaySegments(e.getPoint(), OsmPrimitive.isSelectablePredicate);
                Map<Way, List<Integer>> insertPoints = new HashMap<Way, List<Integer>>();
                for (WaySegment ws : wss) {
                    List<Integer> is;
                    if (insertPoints.containsKey(ws.way)) {
                        is = insertPoints.get(ws.way);
                    } else {
                        is = new ArrayList<Integer>();
                        insertPoints.put(ws.way, is);
                    }

                    is.add(ws.lowerIndex);
                }

                Set<Pair<Node,Node>> segSet = new HashSet<Pair<Node,Node>>();

                for (Map.Entry<Way, List<Integer>> insertPoint : insertPoints.entrySet()) {
                    Way w = insertPoint.getKey();
                    List<Integer> is = insertPoint.getValue();

                    Way wnew = new Way(w);

                    pruneSuccsAndReverse(is);
                    for (int i : is) {
                        segSet.add(
                                Pair.sort(new Pair<Node,Node>(w.getNode(i), w.getNode(i+1))));
                    }
                    for (int i : is) {
                        wnew.addNode(i + 1, n);
                    }

                    // If ALT is pressed, a new way should be created and that new way should get
                    // selected. This works everytime unless the ways the nodes get inserted into
                    // are already selected. This is the case when creating a self-overlapping way
                    // but pressing ALT prevents this. Therefore we must de-select the way manually
                    // here so /only/ the new way will be selected after this method finishes.
                    if(alt) {
                        newSelection.add(insertPoint.getKey());
                    }

                    cmds.add(new ChangeCommand(insertPoint.getKey(), wnew));
                    replacedWays.add(insertPoint.getKey());
                    reuseWays.add(wnew);
                }

                adjustNode(segSet, n);
            }
        }

        // This part decides whether or not a "segment" (i.e. a connection) is made to an
        // existing node.

        // For a connection to be made, the user must either have a node selected (connection
        // is made to that node), or he must have a way selected *and* one of the endpoints
        // of that way must be the last used node (connection is made to last used node), or
        // he must have a way and a node selected (connection is made to the selected node).

        // If the above does not apply, the selection is cleared and a new try is started

        boolean extendedWay = false;
        boolean wayIsFinishedTemp = wayIsFinished;
        wayIsFinished = false;

        // don't draw lines if shift is held
        if (selection.size() > 0 && !shift) {
            Node selectedNode = null;
            Way selectedWay = null;

            for (OsmPrimitive p : selection) {
                if (p instanceof Node) {
                    if (selectedNode != null) {
                        // Too many nodes selected to do something useful
                        tryAgain(e);
                        return;
                    }
                    selectedNode = (Node) p;
                } else if (p instanceof Way) {
                    if (selectedWay != null) {
                        // Too many ways selected to do something useful
                        tryAgain(e);
                        return;
                    }
                    selectedWay = (Way) p;
                }
            }

            // the node from which we make a connection
            Node n0 = findNodeToContinueFrom(selectedNode, selectedWay);
            // We have a selection but it isn't suitable. Try again.
            if(n0 == null) {
                tryAgain(e);
                return;
            }
            if(!wayIsFinishedTemp){
                if(isSelfContainedWay(selectedWay, n0, n))
                    return;

                // User clicked last node again, finish way
                if(n0 == n) {
                    finishDrawing();
                    return;
                }

                // Ok we know now that we'll insert a line segment, but will it connect to an
                // existing way or make a new way of its own? The "alt" modifier means that the
                // user wants a new way.
                Way way = alt ? null : (selectedWay != null) ? selectedWay : getWayForNode(n0);
                Way wayToSelect;

                // Don't allow creation of self-overlapping ways
                if(way != null) {
                    int nodeCount=0;
                    for (Node p : way.getNodes())
                        if(p.equals(n0)) {
                            nodeCount++;
                        }
                    if(nodeCount > 1) {
                        way = null;
                    }
                }

                if (way == null) {
                    way = new Way();
                    way.addNode(n0);
                    cmds.add(new AddCommand(way));
                    wayToSelect = way;
                } else {
                    int i;
                    if ((i = replacedWays.indexOf(way)) != -1) {
                        way = reuseWays.get(i);
                        wayToSelect = way;
                    } else {
                        wayToSelect = way;
                        Way wnew = new Way(way);
                        cmds.add(new ChangeCommand(way, wnew));
                        way = wnew;
                    }
                }

                // Connected to a node that's already in the way
                if(way.containsNode(n)) {
                    wayIsFinished = true;
                    selection.clear();
                }

                // Add new node to way
                if (way.getNode(way.getNodesCount() - 1) == n0) {
                    way.addNode(n);
                } else {
                    way.addNode(0, n);
                }

                extendedWay = true;
                newSelection.clear();
                newSelection.add(wayToSelect);
            }
        }

        String title;
        if (!extendedWay) {
            if (!newNode)
                return; // We didn't do anything.
            else if (reuseWays.isEmpty()) {
                title = tr("Add node");
            } else {
                title = tr("Add node into way");
                for (Way w : reuseWays) {
                    newSelection.remove(w);
                }
            }
            newSelection.clear();
            newSelection.add(n);
        } else if (!newNode) {
            title = tr("Connect existing way to node");
        } else if (reuseWays.isEmpty()) {
            title = tr("Add a new node to an existing way");
        } else {
            title = tr("Add node into way and connect");
        }

        Command c = new SequenceCommand(title, cmds);

        Main.main.undoRedo.add(c);
        if(!wayIsFinished) {
            lastUsedNode = n;
        }

        getCurrentDataSet().setSelected(newSelection);

        computeHelperLine();
        removeHighlighting();
        redrawIfRequired();
    }

    /**
     * Prevent creation of ways that look like this: <---->
     * This happens if users want to draw a no-exit-sideway from the main way like this:
     * ^
     * |<---->
     * |
     * The solution isn't ideal because the main way will end in the side way, which is bad for
     * navigation software ("drive straight on") but at least easier to fix. Maybe users will fix
     * it on their own, too. At least it's better than producing an error.
     *
     * @param Way the way to check
     * @param Node the current node (i.e. the one the connection will be made from)
     * @param Node the target node (i.e. the one the connection will be made to)
     * @return Boolean True if this would create a selfcontaining way, false otherwise.
     */
    private boolean isSelfContainedWay(Way selectedWay, Node currentNode, Node targetNode) {
        if(selectedWay != null) {
            int posn0 = selectedWay.getNodes().indexOf(currentNode);
            if( posn0 != -1 && // n0 is part of way
                    (posn0 >= 1                             && targetNode.equals(selectedWay.getNode(posn0-1))) || // previous node
                    (posn0 < selectedWay.getNodesCount()-1) && targetNode.equals(selectedWay.getNode(posn0+1))) {  // next node
                getCurrentDataSet().setSelected(targetNode);
                lastUsedNode = targetNode;
                return true;
            }
        }

        return false;
    }

    /**
     * Finds a node to continue drawing from. Decision is based upon given node and way.
     * @param selectedNode Currently selected node, may be null
     * @param selectedWay Currently selected way, may be null
     * @return Node if a suitable node is found, null otherwise
     */
    private Node findNodeToContinueFrom(Node selectedNode, Way selectedWay) {
        // No nodes or ways have been selected, this occurs when a relation
        // has been selected or the selection is empty
        if(selectedNode == null && selectedWay == null)
            return null;

        if (selectedNode == null) {
            if (selectedWay.isFirstLastNode(lastUsedNode))
                return lastUsedNode;

            // We have a way selected, but no suitable node to continue from. Start anew.
            return null;
        }

        if (selectedWay == null)
            return selectedNode;

        if (selectedWay.isFirstLastNode(selectedNode))
            return selectedNode;

        // We have a way and node selected, but it's not at the start/end of the way. Start anew.
        return null;
    }

    @Override public void mouseDragged(MouseEvent e) {
        mouseMoved(e);
    }

    @Override public void mouseMoved(MouseEvent e) {
        if(!Main.map.mapView.isActiveLayerDrawable())
            return;

        // we copy ctrl/alt/shift from the event just in case our global
        // AWTEvent didn't make it through the security manager. Unclear
        // if that can ever happen but better be safe.
        updateKeyModifiers(e);
        mousePos = e.getPoint();

        computeHelperLine();
        addHighlighting();
        redrawIfRequired();
    }

    private void updateKeyModifiers(InputEvent e) {
        ctrl = (e.getModifiers() & ActionEvent.CTRL_MASK) != 0;
        alt = (e.getModifiers() & (ActionEvent.ALT_MASK|InputEvent.ALT_GRAPH_MASK)) != 0;
        shift = (e.getModifiers() & ActionEvent.SHIFT_MASK) != 0;
    }

    private void updateKeyModifiers(MouseEvent e) {
        ctrl = (e.getModifiers() & ActionEvent.CTRL_MASK) != 0;
        alt = (e.getModifiers() & (ActionEvent.ALT_MASK|InputEvent.ALT_GRAPH_MASK)) != 0;
        shift = (e.getModifiers() & ActionEvent.SHIFT_MASK) != 0;
    }

    /**
     * This method prepares data required for painting the "helper line" from
     * the last used position to the mouse cursor. It duplicates some code from
     * mouseReleased() (FIXME).
     */
    private void computeHelperLine() {
        MapView mv = Main.map.mapView;
        if (mousePos == null) {
            // Don't draw the line.
            currentMouseEastNorth = null;
            currentBaseNode = null;
            return;
        }

        double distance = -1;
        double angle = -1;

        Collection<OsmPrimitive> selection = getCurrentDataSet().getSelected();

        Node selectedNode = null;
        Way selectedWay = null;
        Node currentMouseNode = null;
        mouseOnExistingNode = null;
        mouseOnExistingWays = new HashSet<Way>();

        Main.map.statusLine.setAngle(-1);
        Main.map.statusLine.setHeading(-1);
        Main.map.statusLine.setDist(-1);

        if (!ctrl && mousePos != null) {
            currentMouseNode = mv.getNearestNode(mousePos, OsmPrimitive.isSelectablePredicate);
        }

        // We need this for highlighting and we'll only do so if we actually want to re-use
        // *and* there is no node nearby (because nodes beat ways when re-using)
        if(!ctrl && currentMouseNode == null) {
            List<WaySegment> wss = mv.getNearestWaySegments(mousePos, OsmPrimitive.isSelectablePredicate);
            for(WaySegment ws : wss) {
                mouseOnExistingWays.add(ws.way);
            }
        }

        if (currentMouseNode != null) {
            // user clicked on node
            if (selection.isEmpty()) return;
            currentMouseEastNorth = currentMouseNode.getEastNorth();
            mouseOnExistingNode = currentMouseNode;
        } else {
            // no node found in clicked area
            currentMouseEastNorth = mv.getEastNorth(mousePos.x, mousePos.y);
        }

        for (OsmPrimitive p : selection) {
            if (p instanceof Node) {
                if (selectedNode != null) return;
                selectedNode = (Node) p;
            } else if (p instanceof Way) {
                if (selectedWay != null) return;
                selectedWay = (Way) p;
            }
        }

        // the node from which we make a connection
        currentBaseNode = null;
        Node previousNode = null;

        if (selectedNode == null) {
            if (selectedWay == null)
                return;
            if (selectedWay.isFirstLastNode(lastUsedNode)) {
                currentBaseNode = lastUsedNode;
                if (lastUsedNode == selectedWay.getNode(selectedWay.getNodesCount()-1) && selectedWay.getNodesCount() > 1) {
                    previousNode = selectedWay.getNode(selectedWay.getNodesCount()-2);
                }
            }
        } else if (selectedWay == null) {
            currentBaseNode = selectedNode;
        } else {
            if (selectedNode == selectedWay.getNode(0) || selectedNode == selectedWay.getNode(selectedWay.getNodesCount()-1)) {
                currentBaseNode = selectedNode;
            }
        }

        if (currentBaseNode == null || currentBaseNode == currentMouseNode)
            return; // Don't create zero length way segments.

        // find out the distance, in metres, between the base point and the mouse cursor
        LatLon mouseLatLon = mv.getProjection().eastNorth2latlon(currentMouseEastNorth);
        distance = currentBaseNode.getCoor().greatCircleDistance(mouseLatLon);

        double hdg = Math.toDegrees(currentBaseNode.getEastNorth()
                .heading(currentMouseEastNorth));
        if (previousNode != null) {
            angle = hdg - Math.toDegrees(previousNode.getEastNorth()
                    .heading(currentBaseNode.getEastNorth()));
            angle += angle < 0 ? 360 : 0;
        }

        Main.map.statusLine.setAngle(angle);
        Main.map.statusLine.setHeading(hdg);
        Main.map.statusLine.setDist(distance);
        // Now done in redrawIfRequired()
        //updateStatusLine();
    }

    /**
     * Repaint on mouse exit so that the helper line goes away.
     */
    @Override public void mouseExited(MouseEvent e) {
        if(!Main.map.mapView.isActiveLayerDrawable())
            return;
        mousePos = e.getPoint();
        Main.map.mapView.repaint();
    }

    /**
     * @return If the node is the end of exactly one way, return this.
     *  <code>null</code> otherwise.
     */
    public Way getWayForNode(Node n) {
        Way way = null;
        for (Way w : OsmPrimitive.getFilteredList(n.getReferrers(), Way.class)) {
            if (!w.isUsable() || w.getNodesCount() < 1) {
                continue;
            }
            Node firstNode = w.getNode(0);
            Node lastNode = w.getNode(w.getNodesCount() - 1);
            if ((firstNode == n || lastNode == n) && (firstNode != lastNode)) {
                if (way != null)
                    return null;
                way = w;
            }
        }
        return way;
    }

    private static void pruneSuccsAndReverse(List<Integer> is) {
        //if (is.size() < 2) return;

        HashSet<Integer> is2 = new HashSet<Integer>();
        for (int i : is) {
            if (!is2.contains(i - 1) && !is2.contains(i + 1)) {
                is2.add(i);
            }
        }
        is.clear();
        is.addAll(is2);
        Collections.sort(is);
        Collections.reverse(is);
    }

    /**
     * Adjusts the position of a node to lie on a segment (or a segment
     * intersection).
     *
     * If one or more than two segments are passed, the node is adjusted
     * to lie on the first segment that is passed.
     *
     * If two segments are passed, the node is adjusted to be at their
     * intersection.
     *
     * No action is taken if no segments are passed.
     *
     * @param segs the segments to use as a reference when adjusting
     * @param n the node to adjust
     */
    private static void adjustNode(Collection<Pair<Node,Node>> segs, Node n) {

        switch (segs.size()) {
        case 0:
            return;
        case 2:
            // This computes the intersection between
            // the two segments and adjusts the node position.
            Iterator<Pair<Node,Node>> i = segs.iterator();
            Pair<Node,Node> seg = i.next();
            EastNorth A = seg.a.getEastNorth();
            EastNorth B = seg.b.getEastNorth();
            seg = i.next();
            EastNorth C = seg.a.getEastNorth();
            EastNorth D = seg.b.getEastNorth();

            double u=det(B.east() - A.east(), B.north() - A.north(), C.east() - D.east(), C.north() - D.north());

            // Check for parallel segments and do nothing if they are
            // In practice this will probably only happen when a way has been duplicated

            if (u == 0) return;

            // q is a number between 0 and 1
            // It is the point in the segment where the intersection occurs
            // if the segment is scaled to lenght 1

            double q = det(B.north() - C.north(), B.east() - C.east(), D.north() - C.north(), D.east() - C.east()) / u;
            EastNorth intersection = new EastNorth(
                    B.east() + q * (A.east() - B.east()),
                    B.north() + q * (A.north() - B.north()));

            int snapToIntersectionThreshold
            = Main.pref.getInteger("edit.snap-intersection-threshold",10);

            // only adjust to intersection if within snapToIntersectionThreshold pixel of mouse click; otherwise
            // fall through to default action.
            // (for semi-parallel lines, intersection might be miles away!)
            if (Main.map.mapView.getPoint(n).distance(Main.map.mapView.getPoint(intersection)) < snapToIntersectionThreshold) {
                n.setEastNorth(intersection);
                return;
            }

        default:
            EastNorth P = n.getEastNorth();
            seg = segs.iterator().next();
            A = seg.a.getEastNorth();
            B = seg.b.getEastNorth();
            double a = P.distanceSq(B);
            double b = P.distanceSq(A);
            double c = A.distanceSq(B);
            q = (a - b + c) / (2*c);
            n.setEastNorth(new EastNorth(B.east() + q * (A.east() - B.east()), B.north() + q * (A.north() - B.north())));
        }
    }

    // helper for adjustNode
    static double det(double a, double b, double c, double d) {
        return a * d - b * c;
    }

    public void paint(Graphics2D g, MapView mv, Bounds box) {
        if (!drawHelperLine || wayIsFinished || shift) return;

        // sanity checks
        if (Main.map.mapView == null) return;
        if (mousePos == null) return;

        // don't draw line if we don't know where from or where to
        if (currentBaseNode == null || currentMouseEastNorth == null) return;

        // don't draw line if mouse is outside window
        if (!Main.map.mapView.getBounds().contains(mousePos)) return;

        Graphics2D g2 = g;
        g2.setColor(selectedColor);
        g2.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        GeneralPath b = new GeneralPath();
        Point p1=mv.getPoint(currentBaseNode);
        Point p2=mv.getPoint(currentMouseEastNorth);

        double t = Math.atan2(p2.y-p1.y, p2.x-p1.x) + Math.PI;

        b.moveTo(p1.x,p1.y); b.lineTo(p2.x, p2.y);

        // if alt key is held ("start new way"), draw a little perpendicular line
        if (alt) {
            b.moveTo((int)(p1.x + 8*Math.cos(t+PHI)), (int)(p1.y + 8*Math.sin(t+PHI)));
            b.lineTo((int)(p1.x + 8*Math.cos(t-PHI)), (int)(p1.y + 8*Math.sin(t-PHI)));
        }

        g2.draw(b);
        g2.setStroke(new BasicStroke(1));
    }

    @Override public String getModeHelpText() {
        String rv = "";
        /*
         *  No modifiers: all (Connect, Node Re-Use, Auto-Weld)
         *  CTRL: disables node re-use, auto-weld
         *  Shift: do not make connection
         *  ALT: make connection but start new way in doing so
         */

        /*
         * Status line text generation is split into two parts to keep it maintainable.
         * First part looks at what will happen to the new node inserted on click and
         * the second part will look if a connection is made or not.
         *
         * Note that this help text is not absolutely accurate as it doesn't catch any special
         * cases (e.g. when preventing <---> ways). The only special that it catches is when
         * a way is about to be finished.
         *
         * First check what happens to the new node.
         */

        // oldHighlights stores the current highlights. If this
        // list is empty we can assume that we won't do any joins
        if (ctrl || oldHighlights.isEmpty()) {
            rv = tr("Create new node.");
        } else {
            // oldHighlights may store a node or way, check if it's a node
            OsmPrimitive x = oldHighlights.iterator().next();
            if (x instanceof Node) {
                rv = tr("Select node under cursor.");
            } else {
                rv = trn("Insert new node into way.", "Insert new node into {0} ways.",
                        oldHighlights.size(), oldHighlights.size());
            }
        }

        /*
         * Check whether a connection will be made
         */
        if (currentBaseNode != null && !wayIsFinished) {
            if (alt) {
                rv += " " + tr("Start new way from last node.");
            } else {
                rv += " " + tr("Continue way from last node.");
            }
        }

        Node n = mouseOnExistingNode;
        /*
         * Handle special case: Highlighted node == selected node => finish drawing
         */
        if (n != null && getCurrentDataSet() != null && getCurrentDataSet().getSelectedNodes().contains(n)) {
            if (wayIsFinished) {
                rv = tr("Select node under cursor.");
            } else {
                rv = tr("Finish drawing.");
            }
        }

        /*
         * Handle special case: Self-Overlapping or closing way
         */
        if (getCurrentDataSet() != null && getCurrentDataSet().getSelectedWays().size() > 0 && !wayIsFinished && !alt) {
            Way w = getCurrentDataSet().getSelectedWays().iterator().next();
            for (Node m : w.getNodes()) {
                if (m.equals(mouseOnExistingNode) || mouseOnExistingWays.contains(w)) {
                    rv += " " + tr("Finish drawing.");
                    break;
                }
            }
        }
        return rv;
    }

    @Override public boolean layerIsSupported(Layer l) {
        return l instanceof OsmDataLayer;
    }

    @Override
    protected void updateEnabledState() {
        setEnabled(getEditLayer() != null);
    }
}
