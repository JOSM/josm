// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions.mapmode;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.MergeNodesAction;
import org.openstreetmap.josm.command.AddCommand;
import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.MoveCommand;
import org.openstreetmap.josm.command.RotateCommand;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.WaySegment;
import org.openstreetmap.josm.data.osm.visitor.AllNodesVisitor;
import org.openstreetmap.josm.data.osm.visitor.paint.SimplePaintVisitor;
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
public class SelectAction extends MapMode implements SelectionEnded {
    //static private final Logger logger = Logger.getLogger(SelectAction.class.getName());

    enum Mode { move, rotate, select }
    private Mode mode = null;
    private SelectionManager selectionManager;

    private boolean cancelDrawMode = false;
    private boolean didMouseDrag = false;

    /**
     * The component this SelectAction is associated with.
     */
    private final MapView mv;

    /**
     * The old cursor before the user pressed the mouse button.
     */
    private Cursor oldCursor;

    /**
     * The position of the mouse before the user moves a node.
     */
    private Point mousePos;

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
     * Create a new SelectAction
     * @param mapFrame The MapFrame this action belongs to.
     */
    public SelectAction(MapFrame mapFrame) {
        super(tr("Select"), "move/move", tr("Select, move and rotate objects"),
                Shortcut.registerShortcut("mapmode:select", tr("Mode: {0}", tr("Select")), KeyEvent.VK_S, Shortcut.GROUP_EDIT),
                mapFrame,
                getCursor("normal", "selection", Cursor.DEFAULT_CURSOR));
        mv = mapFrame.mapView;
        putValue("help", "Action/Move/Move");
        selectionManager = new SelectionManager(this, false, mv);
        initialMoveDelay = Main.pref.getInteger("edit.initial-move-delay",200);
        initialMoveThreshold = Main.pref.getInteger("edit.initial-move-threshold",5);
    }

    private static Cursor getCursor(String name, String mod, int def) {
        try {
            return ImageProvider.getCursor(name, mod);
        } catch (Exception e) {
        }
        return Cursor.getPredefinedCursor(def);
    }

    private void setCursor(Cursor c) {
        if (oldCursor == null) {
            oldCursor = mv.getCursor();
            mv.setCursor(c);
        }
    }

    private void restoreCursor() {
        if (oldCursor != null) {
            mv.setCursor(oldCursor);
            oldCursor = null;
        }
    }

    @Override public void enterMode() {
        super.enterMode();
        mv.addMouseListener(this);
        mv.addMouseMotionListener(this);
        mv.setVirtualNodesEnabled(
                Main.pref.getInteger("mappaint.node.virtual-size", 8) != 0);
    }

    @Override public void exitMode() {
        super.exitMode();
        selectionManager.unregister(mv);
        mv.removeMouseListener(this);
        mv.removeMouseMotionListener(this);
        mv.setVirtualNodesEnabled(false);
    }

