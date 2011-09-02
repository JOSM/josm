// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions.mapmode;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.AWTEvent;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.MergeNodesAction;
import org.openstreetmap.josm.command.AddCommand;
import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.MoveCommand;
import org.openstreetmap.josm.command.RotateCommand;
import org.openstreetmap.josm.command.ScaleCommand;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.WaySegment;
import org.openstreetmap.josm.data.osm.visitor.AllNodesVisitor;
import org.openstreetmap.josm.data.osm.visitor.paint.WireframeMapRenderer;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.SelectionManager;
import org.openstreetmap.josm.gui.SelectionManager.SelectionEnded;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Pair;
import org.openstreetmap.josm.tools.PlatformHookOsx;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Move is an action that can move all kind of OsmPrimitives (except keys for now).
 *
 * If an selected object is under the mouse when dragging, move all selected objects.
 * If an unselected object is under the mouse when dragging, it becomes selected
 * and will be moved.
 * If no object is under the mouse, move all selected objects (if any)
 *
 * @author imi
 */
public class SelectAction extends MapMode implements AWTEventListener, SelectionEnded {
    // "select" means the selection rectangle and "move" means either dragging
    // or select if no mouse movement occurs (i.e. just clicking)
    enum Mode { move, rotate, scale, select }

    // contains all possible cases the cursor can be in the SelectAction
    static private enum SelectActionCursor {
        rect("normal", "selection"),
        rect_add("normal", "select_add"),
        rect_rm("normal", "select_remove"),
        way("normal", "select_way"),
        way_add("normal", "select_way_add"),
        way_rm("normal", "select_way_remove"),
        node("normal", "select_node"),
        node_add("normal", "select_node_add"),
        node_rm("normal", "select_node_remove"),
        virtual_node("normal", "addnode"),
        scale("scale", null),
        rotate("rotate", null),
        merge("crosshair", null),
        merge_to_node("crosshair", "joinnode"),
        move(Cursor.MOVE_CURSOR);

        private final Cursor c;
        private SelectActionCursor(String main, String sub) {
            c = ImageProvider.getCursor(main, sub);
        }
        private SelectActionCursor(int systemCursor) {
            c = Cursor.getPredefinedCursor(systemCursor);
        }
        public Cursor cursor() {
            return c;
        }
    }

    // Cache previous mouse event (needed when only the modifier keys are
    // pressed but the mouse isn't moved)
    private MouseEvent oldEvent = null;

    private Mode mode = null;
    private SelectionManager selectionManager;
    private boolean cancelDrawMode = false;
    private boolean drawTargetHighlight;
    private boolean didMouseDrag = false;
    /**
     * The component this SelectAction is associated with.
     */
    private final MapView mv;
    /**
     * The old cursor before the user pressed the mouse button.
     */
    private Point startingDraggingPos;
    /**
     * The last known position of the mouse.
     */
    private Point lastMousePos;
    /**
     * The time of the user mouse down event.
     */
    private long mouseDownTime = 0;
    /**
     * The time which needs to pass between click and release before something
     * counts as a move, in milliseconds
     */
    private int initialMoveDelay;
    /**
     * The screen distance which needs to be travelled before something
     * counts as a move, in pixels
     */
    private int initialMoveThreshold;
    private boolean initialMoveThresholdExceeded = false;

    /**
     * elements that have been highlighted in the previous iteration. Used
     * to remove the highlight from them again as otherwise the whole data
     * set would have to be checked.
     */
    private Set<OsmPrimitive> oldHighlights = new HashSet<OsmPrimitive>();

