// License: GPL. See LICENSE file for details.
package org.openstreetmap.josm.actions.mapmode;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.AWTEvent;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
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
import org.openstreetmap.josm.data.SelectionChangedListener;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.WaySegment;
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

    private static Node lastUsedNode = null;
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
    }

    private static Cursor getCursor() {
        try {
            return ImageProvider.getCursor("crosshair", null);
        } catch (Exception e) {
        }
        return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
    }

    /**
     * Sets a special cursor to indicate that the next click will automatically join into
     * an existing node or way (if feature is enabled)
     *
     * @param way If true, the cursor will indicate it is joined into a way; node otherwise
     */
    private void setJoinCursor(boolean way) {
        if(!drawTargetCursor) {
            resetCursor();
            return;
        }
        try {
            Main.map.mapView.setCursor(
                    ImageProvider.getCursor("crosshair", (way ? "joinway" : "joinnode"))
            );
            return;
        } catch (Exception e) {
            e.printStackTrace();
        }
        resetCursor();
    }

    /**
     * Resets the cursor to the default cursor for drawing mode (i.e. crosshair)
     */
    private void resetCursor() {
        Main.map.mapView.setCursor(getCursor());
    }

    /**
     * Takes the data from computeHelperLine to determine which ways/nodes should be highlighted
     * (if feature enabled). Also sets the target cursor if appropriate.
     */
    private void addHighlighting() {
        // if ctrl key is held ("no join"), don't highlight anything
        if (ctrl) {
            removeHighlighting();
            resetCursor();
            return;
        }

        if (mouseOnExistingNode != null) {
            setJoinCursor(false);
            // Clean old highlights
            removeHighlighting();
            if(drawTargetHighlight) {
                oldHighlights.add(mouseOnExistingNode);
                mouseOnExistingNode.highlighted = true;
            }
            return;
        }

        // Insert the node into all the nearby way segments
        if(mouseOnExistingWays.size() == 0) {
            removeHighlighting();
            resetCursor();
            return;
        }

        setJoinCursor(true);
        // Clean old highlights
        removeHighlighting();

        if(!drawTargetHighlight) return;
        oldHighlights.addAll(mouseOnExistingWays);
        for(Way w : mouseOnExistingWays) {
            w.highlighted = true;
        }
    }

    /**
     * Removes target highlighting from primitives
     */
    private void removeHighlighting() {
        for(OsmPrimitive prim : oldHighlights) {
            prim.highlighted = false;
        }
    }

    @Override public void enterMode() {
        super.enterMode();
        selectedColor = Main.pref.getColor(marktr("selected"), Color.red);
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
    }

    /**
     * redraw to (possibly) get rid of helper line if selection changes.
     */
    public void eventDispatched(AWTEvent event) {
        if(!Main.map.mapView.isDrawableLayer())
            return;
        InputEvent e = (InputEvent) event;
        ctrl = (e.getModifiers() & ActionEvent.CTRL_MASK) != 0;
        alt = (e.getModifiers() & ActionEvent.ALT_MASK) != 0;
        shift = (e.getModifiers() & ActionEvent.SHIFT_MASK) != 0;
        computeHelperLine();
    }
    /**
     * redraw to (possibly) get rid of helper line if selection changes.
     */
    public void selectionChanged(Collection<? extends OsmPrimitive> newSelection) {
        if(!Main.map.mapView.isDrawableLayer())
            return;
        computeHelperLine();
    }

    private void tryAgain(MouseEvent e) {
        Main.ds.setSelected();
        mouseClicked(e);
    }

    /**
     * If user clicked with the left button, add a node at the current mouse
     * position.
     *
     * If in nodeway mode, insert the node into the way.
     */
    @Override public void mouseClicked(MouseEvent e) {
        if (e.getButton() != MouseEvent.BUTTON1)
            return;
        if(!Main.map.mapView.isDrawableLayer())
            return;
        
        if(e.getClickCount() > 1 && mousePos != null && mousePos.equals(oldMousePos)) {
            // A double click equals "user clicked last node again, finish way"
            // Change draw tool only if mouse position is nearly the same, as
            // otherwise fast clicks will count as a double click
            lastUsedNode = null;
            wayIsFinished = true;

            Main.map.selectSelectTool(true);
            return;
        }
        oldMousePos = mousePos;
        
        // we copy ctrl/alt/shift from the event just in case our global
        // AWTEvent didn't make it through the security manager. Unclear
        // if that can ever happen but better be safe.
        ctrl = (e.getModifiers() & ActionEvent.CTRL_MASK) != 0;
        alt = (e.getModifiers() & ActionEvent.ALT_MASK) != 0;
        shift = (e.getModifiers() & ActionEvent.SHIFT_MASK) != 0;
        mousePos = e.getPoint();

        Collection<OsmPrimitive> selection = Main.ds.getSelected();
        Collection<Command> cmds = new LinkedList<Command>();

        ArrayList<Way> reuseWays = new ArrayList<Way>(),
            replacedWays = new ArrayList<Way>();
        boolean newNode = false;
        Node n = null;

        if (!ctrl)
            n = Main.map.mapView.getNearestNode(mousePos);

        if (n != null) {
            // user clicked on node
            if (shift || selection.isEmpty()) {
                // select the clicked node and do nothing else
                // (this is just a convenience option so that people don't
                // have to switch modes)
                Main.ds.setSelected(n);
                return;
            }
        } else {
            // no node found in clicked area
            n = new Node(Main.map.mapView.getLatLon(e.getX(), e.getY()));
            if (n.coor.isOutSideWorld()) {
                JOptionPane.showMessageDialog(Main.parent,
                    tr("Cannot add a node outside of the world."));
                return;
            }
            newNode = true;

            cmds.add(new AddCommand(n));

            if (!ctrl) {
                // Insert the node into all the nearby way segments
                List<WaySegment> wss = Main.map.mapView.getNearestWaySegments(e.getPoint());
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
                    for (int i : is) segSet.add(
                        Pair.sort(new Pair<Node,Node>(w.nodes.get(i), w.nodes.get(i+1))));
                    for (int i : is) wnew.addNode(i + 1, n);

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
        if (!shift && selection.size() > 0 && !wayIsFinishedTemp) {
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

            // No nodes or ways have been selected, try again with no selection
            // This occurs when a relation has been selected
            if(selectedNode == null && selectedWay == null) {
                tryAgain(e);
                return;
            }

            // the node from which we make a connection
            Node n0 = null;

            if (selectedNode == null) {
                if (selectedWay.isFirstLastNode(lastUsedNode)) {
                    n0 = lastUsedNode;
                } else {
                    // We have a way selected, but no suitable node to continue from. Start anew.
                    tryAgain(e);
                    return;
                }
            } else if (selectedWay == null) {
                n0 = selectedNode;
            } else {
                if (selectedWay.isFirstLastNode(selectedNode)) {
                    n0 = selectedNode;
                } else {
                    // We have a way and node selected, but it's not at the start/end of the way. Start anew.
                    tryAgain(e);
                    return;
                }
            }

            // Prevent creation of ways that look like this: <---->
            // This happens if users want to draw a no-exit-sideway from the main way like this:
            // ^
            // |<---->
            // |
            // The solution isn't ideal because the main way will end in the side way, which is bad for
            // navigation software ("drive straight on") but at least easier to fix. Maybe users will fix
            // it on their own, too. At least it's better than producing an error.
            if(selectedWay != null && selectedWay.nodes != null) {
                int posn0 = selectedWay.nodes.indexOf(n0);
                if( posn0 != -1 && // n0 is part of way
                    (posn0 >= 1                          && n.equals(selectedWay.nodes.get(posn0-1))) || // previous node
                    (posn0 < selectedWay.nodes.size()-1) && n.equals(selectedWay.nodes.get(posn0+1))) {  // next node
                    Main.ds.setSelected(n);
                    lastUsedNode = n;
                    return;
                }
            }

            // User clicked last node again, finish way
            if(n0 == n) {
                lastUsedNode = null;
                wayIsFinished = true;
                Main.map.selectSelectTool(true);
                return;
            }

            // Ok we know now that we'll insert a line segment, but will it connect to an
            // existing way or make a new way of its own? The "alt" modifier means that the
            // user wants a new way.
            Way way = alt ? null : (selectedWay != null) ? selectedWay : getWayForNode(n0);

            // Don't allow creation of self-overlapping ways
            if(way != null) {
                int nodeCount=0;
                for (Node p : way.nodes)
                    if(p.equals(n0)) nodeCount++;
                if(nodeCount > 1) way = null;
            }

            if (way == null) {
                way = new Way();
                way.addNode(n0);
                cmds.add(new AddCommand(way));
            } else {
                int i;
                if ((i = replacedWays.indexOf(way)) != -1) {
                    way = reuseWays.get(i);
                } else {
                    Way wnew = new Way(way);
                    cmds.add(new ChangeCommand(way, wnew));
                    way = wnew;
                }
            }

            // Connected to a node that's already in the way
            if(way.nodes.contains(n)) {
                //System.out.println("Stop drawing, node is part of current way");
                wayIsFinished = true;
                selection.clear();
            }

            // Add new node to way
            if (way.nodes.get(way.nodes.size() - 1) == n0)
                way.addNode(n);
            else
                way.addNode(0, n);

            extendedWay = true;
            Main.ds.setSelected(way);
        }

        String title;
        if (!extendedWay) {
            if (!newNode) {
                return; // We didn't do anything.
            } else if (reuseWays.isEmpty()) {
                title = tr("Add node");
            } else {
                title = tr("Add node into way");
                for (Way w : reuseWays) w.selected = false;
            }
            Main.ds.setSelected(n);
        } else if (!newNode) {
            title = tr("Connect existing way to node");
        } else if (reuseWays.isEmpty()) {
            title = tr("Add a new node to an existing way");
        } else {
            title = tr("Add node into way and connect");
        }

        Command c = new SequenceCommand(title, cmds);

        Main.main.undoRedo.add(c);
        if(!wayIsFinished) lastUsedNode = n;
        computeHelperLine();
        removeHighlighting();
        Main.map.mapView.repaint();
    }

    @Override public void mouseMoved(MouseEvent e) {
        if(!Main.map.mapView.isDrawableLayer())
            return;

        // we copy ctrl/alt/shift from the event just in case our global
        // AWTEvent didn't make it through the security manager. Unclear
        // if that can ever happen but better be safe.

        ctrl = (e.getModifiers() & ActionEvent.CTRL_MASK) != 0;
        alt = (e.getModifiers() & ActionEvent.ALT_MASK) != 0;
        shift = (e.getModifiers() & ActionEvent.SHIFT_MASK) != 0;
        mousePos = e.getPoint();

        addHighlighting();
        computeHelperLine();
    }

    /**
     * This method prepares data required for painting the "helper line" from
     * the last used position to the mouse cursor. It duplicates some code from
     * mouseClicked() (FIXME).
     */
    private void computeHelperLine() {
        if (mousePos == null) {
            // Don't draw the line.
            currentMouseEastNorth = null;
            currentBaseNode = null;
            return;
        }

        double distance = -1;
        double angle = -1;

        Collection<OsmPrimitive> selection = Main.ds.getSelected();

        Node selectedNode = null;
        Way selectedWay = null;
        Node currentMouseNode = null;
        mouseOnExistingNode = null;
        mouseOnExistingWays = new HashSet<Way>();

        Main.map.statusLine.setAngle(-1);
        Main.map.statusLine.setHeading(-1);
        Main.map.statusLine.setDist(-1);

        if (!ctrl && mousePos != null) {
            currentMouseNode = Main.map.mapView.getNearestNode(mousePos);
        }

        // We need this for highlighting and we'll only do so if we actually want to re-use
        // *and* there is no node nearby (because nodes beat ways when re-using)
        if(!ctrl && currentMouseNode == null) {
            List<WaySegment> wss = Main.map.mapView.getNearestWaySegments(mousePos);
            for(WaySegment ws : wss)
                mouseOnExistingWays.add(ws.way);
        }

        if (currentMouseNode != null) {
            // user clicked on node
            if (selection.isEmpty()) return;
            currentMouseEastNorth = currentMouseNode.eastNorth;
            mouseOnExistingNode = currentMouseNode;
        } else {
            // no node found in clicked area
            currentMouseEastNorth = Main.map.mapView.getEastNorth(mousePos.x, mousePos.y);
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
            if (selectedWay == null) return;
            if (lastUsedNode == selectedWay.nodes.get(0) || lastUsedNode == selectedWay.nodes.get(selectedWay.nodes.size()-1)) {
                currentBaseNode = lastUsedNode;
                if (lastUsedNode == selectedWay.nodes.get(selectedWay.nodes.size()-1) && selectedWay.nodes.size() > 1) {
                    previousNode = selectedWay.nodes.get(selectedWay.nodes.size()-2);
                }
            }
        } else if (selectedWay == null) {
            currentBaseNode = selectedNode;
        } else {
            if (selectedNode == selectedWay.nodes.get(0) || selectedNode == selectedWay.nodes.get(selectedWay.nodes.size()-1)) {
                currentBaseNode = selectedNode;
            }
        }

        if (currentBaseNode == null || currentBaseNode == currentMouseNode) {
            return; // Don't create zero length way segments.
        }

        // find out the distance, in metres, between the base point and the mouse cursor
        LatLon mouseLatLon = Main.proj.eastNorth2latlon(currentMouseEastNorth);
        distance = currentBaseNode.coor.greatCircleDistance(mouseLatLon);
        double hdg = Math.toDegrees(currentBaseNode.coor.heading(mouseLatLon));
        if (previousNode != null) {
            angle = hdg - Math.toDegrees(previousNode.coor.heading(currentBaseNode.coor));
            if (angle < 0) angle += 360;
        }
        Main.map.statusLine.setAngle(angle);
        Main.map.statusLine.setHeading(hdg);
        Main.map.statusLine.setDist(distance);
        updateStatusLine();

        if ((!drawHelperLine || wayIsFinished) && !drawTargetHighlight) return;
        Main.map.mapView.repaint();
    }

    /**
     * Repaint on mouse exit so that the helper line goes away.
     */
    @Override public void mouseExited(MouseEvent e) {
        if(!Main.map.mapView.isDrawableLayer())
            return;
        mousePos = e.getPoint();
        Main.map.mapView.repaint();
    }

    /**
     * @return If the node is the end of exactly one way, return this.
     *  <code>null</code> otherwise.
     */
    public static Way getWayForNode(Node n) {
        Way way = null;
        for (Way w : Main.ds.ways) {
            if (w.deleted || w.incomplete || w.nodes.size() < 1) continue;
            Node firstNode = w.nodes.get(0);
            Node lastNode = w.nodes.get(w.nodes.size() - 1);
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
            EastNorth A = seg.a.eastNorth;
            EastNorth B = seg.b.eastNorth;
            seg = i.next();
            EastNorth C = seg.a.eastNorth;
            EastNorth D = seg.b.eastNorth;

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
            if (Main.map.mapView.getPoint(n.eastNorth).distance(Main.map.mapView.getPoint(intersection)) < snapToIntersectionThreshold) {
                n.eastNorth = intersection;
                return;
            }

        default:
            EastNorth P = n.eastNorth;
            seg = segs.iterator().next();
            A = seg.a.eastNorth;
            B = seg.b.eastNorth;
            double a = P.distanceSq(B);
            double b = P.distanceSq(A);
            double c = A.distanceSq(B);
            q = (a - b + c) / (2*c);
            n.eastNorth = new EastNorth(
                B.east() + q * (A.east() - B.east()),
                B.north() + q * (A.north() - B.north()));
        }
    }

    // helper for adjustNode
    static double det(double a, double b, double c, double d)
    {
        return a * d - b * c;
    }

    public void paint(Graphics g, MapView mv) {
        if (!drawHelperLine && !drawTargetHighlight) return;

        // sanity checks
        if (Main.map.mapView == null) return;
        if (mousePos == null) return;

        // if shift key is held ("no auto-connect"), don't draw a line
        if (shift) return;

        // don't draw line if we don't know where from or where to
        if (currentBaseNode == null || currentMouseEastNorth == null) return;

        // don't draw line if mouse is outside window
        if (!Main.map.mapView.getBounds().contains(mousePos)) return;

        Graphics2D g2 = (Graphics2D) g;
        g2.setColor(selectedColor);
        g2.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        GeneralPath b = new GeneralPath();
        Point p1=mv.getPoint(currentBaseNode.eastNorth);
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
        String rv;

        if (currentBaseNode != null && !shift) {
            if (mouseOnExistingNode != null) {
                if (alt && /* FIXME: way exists */true)
                    rv = tr("Click to create a new way to the existing node.");
                else
                    rv =tr("Click to make a connection to the existing node.");
            } else {
                if (alt && /* FIXME: way exists */true)
                    rv = tr("Click to insert a node and create a new way.");
                else
                    rv = tr("Click to insert a new node and make a connection.");
            }
        }
        else {
            rv = tr("Click to insert a new node.");
        }

        //rv.append(tr("Click to add a new node. Ctrl: no node re-use/auto-insert. Shift: no auto-connect. Alt: new way"));
        //rv.append(tr("Click to add a new node. Ctrl: no node re-use/auto-insert. Shift: no auto-connect. Alt: new way"));
        return rv.toString();
    }

    @Override public boolean layerIsSupported(Layer l) {
        return l instanceof OsmDataLayer;
    }
}
