// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.mapmode;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.actions.MergeNodesAction;
import org.openstreetmap.josm.command.AddCommand;
import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.MoveCommand;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.DataIntegrityProblemException;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.WaySegment;
import org.openstreetmap.josm.data.preferences.NamedColorProperty;
import org.openstreetmap.josm.data.projection.ProjectionRegistry;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MainMenu;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.draw.MapViewPath;
import org.openstreetmap.josm.gui.draw.SymbolShape;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.MapViewPaintable;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.gui.util.KeyPressReleaseListener;
import org.openstreetmap.josm.gui.util.ModifierExListener;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.Geometry;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Makes a rectangle from a line, or modifies a rectangle.
 */
public class ExtrudeAction extends MapMode implements MapViewPaintable, KeyPressReleaseListener, ModifierExListener {

    enum Mode { extrude, translate, select, create_new, translate_node }

    private Mode mode = Mode.select;

    /**
     * If {@code true}, when extruding create new node(s) even if segments are parallel.
     */
    private boolean alwaysCreateNodes;
    private boolean nodeDragWithoutCtrl;

    private long mouseDownTime;
    private transient WaySegment selectedSegment;
    private transient Node selectedNode;
    private Color mainColor;
    private transient Stroke mainStroke;

    /** settings value whether shared nodes should be ignored or not */
    private boolean ignoreSharedNodes;

    private boolean keepSegmentDirection;

    /**
     * drawing settings for helper lines
     */
    private Color helperColor;
    private transient Stroke helperStrokeDash;
    private transient Stroke helperStrokeRA;

    private transient Stroke oldLineStroke;
    private double symbolSize;
    /**
     * Possible directions to move to.
     */
    private transient List<ReferenceSegment> possibleMoveDirections;


    /**
     * Collection of nodes that is moved
     */
    private transient List<Node> movingNodeList;

    /**
     * The direction that is currently active.
     */
    private transient ReferenceSegment activeMoveDirection;

    /**
     * The position of the mouse cursor when the drag action was initiated.
     */
    private Point initialMousePos;
    /**
     * The time which needs to pass between click and release before something
     * counts as a move, in milliseconds
     */
    private int initialMoveDelay = 200;
    /**
     * The minimal shift of mouse (in pixels) befire something counts as move
     */
    private int initialMoveThreshold = 1;

    /**
     * The initial EastNorths of node1 and node2
     */
    private EastNorth initialN1en;
    private EastNorth initialN2en;
    /**
     * The new EastNorths of node1 and node2
     */
    private EastNorth newN1en;
    private EastNorth newN2en;

    /**
     * the command that performed last move.
     */
    private transient MoveCommand moveCommand;
    /**
     *  The command used for dual alignment movement.
     *  Needs to be separate, due to two nodes moving in different directions.
     */
    private transient MoveCommand moveCommand2;

    /** The cursor for the 'create_new' mode. */
    private final Cursor cursorCreateNew;

    /** The cursor for the 'translate' mode. */
    private final Cursor cursorTranslate;

    /** The cursor for the 'alwaysCreateNodes' submode. */
    private final Cursor cursorCreateNodes;

    private static class ReferenceSegment {
        public final EastNorth en;
        public final EastNorth p1;
        public final EastNorth p2;
        public final boolean perpendicular;

        ReferenceSegment(EastNorth en, EastNorth p1, EastNorth p2, boolean perpendicular) {
            this.en = en;
            this.p1 = p1;
            this.p2 = p2;
            this.perpendicular = perpendicular;
        }

        @Override
        public String toString() {
            return "ReferenceSegment[en=" + en + ", p1=" + p1 + ", p2=" + p2 + ", perp=" + perpendicular + ']';
        }
    }

    // Dual alignment mode stuff
    /** {@code true}, if dual alignment mode is enabled. User wants following extrude to be dual aligned. */
    private boolean dualAlignEnabled;
    /** {@code true}, if dual alignment is active. User is dragging the mouse, required conditions are met.
     * Treat {@link #mode} (extrude/translate/create_new) as dual aligned. */
    private boolean dualAlignActive;
    /** Dual alignment reference segments */
    private transient ReferenceSegment dualAlignSegment1, dualAlignSegment2;
    /** {@code true}, if new segment was collapsed */
    private boolean dualAlignSegmentCollapsed;
    // Dual alignment UI stuff
    private final DualAlignChangeAction dualAlignChangeAction;
    private final JCheckBoxMenuItem dualAlignCheckboxMenuItem;
    private final transient Shortcut dualAlignShortcut;
    private boolean useRepeatedShortcut;
    private boolean ignoreNextKeyRelease;

