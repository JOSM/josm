// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.mapmode;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Set;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.actions.MergeNodesAction;
import org.openstreetmap.josm.command.AddCommand;
import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.MoveCommand;
import org.openstreetmap.josm.command.RotateCommand;
import org.openstreetmap.josm.command.ScaleCommand;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmData;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.WaySegment;
import org.openstreetmap.josm.data.osm.visitor.AllNodesVisitor;
import org.openstreetmap.josm.data.osm.visitor.paint.WireframeMapRenderer;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.MapViewState.MapViewPoint;
import org.openstreetmap.josm.gui.SelectionManager;
import org.openstreetmap.josm.gui.SelectionManager.SelectionEnded;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.gui.util.KeyPressReleaseListener;
import org.openstreetmap.josm.gui.util.ModifierExListener;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Pair;
import org.openstreetmap.josm.tools.PlatformManager;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Move is an action that can move all kind of OsmPrimitives (except keys for now).
 *
 * If an selected object is under the mouse when dragging, move all selected objects.
 * If an unselected object is under the mouse when dragging, it becomes selected
 * and will be moved.
 * If no object is under the mouse, move all selected objects (if any)
 *
 * On Mac OS X, Ctrl + mouse button 1 simulates right click (map move), so the
 * feature "selection remove" is disabled on this platform.
 */
public class SelectAction extends MapMode implements ModifierExListener, KeyPressReleaseListener, SelectionEnded {

    private static final String NORMAL = /* ICON(cursor/)*/ "normal";

    /**
     * Select action mode.
     * @since 7543
     */
    public enum Mode {
        /** "MOVE" means either dragging or select if no mouse movement occurs (i.e. just clicking) */
        MOVE,
        /** "ROTATE" allows to apply a rotation transformation on the selected object (see {@link RotateCommand}) */
        ROTATE,
        /** "SCALE" allows to apply a scaling transformation on the selected object (see {@link ScaleCommand}) */
        SCALE,
        /** "SELECT" means the selection rectangle */
        SELECT
    }

    // contains all possible cases the cursor can be in the SelectAction
    enum SelectActionCursor {

        rect(NORMAL, /* ICON(cursor/modifier/)*/ "selection"),
        rect_add(NORMAL, /* ICON(cursor/modifier/)*/ "select_add"),
        rect_rm(NORMAL, /* ICON(cursor/modifier/)*/ "select_remove"),
        way(NORMAL, /* ICON(cursor/modifier/)*/ "select_way"),
        way_add(NORMAL, /* ICON(cursor/modifier/)*/ "select_way_add"),
        way_rm(NORMAL, /* ICON(cursor/modifier/)*/ "select_way_remove"),
        node(NORMAL, /* ICON(cursor/modifier/)*/ "select_node"),
        node_add(NORMAL, /* ICON(cursor/modifier/)*/ "select_node_add"),
        node_rm(NORMAL, /* ICON(cursor/modifier/)*/ "select_node_remove"),
        virtual_node(NORMAL, /* ICON(cursor/modifier/)*/ "addnode"),
        scale(/* ICON(cursor/)*/ "scale", null),
        rotate(/* ICON(cursor/)*/ "rotate", null),
        merge(/* ICON(cursor/)*/ "crosshair", null),
        lasso(NORMAL, /* ICON(cursor/modifier/)*/ "rope"),
        merge_to_node(/* ICON(cursor/)*/ "crosshair", /* ICON(cursor/modifier/)*/"joinnode"),
        move(Cursor.MOVE_CURSOR);

        private final Cursor c;
        SelectActionCursor(String main, String sub) {
            c = ImageProvider.getCursor(main, sub);
        }

        SelectActionCursor(int systemCursor) {
            c = Cursor.getPredefinedCursor(systemCursor);
        }

        /**
         * Returns the action cursor.
         * @return the cursor
         */
        public Cursor cursor() {
            return c;
        }
    }

    private boolean lassoMode;
    private boolean repeatedKeySwitchLassoOption;

    // Cache previous mouse event (needed when only the modifier keys are
    // pressed but the mouse isn't moved)
    private MouseEvent oldEvent;

    private Mode mode;
    private final transient SelectionManager selectionManager;
    private boolean cancelDrawMode;
    private boolean drawTargetHighlight;
    private boolean didMouseDrag;
    /**
     * The component this SelectAction is associated with.
     */
    private final MapView mv;
    /**
     * The old cursor before the user pressed the mouse button.
     */
    private Point startingDraggingPos;
    /**
     * point where user pressed the mouse to start movement
     */
    private EastNorth startEN;
    /**
     * The last known position of the mouse.
     */
    private Point lastMousePos;
    /**
     * The time of the user mouse down event.
     */
    private long mouseDownTime;
    /**
     * The pressed button of the user mouse down event.
     */
    private int mouseDownButton;
    /**
     * The time of the user mouse down event.
     */
    private long mouseReleaseTime;
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
    private boolean initialMoveThresholdExceeded;

    /**
     * elements that have been highlighted in the previous iteration. Used
     * to remove the highlight from them again as otherwise the whole data
     * set would have to be checked.
     */
    private transient Optional<OsmPrimitive> currentHighlight = Optional.empty();

    /**
     * Create a new SelectAction
     * @param mapFrame The MapFrame this action belongs to.
     */
    public SelectAction(MapFrame mapFrame) {
        super(tr("Select"), "move/move", tr("Select, move, scale and rotate objects"),
                Shortcut.registerShortcut("mapmode:select", tr("Mode: {0}", tr("Select")), KeyEvent.VK_S, Shortcut.DIRECT),
                ImageProvider.getCursor("normal", "selection"));
        mv = mapFrame.mapView;
        setHelpId(ht("/Action/Select"));
        selectionManager = new SelectionManager(this, false, mv);
    }