    /**
     * Create a new SelectAction
     * @param mapFrame The MapFrame this action belongs to.
     */
    public SelectAction(MapFrame mapFrame) {
        super(tr("Select"), "move/move", tr("Select, move, scale and rotate objects"),
                Shortcut.registerShortcut("mapmode:select", tr("Mode: {0}", tr("Select")), KeyEvent.VK_S, Shortcut.GROUP_EDIT),
                mapFrame,
                ImageProvider.getCursor("normal", "selection"));
        mv = mapFrame.mapView;
        putValue("help", ht("/Action/Move/Move"));
        selectionManager = new SelectionManager(this, false, mv);
        initialMoveDelay = Main.pref.getInteger("edit.initial-move-delay", 200);
        initialMoveThreshold = Main.pref.getInteger("edit.initial-move-threshold", 5);
        drawTargetHighlight = Main.pref.getBoolean("draw.target-highlight", true);
    }

    @Override
    public void enterMode() {
        super.enterMode();
        mv.addMouseListener(this);
        mv.addMouseMotionListener(this);
        mv.setVirtualNodesEnabled(
                Main.pref.getInteger("mappaint.node.virtual-size", 8) != 0);
        // This is required to update the cursors when ctrl/shift/alt is pressed
        try {
            Toolkit.getDefaultToolkit().addAWTEventListener(this, AWTEvent.KEY_EVENT_MASK);
        } catch (SecurityException ex) {}
    }

    @Override
    public void exitMode() {
        super.exitMode();
        selectionManager.unregister(mv);
        mv.removeMouseListener(this);
        mv.removeMouseMotionListener(this);
        mv.setVirtualNodesEnabled(false);
        try {
            Toolkit.getDefaultToolkit().removeAWTEventListener(this);
        } catch (SecurityException ex) {}
        removeHighlighting();
    }

    /**
     * works out which cursor should be displayed for most of SelectAction's
     * features. The only exception is the "move" cursor when actually dragging
     * primitives.
     * @param nearbyStuff  primitives near the cursor
     * @return the cursor that should be displayed
     */
    private Cursor getCursor(Collection<OsmPrimitive> nearbyStuff) {
        String c = "rect";
        switch(mode) {
        case move:
            if(virtualNode != null) {
                c = "virtual_node";
                break;
            }
            final Iterator<OsmPrimitive> it = nearbyStuff.iterator();
            final OsmPrimitive osm = it.hasNext() ? it.next() : null;

            if(dragInProgress()) {
                c = ctrl ? "merge" : "move";
                if(osm != null && osm instanceof Node) {
                    c += "_to_node";
                }
                break;
            }

            c = (osm instanceof Node) ? "node" : c;
            c = (osm instanceof Way) ? "way" : c;
            if(shift) {
                c += "_add";
            } else if(ctrl) {
                c += osm.isSelected() ? "_rm" : "_add";
            }
            break;
        case rotate:
            c = "rotate";
            break;
        case scale:
            c = "scale";
            break;
        case select:
            c = "rect" + (shift ? "_add" : (ctrl ? "_rm" : ""));
            break;
        }
        return SelectActionCursor.valueOf(c).cursor();
    }

    /**
     * Removes all existing highlights.
     * @return true if a repaint is required
     */
    private boolean removeHighlighting() {
        boolean needsRepaint = false;
        DataSet ds = getCurrentDataSet();
        if(ds != null && !ds.getHighlightedVirtualNodes().isEmpty()) {
            needsRepaint = true;
            ds.clearHighlightedVirtualNodes();
        }
        if(oldHighlights.isEmpty())
            return needsRepaint;

        for(OsmPrimitive prim : oldHighlights) {
            prim.setHighlighted(false);
        }
        oldHighlights = new HashSet<OsmPrimitive>();
        return true;
    }

    /**
     * handles adding highlights and updating the cursor for the given mouse event.
     * Please note that the highlighting for merging while moving is handled via mouseDragged.
     * @param MouseEvent which should be used as base for the feedback
     * @return true if repaint is required
     */
    private boolean giveUserFeedback(MouseEvent e) {
        return giveUserFeedback(e, e.getModifiers());
    }

