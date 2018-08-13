// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.mapmode;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.command.AddCommand;
import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.DeleteCommand;
import org.openstreetmap.josm.command.MoveCommand;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.DataSelectionListener;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.WaySegment;
import org.openstreetmap.josm.data.osm.event.SelectionEventManager;
import org.openstreetmap.josm.data.preferences.CachingProperty;
import org.openstreetmap.josm.data.preferences.IntegerProperty;
import org.openstreetmap.josm.data.preferences.NamedColorProperty;
import org.openstreetmap.josm.data.preferences.StrokeProperty;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.draw.MapViewPath;
import org.openstreetmap.josm.gui.draw.SymbolShape;
import org.openstreetmap.josm.gui.layer.AbstractMapViewPaintable;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.util.ModifierExListener;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Pair;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * A special map mode that is optimized for improving way geometry.
 * (by efficiently moving, adding and deleting way-nodes)
 *
 * @author Alexander Kachkaev &lt;alexander@kachkaev.ru&gt;, 2011
 */
public class ImproveWayAccuracyAction extends MapMode implements DataSelectionListener, ModifierExListener {

    private static final String CROSSHAIR = /* ICON(cursor/)*/ "crosshair";

    enum State {
        SELECTING, IMPROVING
    }

    private State state;

    private MapView mv;

    private static final long serialVersionUID = 42L;

    private transient Way targetWay;
    private transient Node candidateNode;
    private transient WaySegment candidateSegment;

    private Point mousePos;
    private boolean dragging;

    private final Cursor cursorSelect = ImageProvider.getCursor(/* ICON(cursor/)*/ "normal", /* ICON(cursor/modifier/)*/ "mode");
    private final Cursor cursorSelectHover = ImageProvider.getCursor(/* ICON(cursor/)*/ "hand", /* ICON(cursor/modifier/)*/ "mode");
    private final Cursor cursorImprove = ImageProvider.getCursor(CROSSHAIR, null);
    private final Cursor cursorImproveAdd = ImageProvider.getCursor(CROSSHAIR, /* ICON(cursor/modifier/)*/ "addnode");
    private final Cursor cursorImproveDelete = ImageProvider.getCursor(CROSSHAIR, /* ICON(cursor/modifier/)*/ "delete_node");
    private final Cursor cursorImproveAddLock = ImageProvider.getCursor(CROSSHAIR, /* ICON(cursor/modifier/)*/ "add_node_lock");
    private final Cursor cursorImproveLock = ImageProvider.getCursor(CROSSHAIR, /* ICON(cursor/modifier/)*/ "lock");

    private Color guideColor;

    private static final CachingProperty<BasicStroke> SELECT_TARGET_WAY_STROKE
            = new StrokeProperty("improvewayaccuracy.stroke.select-target", "2").cached();
    private static final CachingProperty<BasicStroke> MOVE_NODE_STROKE
            = new StrokeProperty("improvewayaccuracy.stroke.move-node", "1 6").cached();
    private static final CachingProperty<BasicStroke> MOVE_NODE_INTERSECTING_STROKE
            = new StrokeProperty("improvewayaccuracy.stroke.move-node-intersecting", "1 2 6").cached();
    private static final CachingProperty<BasicStroke> ADD_NODE_STROKE
            = new StrokeProperty("improvewayaccuracy.stroke.add-node", "1").cached();
    private static final CachingProperty<BasicStroke> DELETE_NODE_STROKE
            = new StrokeProperty("improvewayaccuracy.stroke.delete-node", "1").cached();
    private static final CachingProperty<Integer> DOT_SIZE
            = new IntegerProperty("improvewayaccuracy.dot-size", 6).cached();

    private boolean selectionChangedBlocked;

    protected String oldModeHelpText;

    private final transient AbstractMapViewPaintable temporaryLayer = new AbstractMapViewPaintable() {
        @Override
        public void paint(Graphics2D g, MapView mv, Bounds bbox) {
            ImproveWayAccuracyAction.this.paint(g, mv, bbox);
        }
    };

    /**
     * Constructs a new {@code ImproveWayAccuracyAction}.
     * @since 11713
     */
    public ImproveWayAccuracyAction() {
        super(tr("Improve Way Accuracy"), "improvewayaccuracy",
                tr("Improve Way Accuracy mode"),
                Shortcut.registerShortcut("mapmode:ImproveWayAccuracy",
                tr("Mode: {0}", tr("Improve Way Accuracy")),
                KeyEvent.VK_W, Shortcut.DIRECT), Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));

