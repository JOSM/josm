// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.mapmode;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.JosmAction;
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
import org.openstreetmap.josm.data.osm.visitor.paint.ArrowPaintHelper;
import org.openstreetmap.josm.data.osm.visitor.paint.MapPath2D;
import org.openstreetmap.josm.data.osm.visitor.paint.PaintColors;
import org.openstreetmap.josm.gui.MainMenu;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.MapViewState.MapViewPoint;
import org.openstreetmap.josm.gui.NavigatableComponent;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.MapViewPaintable;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.gui.util.KeyPressReleaseListener;
import org.openstreetmap.josm.gui.util.ModifierListener;
import org.openstreetmap.josm.gui.widgets.PopupMenuLauncher;
import org.openstreetmap.josm.tools.Geometry;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Pair;
import org.openstreetmap.josm.tools.Shortcut;
import org.openstreetmap.josm.tools.Utils;

/**
 * Mapmode to add nodes, create and extend ways.
 */
public class DrawAction extends MapMode implements MapViewPaintable, SelectionChangedListener, KeyPressReleaseListener, ModifierListener {

    private static final Color ORANGE_TRANSPARENT = new Color(Color.ORANGE.getRed(), Color.ORANGE.getGreen(), Color.ORANGE.getBlue(), 128);

    private static final ArrowPaintHelper START_WAY_INDICATOR = new ArrowPaintHelper(Math.toRadians(90), 8);

    private final Cursor cursorJoinNode;
    private final Cursor cursorJoinWay;

    private transient Node lastUsedNode;
    private double toleranceMultiplier;

    private transient Node mouseOnExistingNode;
    private transient Set<Way> mouseOnExistingWays = new HashSet<>();
    // old highlights store which primitives are currently highlighted. This
    // is true, even if target highlighting is disabled since the status bar
    // derives its information from this list as well.
    private transient Set<OsmPrimitive> oldHighlights = new HashSet<>();
    // new highlights contains a list of primitives that should be highlighted
    // but haven’t been so far. The idea is to compare old and new and only
    // repaint if there are changes.
    private transient Set<OsmPrimitive> newHighlights = new HashSet<>();
    private boolean drawHelperLine;
    private boolean wayIsFinished;
    private boolean drawTargetHighlight;
    private Point mousePos;
    private Point oldMousePos;
    private Color rubberLineColor;

    private transient Node currentBaseNode;
    private transient Node previousNode;
    private EastNorth currentMouseEastNorth;

    private final transient SnapHelper snapHelper = new SnapHelper();

    private final transient Shortcut backspaceShortcut;
    private final BackSpaceAction backspaceAction;
    private final transient Shortcut snappingShortcut;
    private boolean ignoreNextKeyRelease;

    private final SnapChangeAction snapChangeAction;
    private final JCheckBoxMenuItem snapCheckboxMenuItem;
    private boolean useRepeatedShortcut;
    private transient Stroke rubberLineStroke;
    private static final BasicStroke BASIC_STROKE = new BasicStroke(1);

    private static int snapToIntersectionThreshold;

    /**
     * Constructs a new {@code DrawAction}.
     * @param mapFrame Map frame
     */
    public DrawAction(MapFrame mapFrame) {
        super(tr("Draw"), "node/autonode", tr("Draw nodes"),
                Shortcut.registerShortcut("mapmode:draw", tr("Mode: {0}", tr("Draw")), KeyEvent.VK_A, Shortcut.DIRECT),
                mapFrame, ImageProvider.getCursor("crosshair", null));

        snappingShortcut = Shortcut.registerShortcut("mapmode:drawanglesnapping",
                tr("Mode: Draw Angle snapping"), KeyEvent.CHAR_UNDEFINED, Shortcut.NONE);
        snapChangeAction = new SnapChangeAction();
        snapCheckboxMenuItem = addMenuItem();
        snapHelper.setMenuCheckBox(snapCheckboxMenuItem);
        backspaceShortcut = Shortcut.registerShortcut("mapmode:backspace",
                tr("Backspace in Add mode"), KeyEvent.VK_BACK_SPACE, Shortcut.DIRECT);
        backspaceAction = new BackSpaceAction();
        cursorJoinNode = ImageProvider.getCursor("crosshair", "joinnode");
        cursorJoinWay = ImageProvider.getCursor("crosshair", "joinway");

        readPreferences();
        snapHelper.init();
        readPreferences();
    }

    private JCheckBoxMenuItem addMenuItem() {
        int n = Main.main.menu.editMenu.getItemCount();
        for (int i = n-1; i > 0; i--) {
            JMenuItem item = Main.main.menu.editMenu.getItem(i);
            if (item != null && item.getAction() != null && item.getAction() instanceof SnapChangeAction) {
                Main.main.menu.editMenu.remove(i);
            }
        }
        return MainMenu.addWithCheckbox(Main.main.menu.editMenu, snapChangeAction, MainMenu.WINDOW_MENU_GROUP.VOLATILE);
    }

    /**
     * Checks if a map redraw is required and does so if needed. Also updates the status bar.
     * @return true if a repaint is needed
     */
    private boolean redrawIfRequired() {
        updateStatusLine();
        // repaint required if the helper line is active.
        boolean needsRepaint = drawHelperLine && !wayIsFinished;
        if (drawTargetHighlight) {
            // move newHighlights to oldHighlights; only update changed primitives
            for (OsmPrimitive x : newHighlights) {
                if (oldHighlights.contains(x)) {
                    continue;
                }
                x.setHighlighted(true);
                needsRepaint = true;
            }
            oldHighlights.removeAll(newHighlights);
            for (OsmPrimitive x : oldHighlights) {
                x.setHighlighted(false);
                needsRepaint = true;
            }
        }
        // required in order to print correct help text
        oldHighlights = newHighlights;

        if (!needsRepaint && !drawTargetHighlight)
            return false;

        // update selection to reflect which way being modified
        OsmDataLayer editLayer = getLayerManager().getEditLayer();
        if (getCurrentBaseNode() != null && editLayer != null && !editLayer.data.selectionEmpty()) {
            DataSet currentDataSet = editLayer.data;
            Way continueFrom = getWayForNode(getCurrentBaseNode());
            if (alt && continueFrom != null && (!getCurrentBaseNode().isSelected() || continueFrom.isSelected())) {
                addRemoveSelection(currentDataSet, getCurrentBaseNode(), continueFrom);
                needsRepaint = true;
            } else if (!alt && continueFrom != null && !continueFrom.isSelected()) {
                currentDataSet.addSelected(continueFrom);
                needsRepaint = true;
            }
        }

        if (needsRepaint && editLayer != null) {
            editLayer.invalidate();
        }
        return needsRepaint;
    }