    /**
     * handles adding highlights and updating the cursor for the given mouse event.
     * Please note that the highlighting for merging while moving is handled via mouseDragged.
     * @param MouseEvent which should be used as base for the feedback
     * @param define custom keyboard modifiers if the ones from MouseEvent are outdated or similar
     * @return true if repaint is required
     */
    private boolean giveUserFeedback(MouseEvent e, int modifiers) {
        boolean needsRepaint = false;

        Collection<OsmPrimitive> c = MapView.asColl(
                mv.getNearestNodeOrWay(e.getPoint(), OsmPrimitive.isSelectablePredicate, true));

        updateKeyModifiers(modifiers);
        determineMapMode(!c.isEmpty());

        if(drawTargetHighlight) {
            needsRepaint = removeHighlighting();
        }

        virtualWays.clear();
        virtualNode = null;
        if(mode == Mode.move && setupVirtual(e)) {
            DataSet ds = getCurrentDataSet();
            if (ds != null) {
                ds.setHighlightedVirtualNodes(virtualWays);
            }
            mv.setNewCursor(SelectActionCursor.virtual_node.cursor(), this);
            // don't highlight anything else if a virtual node will be
            return true;
        }

        mv.setNewCursor(getCursor(c), this);

        // return early if there can't be any highlights
        if(!drawTargetHighlight || mode != Mode.move || c.isEmpty())
            return needsRepaint;

        for(OsmPrimitive x : c) {
            // only highlight primitives that will change the selection
            // when clicked. I.e. don't highlight selected elements unless
            // we are in toggle mode.
            if(ctrl || !x.isSelected()) {
                x.setHighlighted(true);
                oldHighlights.add(x);
            }
        }
        return needsRepaint || !oldHighlights.isEmpty();
    }

    /**
     * This is called whenever the keyboard modifier status changes
     */
    public void eventDispatched(AWTEvent e) {
        if(oldEvent == null)
            return;
        // We don't have a mouse event, so we pass the old mouse event but the
        // new modifiers.
        if(giveUserFeedback(oldEvent, ((InputEvent) e).getModifiers())) {
            mv.repaint();
        }
    }