    @Override
    public void enterMode() {
        super.enterMode();
        mv.addMouseListener(this);
        mv.addMouseMotionListener(this);
        mv.setVirtualNodesEnabled(Config.getPref().getInt("mappaint.node.virtual-size", 8) != 0);
        drawTargetHighlight = Config.getPref().getBoolean("draw.target-highlight", true);
        initialMoveDelay = Config.getPref().getInt("edit.initial-move-delay", 200);
        initialMoveThreshold = Config.getPref().getInt("edit.initial-move-threshold", 5);
        repeatedKeySwitchLassoOption = Config.getPref().getBoolean("mappaint.select.toggle-lasso-on-repeated-S", true);
        cycleManager.init();
        virtualManager.init();
        // This is required to update the cursors when ctrl/shift/alt is pressed
        MapFrame map = MainApplication.getMap();
        map.keyDetector.addModifierExListener(this);
        map.keyDetector.addKeyListener(this);
    }

    @Override
    public void exitMode() {
        super.exitMode();
        cycleManager.cycleStart = null;
        cycleManager.cycleList = asColl(null);
        selectionManager.unregister(mv);
        mv.removeMouseListener(this);
        mv.removeMouseMotionListener(this);
        mv.setVirtualNodesEnabled(false);
        MapFrame map = MainApplication.getMap();
        map.keyDetector.removeModifierExListener(this);
        map.keyDetector.removeKeyListener(this);
        removeHighlighting();
    }

    @Override
    public void modifiersExChanged(int modifiers) {
        if (!MainApplication.isDisplayingMapView() || oldEvent == null) return;
        if (giveUserFeedback(oldEvent, modifiers)) {
            mv.repaint();
        }
    }

    /**
     * handles adding highlights and updating the cursor for the given mouse event.
     * Please note that the highlighting for merging while moving is handled via mouseDragged.
     * @param e {@code MouseEvent} which should be used as base for the feedback
     * @return {@code true} if repaint is required
     */
    private boolean giveUserFeedback(MouseEvent e) {
        return giveUserFeedback(e, e.getModifiersEx());
    }

    /**
     * handles adding highlights and updating the cursor for the given mouse event.
     * Please note that the highlighting for merging while moving is handled via mouseDragged.
     * @param e {@code MouseEvent} which should be used as base for the feedback
     * @param modifiers define custom keyboard extended modifiers if the ones from MouseEvent are outdated or similar
     * @return {@code true} if repaint is required
     */
    private boolean giveUserFeedback(MouseEvent e, int modifiers) {
        Optional<OsmPrimitive> c = Optional.ofNullable(
                mv.getNearestNodeOrWay(e.getPoint(), mv.isSelectablePredicate, true));

        updateKeyModifiersEx(modifiers);
        determineMapMode(c.isPresent());

        Optional<OsmPrimitive> newHighlight = Optional.empty();

        virtualManager.clear();
        if (mode == Mode.MOVE && !dragInProgress() && virtualManager.activateVirtualNodeNearPoint(e.getPoint())) {
            DataSet ds = getLayerManager().getActiveDataSet();
            if (ds != null && drawTargetHighlight) {
                ds.setHighlightedVirtualNodes(virtualManager.virtualWays);
            }
            mv.setNewCursor(SelectActionCursor.virtual_node.cursor(), this);
            // don't highlight anything else if a virtual node will be
            return repaintIfRequired(newHighlight);
        }

        mv.setNewCursor(getCursor(c.orElse(null)), this);

        // return early if there can't be any highlights
        if (!drawTargetHighlight || (mode != Mode.MOVE && mode != Mode.SELECT) || !c.isPresent())
            return repaintIfRequired(newHighlight);

        // CTRL toggles selection, but if while dragging CTRL means merge
        final boolean isToggleMode = ctrl && !dragInProgress();
        if (c.isPresent() && (isToggleMode || !c.get().isSelected())) {
            // only highlight primitives that will change the selection
            // when clicked. I.e. don't highlight selected elements unless
            // we are in toggle mode.
            newHighlight = c;
        }
        return repaintIfRequired(newHighlight);
    }