    private static void addRemoveSelection(DataSet ds, OsmPrimitive toAdd, OsmPrimitive toRemove) {
        ds.beginUpdate(); // to prevent the selection listener to screw around with the state
        ds.addSelected(toAdd);
        ds.clearSelection(toRemove);
        ds.endUpdate();
    }

    @Override
    public void enterMode() {
        if (!isEnabled())
            return;
        super.enterMode();
        readPreferences();

        // determine if selection is suitable to continue drawing. If it
        // isn't, set wayIsFinished to true to avoid superfluous repaints.
        determineCurrentBaseNodeAndPreviousNode(getLayerManager().getEditDataSet().getSelected());
        wayIsFinished = getCurrentBaseNode() == null;

        toleranceMultiplier = 0.01 * NavigatableComponent.PROP_SNAP_DISTANCE.get();

        snapHelper.init();
        snapCheckboxMenuItem.getAction().setEnabled(true);

        Main.map.statusLine.getAnglePanel().addMouseListener(snapHelper.anglePopupListener);
        Main.registerActionShortcut(backspaceAction, backspaceShortcut);

        Main.map.mapView.addMouseListener(this);
        Main.map.mapView.addMouseMotionListener(this);
        Main.map.mapView.addTemporaryLayer(this);
        DataSet.addSelectionListener(this);

        Main.map.keyDetector.addKeyListener(this);
        Main.map.keyDetector.addModifierListener(this);
        ignoreNextKeyRelease = true;
    }

    @Override
    protected void readPreferences() {
        rubberLineColor = Main.pref.getColor(marktr("helper line"), null);
        if (rubberLineColor == null) rubberLineColor = PaintColors.SELECTED.get();

        rubberLineStroke = GuiHelper.getCustomizedStroke(Main.pref.get("draw.stroke.helper-line", "3"));
        drawHelperLine = Main.pref.getBoolean("draw.helper-line", true);
        drawTargetHighlight = Main.pref.getBoolean("draw.target-highlight", true);
        snapToIntersectionThreshold = Main.pref.getInteger("edit.snap-intersection-threshold", 10);
    }

    @Override
    public void exitMode() {
        super.exitMode();
        Main.map.mapView.removeMouseListener(this);
        Main.map.mapView.removeMouseMotionListener(this);
        Main.map.mapView.removeTemporaryLayer(this);
        DataSet.removeSelectionListener(this);
        Main.unregisterActionShortcut(backspaceAction, backspaceShortcut);
        snapHelper.unsetFixedMode();
        snapCheckboxMenuItem.getAction().setEnabled(false);

        Main.map.statusLine.getAnglePanel().removeMouseListener(snapHelper.anglePopupListener);
        Main.map.statusLine.activateAnglePanel(false);

        removeHighlighting();
        Main.map.keyDetector.removeKeyListener(this);
        Main.map.keyDetector.removeModifierListener(this);

        // when exiting we let everybody know about the currently selected
        // primitives
        //
        DataSet ds = getLayerManager().getEditDataSet();
        if (ds != null) {
            ds.fireSelectionChanged();
        }
    }

    /**
     * redraw to (possibly) get rid of helper line if selection changes.
     */
    @Override
    public void modifiersChanged(int modifiers) {
        if (!Main.isDisplayingMapView() || !Main.map.mapView.isActiveLayerDrawable())
            return;
        updateKeyModifiers(modifiers);
        computeHelperLine();
        addHighlighting();
    }

    @Override
    public void doKeyPressed(KeyEvent e) {
        if (!snappingShortcut.isEvent(e) && !(useRepeatedShortcut && getShortcut().isEvent(e)))
            return;
        snapHelper.setFixedMode();
        computeHelperLine();
        redrawIfRequired();
    }

    @Override
    public void doKeyReleased(KeyEvent e) {
        if (!snappingShortcut.isEvent(e) && !(useRepeatedShortcut && getShortcut().isEvent(e)))
            return;
        if (ignoreNextKeyRelease) {
            ignoreNextKeyRelease = false;
            return;
        }
        snapHelper.unFixOrTurnOff();
        computeHelperLine();
        redrawIfRequired();
    }

    /**
     * redraw to (possibly) get rid of helper line if selection changes.
     */
    @Override
    public void selectionChanged(Collection<? extends OsmPrimitive> newSelection) {
        if (!Main.map.mapView.isActiveLayerDrawable())
            return;
        computeHelperLine();
        addHighlighting();
    }

    private void tryAgain(MouseEvent e) {
        getLayerManager().getEditDataSet().setSelected();
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
        Main.getLayerManager().getEditDataSet().fireSelectionChanged();
        lastUsedNode = null;
        wayIsFinished = true;
        Main.map.selectSelectTool(true);
        snapHelper.noSnapNow();

        // Redraw to remove the helper line stub
        computeHelperLine();
        removeHighlighting();
    }