    /**
     * If the left mouse button is pressed, move all currently selected
     * objects (if one of them is under the mouse) or the current one under the
     * mouse (which will become selected).
     */
    @Override
    public void mouseDragged(MouseEvent e) {
        if (!mv.isActiveLayerVisible())
            return;

        cancelDrawMode = true;
        if (mode == Mode.select)
            return;

        // do not count anything as a move if it lasts less than 100 milliseconds.
        if ((mode == Mode.move) && (System.currentTimeMillis() - mouseDownTime < initialMoveDelay))
            return;

        if (mode != Mode.rotate && mode != Mode.scale) // button is pressed in rotate mode
        {
            if ((e.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK) == 0)
                return;
        }

        if (mode == Mode.move) {
            // If ctrl is pressed we are in merge mode. Look for a nearby node,
            // highlight it and adjust the cursor accordingly.
            final OsmPrimitive p = ctrl ? (OsmPrimitive)findNodeToMergeTo(e) : null;
            boolean needsRepaint = removeHighlighting();
            if(p != null) {
                p.setHighlighted(true);
                oldHighlights.add(p);
                needsRepaint = true;
            }
            mv.setNewCursor(getCursor(MapView.asColl(p)), this);
            if(needsRepaint) {
                mv.repaint();
            }
        }

        if (startingDraggingPos == null) {
            startingDraggingPos = new Point(e.getX(), e.getY());
        }

        if( lastMousePos == null ) {
            lastMousePos = e.getPoint();
            return;
        }

        if (!initialMoveThresholdExceeded) {
            int dxp = lastMousePos.x - e.getX();
            int dyp = lastMousePos.y - e.getY();
            int dp = (int) Math.sqrt(dxp * dxp + dyp * dyp);
            if (dp < initialMoveThreshold)
                return;
            initialMoveThresholdExceeded = true;
        }

        EastNorth currentEN = mv.getEastNorth(e.getX(), e.getY());
        EastNorth lastEN = mv.getEastNorth(lastMousePos.x, lastMousePos.y);
        //EastNorth startEN = mv.getEastNorth(startingDraggingPos.x, startingDraggingPos.y);
        double dx = currentEN.east() - lastEN.east();
        double dy = currentEN.north() - lastEN.north();
        if (dx == 0 && dy == 0)
            return;

        if (virtualWays.size() > 0) {
            Collection<Command> virtualCmds = new LinkedList<Command>();
            virtualCmds.add(new AddCommand(virtualNode));
            for (WaySegment virtualWay : virtualWays) {
                Way w = virtualWay.way;
                Way wnew = new Way(w);
                wnew.addNode(virtualWay.lowerIndex + 1, virtualNode);
                virtualCmds.add(new ChangeCommand(w, wnew));
            }
            virtualCmds.add(new MoveCommand(virtualNode, dx, dy));
            String text = trn("Add and move a virtual new node to way",
                    "Add and move a virtual new node to {0} ways", virtualWays.size(),
                    virtualWays.size());
            Main.main.undoRedo.add(new SequenceCommand(text, virtualCmds));
            getCurrentDataSet().setSelected(Collections.singleton((OsmPrimitive) virtualNode));
            virtualWays.clear();
            virtualNode = null;
        } else {
            // Currently we support only transformations which do not affect relations.
            // So don't add them in the first place to make handling easier
            Collection<OsmPrimitive> selection = getCurrentDataSet().getSelectedNodesAndWays();
            Collection<Node> affectedNodes = AllNodesVisitor.getAllNodes(selection);

            // for these transformations, having only one node makes no sense - quit silently
            if (affectedNodes.size() < 2 && (mode == Mode.rotate || mode == Mode.scale))
                return;

            Command c = !Main.main.undoRedo.commands.isEmpty()
                    ? Main.main.undoRedo.commands.getLast() : null;
            if (c instanceof SequenceCommand) {
                c = ((SequenceCommand) c).getLastCommand();
            }

            if (mode == Mode.move) {
                if (c instanceof MoveCommand && affectedNodes.equals(((MoveCommand) c).getParticipatingPrimitives())) {
                    ((MoveCommand) c).moveAgain(dx, dy);
                } else {
                    Main.main.undoRedo.add(
                            c = new MoveCommand(selection, dx, dy));
                }

                for (Node n : affectedNodes) {
                    if (n.getCoor().isOutSideWorld()) {
                        // Revert move
                        ((MoveCommand) c).moveAgain(-dx, -dy);

                        JOptionPane.showMessageDialog(
                                Main.parent,
                                tr("Cannot move objects outside of the world."),
                                tr("Warning"),
                                JOptionPane.WARNING_MESSAGE);
                        mv.setNewCursor(cursor, this);
                        return;
                    }
                }
            } else if (mode == Mode.rotate) {
                if (c instanceof RotateCommand && affectedNodes.equals(((RotateCommand) c).getTransformedNodes())) {
                    ((RotateCommand) c).handleEvent(currentEN);
                } else {
                    Main.main.undoRedo.add(new RotateCommand(selection, currentEN));
                }
            } else if (mode == Mode.scale) {
                if (c instanceof ScaleCommand && affectedNodes.equals(((ScaleCommand) c).getTransformedNodes())) {
                    ((ScaleCommand) c).handleEvent(currentEN);
                } else {
                    Main.main.undoRedo.add(new ScaleCommand(selection, currentEN));
                }
            }
        }

        mv.repaint();
        if (mode != Mode.scale) {
            lastMousePos = e.getPoint();
        }

        didMouseDrag = true;
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        // Mac OSX simulates with  ctrl + mouse 1  the second mouse button hence no dragging events get fired.
        if ((Main.platform instanceof PlatformHookOsx) && (mode == Mode.rotate || mode == Mode.scale)) {
            mouseDragged(e);
            return;
        }
        oldEvent = e;
        if(giveUserFeedback(e)) {
            mv.repaint();
        }
    }

    @Override
    public void mouseExited(MouseEvent e) {
        if(removeHighlighting()) {
            mv.repaint();
        }
    }