    /**
     * works out which cursor should be displayed for most of SelectAction's
     * features. The only exception is the "move" cursor when actually dragging
     * primitives.
     * @param nearbyStuff primitives near the cursor
     * @return the cursor that should be displayed
     */
    private Cursor getCursor(OsmPrimitive nearbyStuff) {
        String c = "rect";
        switch(mode) {
        case MOVE:
            if (virtualManager.hasVirtualNode()) {
                c = "virtual_node";
                break;
            }
            final OsmPrimitive osm = nearbyStuff;

            if (dragInProgress()) {
                // only consider merge if ctrl is pressed and there are nodes in
                // the selection that could be merged
                if (!ctrl || getLayerManager().getEditDataSet().getSelectedNodes().isEmpty()) {
                    c = "move";
                    break;
                }
                // only show merge to node cursor if nearby node and that node is currently
                // not being dragged
                final boolean hasTarget = osm instanceof Node && !osm.isSelected();
                c = hasTarget ? "merge_to_node" : "merge";
                break;
            }

            c = (osm instanceof Node) ? "node" : c;
            c = (osm instanceof Way) ? "way" : c;
            if (shift) {
                c += "_add";
            } else if (ctrl) {
                c += osm == null || osm.isSelected() ? "_rm" : "_add";
            }
            break;
        case ROTATE:
            c = "rotate";
            break;
        case SCALE:
            c = "scale";
            break;
        case SELECT:
            if (lassoMode) {
                c = "lasso";
            } else {
                c = "rect" + (shift ? "_add" : (ctrl && !PlatformManager.isPlatformOsx() ? "_rm" : ""));
            }
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
        OsmData<?, ?, ?, ?> ds = getLayerManager().getActiveData();
        if (ds != null && !ds.getHighlightedVirtualNodes().isEmpty()) {
            needsRepaint = true;
            ds.clearHighlightedVirtualNodes();
        }
        if (!currentHighlight.isPresent()) {
            return needsRepaint;
        } else {
            currentHighlight.get().setHighlighted(false);
        }
        currentHighlight = Optional.empty();
        return true;
    }

    private boolean repaintIfRequired(Optional<OsmPrimitive> newHighlight) {
        if (!drawTargetHighlight || currentHighlight.equals(newHighlight))
            return false;
        currentHighlight.ifPresent(osm -> osm.setHighlighted(false));
        newHighlight.ifPresent(osm -> osm.setHighlighted(true));
        currentHighlight = newHighlight;
        return true;
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
        mouseDownButton = e.getButton();
        // return early
        if (!mv.isActiveLayerVisible() || !(Boolean) this.getValue("active") || mouseDownButton != MouseEvent.BUTTON1)
            return;

        // left-button mouse click only is processed here

        // request focus in order to enable the expected keyboard shortcuts
        mv.requestFocus();

        // update which modifiers are pressed (shift, alt, ctrl)
        updateKeyModifiers(e);

        // We don't want to change to draw tool if the user tries to (de)select
        // stuff but accidentally clicks in an empty area when selection is empty
        cancelDrawMode = shift || ctrl;
        didMouseDrag = false;
        initialMoveThresholdExceeded = false;
        mouseDownTime = System.currentTimeMillis();
        lastMousePos = e.getPoint();
        startEN = mv.getEastNorth(lastMousePos.x, lastMousePos.y);

        // primitives under cursor are stored in c collection

        OsmPrimitive nearestPrimitive = mv.getNearestNodeOrWay(e.getPoint(), mv.isSelectablePredicate, true);

        determineMapMode(nearestPrimitive != null);

        switch(mode) {
        case ROTATE:
        case SCALE:
            //  if nothing was selected, select primitive under cursor for scaling or rotating
            DataSet ds = getLayerManager().getEditDataSet();
            if (ds.selectionEmpty()) {
                ds.setSelected(asColl(nearestPrimitive));
            }

            // Mode.select redraws when selectPrims is called
            // Mode.move   redraws when mouseDragged is called
            // Mode.rotate redraws here
            // Mode.scale redraws here
            break;
        case MOVE:
            // also include case when some primitive is under cursor and no shift+ctrl / alt+ctrl is pressed
            // so this is not movement, but selection on primitive under cursor
            if (!cancelDrawMode && nearestPrimitive instanceof Way) {
                virtualManager.activateVirtualNodeNearPoint(e.getPoint());
            }
            OsmPrimitive toSelect = cycleManager.cycleSetup(nearestPrimitive, e.getPoint());
            selectPrims(asColl(toSelect), false, false);
            useLastMoveCommandIfPossible();
            // Schedule a timer to update status line "initialMoveDelay+1" ms in the future
            GuiHelper.scheduleTimer(initialMoveDelay+1, evt -> updateStatusLine(), false);
            break;
        case SELECT:
        default:
            if (!(ctrl && PlatformManager.isPlatformOsx())) {
                // start working with rectangle or lasso
                selectionManager.register(mv, lassoMode);
                selectionManager.mousePressed(e);
                break;
            }
        }
        if (giveUserFeedback(e)) {
            mv.repaint();
        }
        updateStatusLine();
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        // Mac OSX simulates with ctrl + mouse 1 the second mouse button hence no dragging events get fired.
        if (PlatformManager.isPlatformOsx() && (mode == Mode.ROTATE || mode == Mode.SCALE)) {
            mouseDragged(e);
            return;
        }
        oldEvent = e;
        if (giveUserFeedback(e)) {
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

        // Swing sends random mouseDragged events when closing dialogs by double-clicking their top-left icon on Windows
        // Ignore such false events to prevent issues like #7078
        if (mouseDownButton == MouseEvent.BUTTON1 && mouseReleaseTime > mouseDownTime)
            return;

        cancelDrawMode = true;
        if (mode == Mode.SELECT) {
            // Unregisters selectionManager if ctrl has been pressed after mouse click on Mac OS X in order to move the map
            if (ctrl && PlatformManager.isPlatformOsx()) {
                selectionManager.unregister(mv);
                // Make sure correct cursor is displayed
                mv.setNewCursor(Cursor.MOVE_CURSOR, this);
            }
            return;
        }

        // do not count anything as a move if it lasts less than 100 milliseconds.
        if ((mode == Mode.MOVE) && (System.currentTimeMillis() - mouseDownTime < initialMoveDelay))
            return;

        if (mode != Mode.ROTATE && mode != Mode.SCALE && (e.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK) == 0) {
            // button is pressed in rotate mode
            return;
        }

        if (mode == Mode.MOVE) {
            // If ctrl is pressed we are in merge mode. Look for a nearby node,
            // highlight it and adjust the cursor accordingly.
            final boolean canMerge = ctrl && !getLayerManager().getEditDataSet().getSelectedNodes().isEmpty();
            final OsmPrimitive p = canMerge ? findNodeToMergeTo(e.getPoint()) : null;
            boolean needsRepaint = removeHighlighting();
            if (p != null) {
                p.setHighlighted(true);
                currentHighlight = Optional.of(p);
                needsRepaint = true;
            }
            mv.setNewCursor(getCursor(p), this);
            // also update the stored mouse event, so we can display the correct cursor
            // when dragging a node onto another one and then press CTRL to merge
            oldEvent = e;
            if (needsRepaint) {
                mv.repaint();
            }
        }

        if (startingDraggingPos == null) {
            startingDraggingPos = new Point(e.getX(), e.getY());
        }

        if (lastMousePos == null) {
            lastMousePos = e.getPoint();
            return;
        }

        if (!initialMoveThresholdExceeded) {
            int dp = (int) lastMousePos.distance(e.getX(), e.getY());
            if (dp < initialMoveThreshold)
                return; // ignore small drags
            initialMoveThresholdExceeded = true; //no more ingnoring uintil nex mouse press
        }
        if (e.getPoint().equals(lastMousePos))
            return;

        EastNorth currentEN = mv.getEastNorth(e.getX(), e.getY());

        if (virtualManager.hasVirtualWaysToBeConstructed()) {
            virtualManager.createMiddleNodeFromVirtual(currentEN);
        } else {
            if (!updateCommandWhileDragging(currentEN)) return;
        }

        mv.repaint();
        if (mode != Mode.SCALE) {
            lastMousePos = e.getPoint();
        }

        didMouseDrag = true;
    }

    @Override
    public void mouseExited(MouseEvent e) {
        if (removeHighlighting()) {
            mv.repaint();
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (!mv.isActiveLayerVisible())
            return;

        startingDraggingPos = null;
        mouseReleaseTime = System.currentTimeMillis();
        MapFrame map = MainApplication.getMap();

        if (mode == Mode.SELECT) {
            if (e.getButton() != MouseEvent.BUTTON1) {
                return;
            }
            selectionManager.endSelecting(e);
            selectionManager.unregister(mv);

            // Select Draw Tool if no selection has been made
            if (!cancelDrawMode && getLayerManager().getActiveDataSet().selectionEmpty()) {
                map.selectDrawTool(true);
                updateStatusLine();
                return;
            }
        }

        if (mode == Mode.MOVE && e.getButton() == MouseEvent.BUTTON1) {
            if (!didMouseDrag) {
                // only built in move mode
                virtualManager.clear();
                // do nothing if the click was to short too be recognized as a drag,
                // but the release position is farther than 10px away from the press position
                if (lastMousePos == null || lastMousePos.distanceSq(e.getPoint()) < 100) {
                    updateKeyModifiers(e);
                    selectPrims(cycleManager.cyclePrims(), true, false);

                    // If the user double-clicked a node, change to draw mode
                    Collection<OsmPrimitive> c = getLayerManager().getEditDataSet().getSelected();
                    if (e.getClickCount() >= 2 && c.size() == 1 && c.iterator().next() instanceof Node) {
                        // We need to do it like this as otherwise drawAction will see a double
                        // click and switch back to SelectMode
                        MainApplication.worker.execute(() -> map.selectDrawTool(true));
                        return;
                    }
                }
            } else {
                confirmOrUndoMovement(e);
            }
        }

        mode = null;

        // simply remove any highlights if the middle click popup is active because
        // the highlights don't depend on the cursor position there. If something was
        // selected beforehand this would put us into move mode as well, which breaks
        // the cycling through primitives on top of each other (see #6739).
        if (e.getButton() == MouseEvent.BUTTON2) {
            removeHighlighting();
        } else {
            giveUserFeedback(e);
        }
        updateStatusLine();
    }

    @Override
    public void selectionEnded(Rectangle r, MouseEvent e) {
        updateKeyModifiers(e);
        selectPrims(selectionManager.getSelectedObjects(alt), true, true);
    }

    @Override
    public void doKeyPressed(KeyEvent e) {
        if (!repeatedKeySwitchLassoOption || !MainApplication.isDisplayingMapView() || !getShortcut().isEvent(e))
            return;
        if (Logging.isDebugEnabled()) {
            Logging.debug("{0} consuming event {1}", getClass().getName(), e);
        }
        e.consume();
        MapFrame map = MainApplication.getMap();
        if (!lassoMode) {
            map.selectMapMode(map.mapModeSelectLasso);
        } else {
            map.selectMapMode(map.mapModeSelect);
        }
    }

    @Override
    public void doKeyReleased(KeyEvent e) {
        // Do nothing
    }

    /**
     * sets the mapmode according to key modifiers and if there are any
     * selectables nearby. Everything has to be pre-determined for this
     * function; its main purpose is to centralize what the modifiers do.
     * @param hasSelectionNearby {@code true} if some primitves are selectable nearby
     */
    private void determineMapMode(boolean hasSelectionNearby) {
        if (getLayerManager().getEditDataSet() != null) {
            if (shift && ctrl) {
                mode = Mode.ROTATE;
            } else if (alt && ctrl) {
                mode = Mode.SCALE;
            } else if (hasSelectionNearby || dragInProgress()) {
                mode = Mode.MOVE;
            } else {
                mode = Mode.SELECT;
            }
        } else {
            mode = Mode.SELECT;
        }
    }

    /**
     * Determines whenever elements have been grabbed and moved (i.e. the initial
     * thresholds have been exceeded) and is still in progress (i.e. mouse button still pressed)
     * @return true if a drag is in progress
     */
    private boolean dragInProgress() {
        return didMouseDrag && startingDraggingPos != null;
    }

    /**
     * Create or update data modification command while dragging mouse - implementation of
     * continuous moving, scaling and rotation
     * @param currentEN - mouse position
     * @return status of action (<code>true</code> when action was performed)
     */
    private boolean updateCommandWhileDragging(EastNorth currentEN) {
        // Currently we support only transformations which do not affect relations.
        // So don't add them in the first place to make handling easier
        DataSet ds = getLayerManager().getEditDataSet();
        Collection<OsmPrimitive> selection = ds.getSelectedNodesAndWays();
        if (selection.isEmpty()) { // if nothing was selected to drag, just select nearest node/way to the cursor
            OsmPrimitive nearestPrimitive = mv.getNearestNodeOrWay(mv.getPoint(startEN), mv.isSelectablePredicate, true);
            ds.setSelected(nearestPrimitive);
        }

        Collection<Node> affectedNodes = AllNodesVisitor.getAllNodes(selection);
        // for these transformations, having only one node makes no sense - quit silently
        if (affectedNodes.size() < 2 && (mode == Mode.ROTATE || mode == Mode.SCALE)) {
            return false;
        }
        Command c = getLastCommandInDataset(ds);
        if (mode == Mode.MOVE) {
            if (startEN == null) return false; // fix #8128
            ds.beginUpdate();
            try {
                if (c instanceof MoveCommand && affectedNodes.equals(((MoveCommand) c).getParticipatingPrimitives())) {
                    ((MoveCommand) c).saveCheckpoint();
                    ((MoveCommand) c).applyVectorTo(currentEN);
                } else if (!selection.isEmpty()) {
                    c = new MoveCommand(selection, startEN, currentEN);
                    UndoRedoHandler.getInstance().add(c);
                }
                for (Node n : affectedNodes) {
                    if (n.isOutSideWorld()) {
                        // Revert move
                        if (c instanceof MoveCommand) {
                            ((MoveCommand) c).resetToCheckpoint();
                        }
                        // TODO: We might use a simple notification in the lower left corner.
                        JOptionPane.showMessageDialog(
                                MainApplication.getMainFrame(),
                                tr("Cannot move objects outside of the world."),
                                tr("Warning"),
                                JOptionPane.WARNING_MESSAGE);
                        mv.setNewCursor(cursor, this);
                        return false;
                    }
                }
            } finally {
                ds.endUpdate();
            }
        } else {
            startEN = currentEN; // drag can continue after scaling/rotation

            if (mode != Mode.ROTATE && mode != Mode.SCALE) {
                return false;
            }

            ds.beginUpdate();
            try {
                if (mode == Mode.ROTATE) {
                    if (c instanceof RotateCommand && affectedNodes.equals(((RotateCommand) c).getTransformedNodes())) {
                        ((RotateCommand) c).handleEvent(currentEN);
                    } else {
                        UndoRedoHandler.getInstance().add(new RotateCommand(selection, currentEN));
                    }
                } else if (mode == Mode.SCALE) {
                    if (c instanceof ScaleCommand && affectedNodes.equals(((ScaleCommand) c).getTransformedNodes())) {
                        ((ScaleCommand) c).handleEvent(currentEN);
                    } else {
                        UndoRedoHandler.getInstance().add(new ScaleCommand(selection, currentEN));
                    }
                }

                Collection<Way> ways = ds.getSelectedWays();
                if (doesImpactStatusLine(affectedNodes, ways)) {
                    MainApplication.getMap().statusLine.setDist(ways);
                }
            } finally {
                ds.endUpdate();
            }
        }
        return true;
    }

    private static boolean doesImpactStatusLine(Collection<Node> affectedNodes, Collection<Way> selectedWays) {
        for (Way w : selectedWays) {
            for (Node n : w.getNodes()) {
                if (affectedNodes.contains(n)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Adapt last move command (if it is suitable) to work with next drag, started at point startEN
     */
    private void useLastMoveCommandIfPossible() {
        DataSet dataSet = getLayerManager().getEditDataSet();
        if (dataSet == null) {
            // It may happen that there is no edit layer.
            return;
        }
        Command c = getLastCommandInDataset(dataSet);
        Collection<Node> affectedNodes = AllNodesVisitor.getAllNodes(dataSet.getSelected());
        if (c instanceof MoveCommand && affectedNodes.equals(((MoveCommand) c).getParticipatingPrimitives())) {
            // old command was created with different base point of movement, we need to recalculate it
            ((MoveCommand) c).changeStartPoint(startEN);
        }
    }

    /**
     * Obtain command in undoRedo stack to "continue" when dragging
     * @param ds The data set the command needs to be in.
     * @return last command
     */
    private static Command getLastCommandInDataset(DataSet ds) {
        Command lastCommand = UndoRedoHandler.getInstance().getLastCommand();
        if (lastCommand instanceof SequenceCommand) {
            lastCommand = ((SequenceCommand) lastCommand).getLastCommand();
        }
        if (lastCommand != null && ds.equals(lastCommand.getAffectedDataSet())) {
            return lastCommand;
        } else {
            return null;
        }
    }

    /**
     * Present warning in the following cases and undo unwanted movements: <ul>
     * <li>large and possibly unwanted movements</li>
     * <li>movement of node with attached ways that are hidden by filters</li>
     * </ul>
     *
     * @param e the mouse event causing the action (mouse released)
     */
    private void confirmOrUndoMovement(MouseEvent e) {
        if (movesHiddenWay()) {
            final ExtendedDialog ed = new ConfirmMoveDialog();
            ed.setContent(tr("Are you sure that you want to move elements with attached ways that are hidden by filters?"));
            ed.toggleEnable("movedHiddenElements");
            ed.showDialog();
            if (ed.getValue() != 1) {
                UndoRedoHandler.getInstance().undo();
            }
        }
        Set<Node> nodes = new HashSet<>();
        int max = Config.getPref().getInt("warn.move.maxelements", 20);
        for (OsmPrimitive osm : getLayerManager().getEditDataSet().getSelected()) {
            if (osm instanceof Way) {
                nodes.addAll(((Way) osm).getNodes());
            } else if (osm instanceof Node) {
                nodes.add((Node) osm);
            }
            if (nodes.size() > max) {
                break;
            }
        }
        if (nodes.size() > max) {
            final ExtendedDialog ed = new ConfirmMoveDialog();
            ed.setContent(
                    /* for correct i18n of plural forms - see #9110 */
                    trn("You moved more than {0} element. " + "Moving a large number of elements is often an error.\n" + "Really move them?",
                        "You moved more than {0} elements. " + "Moving a large number of elements is often an error.\n" + "Really move them?",
                        max, max));
            ed.toggleEnable("movedManyElements");
            ed.showDialog();

            if (ed.getValue() != 1) {
                UndoRedoHandler.getInstance().undo();
            }
        } else {
            // if small number of elements were moved,
            updateKeyModifiers(e);
            if (ctrl) mergePrims(e.getPoint());
        }
    }

    static class ConfirmMoveDialog extends ExtendedDialog {
        ConfirmMoveDialog() {
            super(MainApplication.getMainFrame(),
                    tr("Move elements"),
                    tr("Move them"), tr("Undo move"));
            setButtonIcons("reorder", "cancel");
            setCancelButton(2);
        }
    }

    private boolean movesHiddenWay() {
        DataSet ds = getLayerManager().getEditDataSet();
        final Collection<Node> elementsToTest = new HashSet<>(ds.getSelectedNodes());
        for (Way osm : ds.getSelectedWays()) {
            elementsToTest.addAll(osm.getNodes());
        }
        return elementsToTest.stream()
                .flatMap(n -> n.referrers(Way.class))
                .anyMatch(Way::isDisabledAndHidden);
    }

    /**
     * Merges the selected nodes to the one closest to the given mouse position if the control
     * key is pressed. If there is no such node, no action will be done and no error will be
     * reported. If there is, it will execute the merge and add it to the undo buffer.
     * @param p mouse position
     */
    private void mergePrims(Point p) {
        DataSet ds = getLayerManager().getEditDataSet();
        Collection<Node> selNodes = ds.getSelectedNodes();
        if (selNodes.isEmpty())
            return;

        Node target = findNodeToMergeTo(p);
        if (target == null)
            return;

        if (selNodes.size() == 1) {
            // Move all selected primitive to preserve shape #10748
            Collection<OsmPrimitive> selection = ds.getSelectedNodesAndWays();
            Collection<Node> affectedNodes = AllNodesVisitor.getAllNodes(selection);
            Command c = getLastCommandInDataset(ds);
            ds.beginUpdate();
            try {
                if (c instanceof MoveCommand && affectedNodes.equals(((MoveCommand) c).getParticipatingPrimitives())) {
                    Node selectedNode = selNodes.iterator().next();
                    EastNorth selectedEN = selectedNode.getEastNorth();
                    EastNorth targetEN = target.getEastNorth();
                    ((MoveCommand) c).moveAgain(targetEN.getX() - selectedEN.getX(),
                                                targetEN.getY() - selectedEN.getY());
                }
            } finally {
                ds.endUpdate();
            }
        }

        Collection<Node> nodesToMerge = new LinkedList<>(selNodes);
        nodesToMerge.add(target);
        mergeNodes(MainApplication.getLayerManager().getEditLayer(), nodesToMerge, target);
    }

    /**
     * Merge nodes using {@code MergeNodesAction}.
     * Can be overridden for testing purpose.
     * @param layer layer the reference data layer. Must not be null
     * @param nodes the collection of nodes. Ignored if null
     * @param targetLocationNode this node's location will be used for the target node
     */
    public void mergeNodes(OsmDataLayer layer, Collection<Node> nodes,
                           Node targetLocationNode) {
        MergeNodesAction.doMergeNodes(layer, nodes, targetLocationNode);
    }

    /**
     * Tries to find a node to merge to when in move-merge mode for the current mouse
     * position. Either returns the node or null, if no suitable one is nearby.
     * @param p mouse position
     * @return node to merge to, or null
     */
    private Node findNodeToMergeTo(Point p) {
        Collection<Node> target = mv.getNearestNodes(p,
                getLayerManager().getEditDataSet().getSelectedNodes(),
                mv.isSelectablePredicate);
        return target.isEmpty() ? null : target.iterator().next();
    }

    private void selectPrims(Collection<OsmPrimitive> prims, boolean released, boolean area) {
        DataSet ds = getLayerManager().getActiveDataSet();

        // not allowed together: do not change dataset selection, return early
        // Virtual Ways: if non-empty the cursor is above a virtual node. So don't highlight
        // anything if about to drag the virtual node (i.e. !released) but continue if the
        // cursor is only released above a virtual node by accident (i.e. released). See #7018
        if (ds == null || (shift && ctrl) || (ctrl && !released) || (virtualManager.hasVirtualWaysToBeConstructed() && !released))
            return;

        if (!released) {
            // Don't replace the selection if the user clicked on a
            // selected object (it breaks moving of selected groups).
            // Do it later, on mouse release.
            shift |= ds.getSelected().containsAll(prims);
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

    /**
     * Returns the current select mode.
     * @return the select mode
     * @since 7543
     */
    public final Mode getMode() {
        return mode;
    }

    @Override
    public String getModeHelpText() {
        if (mouseDownButton == MouseEvent.BUTTON1 && mouseReleaseTime < mouseDownTime) {
            if (mode == Mode.SELECT)
                return tr("Release the mouse button to select the objects in the rectangle.");
            else if (mode == Mode.MOVE && (System.currentTimeMillis() - mouseDownTime >= initialMoveDelay)) {
                final DataSet ds = getLayerManager().getEditDataSet();
                final boolean canMerge = ds != null && !ds.getSelectedNodes().isEmpty();
                final String mergeHelp = canMerge ? (' ' + tr("Ctrl to merge with nearest node.")) : "";
                return tr("Release the mouse button to stop moving.") + mergeHelp;
            } else if (mode == Mode.ROTATE)
                return tr("Release the mouse button to stop rotating.");
            else if (mode == Mode.SCALE)
                return tr("Release the mouse button to stop scaling.");
        }
        return tr("Move objects by dragging; Shift to add to selection (Ctrl to toggle); Shift-Ctrl to rotate selected; " +
                  "Alt-Ctrl to scale selected; or change selection");
    }

    @Override
    public boolean layerIsSupported(Layer l) {
        return l instanceof OsmDataLayer;
    }

    /**
     * Enable or diable the lasso mode
     * @param lassoMode true to enable the lasso mode, false otherwise
     */
    public void setLassoMode(boolean lassoMode) {
        this.selectionManager.setLassoMode(lassoMode);
        this.lassoMode = lassoMode;
    }

    private final transient CycleManager cycleManager = new CycleManager();
    private final transient VirtualManager virtualManager = new VirtualManager();

    private class CycleManager {

        private Collection<OsmPrimitive> cycleList = Collections.emptyList();
        private boolean cyclePrims;
        private OsmPrimitive cycleStart;
        private boolean waitForMouseUpParameter;
        private boolean multipleMatchesParameter;
        /**
         * read preferences
         */
        private void init() {
            waitForMouseUpParameter = Config.getPref().getBoolean("mappaint.select.waits-for-mouse-up", false);
            multipleMatchesParameter = Config.getPref().getBoolean("selectaction.cycles.multiple.matches", false);
        }

        /**
         * Determine primitive to be selected and build cycleList
         * @param nearest primitive found by simple method
         * @param p point where user clicked
         * @return OsmPrimitive to be selected
         */
        private OsmPrimitive cycleSetup(OsmPrimitive nearest, Point p) {
            OsmPrimitive osm = null;

            if (nearest != null) {
                osm = nearest;

                if (!(alt || multipleMatchesParameter)) {
                    // no real cycling, just one element in cycle list
                    cycleList = asColl(osm);

                    if (waitForMouseUpParameter) {
                        // prefer a selected nearest node or way, if possible
                        osm = mv.getNearestNodeOrWay(p, mv.isSelectablePredicate, true);
                    }
                } else {
                    // Alt + left mouse button pressed: we need to build cycle list
                    cycleList = mv.getAllNearest(p, mv.isSelectablePredicate);

                    if (cycleList.size() > 1) {
                        cyclePrims = false;

                        // find first already selected element in cycle list
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
                        if (cycleList.size() == 2 && !waitForMouseUpParameter) {
                            if (!(osm.equals(old) || osm.isNew() || ctrl)) {
                                cyclePrims = false;
                                osm = old;
                            } // else defer toggling to mouseRelease time in those cases:
                            /*
                             * osm == old -- the true nearest node is the
                             * selected one osm is a new node -- do not break
                             * unglue ways in ALT mode ctrl is pressed -- ctrl
                             * generally works on mouseReleased
                             */
                        }
                    }
                }
            }
            return osm;
        }

        /**
         * Modifies current selection state and returns the next element in a
         * selection cycle given by
         * <code>cycleList</code> field
         * @return the next element of cycle list
         */
        private Collection<OsmPrimitive> cyclePrims() {
            if (cycleList.size() <= 1) {
                // no real cycling, just return one-element collection with nearest primitive in it
                return cycleList;
            }
            // updateKeyModifiers() already called before!

            DataSet ds = getLayerManager().getActiveDataSet();
            OsmPrimitive first = cycleList.iterator().next(), foundInDS = null;
            OsmPrimitive nxt = first;

            if (cyclePrims && shift) {
                for (Iterator<OsmPrimitive> i = cycleList.iterator(); i.hasNext();) {
                    nxt = i.next();
                    if (!nxt.isSelected()) {
                        break; // take first primitive in cycleList not in sel
                    }
                }
                // if primitives 1,2,3 are under cursor, [Alt-press] [Shift-release] gives 1 -> 12 -> 123
            } else {
                for (Iterator<OsmPrimitive> i = cycleList.iterator(); i.hasNext();) {
                    nxt = i.next();
                    if (nxt.isSelected()) {
                        foundInDS = nxt;
                        // first selected primitive in cycleList is found
                        if (cyclePrims || ctrl) {
                            ds.clearSelection(foundInDS); // deselect it
                            nxt = i.hasNext() ? i.next() : first;
                            // return next one in cycle list (last->first)
                        }
                        break; // take next primitive in cycleList
                    }
                }
            }

            // if "no-alt-cycling" is enabled, Ctrl-Click arrives here.
            if (ctrl) {
                // a member of cycleList was found in the current dataset selection
                if (foundInDS != null) {
                    // mouse was moved to a different selection group w/ a previous sel
                    if (!cycleList.contains(cycleStart)) {
                        ds.clearSelection(cycleList);
                        cycleStart = foundInDS;
                    } else if (cycleStart.equals(nxt)) {
                        // loop detected, insert deselect step
                        ds.addSelected(nxt);
                    }
                } else {
                    // setup for iterating a sel group again or a new, different one..
                    nxt = cycleList.contains(cycleStart) ? cycleStart : first;
                    cycleStart = nxt;
                }
            } else {
                cycleStart = null;
            }
            // return one-element collection with one element to be selected (or added  to selection)
            return asColl(nxt);
        }
    }

    private class VirtualManager {

        private Node virtualNode;
        private Collection<WaySegment> virtualWays = new LinkedList<>();
        private int nodeVirtualSize;
        private int virtualSnapDistSq2;
        private int virtualSpace;

        private void init() {
            nodeVirtualSize = Config.getPref().getInt("mappaint.node.virtual-size", 8);
            int virtualSnapDistSq = Config.getPref().getInt("mappaint.node.virtual-snap-distance", 8);
            virtualSnapDistSq2 = virtualSnapDistSq*virtualSnapDistSq;
            virtualSpace = Config.getPref().getInt("mappaint.node.virtual-space", 70);
        }

        /**
         * Calculate a virtual node if there is enough visual space to draw a
         * crosshair node and the middle of a way segment is clicked. If the
         * user drags the crosshair node, it will be added to all ways in
         * <code>virtualWays</code>.
         *
         * @param p the point clicked
         * @return whether
         * <code>virtualNode</code> and
         * <code>virtualWays</code> were setup.
         */
        private boolean activateVirtualNodeNearPoint(Point p) {
            if (nodeVirtualSize > 0) {

                Collection<WaySegment> selVirtualWays = new LinkedList<>();
                Pair<Node, Node> vnp = null, wnp = new Pair<>(null, null);

                for (WaySegment ws : mv.getNearestWaySegments(p, mv.isSelectablePredicate)) {
                    Way w = ws.way;

                    wnp.a = w.getNode(ws.lowerIndex);
                    wnp.b = w.getNode(ws.lowerIndex + 1);
                    MapViewPoint p1 = mv.getState().getPointFor(wnp.a);
                    MapViewPoint p2 = mv.getState().getPointFor(wnp.b);
                    if (WireframeMapRenderer.isLargeSegment(p1, p2, virtualSpace)) {
                        Point2D pc = new Point2D.Double((p1.getInViewX() + p2.getInViewX()) / 2, (p1.getInViewY() + p2.getInViewY()) / 2);
                        if (p.distanceSq(pc) < virtualSnapDistSq2) {
                            // Check that only segments on top of each other get added to the
                            // virtual ways list. Otherwise ways that coincidentally have their
                            // virtual node at the same spot will be joined which is likely unwanted
                            Pair.sort(wnp);
                            if (vnp == null) {
                                vnp = new Pair<>(wnp.a, wnp.b);
                                virtualNode = new Node(mv.getLatLon(pc.getX(), pc.getY()));
                            }
                            if (vnp.equals(wnp)) {
                                // if mutiple line segments have the same points,
                                // add all segments to be splitted to virtualWays list
                                // if some lines are selected, only their segments will go to virtualWays
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

        private void createMiddleNodeFromVirtual(EastNorth currentEN) {
            if (startEN == null) // #13724, #14712, #15087
                return;
            DataSet ds = getLayerManager().getEditDataSet();
            Collection<Command> virtualCmds = new LinkedList<>();
            virtualCmds.add(new AddCommand(ds, virtualNode));
            for (WaySegment virtualWay : virtualWays) {
                Way w = virtualWay.way;
                Way wnew = new Way(w);
                wnew.addNode(virtualWay.lowerIndex + 1, virtualNode);
                virtualCmds.add(new ChangeCommand(ds, w, wnew));
            }
            virtualCmds.add(new MoveCommand(ds, virtualNode, startEN, currentEN));
            String text = trn("Add and move a virtual new node to way",
                    "Add and move a virtual new node to {0} ways", virtualWays.size(),
                    virtualWays.size());
            UndoRedoHandler.getInstance().add(new SequenceCommand(text, virtualCmds));
            ds.setSelected(Collections.singleton((OsmPrimitive) virtualNode));
            clear();
        }

        private void clear() {
            virtualWays.clear();
            virtualNode = null;
        }

        private boolean hasVirtualNode() {
            return virtualNode != null;
        }

        private boolean hasVirtualWaysToBeConstructed() {
            return !virtualWays.isEmpty();
        }
    }

    /**
     * Returns {@code o} as collection of {@code o}'s type.
     * @param <T> object type
     * @param o any object
     * @return {@code o} as collection of {@code o}'s type.
     */
    protected static <T> Collection<T> asColl(T o) {
        return o == null ? Collections.emptySet() : Collections.singleton(o);
    }
}
