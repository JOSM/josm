// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.mapmode;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.AWTEvent;
import java.awt.Cursor;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.DeleteCommand;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.WaySegment;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.dialogs.relation.RelationDialogManager;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.util.HighlightHelper;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * A map mode that enables the user to delete nodes and other objects.
 *
 * The user can click on an object, which gets deleted if possible. When Ctrl is
 * pressed when releasing the button, the objects and all its references are
 * deleted.
 *
 * If the user did not press Ctrl and the object has any references, the user
 * is informed and nothing is deleted.
 *
 * If the user enters the mapmode and any object is selected, all selected
 * objects are deleted, if possible.
 *
 * @author imi
 */
public class DeleteAction extends MapMode implements AWTEventListener {
    // Cache previous mouse event (needed when only the modifier keys are
    // pressed but the mouse isn't moved)
    private MouseEvent oldEvent = null;

    /**
     * elements that have been highlighted in the previous iteration. Used
     * to remove the highlight from them again as otherwise the whole data
     * set would have to be checked.
     */
    private WaySegment oldHighlightedWaySegment = null;

    private static final HighlightHelper highlightHelper = new HighlightHelper();
    private boolean drawTargetHighlight;

    private enum DeleteMode {
        none("delete"),
        segment("delete_segment"),
        node("delete_node"),
        node_with_references("delete_node"),
        way("delete_way_only"),
        way_with_references("delete_way_normal"),
        way_with_nodes("delete_way_node_only");

        private final Cursor c;

        private DeleteMode(String cursorName) {
            c = ImageProvider.getCursor("normal", cursorName);
        }

        public Cursor cursor() {
            return c;
        }
    }

    private static class DeleteParameters {
        DeleteMode mode;
        Node nearestNode;
        WaySegment nearestSegment;
    }

    /**
     * Construct a new DeleteAction. Mnemonic is the delete - key.
     * @param mapFrame The frame this action belongs to.
     */
    public DeleteAction(MapFrame mapFrame) {
        super(tr("Delete Mode"),
                "delete",
                tr("Delete nodes or ways."),
                Shortcut.registerShortcut("mapmode:delete", tr("Mode: {0}",tr("Delete")),
                KeyEvent.VK_DELETE, Shortcut.CTRL),
                mapFrame,
                ImageProvider.getCursor("normal", "delete"));
    }

    @Override public void enterMode() {
        super.enterMode();
        if (!isEnabled())
            return;

        drawTargetHighlight = Main.pref.getBoolean("draw.target-highlight", true);

        Main.map.mapView.addMouseListener(this);
        Main.map.mapView.addMouseMotionListener(this);
        // This is required to update the cursors when ctrl/shift/alt is pressed
        try {
            Toolkit.getDefaultToolkit().addAWTEventListener(this, AWTEvent.KEY_EVENT_MASK);
        } catch (SecurityException ex) {
            Main.warn(ex);
        }
    }

    @Override public void exitMode() {
        super.exitMode();
        Main.map.mapView.removeMouseListener(this);
        Main.map.mapView.removeMouseMotionListener(this);
        try {
            Toolkit.getDefaultToolkit().removeAWTEventListener(this);
        } catch (SecurityException ex) {
            Main.warn(ex);
        }
        removeHighlighting();
    }