    /** returns true whenever elements have been grabbed and moved (i.e. the initial
     * thresholds have been exceeded) and is still in progress (i.e. mouse button
     * still pressed)
     */
    final private boolean dragInProgress() {
        return didMouseDrag && startingDraggingPos != null;
    }

    private Node virtualNode = null;
    private Collection<WaySegment> virtualWays = new LinkedList<WaySegment>();

    /**
     * Calculate a virtual node if there is enough visual space to draw a crosshair
     * node and the middle of a way segment is clicked.  If the user drags the
     * crosshair node, it will be added to all ways in <code>virtualWays</code>.
     * 
     * @param e contains the point clicked
     * @return whether <code>virtualNode</code> and <code>virtualWays</code> were setup.
     */
    private boolean setupVirtual(MouseEvent e) {
        if (Main.pref.getInteger("mappaint.node.virtual-size", 8) > 0) {
            int virtualSnapDistSq = Main.pref.getInteger("mappaint.node.virtual-snap-distance", 8);
            int virtualSpace = Main.pref.getInteger("mappaint.node.virtual-space", 70);
            virtualSnapDistSq *= virtualSnapDistSq;

            Collection<WaySegment> selVirtualWays = new LinkedList<WaySegment>();
            Pair<Node, Node> vnp = null, wnp = new Pair<Node, Node>(null, null);
            Point p = e.getPoint();
            Way w = null;

            for (WaySegment ws : mv.getNearestWaySegments(p, OsmPrimitive.isSelectablePredicate)) {
                w = ws.way;

                Point2D p1 = mv.getPoint2D(wnp.a = w.getNode(ws.lowerIndex));
                Point2D p2 = mv.getPoint2D(wnp.b = w.getNode(ws.lowerIndex + 1));
                if (WireframeMapRenderer.isLargeSegment(p1, p2, virtualSpace)) {
                    Point2D pc = new Point2D.Double((p1.getX() + p2.getX()) / 2, (p1.getY() + p2.getY()) / 2);
                    if (p.distanceSq(pc) < virtualSnapDistSq) {
                        // Check that only segments on top of each other get added to the
                        // virtual ways list. Otherwise ways that coincidentally have their
                        // virtual node at the same spot will be joined which is likely unwanted
                        Pair.sort(wnp);
                        if (vnp == null) {
                            vnp = new Pair<Node, Node>(wnp.a, wnp.b);
                            virtualNode = new Node(mv.getLatLon(pc.getX(), pc.getY()));
                        }
                        if (vnp.equals(wnp)) {
                            (w.isSelected() ? selVirtualWays : virtualWays).add(ws);
                        }
                    }
                }
            }

            if (!selVirtualWays.isEmpty()) {
                virtualWays = selVirtualWays;
            }
        }

        return !virtualWays.isEmpty();
    }
    private Collection<OsmPrimitive> cycleList = Collections.emptyList();
    private boolean cyclePrims = false;
    private OsmPrimitive cycleStart = null;