    private Point rightClickPressPos;

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON3) {
            rightClickPressPos = e.getPoint();
        }
    }

    /**
     * If user clicked with the left button, add a node at the current mouse
     * position.
     *
     * If in nodeway mode, insert the node into the way.
     */
    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON3) {
            Point curMousePos = e.getPoint();
            if (curMousePos.equals(rightClickPressPos)) {
                tryToSetBaseSegmentForAngleSnap();
            }
            return;
        }
        if (e.getButton() != MouseEvent.BUTTON1)
            return;
        if (!Main.map.mapView.isActiveLayerDrawable())
            return;
        // request focus in order to enable the expected keyboard shortcuts
        //
        Main.map.mapView.requestFocus();

        if (e.getClickCount() > 1 && mousePos != null && mousePos.equals(oldMousePos)) {
            // A double click equals "user clicked last node again, finish way"
            // Change draw tool only if mouse position is nearly the same, as
            // otherwise fast clicks will count as a double click
            finishDrawing();
            return;
        }
        oldMousePos = mousePos;

        // we copy ctrl/alt/shift from the event just in case our global
        // keyDetector didn't make it through the security manager. Unclear
        // if that can ever happen but better be safe.
        updateKeyModifiers(e);
        mousePos = e.getPoint();

        DataSet ds = getLayerManager().getEditDataSet();
        Collection<OsmPrimitive> selection = new ArrayList<>(ds.getSelected());

        boolean newNode = false;
        Node n = Main.map.mapView.getNearestNode(mousePos, OsmPrimitive::isSelectable);
        if (ctrl) {
            Iterator<Way> it = ds.getSelectedWays().iterator();
            if (it.hasNext()) {
                // ctrl-click on node of selected way = reuse node despite of ctrl
                if (!it.next().containsNode(n)) n = null;
            } else {
                n = null; // ctrl-click + no selected way = new node
            }
        }

        if (n != null && !snapHelper.isActive()) {
            // user clicked on node
            if (selection.isEmpty() || wayIsFinished) {
                // select the clicked node and do nothing else
                // (this is just a convenience option so that people don't
                // have to switch modes)

                ds.setSelected(n);
                // If we extend/continue an existing way, select it already now to make it obvious
                Way continueFrom = getWayForNode(n);
                if (continueFrom != null) {
                    ds.addSelected(continueFrom);
                }

                // The user explicitly selected a node, so let him continue drawing
                wayIsFinished = false;
                return;
            }
        } else {
            EastNorth newEN;
            if (n != null) {
                EastNorth foundPoint = n.getEastNorth();
                // project found node to snapping line
                newEN = snapHelper.getSnapPoint(foundPoint);
                // do not add new node if there is some node within snapping distance
                double tolerance = Main.map.mapView.getDist100Pixel() * toleranceMultiplier;
                if (foundPoint.distance(newEN) > tolerance) {
                    n = new Node(newEN); // point != projected, so we create new node
                    newNode = true;
                }
            } else { // n==null, no node found in clicked area
                EastNorth mouseEN = Main.map.mapView.getEastNorth(e.getX(), e.getY());
                newEN = snapHelper.isSnapOn() ? snapHelper.getSnapPoint(mouseEN) : mouseEN;
                n = new Node(newEN); //create node at clicked point
                newNode = true;
            }
            snapHelper.unsetFixedMode();
        }

        Collection<Command> cmds = new LinkedList<>();
        Collection<OsmPrimitive> newSelection = new LinkedList<>(ds.getSelected());
        List<Way> reuseWays = new ArrayList<>();
        List<Way> replacedWays = new ArrayList<>();

        if (newNode) {
            if (n.getCoor().isOutSideWorld()) {
                JOptionPane.showMessageDialog(
                        Main.parent,
                        tr("Cannot add a node outside of the world."),
                        tr("Warning"),
                        JOptionPane.WARNING_MESSAGE
                        );
                return;
            }
            cmds.add(new AddCommand(n));

            if (!ctrl) {
                // Insert the node into all the nearby way segments
                List<WaySegment> wss = Main.map.mapView.getNearestWaySegments(
                        Main.map.mapView.getPoint(n), OsmPrimitive::isSelectable);
                if (snapHelper.isActive()) {
                    tryToMoveNodeOnIntersection(wss, n);
                }
                insertNodeIntoAllNearbySegments(wss, n, newSelection, cmds, replacedWays, reuseWays);
            }
        }
        // now "n" is newly created or reused node that shoud be added to some way

        // This part decides whether or not a "segment" (i.e. a connection) is made to an existing node.

        // For a connection to be made, the user must either have a node selected (connection
        // is made to that node), or he must have a way selected *and* one of the endpoints
        // of that way must be the last used node (connection is made to last used node), or
        // he must have a way and a node selected (connection is made to the selected node).

        // If the above does not apply, the selection is cleared and a new try is started

        boolean extendedWay = false;
        boolean wayIsFinishedTemp = wayIsFinished;
        wayIsFinished = false;

        // don't draw lines if shift is held
        if (!selection.isEmpty() && !shift) {
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
            if (n0 == null) {
                tryAgain(e);
                return;
            }
            if (!wayIsFinishedTemp) {
                if (isSelfContainedWay(selectedWay, n0, n))
                    return;

                // User clicked last node again, finish way
                if (n0 == n) {
                    finishDrawing();
                    return;
                }

                // Ok we know now that we'll insert a line segment, but will it connect to an
                // existing way or make a new way of its own? The "alt" modifier means that the
                // user wants a new way.
                Way way = alt ? null : (selectedWay != null ? selectedWay : getWayForNode(n0));
                Way wayToSelect;

                // Don't allow creation of self-overlapping ways
                if (way != null) {
                    int nodeCount = 0;
                    for (Node p : way.getNodes()) {
                        if (p.equals(n0)) {
                            nodeCount++;
                        }
                    }
                    if (nodeCount > 1) {
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
                if (way.containsNode(n)) {
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
        if (!wayIsFinished) {
            lastUsedNode = n;
        }

        ds.setSelected(newSelection);

        // "viewport following" mode for tracing long features
        // from aerial imagery or GPS tracks.
        if (n != null && Main.map.mapView.viewportFollowing) {
            Main.map.mapView.smoothScrollTo(n.getEastNorth());
        }
        computeHelperLine();
        removeHighlighting();
    }

    private void insertNodeIntoAllNearbySegments(List<WaySegment> wss, Node n, Collection<OsmPrimitive> newSelection,
            Collection<Command> cmds, List<Way> replacedWays, List<Way> reuseWays) {
        Map<Way, List<Integer>> insertPoints = new HashMap<>();
        for (WaySegment ws : wss) {
            List<Integer> is;
            if (insertPoints.containsKey(ws.way)) {
                is = insertPoints.get(ws.way);
            } else {
                is = new ArrayList<>();
                insertPoints.put(ws.way, is);
            }

            is.add(ws.lowerIndex);
        }

        Set<Pair<Node, Node>> segSet = new HashSet<>();

        for (Map.Entry<Way, List<Integer>> insertPoint : insertPoints.entrySet()) {
            Way w = insertPoint.getKey();
            List<Integer> is = insertPoint.getValue();

            Way wnew = new Way(w);

            pruneSuccsAndReverse(is);
            for (int i : is) {
                segSet.add(Pair.sort(new Pair<>(w.getNode(i), w.getNode(i+1))));
                wnew.addNode(i + 1, n);
            }

            // If ALT is pressed, a new way should be created and that new way should get
            // selected. This works everytime unless the ways the nodes get inserted into
            // are already selected. This is the case when creating a self-overlapping way
            // but pressing ALT prevents this. Therefore we must de-select the way manually
            // here so /only/ the new way will be selected after this method finishes.
            if (alt) {
                newSelection.add(insertPoint.getKey());
            }

            cmds.add(new ChangeCommand(insertPoint.getKey(), wnew));
            replacedWays.add(insertPoint.getKey());
            reuseWays.add(wnew);
        }

        adjustNode(segSet, n);
    }

    /**
     * Prevent creation of ways that look like this: &lt;----&gt;
     * This happens if users want to draw a no-exit-sideway from the main way like this:
     * ^
     * |&lt;----&gt;
     * |
     * The solution isn't ideal because the main way will end in the side way, which is bad for
     * navigation software ("drive straight on") but at least easier to fix. Maybe users will fix
     * it on their own, too. At least it's better than producing an error.
     *
     * @param selectedWay the way to check
     * @param currentNode the current node (i.e. the one the connection will be made from)
     * @param targetNode the target node (i.e. the one the connection will be made to)
     * @return {@code true} if this would create a selfcontaining way, {@code false} otherwise.
     */
    private boolean isSelfContainedWay(Way selectedWay, Node currentNode, Node targetNode) {
        if (selectedWay != null) {
            int posn0 = selectedWay.getNodes().indexOf(currentNode);
            // CHECKSTYLE.OFF: SingleSpaceSeparator
            if ((posn0 != -1 && // n0 is part of way
                (posn0 >= 1                            && targetNode.equals(selectedWay.getNode(posn0-1)))) || // previous node
                (posn0 < selectedWay.getNodesCount()-1 && targetNode.equals(selectedWay.getNode(posn0+1)))) {  // next node
                getLayerManager().getEditDataSet().setSelected(targetNode);
                lastUsedNode = targetNode;
                return true;
            }
            // CHECKSTYLE.ON: SingleSpaceSeparator
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
        if (selectedNode == null && selectedWay == null)
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

    @Override
    public void mouseDragged(MouseEvent e) {
        mouseMoved(e);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        if (!Main.map.mapView.isActiveLayerDrawable())
            return;

        // we copy ctrl/alt/shift from the event just in case our global
        // keyDetector didn't make it through the security manager. Unclear
        // if that can ever happen but better be safe.
        updateKeyModifiers(e);
        mousePos = e.getPoint();
        if (snapHelper.isSnapOn() && ctrl)
            tryToSetBaseSegmentForAngleSnap();

        computeHelperLine();
        addHighlighting();
    }

    /**
     * This method is used to detect segment under mouse and use it as reference for angle snapping
     */
    private void tryToSetBaseSegmentForAngleSnap() {
        WaySegment seg = Main.map.mapView.getNearestWaySegment(mousePos, OsmPrimitive::isSelectable);
        if (seg != null) {
            snapHelper.setBaseSegment(seg);
        }
    }

    /**
     * This method prepares data required for painting the "helper line" from
     * the last used position to the mouse cursor. It duplicates some code from
     * mouseReleased() (FIXME).
     */
    private void computeHelperLine() {
        if (mousePos == null) {
            // Don't draw the line.
            currentMouseEastNorth = null;
            currentBaseNode = null;
            return;
        }

        Collection<OsmPrimitive> selection = getLayerManager().getEditDataSet().getSelected();

        MapView mv = Main.map.mapView;
        Node currentMouseNode = null;
        mouseOnExistingNode = null;
        mouseOnExistingWays = new HashSet<>();

        showStatusInfo(-1, -1, -1, snapHelper.isSnapOn());

        if (!ctrl && mousePos != null) {
            currentMouseNode = mv.getNearestNode(mousePos, OsmPrimitive::isSelectable);
        }

        // We need this for highlighting and we'll only do so if we actually want to re-use
        // *and* there is no node nearby (because nodes beat ways when re-using)
        if (!ctrl && currentMouseNode == null) {
            List<WaySegment> wss = mv.getNearestWaySegments(mousePos, OsmPrimitive::isSelectable);
            for (WaySegment ws : wss) {
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

        determineCurrentBaseNodeAndPreviousNode(selection);
        if (previousNode == null) {
            snapHelper.noSnapNow();
        }

        if (getCurrentBaseNode() == null || getCurrentBaseNode() == currentMouseNode)
            return; // Don't create zero length way segments.


        double curHdg = Math.toDegrees(getCurrentBaseNode().getEastNorth()
                .heading(currentMouseEastNorth));
        double baseHdg = -1;
        if (previousNode != null) {
            EastNorth en = previousNode.getEastNorth();
            if (en != null) {
                baseHdg = Math.toDegrees(en.heading(getCurrentBaseNode().getEastNorth()));
            }
        }

        snapHelper.checkAngleSnapping(currentMouseEastNorth, baseHdg, curHdg);

        // status bar was filled by snapHelper
    }

    private static void showStatusInfo(double angle, double hdg, double distance, boolean activeFlag) {
        Main.map.statusLine.setAngle(angle);
        Main.map.statusLine.activateAnglePanel(activeFlag);
        Main.map.statusLine.setHeading(hdg);
        Main.map.statusLine.setDist(distance);
    }

    /**
     * Helper function that sets fields currentBaseNode and previousNode
     * @param selection
     * uses also lastUsedNode field
     */
    private void determineCurrentBaseNodeAndPreviousNode(Collection<OsmPrimitive> selection) {
        Node selectedNode = null;
        Way selectedWay = null;
        for (OsmPrimitive p : selection) {
            if (p instanceof Node) {
                if (selectedNode != null)
                    return;
                selectedNode = (Node) p;
            } else if (p instanceof Way) {
                if (selectedWay != null)
                    return;
                selectedWay = (Way) p;
            }
        }
        // we are here, if not more than 1 way or node is selected,

        // the node from which we make a connection
        currentBaseNode = null;
        previousNode = null;

        // Try to find an open way to measure angle from it. The way is not to be continued!
        // warning: may result in changes of currentBaseNode and previousNode
        // please remove if bugs arise
        if (selectedWay == null && selectedNode != null) {
            for (OsmPrimitive p: selectedNode.getReferrers()) {
                if (p.isUsable() && p instanceof Way && ((Way) p).isFirstLastNode(selectedNode)) {
                    if (selectedWay != null) { // two uncontinued ways, nothing to take as reference
                        selectedWay = null;
                        break;
                    } else {
                        // set us ~continue this way (measure angle from it)
                        selectedWay = (Way) p;
                    }
                }
            }
        }

        if (selectedNode == null) {
            if (selectedWay == null)
                return;
            continueWayFromNode(selectedWay, lastUsedNode);
        } else if (selectedWay == null) {
            currentBaseNode = selectedNode;
        } else if (!selectedWay.isDeleted()) { // fix #7118
            continueWayFromNode(selectedWay, selectedNode);
        }
    }

    /**
     * if one of the ends of {@code way} is given {@code node},
     * then set currentBaseNode = node and previousNode = adjacent node of way
     * @param way way to continue
     * @param node starting node
     */
    private void continueWayFromNode(Way way, Node node) {
        int n = way.getNodesCount();
        if (node == way.firstNode()) {
            currentBaseNode = node;
            if (n > 1) previousNode = way.getNode(1);
        } else if (node == way.lastNode()) {
            currentBaseNode = node;
            if (n > 1) previousNode = way.getNode(n-2);
        }
    }

    /**
     * Repaint on mouse exit so that the helper line goes away.
     */
    @Override
    public void mouseExited(MouseEvent e) {
        OsmDataLayer editLayer = Main.getLayerManager().getEditLayer();
        if (editLayer == null)
            return;
        mousePos = e.getPoint();
        snapHelper.noSnapNow();
        boolean repaintIssued = removeHighlighting();
        // force repaint in case snapHelper needs one. If removeHighlighting
        // caused one already, don’t do it again.
        if (!repaintIssued) {
            editLayer.invalidate();
        }
    }

    /**
     * @param n node
     * @return If the node is the end of exactly one way, return this.
     *  <code>null</code> otherwise.
     */
    public static Way getWayForNode(Node n) {
        Way way = null;
        for (Way w : Utils.filteredCollection(n.getReferrers(), Way.class)) {
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

    /**
     * Replies the current base node, after having checked it is still usable (see #11105).
     * @return the current base node (can be null). If not-null, it's guaranteed the node is usable
     */
    public Node getCurrentBaseNode() {
        if (currentBaseNode != null && (currentBaseNode.getDataSet() == null || !currentBaseNode.isUsable())) {
            currentBaseNode = null;
        }
        return currentBaseNode;
    }

    private static void pruneSuccsAndReverse(List<Integer> is) {
        Set<Integer> is2 = new HashSet<>();
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
    private static void adjustNode(Collection<Pair<Node, Node>> segs, Node n) {

        switch (segs.size()) {
        case 0:
            return;
        case 2:
            // This computes the intersection between the two segments and adjusts the node position.
            Iterator<Pair<Node, Node>> i = segs.iterator();
            Pair<Node, Node> seg = i.next();
            EastNorth pA = seg.a.getEastNorth();
            EastNorth pB = seg.b.getEastNorth();
            seg = i.next();
            EastNorth pC = seg.a.getEastNorth();
            EastNorth pD = seg.b.getEastNorth();

            double u = det(pB.east() - pA.east(), pB.north() - pA.north(), pC.east() - pD.east(), pC.north() - pD.north());

            // Check for parallel segments and do nothing if they are
            // In practice this will probably only happen when a way has been duplicated

            if (u == 0)
                return;

            // q is a number between 0 and 1
            // It is the point in the segment where the intersection occurs
            // if the segment is scaled to lenght 1

            double q = det(pB.north() - pC.north(), pB.east() - pC.east(), pD.north() - pC.north(), pD.east() - pC.east()) / u;
            EastNorth intersection = new EastNorth(
                    pB.east() + q * (pA.east() - pB.east()),
                    pB.north() + q * (pA.north() - pB.north()));


            // only adjust to intersection if within snapToIntersectionThreshold pixel of mouse click; otherwise
            // fall through to default action.
            // (for semi-parallel lines, intersection might be miles away!)
            if (Main.map.mapView.getPoint2D(n).distance(Main.map.mapView.getPoint2D(intersection)) < snapToIntersectionThreshold) {
                n.setEastNorth(intersection);
                return;
            }
        default:
            EastNorth p = n.getEastNorth();
            seg = segs.iterator().next();
            pA = seg.a.getEastNorth();
            pB = seg.b.getEastNorth();
            double a = p.distanceSq(pB);
            double b = p.distanceSq(pA);
            double c = pA.distanceSq(pB);
            q = (a - b + c) / (2*c);
            n.setEastNorth(new EastNorth(pB.east() + q * (pA.east() - pB.east()), pB.north() + q * (pA.north() - pB.north())));
        }
    }

    // helper for adjustNode
    static double det(double a, double b, double c, double d) {
        return a * d - b * c;
    }

    private void tryToMoveNodeOnIntersection(List<WaySegment> wss, Node n) {
        if (wss.isEmpty())
            return;
        WaySegment ws = wss.get(0);
        EastNorth p1 = ws.getFirstNode().getEastNorth();
        EastNorth p2 = ws.getSecondNode().getEastNorth();
        if (snapHelper.dir2 != null && getCurrentBaseNode() != null) {
            EastNorth xPoint = Geometry.getSegmentSegmentIntersection(p1, p2, snapHelper.dir2,
                    getCurrentBaseNode().getEastNorth());
            if (xPoint != null) {
                n.setEastNorth(xPoint);
            }
        }
    }

    /**
     * Takes the data from computeHelperLine to determine which ways/nodes should be highlighted
     * (if feature enabled). Also sets the target cursor if appropriate. It adds the to-be-
     * highlighted primitives to newHighlights but does not actually highlight them. This work is
     * done in redrawIfRequired. This means, calling addHighlighting() without redrawIfRequired()
     * will leave the data in an inconsistent state.
     *
     * The status bar derives its information from oldHighlights, so in order to update the status
     * bar both addHighlighting() and repaintIfRequired() are needed, since former fills newHighlights
     * and latter processes them into oldHighlights.
     */
    private void addHighlighting() {
        newHighlights = new HashSet<>();

        // if ctrl key is held ("no join"), don't highlight anything
        if (ctrl) {
            Main.map.mapView.setNewCursor(cursor, this);
            redrawIfRequired();
            return;
        }

        // This happens when nothing is selected, but we still want to highlight the "target node"
        if (mouseOnExistingNode == null && getLayerManager().getEditDataSet().selectionEmpty() && mousePos != null) {
            mouseOnExistingNode = Main.map.mapView.getNearestNode(mousePos, OsmPrimitive::isSelectable);
        }

        if (mouseOnExistingNode != null) {
            Main.map.mapView.setNewCursor(cursorJoinNode, this);
            newHighlights.add(mouseOnExistingNode);
            redrawIfRequired();
            return;
        }

        // Insert the node into all the nearby way segments
        if (mouseOnExistingWays.isEmpty()) {
            Main.map.mapView.setNewCursor(cursor, this);
            redrawIfRequired();
            return;
        }

        Main.map.mapView.setNewCursor(cursorJoinWay, this);
        newHighlights.addAll(mouseOnExistingWays);
        redrawIfRequired();
    }

    /**
     * Removes target highlighting from primitives. Issues repaint if required.
     * @return true if a repaint has been issued.
     */
    private boolean removeHighlighting() {
        newHighlights = new HashSet<>();
        return redrawIfRequired();
    }

    @Override
    public void paint(Graphics2D g, MapView mv, Bounds box) {
        // sanity checks
        if (Main.map.mapView == null || mousePos == null
                // don't draw line if we don't know where from or where to
                || getCurrentBaseNode() == null || currentMouseEastNorth == null
                // don't draw line if mouse is outside window
                || !Main.map.mapView.getBounds().contains(mousePos))
            return;

        Graphics2D g2 = g;
        snapHelper.drawIfNeeded(g2, mv);
        if (!drawHelperLine || wayIsFinished || shift)
            return;

        if (!snapHelper.isActive()) { // else use color and stoke from  snapHelper.draw
            g2.setColor(rubberLineColor);
            g2.setStroke(rubberLineStroke);
        } else if (!snapHelper.drawConstructionGeometry)
            return;
        MapPath2D b = new MapPath2D();
        MapViewPoint p1 = mv.getState().getPointFor(getCurrentBaseNode());
        MapViewPoint p2 = mv.getState().getPointFor(currentMouseEastNorth);

        b.moveTo(p1);
        b.lineTo(p2);

        // if alt key is held ("start new way"), draw a little perpendicular line
        if (alt) {
            START_WAY_INDICATOR.paintArrowAt(b, p1, p2);
        }

        g2.draw(b);
        g2.setStroke(BASIC_STROKE);
    }

    @Override
    public String getModeHelpText() {
        StringBuilder rv;
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
            rv = new StringBuilder(tr("Create new node."));
        } else {
            // oldHighlights may store a node or way, check if it's a node
            OsmPrimitive x = oldHighlights.iterator().next();
            if (x instanceof Node) {
                rv = new StringBuilder(tr("Select node under cursor."));
            } else {
                rv = new StringBuilder(trn("Insert new node into way.", "Insert new node into {0} ways.",
                        oldHighlights.size(), oldHighlights.size()));
            }
        }

        /*
         * Check whether a connection will be made
         */
        if (getCurrentBaseNode() != null && !wayIsFinished) {
            if (alt) {
                rv.append(' ').append(tr("Start new way from last node."));
            } else {
                rv.append(' ').append(tr("Continue way from last node."));
            }
            if (snapHelper.isSnapOn()) {
                rv.append(' ').append(tr("Angle snapping active."));
            }
        }

        Node n = mouseOnExistingNode;
        DataSet ds = getLayerManager().getEditDataSet();
        /*
         * Handle special case: Highlighted node == selected node => finish drawing
         */
        if (n != null && ds != null && ds.getSelectedNodes().contains(n)) {
            if (wayIsFinished) {
                rv = new StringBuilder(tr("Select node under cursor."));
            } else {
                rv = new StringBuilder(tr("Finish drawing."));
            }
        }

        /*
         * Handle special case: Self-Overlapping or closing way
         */
        if (ds != null && !ds.getSelectedWays().isEmpty() && !wayIsFinished && !alt) {
            Way w = ds.getSelectedWays().iterator().next();
            for (Node m : w.getNodes()) {
                if (m.equals(mouseOnExistingNode) || mouseOnExistingWays.contains(w)) {
                    rv.append(' ').append(tr("Finish drawing."));
                    break;
                }
            }
        }
        return rv.toString();
    }

    /**
     * Get selected primitives, while draw action is in progress.
     *
     * While drawing a way, technically the last node is selected.
     * This is inconvenient when the user tries to add/edit tags to the way.
     * For this case, this method returns the current way as selection,
     * to work around this issue.
     * Otherwise the normal selection of the current data layer is returned.
     * @return selected primitives, while draw action is in progress
     */
    public Collection<OsmPrimitive> getInProgressSelection() {
        DataSet ds = getLayerManager().getEditDataSet();
        if (ds == null) return null;
        if (getCurrentBaseNode() != null && !ds.selectionEmpty()) {
            Way continueFrom = getWayForNode(getCurrentBaseNode());
            if (continueFrom != null)
                return Collections.<OsmPrimitive>singleton(continueFrom);
        }
        return ds.getSelected();
    }

    @Override
    public boolean layerIsSupported(Layer l) {
        return l instanceof OsmDataLayer;
    }

    @Override
    protected void updateEnabledState() {
        setEnabled(getLayerManager().getEditLayer() != null);
    }

    @Override
    public void destroy() {
        super.destroy();
        snapChangeAction.destroy();
    }

    public class BackSpaceAction extends AbstractAction {

        @Override
        public void actionPerformed(ActionEvent e) {
            Main.main.undoRedo.undo();
            Command lastCmd = Main.main.undoRedo.commands.peekLast();
            if (lastCmd == null) return;
            Node n = null;
            for (OsmPrimitive p: lastCmd.getParticipatingPrimitives()) {
                if (p instanceof Node) {
                    if (n == null) {
                        n = (Node) p; // found one node
                        wayIsFinished = false;
                    } else {
                        // if more than 1 node were affected by previous command,
                        // we have no way to continue, so we forget about found node
                        n = null;
                        break;
                    }
                }
            }
            // select last added node - maybe we will continue drawing from it
            if (n != null) {
                getLayerManager().getEditDataSet().addSelected(n);
            }
        }
    }

    private class SnapHelper {
        private final class AnglePopupMenu extends JPopupMenu {

            private final JCheckBoxMenuItem repeatedCb = new JCheckBoxMenuItem(
                    new AbstractAction(tr("Toggle snapping by {0}", getShortcut().getKeyText())) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    boolean sel = ((JCheckBoxMenuItem) e.getSource()).getState();
                    Main.pref.put("draw.anglesnap.toggleOnRepeatedA", sel);
                    init();
                }
            });

            private final JCheckBoxMenuItem helperCb = new JCheckBoxMenuItem(
                    new AbstractAction(tr("Show helper geometry")) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    boolean sel = ((JCheckBoxMenuItem) e.getSource()).getState();
                    Main.pref.put("draw.anglesnap.drawConstructionGeometry", sel);
                    Main.pref.put("draw.anglesnap.drawProjectedPoint", sel);
                    Main.pref.put("draw.anglesnap.showAngle", sel);
                    init();
                    enableSnapping();
                }
            });

            private final JCheckBoxMenuItem projectionCb = new JCheckBoxMenuItem(
                    new AbstractAction(tr("Snap to node projections")) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    boolean sel = ((JCheckBoxMenuItem) e.getSource()).getState();
                    Main.pref.put("draw.anglesnap.projectionsnap", sel);
                    init();
                    enableSnapping();
                }
            });

            private AnglePopupMenu() {
                helperCb.setState(Main.pref.getBoolean("draw.anglesnap.drawConstructionGeometry", true));
                projectionCb.setState(Main.pref.getBoolean("draw.anglesnap.projectionsnapgvff", true));
                repeatedCb.setState(Main.pref.getBoolean("draw.anglesnap.toggleOnRepeatedA", true));
                add(repeatedCb);
                add(helperCb);
                add(projectionCb);
                add(new AbstractAction(tr("Disable")) {
                    @Override public void actionPerformed(ActionEvent e) {
                        saveAngles("180");
                        init();
                        enableSnapping();
                    }
                });
                add(new AbstractAction(tr("0,90,...")) {
                    @Override public void actionPerformed(ActionEvent e) {
                        saveAngles("0", "90", "180");
                        init();
                        enableSnapping();
                    }
                });
                add(new AbstractAction(tr("0,45,90,...")) {
                    @Override public void actionPerformed(ActionEvent e) {
                        saveAngles("0", "45", "90", "135", "180");
                        init();
                        enableSnapping();
                    }
                });
                add(new AbstractAction(tr("0,30,45,60,90,...")) {
                    @Override public void actionPerformed(ActionEvent e) {
                        saveAngles("0", "30", "45", "60", "90", "120", "135", "150", "180");
                        init();
                        enableSnapping();
                    }
                });
            }
        }

        private boolean snapOn; // snapping is turned on

        private boolean active; // snapping is active for current mouse position
        private boolean fixed; // snap angle is fixed
        private boolean absoluteFix; // snap angle is absolute

        private boolean drawConstructionGeometry;
        private boolean showProjectedPoint;
        private boolean showAngle;

        private boolean snapToProjections;

        private EastNorth dir2;
        private EastNorth projected;
        private String labelText;
        private double lastAngle;

        private double customBaseHeading = -1; // angle of base line, if not last segment)
        private EastNorth segmentPoint1; // remembered first point of base segment
        private EastNorth segmentPoint2; // remembered second point of base segment
        private EastNorth projectionSource; // point that we are projecting to the line

        private double[] snapAngles;
        private double snapAngleTolerance;

        private double pe, pn; // (pe, pn) - direction of snapping line
        private double e0, n0; // (e0, n0) - origin of snapping line

        private final String fixFmt = "%d "+tr("FIX");
        private Color snapHelperColor;
        private Color highlightColor;

        private Stroke normalStroke;
        private Stroke helperStroke;
        private Stroke highlightStroke;

        private JCheckBoxMenuItem checkBox;

        private final MouseListener anglePopupListener = new PopupMenuLauncher(new AnglePopupMenu()) {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                if (e.getButton() == MouseEvent.BUTTON1) {
                    toggleSnapping();
                    updateStatusLine();
                }
            }
        };

        public void init() {
            snapOn = false;
            checkBox.setState(snapOn);
            fixed = false;
            absoluteFix = false;

            Collection<String> angles = Main.pref.getCollection("draw.anglesnap.angles",
                    Arrays.asList("0", "30", "45", "60", "90", "120", "135", "150", "180"));

            snapAngles = new double[2*angles.size()];
            int i = 0;
            for (String s: angles) {
                try {
                    snapAngles[i] = Double.parseDouble(s); i++;
                    snapAngles[i] = 360-Double.parseDouble(s); i++;
                } catch (NumberFormatException e) {
                    Main.warn("Incorrect number in draw.anglesnap.angles preferences: "+s);
                    snapAngles[i] = 0; i++;
                    snapAngles[i] = 0; i++;
                }
            }
            snapAngleTolerance = Main.pref.getDouble("draw.anglesnap.tolerance", 5.0);
            drawConstructionGeometry = Main.pref.getBoolean("draw.anglesnap.drawConstructionGeometry", true);
            showProjectedPoint = Main.pref.getBoolean("draw.anglesnap.drawProjectedPoint", true);
            snapToProjections = Main.pref.getBoolean("draw.anglesnap.projectionsnap", true);

            showAngle = Main.pref.getBoolean("draw.anglesnap.showAngle", true);
            useRepeatedShortcut = Main.pref.getBoolean("draw.anglesnap.toggleOnRepeatedA", true);

            normalStroke = rubberLineStroke;
            snapHelperColor = Main.pref.getColor(marktr("draw angle snap"), Color.ORANGE);

            highlightColor = Main.pref.getColor(marktr("draw angle snap highlight"), ORANGE_TRANSPARENT);
            highlightStroke = GuiHelper.getCustomizedStroke(Main.pref.get("draw.anglesnap.stroke.highlight", "10"));
            helperStroke = GuiHelper.getCustomizedStroke(Main.pref.get("draw.anglesnap.stroke.helper", "1 4"));
        }

        public void saveAngles(String ... angles) {
            Main.pref.putCollection("draw.anglesnap.angles", Arrays.asList(angles));
        }

        public void setMenuCheckBox(JCheckBoxMenuItem checkBox) {
            this.checkBox = checkBox;
        }

        public void drawIfNeeded(Graphics2D g2, MapView mv) {
            if (!snapOn || !active)
                return;
            MapViewPoint p1 = mv.getState().getPointFor(getCurrentBaseNode());
            MapViewPoint p2 = mv.getState().getPointFor(dir2);
            MapViewPoint p3 = mv.getState().getPointFor(projected);
            if (drawConstructionGeometry) {
                g2.setColor(snapHelperColor);
                g2.setStroke(helperStroke);

                MapPath2D b = new MapPath2D();
                b.moveTo(p2);
                if (absoluteFix) {
                    b.lineTo(2d*p1.getInViewX()-p2.getInViewX(), 2d*p1.getInViewY()-p2.getInViewY()); // bi-directional line
                } else {
                    b.lineTo(p3);
                }
                g2.draw(b);
            }
            if (projectionSource != null) {
                g2.setColor(snapHelperColor);
                g2.setStroke(helperStroke);
                MapPath2D b = new MapPath2D();
                b.moveTo(p3);
                b.lineTo(mv.getState().getPointFor(projectionSource));
                g2.draw(b);
            }

            if (customBaseHeading >= 0) {
                g2.setColor(highlightColor);
                g2.setStroke(highlightStroke);
                MapPath2D b = new MapPath2D();
                b.moveTo(mv.getState().getPointFor(segmentPoint1));
                b.lineTo(mv.getState().getPointFor(segmentPoint2));
                g2.draw(b);
            }

            g2.setColor(rubberLineColor);
            g2.setStroke(normalStroke);
            MapPath2D b = new MapPath2D();
            b.moveTo(p1);
            b.lineTo(p3);
            g2.draw(b);

            g2.drawString(labelText, (int) p3.getInViewX()-5, (int) p3.getInViewY()+20);
            if (showProjectedPoint) {
                g2.setStroke(normalStroke);
                g2.drawOval((int) p3.getInViewX()-5, (int) p3.getInViewY()-5, 10, 10); // projected point
            }

            g2.setColor(snapHelperColor);
            g2.setStroke(helperStroke);
        }

        /* If mouse position is close to line at 15-30-45-... angle, remembers this direction
         */
        public void checkAngleSnapping(EastNorth currentEN, double baseHeading, double curHeading) {
            EastNorth p0 = getCurrentBaseNode().getEastNorth();
            EastNorth snapPoint = currentEN;
            double angle = -1;

            double activeBaseHeading = (customBaseHeading >= 0) ? customBaseHeading : baseHeading;

            if (snapOn && (activeBaseHeading >= 0)) {
                angle = curHeading - activeBaseHeading;
                if (angle < 0) {
                    angle += 360;
                }
                if (angle > 360) {
                    angle = 0;
                }

                double nearestAngle;
                if (fixed) {
                    nearestAngle = lastAngle; // if direction is fixed use previous angle
                    active = true;
                } else {
                    nearestAngle = getNearestAngle(angle);
                    if (getAngleDelta(nearestAngle, angle) < snapAngleTolerance) {
                        active = customBaseHeading >= 0 || Math.abs(nearestAngle - 180) > 1e-3;
                        // if angle is to previous segment, exclude 180 degrees
                        lastAngle = nearestAngle;
                    } else {
                        active = false;
                    }
                }

                if (active) {
                    double phi;
                    e0 = p0.east();
                    n0 = p0.north();
                    buildLabelText((nearestAngle <= 180) ? nearestAngle : (nearestAngle-360));

                    phi = (nearestAngle + activeBaseHeading) * Math.PI / 180;
                    // (pe,pn) - direction of snapping line
                    pe = Math.sin(phi);
                    pn = Math.cos(phi);
                    double scale = 20 * Main.map.mapView.getDist100Pixel();
                    dir2 = new EastNorth(e0 + scale * pe, n0 + scale * pn);
                    snapPoint = getSnapPoint(currentEN);
                } else {
                    noSnapNow();
                }
            }

            // find out the distance, in metres, between the base point and projected point
            LatLon mouseLatLon = Main.map.mapView.getProjection().eastNorth2latlon(snapPoint);
            double distance = getCurrentBaseNode().getCoor().greatCircleDistance(mouseLatLon);
            double hdg = Math.toDegrees(p0.heading(snapPoint));
            // heading of segment from current to calculated point, not to mouse position

            if (baseHeading >= 0) { // there is previous line segment with some heading
                angle = hdg - baseHeading;
                if (angle < 0) {
                    angle += 360;
                }
                if (angle > 360) {
                    angle = 0;
                }
            }
            showStatusInfo(angle, hdg, distance, isSnapOn());
        }

        private void buildLabelText(double nearestAngle) {
            if (showAngle) {
                if (fixed) {
                    if (absoluteFix) {
                        labelText = "=";
                    } else {
                        labelText = String.format(fixFmt, (int) nearestAngle);
                    }
                } else {
                    labelText = String.format("%d", (int) nearestAngle);
                }
            } else {
                if (fixed) {
                    if (absoluteFix) {
                        labelText = "=";
                    } else {
                        labelText = String.format(tr("FIX"), 0);
                    }
                } else {
                    labelText = "";
                }
            }
        }

        public EastNorth getSnapPoint(EastNorth p) {
            if (!active)
                return p;
            double de = p.east()-e0;
            double dn = p.north()-n0;
            double l = de*pe+dn*pn;
            double delta = Main.map.mapView.getDist100Pixel()/20;
            if (!absoluteFix && l < delta) {
                active = false;
                return p;
            } //  do not go backward!

            projectionSource = null;
            if (snapToProjections) {
                DataSet ds = getLayerManager().getEditDataSet();
                Collection<Way> selectedWays = ds.getSelectedWays();
                if (selectedWays.size() == 1) {
                    Way w = selectedWays.iterator().next();
                    Collection<EastNorth> pointsToProject = new ArrayList<>();
                    if (w.getNodesCount() < 1000) {
                        for (Node n: w.getNodes()) {
                            pointsToProject.add(n.getEastNorth());
                        }
                    }
                    if (customBaseHeading >= 0) {
                        pointsToProject.add(segmentPoint1);
                        pointsToProject.add(segmentPoint2);
                    }
                    EastNorth enOpt = null;
                    double dOpt = 1e5;
                    for (EastNorth en: pointsToProject) { // searching for besht projection
                        double l1 = (en.east()-e0)*pe+(en.north()-n0)*pn;
                        double d1 = Math.abs(l1-l);
                        if (d1 < delta && d1 < dOpt) {
                            l = l1;
                            enOpt = en;
                            dOpt = d1;
                        }
                    }
                    if (enOpt != null) {
                        projectionSource = enOpt;
                    }
                }
            }
            projected = new EastNorth(e0+l*pe, n0+l*pn);
            return projected;
        }

        public void noSnapNow() {
            active = false;
            dir2 = null;
            projected = null;
            labelText = null;
        }

        public void setBaseSegment(WaySegment seg) {
            if (seg == null) return;
            segmentPoint1 = seg.getFirstNode().getEastNorth();
            segmentPoint2 = seg.getSecondNode().getEastNorth();

            double hdg = segmentPoint1.heading(segmentPoint2);
            hdg = Math.toDegrees(hdg);
            if (hdg < 0) {
                hdg += 360;
            }
            if (hdg > 360) {
                hdg -= 360;
            }
            customBaseHeading = hdg;
        }

        private void enableSnapping() {
            snapOn = true;
            checkBox.setState(snapOn);
            customBaseHeading = -1;
            unsetFixedMode();
        }

        private void toggleSnapping() {
            snapOn = !snapOn;
            checkBox.setState(snapOn);
            customBaseHeading = -1;
            unsetFixedMode();
        }

        public void setFixedMode() {
            if (active) {
                fixed = true;
            }
        }

        public void unsetFixedMode() {
            fixed = false;
            absoluteFix = false;
            lastAngle = 0;
            active = false;
        }

        public boolean isActive() {
            return active;
        }

        public boolean isSnapOn() {
            return snapOn;
        }

        private double getNearestAngle(double angle) {
            double delta, minDelta = 1e5, bestAngle = 0.0;
            for (double snapAngle : snapAngles) {
                delta = getAngleDelta(angle, snapAngle);
                if (delta < minDelta) {
                    minDelta = delta;
                    bestAngle = snapAngle;
                }
            }
            if (Math.abs(bestAngle-360) < 1e-3) {
                bestAngle = 0;
            }
            return bestAngle;
        }

        private double getAngleDelta(double a, double b) {
            double delta = Math.abs(a-b);
            if (delta > 180)
                return 360-delta;
            else
                return delta;
        }

        private void unFixOrTurnOff() {
            if (absoluteFix) {
                unsetFixedMode();
            } else {
                toggleSnapping();
            }
        }
    }

    private class SnapChangeAction extends JosmAction {
        /**
         * Constructs a new {@code SnapChangeAction}.
         */
        SnapChangeAction() {
            super(tr("Angle snapping"), /* ICON() */ "anglesnap",
                    tr("Switch angle snapping mode while drawing"), null, false);
            putValue("help", ht("/Action/Draw/AngleSnap"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (snapHelper != null) {
                snapHelper.toggleSnapping();
            }
        }

        @Override
        protected void updateEnabledState() {
            setEnabled(Main.map != null && Main.map.mapMode instanceof DrawAction);
        }
    }
}