    /**
     * If the left mouse button is pressed, move all currently selected
     * objects (if one of them is under the mouse) or the current one under the
     * mouse (which will become selected).
     */
    @Override public void mouseDragged(MouseEvent e) {
        if(!mv.isActiveLayerVisible())
            return;

        cancelDrawMode = true;
        if (mode == Mode.select) return;

        // do not count anything as a move if it lasts less than 100 milliseconds.
        if ((mode == Mode.move) && (System.currentTimeMillis() - mouseDownTime < initialMoveDelay)) return;

        if(mode != Mode.rotate) // button is pressed in rotate mode
            if ((e.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK) == 0)
                return;

        if (mode == Mode.move) {
            setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
        }

        if (!initialMoveThresholdExceeded) {
            int dxp = mousePos.x - e.getX();
            int dyp = mousePos.y - e.getY();
            int dp = (int) Math.sqrt(dxp*dxp+dyp*dyp);
            if (dp < initialMoveThreshold) return;
            initialMoveThresholdExceeded = true;
        }

        EastNorth mouseEN = mv.getEastNorth(e.getX(), e.getY());
        EastNorth mouseStartEN = mv.getEastNorth(mousePos.x, mousePos.y);
        double dx = mouseEN.east() - mouseStartEN.east();
        double dy = mouseEN.north() - mouseStartEN.north();
        if (dx == 0 && dy == 0)
            return;

        if (virtualWays.size() > 0) {
            Collection<Command> virtualCmds = new LinkedList<Command>();
            virtualCmds.add(new AddCommand(virtualNode));
            for (WaySegment virtualWay : virtualWays) {
                Way w = virtualWay.way;
                Way wnew = new Way(w);
                wnew.addNode(virtualWay.lowerIndex+1, virtualNode);
                virtualCmds.add(new ChangeCommand(w, wnew));
            }
            virtualCmds.add(new MoveCommand(virtualNode, dx, dy));
            String text = trn("Add and move a virtual new node to way",
                    "Add and move a virtual new node to {0} ways", virtualWays.size(),
                    virtualWays.size());
            Main.main.undoRedo.add(new SequenceCommand(text, virtualCmds));
            getCurrentDataSet().setSelected(Collections.singleton((OsmPrimitive)virtualNode));
            virtualWays.clear();
            virtualNode = null;
        } else {
            // Currently we support moving and rotating, which do not affect relations.
            // So don't add them in the first place to make handling easier
            Collection<OsmPrimitive> selection = getCurrentDataSet().getSelectedNodesAndWays();
            Collection<Node> affectedNodes = AllNodesVisitor.getAllNodes(selection);

            // when rotating, having only one node makes no sense - quit silently
            if (mode == Mode.rotate && affectedNodes.size() < 2)
                return;

            Command c = !Main.main.undoRedo.commands.isEmpty()
            ? Main.main.undoRedo.commands.getLast() : null;
            if (c instanceof SequenceCommand) {
                c = ((SequenceCommand)c).getLastCommand();
            }

            if (mode == Mode.move) {
                if (c instanceof MoveCommand && affectedNodes.equals(((MoveCommand)c).getParticipatingPrimitives())) {
                    ((MoveCommand)c).moveAgain(dx,dy);
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
                                JOptionPane.WARNING_MESSAGE

                        );
                        restoreCursor();
                        return;
                    }
                }
            } else if (mode == Mode.rotate) {
                if (c instanceof RotateCommand && affectedNodes.equals(((RotateCommand)c).getRotatedNodes())) {
                    ((RotateCommand)c).rotateAgain(mouseStartEN, mouseEN);
                } else {
                    Main.main.undoRedo.add(new RotateCommand(selection, mouseStartEN, mouseEN));
                }
            }
        }

        mv.repaint();
        mousePos = e.getPoint();

        didMouseDrag = true;
    }

    @Override public void mouseMoved(MouseEvent e) {
        // Mac OSX simulates with  ctrl + mouse 1  the second mouse button hence no dragging events get fired.
        //
        if ((Main.platform instanceof PlatformHookOsx) && mode == Mode.rotate) {
            mouseDragged(e);
        }
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

            for(WaySegment ws : mv.getNearestWaySegments(p, OsmPrimitive.isSelectablePredicate)) {
                w = ws.way;

                Point2D p1 = mv.getPoint2D(wnp.a = w.getNode(ws.lowerIndex));
                Point2D p2 = mv.getPoint2D(wnp.b = w.getNode(ws.lowerIndex+1));
                if(SimplePaintVisitor.isLargeSegment(p1, p2, virtualSpace))
                {
                    Point2D pc = new Point2D.Double((p1.getX()+p2.getX())/2, (p1.getY()+p2.getY())/2);
                    if (p.distanceSq(pc) < virtualSnapDistSq)
                    {
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

        if (single == null) {
            single = MapView.asColl(
                    mv.getNearestNodeOrWay(e.getPoint(), OsmPrimitive.isSelectablePredicate, false));
        }

        if (!single.isEmpty()) {
            boolean waitForMouseUp = Main.pref.getBoolean("mappaint.select.waits-for-mouse-up", false);
            boolean ctrl = (e.getModifiers() & ActionEvent.CTRL_MASK) != 0;
            boolean alt = ((e.getModifiers() & (ActionEvent.ALT_MASK|InputEvent.ALT_GRAPH_MASK)) != 0
                    || Main.pref.getBoolean("selectaction.cycles.multiple.matches", false));

            Point p = e.getPoint();
            osm = single.iterator().next();

            if (!alt) {
                cycleList = MapView.asColl(osm);
                if (waitForMouseUp) {
                    // find a selected nearest node or way, not the true nearest..
                    osm = mv.getNearestNodeOrWay(p, OsmPrimitive.isSelectablePredicate, true);
                }
            } else {
                if (osm instanceof Node) {
                    cycleList = new LinkedList<OsmPrimitive>(mv.getNearestNodes(p, OsmPrimitive.isSelectablePredicate));
                } else if (osm instanceof Way) {
                    cycleList = new LinkedList<OsmPrimitive>(mv.getNearestWays(p, OsmPrimitive.isSelectablePredicate));
                }

                if (!waitForMouseUp && cycleList.size()>1) {
                    cyclePrims = false;

                    OsmPrimitive old = osm;
                    for (OsmPrimitive o : cycleList) {
                        if (o.isSelected()) {
                            cyclePrims = true;
                            osm = o;
                            break;
                        }
                    }

                    // for cycle groups of 2, we can toggle to the true nearest
                    // primitive on mouse presses, if it is not selected and if ctrl is not used
                    // else, if rotation is possible, defer sel change to mouse release
                    if (cycleList.size()==2) {
                        if (!(old.equals(osm) || ctrl)) {
                            cyclePrims = false;
                            osm = old;
                        }
                    }
                }
            }
        }

        return MapView.asColl(osm);
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
    @Override public void mousePressed(MouseEvent e) {
        debug("mousePressed: e.getPoint()=" + e.getPoint());

        // return early
        if(!mv.isActiveLayerVisible()
                || !(Boolean)this.getValue("active")
                || e.getButton() != MouseEvent.BUTTON1)
            return;

        // request focus in order to enable the expected keyboard shortcuts
        mv.requestFocus();

        boolean ctrl = (e.getModifiers() & ActionEvent.CTRL_MASK) != 0;
        boolean shift = (e.getModifiers() & ActionEvent.SHIFT_MASK) != 0;

        // We don't want to change to draw tool if the user tries to (de)select
        // stuff but accidentally clicks in an empty area when selection is empty
        cancelDrawMode = (shift || ctrl);
        didMouseDrag = false;
        initialMoveThresholdExceeded = false;
        mouseDownTime = System.currentTimeMillis();
        mousePos = e.getPoint();

        Collection<OsmPrimitive> c = MapView.asColl(
                mv.getNearestNodeOrWay(e.getPoint(), OsmPrimitive.isSelectablePredicate, false));

        if (shift && ctrl) {
            mode = Mode.rotate;

            if (getCurrentDataSet().getSelected().isEmpty()) {
                getCurrentDataSet().setSelected(c);
            }

            // Mode.select redraws when selectPrims is called
            // Mode.move   redraws when mouseDragged is called
            // Mode.rotate redraws here
            setCursor(ImageProvider.getCursor("rotate", null));
            mv.repaint();
        } else if (!c.isEmpty()) {
            mode = Mode.move;

            if (!cancelDrawMode && c.iterator().next() instanceof Way) {
                setupVirtual(e);
            }

            selectPrims(cycleSetup(c, e), e, false, false);
        } else {
            mode = Mode.select;

            oldCursor = mv.getCursor();
            selectionManager.register(mv);
            selectionManager.mousePressed(e);
        }

        updateStatusLine();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        debug("mouseReleased: e.getPoint()=" + e.getPoint());

        if(!mv.isActiveLayerVisible())
            return;

        restoreCursor();
        if (mode == Mode.select) {
            selectionManager.unregister(mv);

            // Select Draw Tool if no selection has been made
            if(getCurrentDataSet().getSelected().size() == 0 && !cancelDrawMode) {
                Main.map.selectDrawTool(true);
                return;
            }
        }

        if (mode == Mode.move) {
            if (!didMouseDrag) {
                // only built in move mode
                virtualWays.clear();
                virtualNode = null;

                // do nothing if the click was to short to be recognized as a drag,
                // but the release position is farther than 10px away from the press position
                if (mousePos.distanceSq(e.getPoint())<100) {
                    selectPrims(cyclePrims(cycleList, e), e, true, false);

                    // If the user double-clicked a node, change to draw mode
                    Collection<OsmPrimitive> c = getCurrentDataSet().getSelected();
                    if(e.getClickCount() >=2 && c.size() == 1 && c.iterator().next() instanceof Node) {
                        // We need to do it like this as otherwise drawAction will see a double
                        // click and switch back to SelectMode
                        Main.worker.execute(new Runnable(){
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
                        limit -= ((Way)osm).getNodes().size();
                    }
                    if ((limit -= 1) < 0) {
                        break;
                    }
                }
                if (limit < 0) {
                    ExtendedDialog ed = new ExtendedDialog(
                            Main.parent,
                            tr("Move elements"),
                            new String[] {tr("Move them"), tr("Undo move")});
                    ed.setButtonIcons(new String[] {"reorder.png", "cancel.png"});
                    ed.setContent(tr("You moved more than {0} elements. "
                            + "Moving a large number of elements is often an error.\n"
                            + "Really move them?", max));
                    ed.setCancelButton(2);
                    ed.toggleEnable("movedManyElements");
                    ed.showDialog();

                    if(ed.getValue() != 1) {
                        Main.main.undoRedo.undo();
                    }
                } else {
                    mergePrims(getCurrentDataSet().getSelectedNodes(), e);
                }
                getCurrentDataSet().fireSelectionChanged();
            }
        }

        // I don't see why we need this.
        //updateStatusLine();
        mode = null;
        updateStatusLine();
    }

    public void selectionEnded(Rectangle r, MouseEvent e) {
        boolean alt = (e.getModifiersEx() & (MouseEvent.ALT_DOWN_MASK | MouseEvent.ALT_GRAPH_DOWN_MASK)) != 0;
        selectPrims(selectionManager.getObjectsInRectangle(r, alt), e, true, true);
    }

    /**
     * Modifies current selection state and returns the next element in a
     * selection cycle given by <code>prims</code>.
     * @param prims the primitives that form the selection cycle
     * @param shift whether shift is pressed
     * @param ctrl whether ctrl is pressed
     * @return the next element of cycle list <code>prims</code>.
     */
    private Collection<OsmPrimitive> cyclePrims(Collection<OsmPrimitive> prims, MouseEvent e) {
        OsmPrimitive nxt = null;

        debug("cyclePrims(): entry.....");
        for (OsmPrimitive osm : prims) {
            debug("cyclePrims(): prims id=" + osm.getId());
        }

        if (prims.size() > 1) {
            boolean ctrl = (e.getModifiers() & ActionEvent.CTRL_MASK) != 0;
            boolean shift = (e.getModifiers() & ActionEvent.SHIFT_MASK) != 0;

            DataSet ds = getCurrentDataSet();
            OsmPrimitive first = prims.iterator().next(), foundInDS = null;
            nxt = first;

            for (Iterator<OsmPrimitive> i = prims.iterator(); i.hasNext(); ) {
                if (cyclePrims && shift) {
                    if (!(nxt = i.next()).isSelected()) {
                        debug("cyclePrims(): taking " + nxt.getId());
                        break; // take first primitive in prims list not in sel
                    }
                } else {
                    if ((nxt = i.next()).isSelected()) {
                        foundInDS = nxt;
                        if (cyclePrims || ctrl) {
                            ds.clearSelection(foundInDS);
                            nxt = i.hasNext() ? i.next() : first;
                        }
                        debug("selectPrims(): taking " + nxt.getId());
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
                        debug("selectPrims(): cycleStart set to foundInDS=" + cycleStart.getId());
                    } else if (cycleStart.equals(nxt)) {
                        // loop detected, insert deselect step
                        ds.addSelected(nxt);
                        debug("selectPrims(): cycleStart hit");
                    }
                } else {
                    // setup for iterating a sel group again or a new, different one..
                    nxt = (prims.contains(cycleStart)) ? cycleStart : first;
                    cycleStart = nxt;
                    debug("selectPrims(): cycleStart set to nxt=" + cycleStart.getId());
                }
            } else {
                cycleStart = null;
            }

            debug("cyclePrims(): truncated prims list to id=" + nxt.getId());
        }

        // pass on prims, if it had less than 2 elements
        return (nxt != null) ? MapView.asColl(nxt) : prims;
    }

    private void mergePrims(Collection<Node> affectedNodes, MouseEvent e) {
        boolean ctrl = (e.getModifiers() & ActionEvent.CTRL_MASK) != 0;

        if (ctrl && !affectedNodes.isEmpty()) {
            Collection<Node> target = mv.getNearestNodes(e.getPoint(), affectedNodes, OsmPrimitive.isSelectablePredicate);
            if (!target.isEmpty()) {
                Collection<Node> nodesToMerge = new LinkedList<Node>(affectedNodes);
                nodesToMerge.add(target.iterator().next());

                Command cmd = MergeNodesAction.mergeNodes(Main.main.getEditLayer(), nodesToMerge, target.iterator().next());
                if(cmd != null) {
                    Main.main.undoRedo.add(cmd);
                }
            }
        }
    }

    private void selectPrims(Collection<OsmPrimitive> prims, MouseEvent e, boolean released, boolean area) {
        boolean ctrl = (e.getModifiers() & ActionEvent.CTRL_MASK) != 0;
        boolean shift = (e.getModifiers() & ActionEvent.SHIFT_MASK) != 0;
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

    @Override public String getModeHelpText() {
        if (mode == Mode.select)
            return tr("Release the mouse button to select the objects in the rectangle.");
        else if (mode == Mode.move)
            return tr("Release the mouse button to stop moving. Ctrl to merge with nearest node.");
        else if (mode == Mode.rotate)
            return tr("Release the mouse button to stop rotating.");
        else
            return tr("Move objects by dragging; Shift to add to selection (Ctrl to toggle); Shift-Ctrl to rotate selected; or change selection");
    }

    @Override public boolean layerIsSupported(Layer l) {
        return l instanceof OsmDataLayer;
    }

    private static void debug(String s) {
        //System.err.println("SelectAction:" + s);
    }
}