    /**
     * 
     * @param osm nearest primitive found by simple method
     * @param e
     * @return
     */
    private Collection<OsmPrimitive> cycleSetup(Collection<OsmPrimitive> single, MouseEvent e) {
        OsmPrimitive osm = null;

        if (single != null && !single.isEmpty()) {
            osm = single.iterator().next();

            Point p = e.getPoint();
            boolean waitForMouseUp = Main.pref.getBoolean("mappaint.select.waits-for-mouse-up", false);
            updateKeyModifiers(e);
            alt = alt || Main.pref.getBoolean("selectaction.cycles.multiple.matches", false);

            if (!alt) {
                cycleList = MapView.asColl(osm);

                if (waitForMouseUp) {
                    // prefer a selected nearest node or way, if possible
                    osm = mv.getNearestNodeOrWay(p, OsmPrimitive.isSelectablePredicate, true);
                }
            } else {
                if (osm instanceof Node) {
                    cycleList = new LinkedList<OsmPrimitive>(mv.getNearestNodes(p, OsmPrimitive.isSelectablePredicate));
                } else if (osm instanceof Way) {
                    cycleList = new LinkedList<OsmPrimitive>(mv.getNearestWays(p, OsmPrimitive.isSelectablePredicate));
                }

                if (cycleList.size() > 1) {
                    cyclePrims = false;

                    OsmPrimitive old = osm;
                    for (OsmPrimitive o : cycleList) {
                        if (o.isSelected()) {
                            cyclePrims = true;
                            osm = o;
                            break;
                        }
                    }

                    // special case:  for cycle groups of 2, we can toggle to the
                    // true nearest primitive on mousePressed right away
                    if (cycleList.size() == 2 && !waitForMouseUp) {
                        if (!(osm.equals(old) || osm.isNew() || ctrl)) {
                            cyclePrims = false;
                            osm = old;
                        } // else defer toggling to mouseRelease time in those cases:
                        /*
                         * osm == old        -- the true nearest node is the selected one
                         * osm is a new node -- do not break unglue ways in ALT mode
                         * ctrl is pressed   -- ctrl generally works on mouseReleased
                         */
                    }
                }
            }
        }

        return MapView.asColl(osm);
    }

    /**
     * sets the mapmode according to key modifiers and if there are any
     * selectables nearby. Everything has to be pre-determined for this
     * function; its main purpose is to centralize what the modifiers do.
     * @param nearSelectables
     */
    private void determineMapMode(boolean hasSelectionNearby) {
        if (shift && ctrl) {
            mode = Mode.rotate;
        } else if (alt && ctrl) {
            mode = Mode.scale;
        } else if (hasSelectionNearby || dragInProgress()) {
            mode = Mode.move;
        } else {
            mode = Mode.select;
        }
    }