    private class DualAlignChangeAction extends JosmAction {
        DualAlignChangeAction() {
            super(tr("Dual alignment"), /* ICON() */ "mapmode/extrude/dualalign",
                    tr("Switch dual alignment mode while extruding"), null, false);
            putValue("help", ht("/Action/Extrude#DualAlign"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            toggleDualAlign();
        }

        @Override
        protected void updateEnabledState() {
            MapFrame map = MainApplication.getMap();
            setEnabled(map != null && map.mapMode instanceof ExtrudeAction);
        }
    }

    /**
     * Creates a new ExtrudeAction
     * @since 11713
     */
    public ExtrudeAction() {
        super(tr("Extrude"), /* ICON(mapmode/) */ "extrude/extrude", tr("Create areas"),
                Shortcut.registerShortcut("mapmode:extrude", tr("Mode: {0}", tr("Extrude")), KeyEvent.VK_X, Shortcut.DIRECT),
                ImageProvider.getCursor("normal", "rectangle"));
        putValue("help", ht("/Action/Extrude"));
        cursorCreateNew = ImageProvider.getCursor("normal", "rectangle_plus");
        cursorTranslate = ImageProvider.getCursor("normal", "rectangle_move");
        cursorCreateNodes = ImageProvider.getCursor("normal", "rectangle_plussmall");

        dualAlignEnabled = false;
        dualAlignChangeAction = new DualAlignChangeAction();
        dualAlignCheckboxMenuItem = addDualAlignMenuItem();
        dualAlignCheckboxMenuItem.getAction().setEnabled(false);
        dualAlignCheckboxMenuItem.setState(dualAlignEnabled);
        dualAlignShortcut = Shortcut.registerShortcut("mapmode:extrudedualalign",
                tr("Mode: {0}", tr("Extrude Dual alignment")), KeyEvent.CHAR_UNDEFINED, Shortcut.NONE);
        readPreferences(); // to show prefernces in table before entering the mode
    }

    @Override
    public void destroy() {
        super.destroy();
        dualAlignChangeAction.destroy();
    }

    private JCheckBoxMenuItem addDualAlignMenuItem() {
        int n = MainApplication.getMenu().editMenu.getItemCount();
        for (int i = n-1; i > 0; i--) {
            JMenuItem item = MainApplication.getMenu().editMenu.getItem(i);
            if (item != null && item.getAction() != null && item.getAction() instanceof DualAlignChangeAction) {
                MainApplication.getMenu().editMenu.remove(i);
            }
        }
        return MainMenu.addWithCheckbox(MainApplication.getMenu().editMenu, dualAlignChangeAction, MainMenu.WINDOW_MENU_GROUP.VOLATILE);
    }

    // -------------------------------------------------------------------------
    // Mode methods
    // -------------------------------------------------------------------------

    @Override
    public String getModeHelpText() {
        StringBuilder rv;
        if (mode == Mode.select) {
            rv = new StringBuilder(tr("Drag a way segment to make a rectangle. Ctrl-drag to move a segment along its normal, " +
                "Alt-drag to create a new rectangle, double click to add a new node."));
            if (dualAlignEnabled) {
                rv.append(' ').append(tr("Dual alignment active."));
                if (dualAlignSegmentCollapsed)
                    rv.append(' ').append(tr("Segment collapsed due to its direction reversing."));
            }
        } else {
            if (mode == Mode.translate)
                rv = new StringBuilder(tr("Move a segment along its normal, then release the mouse button."));
            else if (mode == Mode.translate_node)
                rv = new StringBuilder(tr("Move the node along one of the segments, then release the mouse button."));
            else if (mode == Mode.extrude || mode == Mode.create_new)
                rv = new StringBuilder(tr("Draw a rectangle of the desired size, then release the mouse button."));
            else {
                Logging.warn("Extrude: unknown mode " + mode);
                rv = new StringBuilder();
            }
            if (dualAlignActive) {
                rv.append(' ').append(tr("Dual alignment active."));
                if (dualAlignSegmentCollapsed) {
                    rv.append(' ').append(tr("Segment collapsed due to its direction reversing."));
                }
            }
        }
        return rv.toString();
    }

    @Override
    public boolean layerIsSupported(Layer l) {
        return isEditableDataLayer(l);
    }

    @Override
    public void enterMode() {
        super.enterMode();
        MapFrame map = MainApplication.getMap();
        map.mapView.addMouseListener(this);
        map.mapView.addMouseMotionListener(this);
        ignoreNextKeyRelease = true;
        map.keyDetector.addKeyListener(this);
        map.keyDetector.addModifierExListener(this);
    }

    @Override
    protected void readPreferences() {
        initialMoveDelay = Config.getPref().getInt("edit.initial-move-delay", 200);
        initialMoveThreshold = Config.getPref().getInt("extrude.initial-move-threshold", 1);
        mainColor = new NamedColorProperty(marktr("Extrude: main line"), Color.RED).get();
        helperColor = new NamedColorProperty(marktr("Extrude: helper line"), Color.ORANGE).get();
        helperStrokeDash = GuiHelper.getCustomizedStroke(Config.getPref().get("extrude.stroke.helper-line", "1 4"));
        helperStrokeRA = new BasicStroke(1);
        symbolSize = Config.getPref().getDouble("extrude.angle-symbol-radius", 8);
        nodeDragWithoutCtrl = Config.getPref().getBoolean("extrude.drag-nodes-without-ctrl", false);
        oldLineStroke = GuiHelper.getCustomizedStroke(Config.getPref().get("extrude.ctrl.stroke.old-line", "1"));
        mainStroke = GuiHelper.getCustomizedStroke(Config.getPref().get("extrude.stroke.main", "3"));

        ignoreSharedNodes = Config.getPref().getBoolean("extrude.ignore-shared-nodes", true);
        dualAlignCheckboxMenuItem.getAction().setEnabled(true);
        useRepeatedShortcut = Config.getPref().getBoolean("extrude.dualalign.toggleOnRepeatedX", true);
        keepSegmentDirection = Config.getPref().getBoolean("extrude.dualalign.keep-segment-direction", true);
    }

    @Override
    public void exitMode() {
        MapFrame map = MainApplication.getMap();
        map.mapView.removeMouseListener(this);
        map.mapView.removeMouseMotionListener(this);
        map.mapView.removeTemporaryLayer(this);
        dualAlignCheckboxMenuItem.getAction().setEnabled(false);
        map.keyDetector.removeKeyListener(this);
        map.keyDetector.removeModifierExListener(this);
        super.exitMode();
    }

    // -------------------------------------------------------------------------
    // Event handlers
    // -------------------------------------------------------------------------

    /**
     * This method is called to indicate different modes via cursor when the Alt/Ctrl/Shift modifier is pressed,
     */
    @Override
    public void modifiersExChanged(int modifiers) {
        MapFrame map = MainApplication.getMap();
        if (!MainApplication.isDisplayingMapView() || !map.mapView.isActiveLayerDrawable())
            return;
        updateKeyModifiersEx(modifiers);
        if (mode == Mode.select) {
            map.mapView.setNewCursor(ctrl ? cursorTranslate : alt ? cursorCreateNew : shift ? cursorCreateNodes : cursor, this);
        }
    }

    @Override
    public void doKeyPressed(KeyEvent e) {
        // Do nothing
    }

    @Override
    public void doKeyReleased(KeyEvent e) {
        if (!dualAlignShortcut.isEvent(e) && !(useRepeatedShortcut && getShortcut().isEvent(e)))
             return;
        if (ignoreNextKeyRelease) {
            ignoreNextKeyRelease = false;
        } else {
            toggleDualAlign();
        }
    }

    /**
     * Toggles dual alignment mode.
     */
    private void toggleDualAlign() {
        dualAlignEnabled = !dualAlignEnabled;
        dualAlignCheckboxMenuItem.setState(dualAlignEnabled);
        updateStatusLine();
    }

    /**
     * If the left mouse button is pressed over a segment or a node, switches
     * to appropriate {@link #mode}, depending on Ctrl/Alt/Shift modifiers and
     * {@link #dualAlignEnabled}.
     * @param e current mouse event
     */
    @Override
    public void mousePressed(MouseEvent e) {
        MapFrame map = MainApplication.getMap();
        if (!map.mapView.isActiveLayerVisible())
            return;
        if (!(Boolean) this.getValue("active"))
            return;
        if (e.getButton() != MouseEvent.BUTTON1)
            return;

        requestFocusInMapView();
        updateKeyModifiers(e);

        selectedNode = map.mapView.getNearestNode(e.getPoint(), OsmPrimitive::isSelectable);
        selectedSegment = map.mapView.getNearestWaySegment(e.getPoint(), OsmPrimitive::isSelectable);

        // If nothing gets caught, stay in select mode
        if (selectedSegment == null && selectedNode == null) return;

        if (selectedNode != null) {
            if (ctrl || nodeDragWithoutCtrl) {
                movingNodeList = new ArrayList<>();
                movingNodeList.add(selectedNode);
                calculatePossibleDirectionsByNode();
                if (possibleMoveDirections.isEmpty()) {
                    // if no directions fould, do not enter dragging mode
                    return;
                }
                mode = Mode.translate_node;
                dualAlignActive = false;
            }
        } else {
            // Otherwise switch to another mode
            if (dualAlignEnabled && checkDualAlignConditions()) {
                dualAlignActive = true;
                calculatePossibleDirectionsForDualAlign();
                dualAlignSegmentCollapsed = false;
            } else {
                dualAlignActive = false;
                calculatePossibleDirectionsBySegment();
            }
            if (ctrl) {
                mode = Mode.translate;
                movingNodeList = new ArrayList<>();
                movingNodeList.add(selectedSegment.getFirstNode());
                movingNodeList.add(selectedSegment.getSecondNode());
            } else if (alt) {
                mode = Mode.create_new;
                // create a new segment and then select and extrude the new segment
                getLayerManager().getEditDataSet().setSelected(selectedSegment.way);
                alwaysCreateNodes = true;
            } else {
                mode = Mode.extrude;
                getLayerManager().getEditDataSet().setSelected(selectedSegment.way);
                alwaysCreateNodes = shift;
            }
        }

        // Signifies that nothing has happened yet
        newN1en = null;
        newN2en = null;
        moveCommand = null;
        moveCommand2 = null;

        map.mapView.addTemporaryLayer(this);

        updateStatusLine();
        map.mapView.repaint();

        // Make note of time pressed
        mouseDownTime = System.currentTimeMillis();

        // Make note of mouse position
        initialMousePos = e.getPoint();
   }

    /**
     * Performs action depending on what {@link #mode} we're in.
     * @param e current mouse event
     */
    @Override
    public void mouseDragged(MouseEvent e) {
        MapView mapView = MainApplication.getMap().mapView;
        if (!mapView.isActiveLayerVisible())
            return;

        // do not count anything as a drag if it lasts less than 100 milliseconds.
        if (System.currentTimeMillis() - mouseDownTime < initialMoveDelay)
            return;

        if (mode == Mode.select) {
            // Just sit tight and wait for mouse to be released.
        } else {
            //move, create new and extrude mode - move the selected segment

            EastNorth mouseEn = mapView.getEastNorth(e.getPoint().x, e.getPoint().y);
            EastNorth bestMovement = calculateBestMovementAndNewNodes(mouseEn);

            mapView.setNewCursor(Cursor.MOVE_CURSOR, this);

            if (dualAlignActive) {
                if (mode == Mode.extrude || mode == Mode.create_new) {
                    // nothing here
                } else if (mode == Mode.translate) {
                    EastNorth movement1 = newN1en.subtract(initialN1en);
                    EastNorth movement2 = newN2en.subtract(initialN2en);
                    // move nodes to new position
                    if (moveCommand == null || moveCommand2 == null) {
                        // make a new move commands
                        moveCommand = new MoveCommand(movingNodeList.get(0), movement1.getX(), movement1.getY());
                        moveCommand2 = new MoveCommand(movingNodeList.get(1), movement2.getX(), movement2.getY());
                        Command c = new SequenceCommand(tr("Extrude Way"), moveCommand, moveCommand2);
                        MainApplication.undoRedo.add(c);
                    } else {
                        // reuse existing move commands
                        moveCommand.moveAgainTo(movement1.getX(), movement1.getY());
                        moveCommand2.moveAgainTo(movement2.getX(), movement2.getY());
                    }
                }
            } else if (bestMovement != null) {
                if (mode == Mode.extrude || mode == Mode.create_new) {
                    //nothing here
                } else if (mode == Mode.translate_node || mode == Mode.translate) {
                    //move nodes to new position
                    if (moveCommand == null) {
                        //make a new move command
                        moveCommand = new MoveCommand(new ArrayList<OsmPrimitive>(movingNodeList), bestMovement);
                        MainApplication.undoRedo.add(moveCommand);
                    } else {
                        //reuse existing move command
                        moveCommand.moveAgainTo(bestMovement.getX(), bestMovement.getY());
                    }
                }
            }

            mapView.repaint();
        }
    }

    /**
     * Does anything that needs to be done, then switches back to select mode.
     * @param e current mouse event
     */
    @Override
    public void mouseReleased(MouseEvent e) {

        MapView mapView = MainApplication.getMap().mapView;
        if (!mapView.isActiveLayerVisible())
            return;

        if (mode == Mode.select) {
            // Nothing to be done
        } else {
            if (mode == Mode.create_new) {
                if (e.getPoint().distance(initialMousePos) > initialMoveThreshold && newN1en != null) {
                    createNewRectangle();
                }
            } else if (mode == Mode.extrude) {
                if (e.getClickCount() == 2 && e.getPoint().equals(initialMousePos)) {
                    // double click adds a new node
                    addNewNode(e);
                } else if (e.getPoint().distance(initialMousePos) > initialMoveThreshold && newN1en != null && selectedSegment != null) {
                    try {
                        // main extrusion commands
                        performExtrusion();
                    } catch (DataIntegrityProblemException ex) {
                        // Can occur if calling undo while extruding, see #12870
                        Logging.error(ex);
                    }
                }
            } else if (mode == Mode.translate || mode == Mode.translate_node) {
                //Commit translate
                //the move command is already committed in mouseDragged
                joinNodesIfCollapsed(movingNodeList);
            }

            updateKeyModifiers(e);
            // Switch back into select mode
            mapView.setNewCursor(ctrl ? cursorTranslate : alt ? cursorCreateNew : shift ? cursorCreateNodes : cursor, this);
            mapView.removeTemporaryLayer(this);
            selectedSegment = null;
            moveCommand = null;
            mode = Mode.select;
            dualAlignSegmentCollapsed = false;
            updateStatusLine();
            mapView.repaint();
        }
    }

    // -------------------------------------------------------------------------
    // Custom methods
    // -------------------------------------------------------------------------

    /**
     * Inserts node into nearby segment.
     * @param e current mouse point
     */
    private static void addNewNode(MouseEvent e) {
        // Should maybe do the same as in DrawAction and fetch all nearby segments?
        MapView mapView = MainApplication.getMap().mapView;
        WaySegment ws = mapView.getNearestWaySegment(e.getPoint(), OsmPrimitive::isSelectable);
        if (ws != null) {
            Node n = new Node(mapView.getLatLon(e.getX(), e.getY()));
            EastNorth a = ws.getFirstNode().getEastNorth();
            EastNorth b = ws.getSecondNode().getEastNorth();
            n.setEastNorth(Geometry.closestPointToSegment(a, b, n.getEastNorth()));
            Way wnew = new Way(ws.way);
            wnew.addNode(ws.lowerIndex+1, n);
            DataSet ds = ws.way.getDataSet();
            MainApplication.undoRedo.add(new SequenceCommand(tr("Add a new node to an existing way"),
                    new AddCommand(ds, n), new ChangeCommand(ds, ws.way, wnew)));
        }
    }

    /**
     * Creates a new way that shares segment with selected way.
     */
    private void createNewRectangle() {
        if (selectedSegment == null) return;
        DataSet ds = getLayerManager().getEditDataSet();
        // create a new rectangle
        Collection<Command> cmds = new LinkedList<>();
        Node third = new Node(newN2en);
        Node fourth = new Node(newN1en);
        Way wnew = new Way();
        wnew.addNode(selectedSegment.getFirstNode());
        wnew.addNode(selectedSegment.getSecondNode());
        wnew.addNode(third);
        if (!dualAlignSegmentCollapsed) {
            // rectangle can degrade to triangle for dual alignment after collapsing
            wnew.addNode(fourth);
        }
        // ... and close the way
        wnew.addNode(selectedSegment.getFirstNode());
        // undo support
        cmds.add(new AddCommand(ds, third));
        if (!dualAlignSegmentCollapsed) {
            cmds.add(new AddCommand(ds, fourth));
        }
        cmds.add(new AddCommand(ds, wnew));
        Command c = new SequenceCommand(tr("Extrude Way"), cmds);
        MainApplication.undoRedo.add(c);
        ds.setSelected(wnew);
    }

    /**
     * Does actual extrusion of {@link #selectedSegment}.
     * Uses {@link #initialN1en}, {@link #initialN2en} saved in calculatePossibleDirections* call
     * Uses {@link #newN1en}, {@link #newN2en} calculated by {@link #calculateBestMovementAndNewNodes}
     */
    private void performExtrusion() {
        DataSet ds = getLayerManager().getEditDataSet();
        // create extrusion
        Collection<Command> cmds = new LinkedList<>();
        Way wnew = new Way(selectedSegment.way);
        boolean wayWasModified = false;
        boolean wayWasSingleSegment = wnew.getNodesCount() == 2;
        int insertionPoint = selectedSegment.lowerIndex + 1;

        //find if the new points overlap existing segments (in case of 90 degree angles)
        Node prevNode = getPreviousNode(selectedSegment.lowerIndex);
        boolean nodeOverlapsSegment = prevNode != null && Geometry.segmentsParallel(initialN1en, prevNode.getEastNorth(), initialN1en, newN1en);
        // segmentAngleZero marks subset of nodeOverlapsSegment.
        // nodeOverlapsSegment is true if angle between segments is 0 or PI, segmentAngleZero only if angle is 0
        boolean segmentAngleZero = prevNode != null && Math.abs(Geometry.getCornerAngle(prevNode.getEastNorth(), initialN1en, newN1en)) < 1e-5;
        boolean hasOtherWays = hasNodeOtherWays(selectedSegment.getFirstNode(), selectedSegment.way);
        List<Node> changedNodes = new ArrayList<>();
        if (nodeOverlapsSegment && !alwaysCreateNodes && !hasOtherWays) {
            //move existing node
            Node n1Old = selectedSegment.getFirstNode();
            cmds.add(new MoveCommand(n1Old, ProjectionRegistry.getProjection().eastNorth2latlon(newN1en)));
            changedNodes.add(n1Old);
        } else if (ignoreSharedNodes && segmentAngleZero && !alwaysCreateNodes && hasOtherWays) {
            // replace shared node with new one
            Node n1Old = selectedSegment.getFirstNode();
            Node n1New = new Node(ProjectionRegistry.getProjection().eastNorth2latlon(newN1en));
            wnew.addNode(insertionPoint, n1New);
            wnew.removeNode(n1Old);
            wayWasModified = true;
            cmds.add(new AddCommand(ds, n1New));
            changedNodes.add(n1New);
        } else {
            //introduce new node
            Node n1New = new Node(ProjectionRegistry.getProjection().eastNorth2latlon(newN1en));
            wnew.addNode(insertionPoint, n1New);
            wayWasModified = true;
            insertionPoint++;
            cmds.add(new AddCommand(ds, n1New));
            changedNodes.add(n1New);
        }

        //find if the new points overlap existing segments (in case of 90 degree angles)
        Node nextNode = getNextNode(selectedSegment.lowerIndex + 1);
        nodeOverlapsSegment = nextNode != null && Geometry.segmentsParallel(initialN2en, nextNode.getEastNorth(), initialN2en, newN2en);
        segmentAngleZero = nextNode != null && Math.abs(Geometry.getCornerAngle(nextNode.getEastNorth(), initialN2en, newN2en)) < 1e-5;
        hasOtherWays = hasNodeOtherWays(selectedSegment.getSecondNode(), selectedSegment.way);

        if (nodeOverlapsSegment && !alwaysCreateNodes && !hasOtherWays) {
            //move existing node
            Node n2Old = selectedSegment.getSecondNode();
            cmds.add(new MoveCommand(n2Old, ProjectionRegistry.getProjection().eastNorth2latlon(newN2en)));
            changedNodes.add(n2Old);
        } else if (ignoreSharedNodes && segmentAngleZero && !alwaysCreateNodes && hasOtherWays) {
            // replace shared node with new one
            Node n2Old = selectedSegment.getSecondNode();
            Node n2New = new Node(ProjectionRegistry.getProjection().eastNorth2latlon(newN2en));
            wnew.addNode(insertionPoint, n2New);
            wnew.removeNode(n2Old);
            wayWasModified = true;
            cmds.add(new AddCommand(ds, n2New));
            changedNodes.add(n2New);
        } else {
            //introduce new node
            Node n2New = new Node(ProjectionRegistry.getProjection().eastNorth2latlon(newN2en));
            wnew.addNode(insertionPoint, n2New);
            wayWasModified = true;
            cmds.add(new AddCommand(ds, n2New));
            changedNodes.add(n2New);
        }

        //the way was a single segment, close the way
        if (wayWasSingleSegment) {
            wnew.addNode(selectedSegment.getFirstNode());
            wayWasModified = true;
        }
        if (wayWasModified) {
            // we only need to change the way if its node list was really modified
            cmds.add(new ChangeCommand(selectedSegment.way, wnew));
        }
        Command c = new SequenceCommand(tr("Extrude Way"), cmds);
        MainApplication.undoRedo.add(c);
        joinNodesIfCollapsed(changedNodes);
    }

    private void joinNodesIfCollapsed(List<Node> changedNodes) {
        if (!dualAlignActive || newN1en == null || newN2en == null) return;
        if (newN1en.distance(newN2en) > 1e-6) return;
        // If the dual alignment moved two nodes to the same point, merge them
        Node targetNode = MergeNodesAction.selectTargetNode(changedNodes);
        Node locNode = MergeNodesAction.selectTargetLocationNode(changedNodes);
        Command mergeCmd = MergeNodesAction.mergeNodes(changedNodes, targetNode, locNode);
        if (mergeCmd != null) {
            MainApplication.undoRedo.add(mergeCmd);
        } else {
            // undo extruding command itself
            MainApplication.undoRedo.undo();
        }
    }

    /**
     * This method tests if {@code node} has other ways apart from the given one.
     * @param node node to test
     * @param myWay way known to contain this node
     * @return {@code true} if {@code node} belongs only to {@code myWay}, false if there are more ways.
     */
    private static boolean hasNodeOtherWays(Node node, Way myWay) {
        for (OsmPrimitive p : node.getReferrers()) {
            if (p instanceof Way && p.isUsable() && p != myWay)
                return true;
        }
        return false;
    }

    /**
     * Determines best movement from {@link #initialMousePos} to current mouse position,
     * choosing one of the directions from {@link #possibleMoveDirections}.
     * @param mouseEn current mouse position
     * @return movement vector
     */
    private EastNorth calculateBestMovement(EastNorth mouseEn) {

        EastNorth initialMouseEn = MainApplication.getMap().mapView.getEastNorth(initialMousePos.x, initialMousePos.y);
        EastNorth mouseMovement = mouseEn.subtract(initialMouseEn);

        double bestDistance = Double.POSITIVE_INFINITY;
        EastNorth bestMovement = null;
        activeMoveDirection = null;

        //find the best movement direction and vector
        for (ReferenceSegment direction : possibleMoveDirections) {
            EastNorth movement = calculateSegmentOffset(initialN1en, initialN2en, direction.en, mouseEn);
            if (movement == null) {
                //if direction parallel to segment.
                continue;
            }

            double distanceFromMouseMovement = movement.distance(mouseMovement);
            if (bestDistance > distanceFromMouseMovement) {
                bestDistance = distanceFromMouseMovement;
                activeMoveDirection = direction;
                bestMovement = movement;
            }
        }
        return bestMovement;
    }

    /***
     * This method calculates offset amount by which to move the given segment
     * perpendicularly for it to be in line with mouse position.
     * @param segmentP1 segment's first point
     * @param segmentP2 segment's second point
     * @param moveDirection direction of movement
     * @param targetPos mouse position
     * @return offset amount of P1 and P2.
     */
    private static EastNorth calculateSegmentOffset(EastNorth segmentP1, EastNorth segmentP2, EastNorth moveDirection,
            EastNorth targetPos) {
        EastNorth intersectionPoint;
        if (segmentP1.distanceSq(segmentP2) > 1e-7) {
            intersectionPoint = Geometry.getLineLineIntersection(segmentP1, segmentP2, targetPos, targetPos.add(moveDirection));
        } else {
            intersectionPoint = Geometry.closestPointToLine(targetPos, targetPos.add(moveDirection), segmentP1);
        }

        if (intersectionPoint == null)
            return null;
        else
            //return distance form base to target position
            return targetPos.subtract(intersectionPoint);
    }

    /**
     * Gathers possible move directions - perpendicular to the selected segment
     * and parallel to neighboring segments.
     */
    private void calculatePossibleDirectionsBySegment() {
        // remember initial positions for segment nodes.
        initialN1en = selectedSegment.getFirstNode().getEastNorth();
        initialN2en = selectedSegment.getSecondNode().getEastNorth();

        //add direction perpendicular to the selected segment
        possibleMoveDirections = new ArrayList<>();
        possibleMoveDirections.add(new ReferenceSegment(new EastNorth(
                initialN1en.getY() - initialN2en.getY(),
                initialN2en.getX() - initialN1en.getX()
                ), initialN1en, initialN2en, true));


        //add directions parallel to neighbor segments
        Node prevNode = getPreviousNode(selectedSegment.lowerIndex);
        if (prevNode != null) {
            EastNorth en = prevNode.getEastNorth();
            possibleMoveDirections.add(new ReferenceSegment(new EastNorth(
                    initialN1en.getX() - en.getX(),
                    initialN1en.getY() - en.getY()
                    ), initialN1en, en, false));
        }

        Node nextNode = getNextNode(selectedSegment.lowerIndex + 1);
        if (nextNode != null) {
            EastNorth en = nextNode.getEastNorth();
            possibleMoveDirections.add(new ReferenceSegment(new EastNorth(
                    initialN2en.getX() - en.getX(),
                    initialN2en.getY() - en.getY()
                    ), initialN2en, en, false));
        }
    }

    /**
     * Gathers possible move directions - along all adjacent segments.
     */
    private void calculatePossibleDirectionsByNode() {
        // remember initial positions for segment nodes.
        initialN1en = selectedNode.getEastNorth();
        initialN2en = initialN1en;
        possibleMoveDirections = new ArrayList<>();
        for (OsmPrimitive p: selectedNode.getReferrers()) {
            if (p instanceof Way && p.isUsable()) {
                for (Node neighbor: ((Way) p).getNeighbours(selectedNode)) {
                    EastNorth en = neighbor.getEastNorth();
                    possibleMoveDirections.add(new ReferenceSegment(new EastNorth(
                        initialN1en.getX() - en.getX(),
                        initialN1en.getY() - en.getY()
                    ), initialN1en, en, false));
                }
            }
        }
    }

    /**
     * Checks dual alignment conditions:
     *  1. selected segment has both neighboring segments,
     *  2. selected segment is not parallel with neighboring segments.
     * @return {@code true} if dual alignment conditions are satisfied
     */
    private boolean checkDualAlignConditions() {
        Node prevNode = getPreviousNode(selectedSegment.lowerIndex);
        Node nextNode = getNextNode(selectedSegment.lowerIndex + 1);
        if (prevNode == null || nextNode == null) {
            return false;
        }

        EastNorth n1en = selectedSegment.getFirstNode().getEastNorth();
        EastNorth n2en = selectedSegment.getSecondNode().getEastNorth();
        if (n1en.distance(prevNode.getEastNorth()) < 1e-4 ||
            n2en.distance(nextNode.getEastNorth()) < 1e-4) {
            return false;
        }

        boolean prevSegmentParallel = Geometry.segmentsParallel(n1en, prevNode.getEastNorth(), n1en, n2en);
        boolean nextSegmentParallel = Geometry.segmentsParallel(n2en, nextNode.getEastNorth(), n1en, n2en);
        return !prevSegmentParallel && !nextSegmentParallel;
    }

    /**
     * Gathers possible move directions - perpendicular to the selected segment only.
     * Neighboring segments go to {@link #dualAlignSegment1} and {@link #dualAlignSegment2}.
     */
    private void calculatePossibleDirectionsForDualAlign() {
        // remember initial positions for segment nodes.
        initialN1en = selectedSegment.getFirstNode().getEastNorth();
        initialN2en = selectedSegment.getSecondNode().getEastNorth();

        // add direction perpendicular to the selected segment
        possibleMoveDirections = new ArrayList<>();
        possibleMoveDirections.add(new ReferenceSegment(new EastNorth(
                initialN1en.getY() - initialN2en.getY(),
                initialN2en.getX() - initialN1en.getX()
                ), initialN1en, initialN2en, true));

        // set neighboring segments
        Node prevNode = getPreviousNode(selectedSegment.lowerIndex);
        if (prevNode != null) {
            EastNorth prevNodeEn = prevNode.getEastNorth();
            dualAlignSegment1 = new ReferenceSegment(new EastNorth(
                initialN1en.getX() - prevNodeEn.getX(),
                initialN1en.getY() - prevNodeEn.getY()
                ), initialN1en, prevNodeEn, false);
        }

        Node nextNode = getNextNode(selectedSegment.lowerIndex + 1);
        if (nextNode != null) {
            EastNorth nextNodeEn = nextNode.getEastNorth();
            dualAlignSegment2 = new ReferenceSegment(new EastNorth(
                initialN2en.getX() - nextNodeEn.getX(),
                initialN2en.getY() - nextNodeEn.getY()
                ), initialN2en, nextNodeEn, false);
        }
    }

    /**
     * Calculate newN1en, newN2en best suitable for given mouse coordinates
     * For dual align, calculates positions of new nodes, aligning them to neighboring segments.
     * Elsewhere, just adds the vetor returned by calculateBestMovement to {@link #initialN1en},  {@link #initialN2en}.
     * @param mouseEn mouse coordinates
     * @return best movement vector
     */
    private EastNorth calculateBestMovementAndNewNodes(EastNorth mouseEn) {
        EastNorth bestMovement = calculateBestMovement(mouseEn);
        EastNorth n1movedEn = initialN1en.add(bestMovement), n2movedEn;

        // find out the movement distance, in metres
        double distance = ProjectionRegistry.getProjection().eastNorth2latlon(initialN1en).greatCircleDistance(
                ProjectionRegistry.getProjection().eastNorth2latlon(n1movedEn));
        MainApplication.getMap().statusLine.setDist(distance);
        updateStatusLine();

        if (dualAlignActive) {
            // new positions of selected segment's nodes, without applying dual alignment
            n1movedEn = initialN1en.add(bestMovement);
            n2movedEn = initialN2en.add(bestMovement);

            // calculate intersections of parallel shifted segment and the adjacent lines
            newN1en = Geometry.getLineLineIntersection(n1movedEn, n2movedEn, dualAlignSegment1.p1, dualAlignSegment1.p2);
            newN2en = Geometry.getLineLineIntersection(n1movedEn, n2movedEn, dualAlignSegment2.p1, dualAlignSegment2.p2);
            if (newN1en == null || newN2en == null) return bestMovement;
            if (keepSegmentDirection && isOppositeDirection(newN1en, newN2en, initialN1en, initialN2en)) {
                EastNorth collapsedSegmentPosition = Geometry.getLineLineIntersection(dualAlignSegment1.p1, dualAlignSegment1.p2,
                        dualAlignSegment2.p1, dualAlignSegment2.p2);
                newN1en = collapsedSegmentPosition;
                newN2en = collapsedSegmentPosition;
                dualAlignSegmentCollapsed = true;
            } else {
                dualAlignSegmentCollapsed = false;
            }
        } else {
            newN1en = n1movedEn;
            newN2en = initialN2en.add(bestMovement);
        }
        return bestMovement;
    }

    /**
     * Gets a node index from selected way before given index.
     * @param index  index of current node
     * @return index of previous node or <code>-1</code> if there are no nodes there.
     */
    private int getPreviousNodeIndex(int index) {
        if (index > 0)
            return index - 1;
        else if (selectedSegment.way.isClosed())
            return selectedSegment.way.getNodesCount() - 2;
        else
            return -1;
    }

    /**
     * Gets a node from selected way before given index.
     * @param index  index of current node
     * @return previous node or <code>null</code> if there are no nodes there.
     */
    private Node getPreviousNode(int index) {
        int indexPrev = getPreviousNodeIndex(index);
        if (indexPrev >= 0)
            return selectedSegment.way.getNode(indexPrev);
        else
            return null;
    }


    /**
     * Gets a node index from selected way after given index.
     * @param index index of current node
     * @return index of next node or <code>-1</code> if there are no nodes there.
     */
    private int getNextNodeIndex(int index) {
        int count = selectedSegment.way.getNodesCount();
        if (index < count - 1)
            return index + 1;
        else if (selectedSegment.way.isClosed())
            return 1;
        else
            return -1;
    }

    /**
     * Gets a node from selected way after given index.
     * @param index index of current node
     * @return next node or <code>null</code> if there are no nodes there.
     */
    private Node getNextNode(int index) {
        int indexNext = getNextNodeIndex(index);
        if (indexNext >= 0)
            return selectedSegment.way.getNode(indexNext);
        else
            return null;
    }

    // -------------------------------------------------------------------------
    // paint methods
    // -------------------------------------------------------------------------

    @Override
    public void paint(Graphics2D g, MapView mv, Bounds box) {
        Graphics2D g2 = g;
        if (mode == Mode.select) {
            // Nothing to do
        } else {
            if (newN1en != null) {

                EastNorth p1 = initialN1en;
                EastNorth p2 = initialN2en;
                EastNorth p3 = newN1en;
                EastNorth p4 = newN2en;

                Point2D normalUnitVector = activeMoveDirection != null ? getNormalUniVector() : null;

                if (mode == Mode.extrude || mode == Mode.create_new) {
                    g2.setColor(mainColor);
                    g2.setStroke(mainStroke);
                    // Draw rectangle around new area.
                    MapViewPath b = new MapViewPath(mv);
                    b.moveTo(p1);
                    b.lineTo(p3);
                    b.lineTo(p4);
                    b.lineTo(p2);
                    b.lineTo(p1);
                    g2.draw(b);

                    if (dualAlignActive) {
                        // Draw reference ways
                        drawReferenceSegment(g2, mv, dualAlignSegment1);
                        drawReferenceSegment(g2, mv, dualAlignSegment2);
                    } else if (activeMoveDirection != null && normalUnitVector != null) {
                        // Draw reference way
                        drawReferenceSegment(g2, mv, activeMoveDirection);

                        // Draw right angle marker on first node position, only when moving at right angle
                        if (activeMoveDirection.perpendicular) {
                            // mirror RightAngle marker, so it is inside the extrude
                            double headingRefWS = activeMoveDirection.p1.heading(activeMoveDirection.p2);
                            double headingMoveDir = Math.atan2(normalUnitVector.getY(), normalUnitVector.getX());
                            double headingDiff = headingRefWS - headingMoveDir;
                            if (headingDiff < 0)
                                headingDiff += 2 * Math.PI;
                            boolean mirrorRA = Math.abs(headingDiff - Math.PI) > 1e-5;
                            Point pr1 = mv.getPoint(activeMoveDirection.p1);
                            drawAngleSymbol(g2, pr1, normalUnitVector, mirrorRA);
                        }
                    }
                } else if (mode == Mode.translate || mode == Mode.translate_node) {
                    g2.setColor(mainColor);
                    if (p1.distance(p2) < 3) {
                        g2.setStroke(mainStroke);
                        g2.draw(new MapViewPath(mv).shapeAround(p1, SymbolShape.CIRCLE, symbolSize));
                    } else {
                        g2.setStroke(oldLineStroke);
                        g2.draw(new MapViewPath(mv).moveTo(p1).lineTo(p2));
                    }

                    if (dualAlignActive) {
                        // Draw reference ways
                        drawReferenceSegment(g2, mv, dualAlignSegment1);
                        drawReferenceSegment(g2, mv, dualAlignSegment2);
                    } else if (activeMoveDirection != null) {

                        g2.setColor(helperColor);
                        g2.setStroke(helperStrokeDash);
                        // Draw a guideline along the normal.
                        Point2D centerpoint = mv.getPoint2D(p1.interpolate(p2, .5));
                        g2.draw(createSemiInfiniteLine(centerpoint, normalUnitVector, g2));
                        // Draw right angle marker on initial position, only when moving at right angle
                        if (activeMoveDirection.perpendicular) {
                            // EastNorth units per pixel
                            g2.setStroke(helperStrokeRA);
                            g2.setColor(mainColor);
                            drawAngleSymbol(g2, centerpoint, normalUnitVector, false);
                        }
                    }
                }
            }
            g2.setStroke(helperStrokeRA); // restore default stroke to prevent starnge occasional drawings
        }
    }

    private Point2D getNormalUniVector() {
        double fac = 1.0 / activeMoveDirection.en.length();
        // mult by factor to get unit vector.
        Point2D normalUnitVector = new Point2D.Double(activeMoveDirection.en.getX() * fac, activeMoveDirection.en.getY() * fac);

        // Check to see if our new N1 is in a positive direction with respect to the normalUnitVector.
        // Even if the x component is zero, we should still be able to discern using +0.0 and -0.0
        if (newN1en != null && ((newN1en.getX() > initialN1en.getX()) != (normalUnitVector.getX() > -0.0))) {
            // If not, use a sign-flipped version of the normalUnitVector.
            normalUnitVector = new Point2D.Double(-normalUnitVector.getX(), -normalUnitVector.getY());
        }

        //HACK: swap Y, because the target pixels are top down, but EastNorth is bottom-up.
        //This is normally done by MapView.getPoint, but it does not work on vectors.
        normalUnitVector.setLocation(normalUnitVector.getX(), -normalUnitVector.getY());
        return normalUnitVector;
    }

    /**
     * Determines if from1-to1 and from2-to2 vectors directions are opposite
     * @param from1 vector1 start
     * @param to1 vector1 end
     * @param from2 vector2 start
     * @param to2 vector2 end
     * @return true if from1-to1 and from2-to2 vectors directions are opposite
     */
    private static boolean isOppositeDirection(EastNorth from1, EastNorth to1, EastNorth from2, EastNorth to2) {
        return (from1.getX()-to1.getX())*(from2.getX()-to2.getX())
              +(from1.getY()-to1.getY())*(from2.getY()-to2.getY()) < 0;
    }

    /**
     * Draws right angle symbol at specified position.
     * @param g2 the Graphics2D object used to draw on
     * @param center center point of angle
     * @param normal vector of normal
     * @param mirror {@code true} if symbol should be mirrored by the normal
     */
    private void drawAngleSymbol(Graphics2D g2, Point2D center, Point2D normal, boolean mirror) {
        // EastNorth units per pixel
        double factor = 1.0/g2.getTransform().getScaleX();
        double raoffsetx = symbolSize*factor*normal.getX();
        double raoffsety = symbolSize*factor*normal.getY();

        double cx = center.getX(), cy = center.getY();
        double k = mirror ? -1 : 1;
        Point2D ra1 = new Point2D.Double(cx + raoffsetx, cy + raoffsety);
        Point2D ra3 = new Point2D.Double(cx - raoffsety*k, cy + raoffsetx*k);
        Point2D ra2 = new Point2D.Double(ra1.getX() - raoffsety*k, ra1.getY() + raoffsetx*k);

        GeneralPath ra = new GeneralPath();
        ra.moveTo((float) ra1.getX(), (float) ra1.getY());
        ra.lineTo((float) ra2.getX(), (float) ra2.getY());
        ra.lineTo((float) ra3.getX(), (float) ra3.getY());
        g2.setStroke(helperStrokeRA);
        g2.draw(ra);
    }

    /**
     * Draws given reference segment.
     * @param g2 the Graphics2D object used to draw on
     * @param mv map view
     * @param seg the reference segment
     */
    private void drawReferenceSegment(Graphics2D g2, MapView mv, ReferenceSegment seg) {
        g2.setColor(helperColor);
        g2.setStroke(helperStrokeDash);
        g2.draw(new MapViewPath(mv).moveTo(seg.p1).lineTo(seg.p2));
    }

    /**
     * Creates a new Line that extends off the edge of the viewport in one direction
     * @param start The start point of the line
     * @param unitvector A unit vector denoting the direction of the line
     * @param g the Graphics2D object  it will be used on
     * @return created line
     */
    private static Line2D createSemiInfiniteLine(Point2D start, Point2D unitvector, Graphics2D g) {
        Rectangle bounds = g.getClipBounds();
        try {
            AffineTransform invtrans = g.getTransform().createInverse();
            Point2D widthpoint = invtrans.deltaTransform(new Point2D.Double(bounds.width, 0), null);
            Point2D heightpoint = invtrans.deltaTransform(new Point2D.Double(0, bounds.height), null);

            // Here we should end up with a gross overestimate of the maximum viewport diagonal in what
            // Graphics2D calls 'user space'. Essentially a manhattan distance of manhattan distances.
            // This can be used as a safe length of line to generate which will always go off-viewport.
            double linelength = Math.abs(widthpoint.getX()) + Math.abs(widthpoint.getY())
                    + Math.abs(heightpoint.getX()) + Math.abs(heightpoint.getY());

            return new Line2D.Double(start, new Point2D.Double(start.getX() + (unitvector.getX() * linelength), start.getY()
                    + (unitvector.getY() * linelength)));
        } catch (NoninvertibleTransformException e) {
            Logging.debug(e);
            return new Line2D.Double(start, new Point2D.Double(start.getX() + (unitvector.getX() * 10), start.getY()
                    + (unitvector.getY() * 10)));
        }
    }
}
