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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.TreeSet;

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
    private long mouseDownTime = 0;
    private boolean didMove = false;
    private boolean cancelDrawMode = false;
    private Node virtualNode = null;
    private Collection<WaySegment> virtualWays = new ArrayList<WaySegment>();

    /**
     * The old cursor before the user pressed the mouse button.
     */
    private Cursor oldCursor;
    /**
     * The position of the mouse before the user moves a node.
     */
    private Point mousePos;
    private SelectionManager selectionManager;

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
        putValue("help", "Action/Move/Move");
        selectionManager = new SelectionManager(this, false, mapFrame.mapView);
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
            oldCursor = Main.map.mapView.getCursor();
            Main.map.mapView.setCursor(c);
        }
    }

    private void restoreCursor() {
        if (oldCursor != null) {
            Main.map.mapView.setCursor(oldCursor);
            oldCursor = null;
        }
    }

    @Override public void enterMode() {
        super.enterMode();
        Main.map.mapView.addMouseListener(this);
        Main.map.mapView.addMouseMotionListener(this);
        Main.map.mapView.setVirtualNodesEnabled(
                Main.pref.getInteger("mappaint.node.virtual-size", 8) != 0);
    }

    @Override public void exitMode() {
        super.exitMode();
        selectionManager.unregister(Main.map.mapView);
        Main.map.mapView.removeMouseListener(this);
        Main.map.mapView.removeMouseMotionListener(this);
        Main.map.mapView.setVirtualNodesEnabled(false);
    }

    /**
     * If the left mouse button is pressed, move all currently selected
     * objects (if one of them is under the mouse) or the current one under the
     * mouse (which will become selected).
     */
    @Override public void mouseDragged(MouseEvent e) {
        if(!Main.map.mapView.isActiveLayerVisible())
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

        if (mousePos == null) {
            mousePos = e.getPoint();
            return;
        }

        if (!initialMoveThresholdExceeded) {
            int dxp = mousePos.x - e.getX();
            int dyp = mousePos.y - e.getY();
            int dp = (int) Math.sqrt(dxp*dxp+dyp*dyp);
            if (dp < initialMoveThreshold) return;
            initialMoveThresholdExceeded = true;
        }

        EastNorth mouseEN = Main.map.mapView.getEastNorth(e.getX(), e.getY());
        EastNorth mouseStartEN = Main.map.mapView.getEastNorth(mousePos.x, mousePos.y);
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

        Main.map.mapView.repaint();
        mousePos = e.getPoint();

        didMove = true;
    }

    @Override public void mouseMoved(MouseEvent e) {
        // Mac OSX simulates with  ctrl + mouse 1  the second mouse button hence no dragging events get fired.
        //
        if ((Main.platform instanceof PlatformHookOsx) && mode == Mode.rotate) {
            mouseDragged(e);
        }
    }

    private Collection<OsmPrimitive> getNearestCollectionVirtual(Point p) {
        int snapDistance = Main.pref.getInteger("mappaint.node.virtual-snap-distance", 8);
        int virtualSpace = Main.pref.getInteger("mappaint.node.virtual-space", 70);
        snapDistance *= snapDistance;

        MapView c = Main.map.mapView;

        Collection<OsmPrimitive> sel = getCurrentDataSet().getSelected();
        OsmPrimitive osm = c.getNearestNode(p, OsmPrimitive.isSelectablePredicate);

        if (osm != null) {
            for (Node n : c.getNearestNodes(p, OsmPrimitive.isSelectablePredicate)) {
                if (sel.contains(n)) {
                    osm = n;
                }
            }
        } else {
            Node virtualWayNode = null;
            Way w = null;

            Collection<WaySegment> virtualWaysInSel = new ArrayList<WaySegment>();
            osm = c.getNearestWay(p, OsmPrimitive.isSelectablePredicate);

            for(WaySegment nearestWS : c.getNearestWaySegments(p, OsmPrimitive.isSelectablePredicate)) {
                if (nearestWS == null) {
                    continue;
                }
                if (sel.contains(w = nearestWS.way)) {
                    osm = w;
                }

                if (Main.pref.getInteger("mappaint.node.virtual-size", 8) > 0) {
                    Point p1 = c.getPoint(w.getNode(nearestWS.lowerIndex));
                    Point p2 = c.getPoint(w.getNode(nearestWS.lowerIndex+1));
                    if(SimplePaintVisitor.isLargeSegment(p1, p2, virtualSpace))
                    {
                        Point pc = new Point((p1.x+p2.x)/2, (p1.y+p2.y)/2);
                        if (p.distanceSq(pc) < snapDistance)
                        {
                            // Check that only segments on top of each other get added to the
                            // virtual ways list. Otherwise ways that coincidentally have their
                            // virtual node at the same spot will be joined which is likely unwanted
                            if(virtualWayNode != null) {
                                if(!w.getNode(nearestWS.lowerIndex+1).equals(virtualWayNode)
                                        && !w.getNode(nearestWS.lowerIndex).equals(virtualWayNode)) {
                                    continue;
                                }
                            } else {
                                virtualWayNode = w.getNode(nearestWS.lowerIndex+1);
                            }

                            (!sel.contains(w) ? virtualWays : virtualWaysInSel).add(nearestWS);
                            if(virtualNode == null) {
                                virtualNode = new Node(Main.map.mapView.getLatLon(pc.x, pc.y));
                            }
                        }
                    }
                }
            }

            if (virtualNode != null) {
                virtualWays = virtualWaysInSel.isEmpty() ? virtualWays : virtualWaysInSel;
                osm = virtualWays.iterator().next().way;
            }
        }

        if (osm == null)
            return Collections.emptySet();
        return Collections.singleton(osm);
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
        if(!Main.map.mapView.isActiveLayerVisible())
            return;

        // request focus in order to enable the expected keyboard shortcuts
        Main.map.mapView.requestFocus();

        cancelDrawMode = false;
        if (! (Boolean)this.getValue("active")) return;
        if (e.getButton() != MouseEvent.BUTTON1)
            return;
        boolean ctrl = (e.getModifiers() & ActionEvent.CTRL_MASK) != 0;
        boolean shift = (e.getModifiers() & ActionEvent.SHIFT_MASK) != 0;

        // We don't want to change to draw tool if the user tries to (de)select
        // stuff but accidentally clicks in an empty area when selection is empty
        if(shift || ctrl) {
            cancelDrawMode = true;
        }

        mouseDownTime = System.currentTimeMillis();
        didMove = false;
        initialMoveThresholdExceeded = false;

        Collection<OsmPrimitive> osmColl = getNearestCollectionVirtual(e.getPoint());

        if (ctrl && shift) {
            if (getCurrentDataSet().getSelected().isEmpty()) {
                selectPrims(osmColl, true, false, false, false);
            }
            mode = Mode.rotate;
            setCursor(ImageProvider.getCursor("rotate", null));
        } else if (!osmColl.isEmpty()) {
            // Don't replace the selection now if the user clicked on a
            // selected object (this would break moving of selected groups).
            // We'll do that later in mouseReleased if the user didn't try to
            // move.
            selectPrims(osmColl,
                    shift || getCurrentDataSet().getSelected().containsAll(osmColl),
                    ctrl, false, false);
            mode = Mode.move;
        } else {
            mode = Mode.select;
            oldCursor = Main.map.mapView.getCursor();
            selectionManager.register(Main.map.mapView);
            selectionManager.mousePressed(e);
        }

        if(mode != Mode.move || shift || ctrl) {
            virtualNode = null;
            virtualWays.clear();
        }

        updateStatusLine();
        // Mode.select redraws when selectPrims is called
        // Mode.move   redraws when mouseDragged is called
        // Mode.rotate redraws here
        if(mode == Mode.rotate) {
            Main.map.mapView.repaint();
        }

        mousePos = e.getPoint();
    }

    /**
     * Restore the old mouse cursor.
     */
    @Override public void mouseReleased(MouseEvent e) {
        if(!Main.map.mapView.isActiveLayerVisible())
            return;

        if (mode == Mode.select) {
            selectionManager.unregister(Main.map.mapView);

            // Select Draw Tool if no selection has been made
            if(getCurrentDataSet().getSelected().size() == 0 && !cancelDrawMode) {
                Main.map.selectDrawTool(true);
                return;
            }
        }
        restoreCursor();

        if (mode == Mode.move) {
            boolean ctrl = (e.getModifiers() & ActionEvent.CTRL_MASK) != 0;
            boolean shift = (e.getModifiers() & ActionEvent.SHIFT_MASK) != 0;
            boolean alt = (e.getModifiers() & (ActionEvent.ALT_MASK|InputEvent.ALT_GRAPH_MASK)) != 0
                            || Main.pref.getBoolean("selectaction.rotates", false);

            virtualWays.clear();
            virtualNode = null;

            if (!didMove) {
                Collection<OsmPrimitive> c = Main.map.mapView.getNearestCollection(e.getPoint(), OsmPrimitive.isSelectablePredicate);
                if (!c.isEmpty() && alt) {
                    if (c.iterator().next() instanceof Node) {
                        // there is at least one node under the cursor:
                        //   - make sure (first element of new list) equals (result of getNearestCollection)
                        //   - do not consider ways at all, but all nearest nodes
                        c = new ArrayList<OsmPrimitive>(Main.map.mapView.getNearestNodes(e.getPoint(), OsmPrimitive.isSelectablePredicate));
                    } else {
                        // consider all ways..
                        c = Main.map.mapView.getAllNearest(e.getPoint(), OsmPrimitive.isSelectablePredicate);
                    }
                }
                selectPrims(c, shift, ctrl, true, false);

                // If the user double-clicked a node, change to draw mode
                c = getCurrentDataSet().getSelected();
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
            } else {
                Collection<OsmPrimitive> selection = getCurrentDataSet().getSelected();
                Collection<OsmPrimitive> s = new TreeSet<OsmPrimitive>();
                int max = Main.pref.getInteger("warn.move.maxelements", 20);
                for (OsmPrimitive osm : selection)
                {
                    if(osm instanceof Node) {
                        s.add(osm);
                    } else if(osm instanceof Way)
                    {
                        s.add(osm);
                        s.addAll(((Way)osm).getNodes());
                    }
                    if(s.size() > max)
                    {
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

                        if(ed.getValue() != 1)
                        {
                            Main.main.undoRedo.undo();
                        }
                        break;
                    }
                }
                if (ctrl) {
                    Collection<Node> affectedNodes = OsmPrimitive.getFilteredSet(selection, Node.class);
                    Collection<Node> nn = Main.map.mapView.getNearestNodes(e.getPoint(), affectedNodes, OsmPrimitive.isSelectablePredicate);
                    if (nn != null) {
                        Node targetNode = nn.iterator().next();
                        Set<Node> nodesToMerge = new HashSet<Node>(affectedNodes);
                        nodesToMerge.add(targetNode);
                        if (!nodesToMerge.isEmpty()) {
                            Command cmd = MergeNodesAction.mergeNodes(Main.main.getEditLayer(),nodesToMerge, targetNode);
                            if(cmd != null) {
                                Main.main.undoRedo.add(cmd);
                            }
                        }
                    }
                }
                getCurrentDataSet().fireSelectionChanged();
            }
        }

        // I don't see why we need this.
        //updateStatusLine();
        mode = null;
        updateStatusLine();
    }

    public void selectionEnded(Rectangle r, boolean alt, boolean shift, boolean ctrl) {
        selectPrims(selectionManager.getObjectsInRectangle(r, alt), shift, ctrl, true, true);
    }

    private boolean selMorePrims = false;
    private OsmPrimitive selCycleStart = null;

    public void selectPrims(Collection<OsmPrimitive> selectionList, boolean shift,
            boolean ctrl, boolean released, boolean area) {
        DataSet ds = getCurrentDataSet();
        if ((shift && ctrl) || (ctrl && !released) || (!virtualWays.isEmpty()))
            return; // not allowed together

        // toggle through possible objects on mouse release
        if (released && !area) {
            if (selectionList.size() > 1) {
                Collection<OsmPrimitive> coll = ds.getSelected();

                OsmPrimitive first, foundInDS, node, nxt;
                first = nxt = selectionList.iterator().next();
                foundInDS = node = null;

                for (Iterator<OsmPrimitive> i = selectionList.iterator(); i.hasNext(); ) {
                    if (selMorePrims && shift) {
                        if (!coll.contains(nxt = i.next())) {
                            break; // take first primitive not in dsSel or last if all contained
                        }
                    } else {
                        if (coll.contains(nxt = i.next())) {
                            foundInDS = nxt;
                            if (selMorePrims || ctrl) {
                                ds.clearSelection(nxt);
                                nxt = i.hasNext() ? i.next() : first;
                            }
                            break; // take next primitive of selList
                        } else if (nxt instanceof Node && node == null) {
                            node = nxt;
                        }
                    }
                }

                if (ctrl) {
                    if (foundInDS != null) {
                        // a member of selList was foundInDS
                        if (!selectionList.contains(selCycleStart)) {
                            selCycleStart = foundInDS;
                        }
                        // check if selCycleStart == prim (equals next(foundInDS))
                        if (selCycleStart.equals(nxt)) {
                            ds.addSelected(nxt);   // cycle complete, prim toggled below
                            selCycleStart = null;  // check: might do w/out ??
                        }
                    } else {
                        // no member of selList was foundInDS (sets were disjunct), setup for new cycle
                        selCycleStart = nxt = (node != null) ? node : first;
                    }
                }

                selectionList = new ArrayList<OsmPrimitive>(1); // do not modify the passed object..
                selectionList.add(nxt);
            }
        }

        // hard-wiring to false due to performance reasons, should do w/out
        selMorePrims = (released || area) ? false : ds.getSelected().containsAll(selectionList);

        if (ctrl) {
            // Ctrl on an item toggles its selection status,
            // but Ctrl on an *area* just clears those items
            // out of the selection.
            if (area) {
                ds.clearSelection(selectionList);
            } else {
                ds.toggleSelected(selectionList);
            }
        } else {
            // plain clicks with no modifiers
            if (!shift) {
                ds.setSelected(selectionList);
            } else {
                // add things to an
                // existing selection.
                ds.addSelected(selectionList);
            }
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
}