    /**
     * Look, whether any object is selected. If not, select the nearest node.
     * If there are no nodes in the dataset, do nothing.
     *
     * If the user did not press the left mouse button, do nothing.
     *
     * Also remember the starting position of the movement and change the mouse
     * cursor to movement.
     */
    @Override
    public void mousePressed(MouseEvent e) {
        // return early
        if (!mv.isActiveLayerVisible() || !(Boolean) this.getValue("active") || e.getButton() != MouseEvent.BUTTON1)
            return;

        // request focus in order to enable the expected keyboard shortcuts
        mv.requestFocus();

        // update which modifiers are pressed (shift, alt, ctrl)
        updateKeyModifiers(e);

        // We don't want to change to draw tool if the user tries to (de)select
        // stuff but accidentally clicks in an empty area when selection is empty
        cancelDrawMode = (shift || ctrl);
        didMouseDrag = false;
        initialMoveThresholdExceeded = false;
        mouseDownTime = System.currentTimeMillis();
        lastMousePos = e.getPoint();

        Collection<OsmPrimitive> c = MapView.asColl(
                mv.getNearestNodeOrWay(e.getPoint(), OsmPrimitive.isSelectablePredicate, true));

        determineMapMode(!c.isEmpty());
        switch(mode) {
        case rotate:
        case scale:
            if (getCurrentDataSet().getSelected().isEmpty()) {
                getCurrentDataSet().setSelected(c);
            }

            // Mode.select redraws when selectPrims is called
            // Mode.move   redraws when mouseDragged is called
            // Mode.rotate redraws here
            // Mode.scale redraws here
            break;
        case move:
            if (!cancelDrawMode && c.iterator().next() instanceof Way) {
                setupVirtual(e);
            }

            selectPrims(cycleSetup(c, e), e, false, false);
            break;
        case select:
        default:
            selectionManager.register(mv);
            selectionManager.mousePressed(e);
            break;
        }
        giveUserFeedback(e);
        mv.repaint();
        updateStatusLine();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (!mv.isActiveLayerVisible())
            return;

        startingDraggingPos = null;

        if (mode == Mode.select) {
            selectionManager.unregister(mv);

            // Select Draw Tool if no selection has been made
            if (getCurrentDataSet().getSelected().size() == 0 && !cancelDrawMode) {
                Main.map.selectDrawTool(true);
                return;
            }
        }

        if (mode == Mode.move) {
            if (!didMouseDrag) {
                // only built in move mode
                virtualWays.clear();
                virtualNode = null;

                // do nothing if the click was to short too be recognized as a drag,
                // but the release position is farther than 10px away from the press position
                if (lastMousePos == null || lastMousePos.distanceSq(e.getPoint()) < 100) {
                    selectPrims(cyclePrims(cycleList, e), e, true, false);

                    // If the user double-clicked a node, change to draw mode
                    Collection<OsmPrimitive> c = getCurrentDataSet().getSelected();
                    if (e.getClickCount() >= 2 && c.size() == 1 && c.iterator().next() instanceof Node) {
                        // We need to do it like this as otherwise drawAction will see a double
                        // click and switch back to SelectMode
                        Main.worker.execute(new Runnable() {
                            public void run() {
                                Main.map.selectDrawTool(true);
                            }
                        });
                        return;
                    }
                }
            } else {
                int max = Main.pref.getInteger("warn.move.maxelements", 20), limit = max;
                for (OsmPrimitive osm : getCurrentDataSet().getSelected()) {
                    if (osm instanceof Way) {
                        limit -= ((Way) osm).getNodes().size();
                    }
                    if ((limit -= 1) < 0) {
                        break;
                    }
                }
                if (limit < 0) {
                    ExtendedDialog ed = new ExtendedDialog(
                            Main.parent,
                            tr("Move elements"),
                            new String[]{tr("Move them"), tr("Undo move")});
                    ed.setButtonIcons(new String[]{"reorder.png", "cancel.png"});
                    ed.setContent(tr("You moved more than {0} elements. " + "Moving a large number of elements is often an error.\n" + "Really move them?", max));
                    ed.setCancelButton(2);
                    ed.toggleEnable("movedManyElements");
                    ed.showDialog();

                    if (ed.getValue() != 1) {
                        Main.main.undoRedo.undo();
                    }
                } else {
                    mergePrims(e);
                }
                getCurrentDataSet().fireSelectionChanged();
            }
        }

        mode = null;

        // simply remove any highlights if the middle click popup is active because
        // the highlights don't depend on the cursor position there. If something was
        // selected beforehand this would put us into move mode as well, which breaks
        // the cycling through primitives on top of each other (see #6739).
        if(e.getButton() == MouseEvent.BUTTON2) {
            removeHighlighting();
        } else {
            giveUserFeedback(e);
        }
        updateStatusLine();
    }

    public void selectionEnded(Rectangle r, MouseEvent e) {
        updateKeyModifiers(e);
        selectPrims(selectionManager.getObjectsInRectangle(r, alt), e, true, true);
    }

    /**
     * Modifies current selection state and returns the next element in a
     * selection cycle given by <code>prims</code>.
     * @param prims the primitives that form the selection cycle
     * @param mouse event
     * @return the next element of cycle list <code>prims</code>.
     */
    private Collection<OsmPrimitive> cyclePrims(Collection<OsmPrimitive> prims, MouseEvent e) {
        OsmPrimitive nxt = null;

        if (prims.size() > 1) {
            updateKeyModifiers(e);

            DataSet ds = getCurrentDataSet();
            OsmPrimitive first = prims.iterator().next(), foundInDS = null;
            nxt = first;

            for (Iterator<OsmPrimitive> i = prims.iterator(); i.hasNext();) {
                if (cyclePrims && shift) {
                    if (!(nxt = i.next()).isSelected()) {
                        break; // take first primitive in prims list not in sel
                    }
                } else {
                    if ((nxt = i.next()).isSelected()) {
                        foundInDS = nxt;
                        if (cyclePrims || ctrl) {
                            ds.clearSelection(foundInDS);
                            nxt = i.hasNext() ? i.next() : first;
                        }
                        break; // take next primitive in prims list
                    }
                }
            }

            if (ctrl) {
                // a member of prims was found in the current dataset selection
                if (foundInDS != null) {
                    // mouse was moved to a different selection group w/ a previous sel
                    if (!prims.contains(cycleStart)) {
                        ds.clearSelection(prims);
                        cycleStart = foundInDS;
                    } else if (cycleStart.equals(nxt)) {
                        // loop detected, insert deselect step
                        ds.addSelected(nxt);
                    }
                } else {
                    // setup for iterating a sel group again or a new, different one..
                    nxt = (prims.contains(cycleStart)) ? cycleStart : first;
                    cycleStart = nxt;
                }
            } else {
                cycleStart = null;
            }
        }

        // pass on prims, if it had less than 2 elements
        return (nxt != null) ? MapView.asColl(nxt) : prims;
    }