    @Override public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        doActionPerformed(e);
    }

    static public void doActionPerformed(ActionEvent e) {
        if(!Main.map.mapView.isActiveLayerDrawable())
            return;
        boolean ctrl = (e.getModifiers() & ActionEvent.CTRL_MASK) != 0;
        boolean alt = (e.getModifiers() & (ActionEvent.ALT_MASK|InputEvent.ALT_GRAPH_MASK)) != 0;

        Command c;
        if (ctrl) {
            c = DeleteCommand.deleteWithReferences(getEditLayer(),getCurrentDataSet().getSelected());
        } else {
            c = DeleteCommand.delete(getEditLayer(),getCurrentDataSet().getSelected(), !alt /* also delete nodes in way */);
        }
        // if c is null, an error occurred or the user aborted. Don't do anything in that case.
        if (c != null) {
            Main.main.undoRedo.add(c);
            getCurrentDataSet().setSelected();
            Main.map.repaint();
        }
    }

    @Override public void mouseDragged(MouseEvent e) {
        mouseMoved(e);
    }

    /**
     * Listen to mouse move to be able to update the cursor (and highlights)
     * @param e The mouse event that has been captured
     */
    @Override public void mouseMoved(MouseEvent e) {
        oldEvent = e;
        giveUserFeedback(e);
    }

    /**
     * removes any highlighting that may have been set beforehand.
     */
    private void removeHighlighting() {
        highlightHelper.clear();
        DataSet ds = getCurrentDataSet();
        if(ds != null) {
            ds.clearHighlightedWaySegments();
        }
    }

    /**
     * handles everything related to highlighting primitives and way
     * segments for the given pointer position (via MouseEvent) and
     * modifiers.
     * @param e
     * @param modifiers
     */
    private void addHighlighting(MouseEvent e, int modifiers) {
        if(!drawTargetHighlight)
            return;

        Set<OsmPrimitive> newHighlights = new HashSet<OsmPrimitive>();
        DeleteParameters parameters = getDeleteParameters(e, modifiers);

        if(parameters.mode == DeleteMode.segment) {
            // deleting segments is the only action not working on OsmPrimitives
            // so we have to handle them separately.
            repaintIfRequired(newHighlights, parameters.nearestSegment);
        } else {
            // don't call buildDeleteCommands for DeleteMode.segment because it doesn't support
            // silent operation and SplitWayAction will show dialogs. A lot.
            Command delCmd = buildDeleteCommands(e, modifiers, true);
            if(delCmd != null) {
                // all other cases delete OsmPrimitives directly, so we can
                // safely do the following
                for(OsmPrimitive osm : delCmd.getParticipatingPrimitives()) {
                    newHighlights.add(osm);
                }
            }
            repaintIfRequired(newHighlights, null);
        }
    }

    private void repaintIfRequired(Set<OsmPrimitive> newHighlights, WaySegment newHighlightedWaySegment) {
        boolean needsRepaint = false;
        DataSet ds = getCurrentDataSet();

        if(newHighlightedWaySegment == null && oldHighlightedWaySegment != null) {
            if(ds != null) {
                ds.clearHighlightedWaySegments();
                needsRepaint = true;
            }
            oldHighlightedWaySegment = null;
        } else if(newHighlightedWaySegment != null && !newHighlightedWaySegment.equals(oldHighlightedWaySegment)) {
            if(ds != null) {
                ds.setHighlightedWaySegments(Collections.singleton(newHighlightedWaySegment));
                needsRepaint = true;
            }
            oldHighlightedWaySegment = newHighlightedWaySegment;
        }
        needsRepaint |= highlightHelper.highlightOnly(newHighlights);
        if(needsRepaint) {
            Main.map.mapView.repaint();
        }
    }

    /**
     * This function handles all work related to updating the cursor and
     * highlights
     *
     * @param e
     * @param modifiers
     */
    private void updateCursor(MouseEvent e, int modifiers) {
        if (!Main.isDisplayingMapView())
            return;
        if(!Main.map.mapView.isActiveLayerVisible() || e == null)
            return;

        DeleteParameters parameters = getDeleteParameters(e, modifiers);
        Main.map.mapView.setNewCursor(parameters.mode.cursor(), this);
    }
    /**
     * Gives the user feedback for the action he/she is about to do. Currently
     * calls the cursor and target highlighting routines. Allows for modifiers
     * not taken from the given mouse event.
     *
     * Normally the mouse event also contains the modifiers. However, when the
     * mouse is not moved and only modifier keys are pressed, no mouse event
     * occurs. We can use AWTEvent to catch those but still lack a proper
     * mouseevent. Instead we copy the previous event and only update the
     * modifiers.
     */
    private void giveUserFeedback(MouseEvent e, int modifiers) {
        updateCursor(e, modifiers);
        addHighlighting(e, modifiers);
    }

    /**
     * Gives the user feedback for the action he/she is about to do. Currently
     * calls the cursor and target highlighting routines. Extracts modifiers
     * from mouse event.
     */
    private void giveUserFeedback(MouseEvent e) {
        giveUserFeedback(e, e.getModifiers());
    }

    /**
     * If user clicked with the left button, delete the nearest object.
     * position.
     */
    @Override public void mouseReleased(MouseEvent e) {
        if (e.getButton() != MouseEvent.BUTTON1)
            return;
        if(!Main.map.mapView.isActiveLayerVisible())
            return;

        // request focus in order to enable the expected keyboard shortcuts
        //
        Main.map.mapView.requestFocus();

        Command c = buildDeleteCommands(e, e.getModifiers(), false);
        if (c != null) {
            Main.main.undoRedo.add(c);
        }

        getCurrentDataSet().setSelected();
        giveUserFeedback(e);
    }

    @Override public String getModeHelpText() {
        return tr("Click to delete. Shift: delete way segment. Alt: do not delete unused nodes when deleting a way. Ctrl: delete referring objects.");
    }

    @Override public boolean layerIsSupported(Layer l) {
        return l instanceof OsmDataLayer;
    }

    @Override
    protected void updateEnabledState() {
        setEnabled(Main.isDisplayingMapView() && Main.map.mapView.isActiveLayerDrawable());
    }

    /**
     * Deletes the relation in the context of the given layer.
     *
     * @param layer the layer in whose context the relation is deleted. Must not be null.
     * @param toDelete  the relation to be deleted. Must  not be null.
     * @exception IllegalArgumentException thrown if layer is null
     * @exception IllegalArgumentException thrown if toDelete is nul
     */
    public static void deleteRelation(OsmDataLayer layer, Relation toDelete) {
        CheckParameterUtil.ensureParameterNotNull(layer, "layer");
        CheckParameterUtil.ensureParameterNotNull(toDelete, "toDelete");

        Command cmd = DeleteCommand.delete(layer, Collections.singleton(toDelete));
        if (cmd != null) {
            // cmd can be null if the user cancels dialogs DialogCommand displays
            Main.main.undoRedo.add(cmd);
            if (getCurrentDataSet().getSelectedRelations().contains(toDelete)) {
                getCurrentDataSet().toggleSelected(toDelete);
            }
            RelationDialogManager.getRelationDialogManager().close(layer, toDelete);
        }
    }

    private DeleteParameters getDeleteParameters(MouseEvent e, int modifiers) {
        updateKeyModifiers(modifiers);

        DeleteParameters result = new DeleteParameters();

        result.nearestNode = Main.map.mapView.getNearestNode(e.getPoint(), OsmPrimitive.isSelectablePredicate);
        if (result.nearestNode == null) {
            result.nearestSegment = Main.map.mapView.getNearestWaySegment(e.getPoint(), OsmPrimitive.isSelectablePredicate);
            if (result.nearestSegment != null) {
                if (shift) {
                    result.mode = DeleteMode.segment;
                } else if (ctrl) {
                    result.mode = DeleteMode.way_with_references;
                } else {
                    result.mode = alt?DeleteMode.way:DeleteMode.way_with_nodes;
                }
            } else {
                result.mode = DeleteMode.none;
            }
        } else if (ctrl) {
            result.mode = DeleteMode.node_with_references;
        } else {
            result.mode = DeleteMode.node;
        }

        return result;
    }

    /**
     * This function takes any mouse event argument and builds the list of elements
     * that should be deleted but does not actually delete them.
     * @param e MouseEvent from which modifiers and position are taken
     * @param modifiers For explanation, see {@link #updateCursor}
     * @param silent Set to true if the user should not be bugged with additional
     *        dialogs
     * @return delete command
     */
    private Command buildDeleteCommands(MouseEvent e, int modifiers, boolean silent) {
        DeleteParameters parameters = getDeleteParameters(e, modifiers);
        switch (parameters.mode) {
        case node:
            return DeleteCommand.delete(getEditLayer(),Collections.singleton(parameters.nearestNode), false, silent);
        case node_with_references:
            return DeleteCommand.deleteWithReferences(getEditLayer(),Collections.singleton(parameters.nearestNode), silent);
        case segment:
            return DeleteCommand.deleteWaySegment(getEditLayer(), parameters.nearestSegment);
        case way:
            return DeleteCommand.delete(getEditLayer(), Collections.singleton(parameters.nearestSegment.way), false, silent);
        case way_with_nodes:
            return DeleteCommand.delete(getEditLayer(), Collections.singleton(parameters.nearestSegment.way), true, silent);
        case way_with_references:
            return DeleteCommand.deleteWithReferences(getEditLayer(), Collections.singleton(parameters.nearestSegment.way), true);
        default:
            return null;
        }
    }

    /**
     * This is required to update the cursors when ctrl/shift/alt is pressed
     */
    @Override
    public void eventDispatched(AWTEvent e) {
        if(oldEvent == null)
            return;
        // We don't have a mouse event, so we pass the old mouse event but the
        // new modifiers.
        giveUserFeedback(oldEvent, ((InputEvent) e).getModifiers());
    }
}