        readPreferences();
    }

    // -------------------------------------------------------------------------
    // Mode methods
    // -------------------------------------------------------------------------
    @Override
    public void enterMode() {
        if (!isEnabled()) {
            return;
        }
        super.enterMode();
        readPreferences();

        MapFrame map = MainApplication.getMap();
        mv = map.mapView;
        mousePos = null;
        oldModeHelpText = "";

        if (getLayerManager().getEditDataSet() == null) {
            return;
        }

        updateStateByCurrentSelection();

        map.mapView.addMouseListener(this);
        map.mapView.addMouseMotionListener(this);
        map.mapView.addTemporaryLayer(temporaryLayer);
        SelectionEventManager.getInstance().addSelectionListener(this);

        map.keyDetector.addModifierExListener(this);
    }

    @Override
    protected void readPreferences() {
        guideColor = new NamedColorProperty(marktr("improve way accuracy helper line"), Color.RED).get();
    }

    @Override
    public void exitMode() {
        super.exitMode();

        MapFrame map = MainApplication.getMap();
        map.mapView.removeMouseListener(this);
        map.mapView.removeMouseMotionListener(this);
        map.mapView.removeTemporaryLayer(temporaryLayer);
        SelectionEventManager.getInstance().removeSelectionListener(this);

        map.keyDetector.removeModifierExListener(this);
        temporaryLayer.invalidate();
    }

    @Override
    protected void updateStatusLine() {
        String newModeHelpText = getModeHelpText();
        if (!newModeHelpText.equals(oldModeHelpText)) {
            oldModeHelpText = newModeHelpText;
            MapFrame map = MainApplication.getMap();
            map.statusLine.setHelpText(newModeHelpText);
            map.statusLine.repaint();
        }
    }

    @Override
    public String getModeHelpText() {
        if (state == State.SELECTING) {
            if (targetWay != null) {
                return tr("Click on the way to start improving its shape.");
            } else {
                return tr("Select a way that you want to make more accurate.");
            }
        } else {
            if (ctrl) {
                return tr("Click to add a new node. Release Ctrl to move existing nodes or hold Alt to delete.");
            } else if (alt) {
                return tr("Click to delete the highlighted node. Release Alt to move existing nodes or hold Ctrl to add new nodes.");
            } else {
                return tr("Click to move the highlighted node. Hold Ctrl to add new nodes, or Alt to delete.");
            }
        }
    }

    @Override
    public boolean layerIsSupported(Layer l) {
        return isEditableDataLayer(l);
    }

    @Override
    protected void updateEnabledState() {
        setEnabled(getLayerManager().getEditLayer() != null);
    }

    // -------------------------------------------------------------------------
    // MapViewPaintable methods
    // -------------------------------------------------------------------------
    /**
     * Redraws temporary layer. Highlights targetWay in select mode. Draws
     * preview lines in improve mode and highlights the candidateNode
     * @param g The graphics
     * @param mv The map view
     * @param bbox The bounding box
     */
    public void paint(Graphics2D g, MapView mv, Bounds bbox) {
        if (mousePos == null) {
            return;
        }

        g.setColor(guideColor);

        if (state == State.SELECTING && targetWay != null) {
            // Highlighting the targetWay in Selecting state
            // Non-native highlighting is used, because sometimes highlighted
            // segments are covered with others, which is bad.
            BasicStroke stroke = SELECT_TARGET_WAY_STROKE.get();
            g.setStroke(stroke);

            List<Node> nodes = targetWay.getNodes();

            g.draw(new MapViewPath(mv).append(nodes, false).computeClippedLine(stroke));

        } else if (state == State.IMPROVING) {
            // Drawing preview lines and highlighting the node
            // that is going to be moved.
            // Non-native highlighting is used here as well.

            // Finding endpoints
            Node p1 = null;
            Node p2 = null;
            if (ctrl && candidateSegment != null) {
                g.setStroke(ADD_NODE_STROKE.get());
                try {
                    p1 = candidateSegment.getFirstNode();
                    p2 = candidateSegment.getSecondNode();
                } catch (ArrayIndexOutOfBoundsException e) {
                    Logging.error(e);
                }
            } else if (!alt && !ctrl && candidateNode != null) {
                g.setStroke(MOVE_NODE_STROKE.get());
                List<Pair<Node, Node>> wpps = targetWay.getNodePairs(false);
                for (Pair<Node, Node> wpp : wpps) {
                    if (wpp.a == candidateNode) {
                        p1 = wpp.b;
                    }
                    if (wpp.b == candidateNode) {
                        p2 = wpp.a;
                    }
                    if (p1 != null && p2 != null) {
                        break;
                    }
                }
            } else if (alt && !ctrl && candidateNode != null) {
                g.setStroke(DELETE_NODE_STROKE.get());
                List<Node> nodes = targetWay.getNodes();
                int index = nodes.indexOf(candidateNode);

                // Only draw line if node is not first and/or last
                if (index > 0 && index < (nodes.size() - 1)) {
                    p1 = nodes.get(index - 1);
                    p2 = nodes.get(index + 1);
                } else if (targetWay.isClosed()) {
                    p1 = targetWay.getNode(1);
                    p2 = targetWay.getNode(nodes.size() - 2);
                }
                // TODO: indicate what part that will be deleted? (for end nodes)
            }


            // Drawing preview lines
            MapViewPath b = new MapViewPath(mv);
            if (alt && !ctrl) {
                // In delete mode
                if (p1 != null && p2 != null) {
                    b.moveTo(p1);
                    b.lineTo(p2);
                }
            } else {
                // In add or move mode
                if (p1 != null) {
                    b.moveTo(mousePos.x, mousePos.y);
                    b.lineTo(p1);
                }
                if (p2 != null) {
                    b.moveTo(mousePos.x, mousePos.y);
                    b.lineTo(p2);
                }
            }
            g.draw(b.computeClippedLine(g.getStroke()));

            // Highlighting candidateNode
            if (candidateNode != null) {
                p1 = candidateNode;
                g.fill(new MapViewPath(mv).shapeAround(p1, SymbolShape.SQUARE, DOT_SIZE.get()));
            }

            if (!alt && !ctrl && candidateNode != null) {
                b.reset();
                drawIntersectingWayHelperLines(mv, b);
                g.setStroke(MOVE_NODE_INTERSECTING_STROKE.get());
                g.draw(b.computeClippedLine(g.getStroke()));
            }

        }
    }

    protected void drawIntersectingWayHelperLines(MapView mv, MapViewPath b) {
        for (final OsmPrimitive referrer : candidateNode.getReferrers()) {
            if (!(referrer instanceof Way) || targetWay.equals(referrer)) {
                continue;
            }
            final List<Node> nodes = ((Way) referrer).getNodes();
            for (int i = 0; i < nodes.size(); i++) {
                if (!candidateNode.equals(nodes.get(i))) {
                    continue;
                }
                if (i > 0) {
                    b.moveTo(mousePos.x, mousePos.y);
                    b.lineTo(nodes.get(i - 1));
                }
                if (i < nodes.size() - 1) {
                    b.moveTo(mousePos.x, mousePos.y);
                    b.lineTo(nodes.get(i + 1));
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Event handlers
    // -------------------------------------------------------------------------
    @Override
    public void modifiersExChanged(int modifiers) {
        if (!MainApplication.isDisplayingMapView() || !MainApplication.getMap().mapView.isActiveLayerDrawable()) {
            return;
        }
        updateKeyModifiersEx(modifiers);
        updateCursorDependentObjectsIfNeeded();
        updateCursor();
        updateStatusLine();
        temporaryLayer.invalidate();
    }

    @Override
    public void selectionChanged(SelectionChangeEvent event) {
        if (selectionChangedBlocked) {
            return;
        }
        updateStateByCurrentSelection();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        dragging = true;
        mouseMoved(e);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        if (!isEnabled()) {
            return;
        }

        mousePos = e.getPoint();

        updateKeyModifiers(e);
        updateCursorDependentObjectsIfNeeded();
        updateCursor();
        updateStatusLine();
        temporaryLayer.invalidate();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        dragging = false;
        if (!isEnabled() || e.getButton() != MouseEvent.BUTTON1) {
            return;
        }

        DataSet ds = getLayerManager().getEditDataSet();
        updateKeyModifiers(e);
        mousePos = e.getPoint();

        if (state == State.SELECTING) {
            if (targetWay != null) {
                ds.setSelected(targetWay.getPrimitiveId());
                updateStateByCurrentSelection();
            }
        } else if (state == State.IMPROVING) {
            // Checking if the new coordinate is outside of the world
            if (mv.getLatLon(mousePos.x, mousePos.y).isOutSideWorld()) {
                JOptionPane.showMessageDialog(MainApplication.getMainFrame(),
                        tr("Cannot add a node outside of the world."),
                        tr("Warning"), JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (ctrl && !alt && candidateSegment != null) {
                // Adding a new node to the highlighted segment
                // Important: If there are other ways containing the same
                // segment, a node must added to all of that ways.
                Collection<Command> virtualCmds = new LinkedList<>();

                // Creating a new node
                Node virtualNode = new Node(mv.getEastNorth(mousePos.x,
                        mousePos.y));
                virtualCmds.add(new AddCommand(ds, virtualNode));

                // Looking for candidateSegment copies in ways that are
                // referenced
                // by candidateSegment nodes
                List<Way> firstNodeWays = OsmPrimitive.getFilteredList(
                        candidateSegment.getFirstNode().getReferrers(),
                        Way.class);
                List<Way> secondNodeWays = OsmPrimitive.getFilteredList(
                        candidateSegment.getFirstNode().getReferrers(),
                        Way.class);

                Collection<WaySegment> virtualSegments = new LinkedList<>();
                for (Way w : firstNodeWays) {
                    List<Pair<Node, Node>> wpps = w.getNodePairs(true);
                    for (Way w2 : secondNodeWays) {
                        if (!w.equals(w2)) {
                            continue;
                        }
                        // A way is referenced in both nodes.
                        // Checking if there is such segment
                        int i = -1;
                        for (Pair<Node, Node> wpp : wpps) {
                            ++i;
                            boolean ab = wpp.a.equals(candidateSegment.getFirstNode())
                                    && wpp.b.equals(candidateSegment.getSecondNode());
                            boolean ba = wpp.b.equals(candidateSegment.getFirstNode())
                                    && wpp.a.equals(candidateSegment.getSecondNode());
                            if (ab || ba) {
                                virtualSegments.add(new WaySegment(w, i));
                            }
                        }
                    }
                }

                // Adding the node to all segments found
                for (WaySegment virtualSegment : virtualSegments) {
                    Way w = virtualSegment.way;
                    Way wnew = new Way(w);
                    wnew.addNode(virtualSegment.lowerIndex + 1, virtualNode);
                    virtualCmds.add(new ChangeCommand(w, wnew));
                }

                // Finishing the sequence command
                String text = trn("Add a new node to way",
                        "Add a new node to {0} ways",
                        virtualSegments.size(), virtualSegments.size());

                UndoRedoHandler.getInstance().add(new SequenceCommand(text, virtualCmds));

            } else if (alt && !ctrl && candidateNode != null) {
                // Deleting the highlighted node

                //check to see if node is in use by more than one object
                List<OsmPrimitive> referrers = candidateNode.getReferrers();
                List<Way> ways = OsmPrimitive.getFilteredList(referrers, Way.class);
                if (referrers.size() != 1 || ways.size() != 1) {
                    // detach node from way
                    final Way newWay = new Way(targetWay);
                    final List<Node> nodes = newWay.getNodes();
                    nodes.remove(candidateNode);
                    newWay.setNodes(nodes);
                    if (nodes.size() < 2) {
                        final Command deleteCmd = DeleteCommand.delete(Collections.singleton(targetWay), true);
                        if (deleteCmd != null) {
                            UndoRedoHandler.getInstance().add(deleteCmd);
                        }
                    } else {
                        UndoRedoHandler.getInstance().add(new ChangeCommand(targetWay, newWay));
                    }
                } else if (candidateNode.isTagged()) {
                    JOptionPane.showMessageDialog(MainApplication.getMainFrame(),
                            tr("Cannot delete node that has tags"),
                            tr("Error"), JOptionPane.ERROR_MESSAGE);
                } else {
                    final Command deleteCmd = DeleteCommand.delete(Collections.singleton(candidateNode), true);
                    if (deleteCmd != null) {
                        UndoRedoHandler.getInstance().add(deleteCmd);
                    }
                }

            } else if (candidateNode != null) {
                // Moving the highlighted node
                EastNorth nodeEN = candidateNode.getEastNorth();
                EastNorth cursorEN = mv.getEastNorth(mousePos.x, mousePos.y);

                UndoRedoHandler.getInstance().add(
                        new MoveCommand(candidateNode, cursorEN.east() - nodeEN.east(), cursorEN.north() - nodeEN.north()));
            }
        }

        mousePos = null;
        updateCursor();
        updateStatusLine();
        temporaryLayer.invalidate();
    }

    @Override
    public void mouseExited(MouseEvent e) {
        if (!isEnabled()) {
            return;
        }

        if (!dragging) {
            mousePos = null;
        }
        temporaryLayer.invalidate();
    }

    // -------------------------------------------------------------------------
    // Custom methods
    // -------------------------------------------------------------------------
    /**
     * Sets new cursor depending on state, mouse position
     */
    private void updateCursor() {
        if (!isEnabled()) {
            mv.setNewCursor(null, this);
            return;
        }

        if (state == State.SELECTING) {
            mv.setNewCursor(targetWay == null ? cursorSelect
                    : cursorSelectHover, this);
        } else if (state == State.IMPROVING) {
            if (alt && !ctrl) {
                mv.setNewCursor(cursorImproveDelete, this);
            } else if (shift || dragging) {
                if (ctrl) {
                    mv.setNewCursor(cursorImproveAddLock, this);
                } else {
                    mv.setNewCursor(cursorImproveLock, this);
                }
            } else if (ctrl && !alt) {
                mv.setNewCursor(cursorImproveAdd, this);
            } else {
                mv.setNewCursor(cursorImprove, this);
            }
        }
    }

    /**
     * Updates these objects under cursor: targetWay, candidateNode,
     * candidateSegment
     */
    public void updateCursorDependentObjectsIfNeeded() {
        if (state == State.IMPROVING && (shift || dragging)
                && !(candidateNode == null && candidateSegment == null)) {
            return;
        }

        if (mousePos == null) {
            candidateNode = null;
            candidateSegment = null;
            return;
        }

        if (state == State.SELECTING) {
            targetWay = ImproveWayAccuracyHelper.findWay(mv, mousePos);
        } else if (state == State.IMPROVING) {
            if (ctrl && !alt) {
                candidateSegment = ImproveWayAccuracyHelper.findCandidateSegment(mv,
                        targetWay, mousePos);
                candidateNode = null;
            } else {
                candidateNode = ImproveWayAccuracyHelper.findCandidateNode(mv,
                        targetWay, mousePos);
                candidateSegment = null;
            }
        }
    }

    /**
     * Switches to Selecting state
     */
    public void startSelecting() {
        state = State.SELECTING;

        targetWay = null;

        temporaryLayer.invalidate();
        updateStatusLine();
    }

    /**
     * Switches to Improving state
     *
     * @param targetWay Way that is going to be improved
     */
    public void startImproving(Way targetWay) {
        state = State.IMPROVING;

        DataSet ds = getLayerManager().getEditDataSet();
        Collection<OsmPrimitive> currentSelection = ds.getSelected();
        if (currentSelection.size() != 1
                || !currentSelection.iterator().next().equals(targetWay)) {
            selectionChangedBlocked = true;
            ds.clearSelection();
            ds.setSelected(targetWay.getPrimitiveId());
            selectionChangedBlocked = false;
        }

        this.targetWay = targetWay;
        this.candidateNode = null;
        this.candidateSegment = null;

        temporaryLayer.invalidate();
        updateStatusLine();
    }

    /**
     * Updates the state according to the current selection. Goes to Improve
     * state if a single way or node is selected. Extracts a way by a node in
     * the second case.
     */
    private void updateStateByCurrentSelection() {
        final List<Node> nodeList = new ArrayList<>();
        final List<Way> wayList = new ArrayList<>();
        final DataSet ds = getLayerManager().getEditDataSet();
        if (ds != null) {
            final Collection<OsmPrimitive> sel = ds.getSelected();

            // Collecting nodes and ways from the selection
            for (OsmPrimitive p : sel) {
                if (p instanceof Way) {
                    wayList.add((Way) p);
                }
                if (p instanceof Node) {
                    nodeList.add((Node) p);
                }
            }

            if (wayList.size() == 1) {
                // Starting improving the single selected way
                startImproving(wayList.get(0));
                return;
            } else if (nodeList.size() == 1) {
                // Starting improving the only way of the single selected node
                List<OsmPrimitive> r = nodeList.get(0).getReferrers();
                if (r.size() == 1 && (r.get(0) instanceof Way)) {
                    startImproving((Way) r.get(0));
                    return;
                }
            }
        }

        // Starting selecting by default
        startSelecting();
    }
}