    /** Merges the selected nodes to the one closest to the given mouse position iff the control
     * key is pressed. If there is no such node, no action will be done and no error will be
     * reported. If there is, it will execute the merge and add it to the undo buffer. */
    final private void mergePrims(MouseEvent e) {
        updateKeyModifiers(e);
        Collection<Node> selNodes = getCurrentDataSet().getSelectedNodes();
        if (!ctrl || selNodes.isEmpty())
            return;

        Node target = findNodeToMergeTo(e);
        if (target == null)
            return;

        Collection<Node> nodesToMerge = new LinkedList<Node>(selNodes);
        nodesToMerge.add(target);
        Command cmd = MergeNodesAction.mergeNodes(Main.main.getEditLayer(), nodesToMerge, target);
        if (cmd != null) {
            Main.main.undoRedo.add(cmd);
            getCurrentDataSet().setSelected(target);
        }
    }

    /** tries to find a node to merge to when in move-merge mode for the current mouse
     * position. Either returns the node or null, if no suitable one is nearby. */
    final private Node findNodeToMergeTo(MouseEvent e) {
        Collection<Node> target = mv.getNearestNodes(e.getPoint(),
                getCurrentDataSet().getSelectedNodes(),
                OsmPrimitive.isSelectablePredicate);
        return target.isEmpty() ? null : target.iterator().next();
    }

    private void selectPrims(Collection<OsmPrimitive> prims, MouseEvent e, boolean released, boolean area) {
        updateKeyModifiers(e);
        DataSet ds = getCurrentDataSet();

        // not allowed together: do not change dataset selection, return early
        if ((shift && ctrl) || (ctrl && !released) || (!virtualWays.isEmpty()))
            return;

        if (!released) {
            // Don't replace the selection if the user clicked on a
            // selected object (it breaks moving of selected groups).
            // Do it later, on mouse release.
            shift |= getCurrentDataSet().getSelected().containsAll(prims);
        }

        if (ctrl) {
            // Ctrl on an item toggles its selection status,
            // but Ctrl on an *area* just clears those items
            // out of the selection.
            if (area) {
                ds.clearSelection(prims);
            } else {
                ds.toggleSelected(prims);
            }
        } else if (shift) {
            // add prims to an existing selection
            ds.addSelected(prims);
        } else {
            // clear selection, then select the prims clicked
            ds.setSelected(prims);
        }
    }

    @Override
    public String getModeHelpText() {
        if (mode == Mode.select)
            return tr("Release the mouse button to select the objects in the rectangle.");
        else if (mode == Mode.move)
            return tr("Release the mouse button to stop moving. Ctrl to merge with nearest node.");
        else if (mode == Mode.rotate)
            return tr("Release the mouse button to stop rotating.");
        else if (mode == Mode.scale)
            return tr("Release the mouse button to stop scaling.");
        else
            return tr("Move objects by dragging; Shift to add to selection (Ctrl to toggle); Shift-Ctrl to rotate selected; Alt-Ctrl to scale selected; or change selection");
    }

    @Override
    public boolean layerIsSupported(Layer l) {
        return l instanceof OsmDataLayer;
    }
}
