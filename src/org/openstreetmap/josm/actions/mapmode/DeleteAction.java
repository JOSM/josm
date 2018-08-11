// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.mapmode;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.DeleteCommand;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.WaySegment;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.dialogs.relation.RelationDialogManager;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.MainLayerManager;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.util.HighlightHelper;
import org.openstreetmap.josm.gui.util.ModifierExListener;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * A map mode that enables the user to delete nodes and other objects.
 *
 * The user can click on an object, which gets deleted if possible. When Ctrl is
 * pressed when releasing the button, the objects and all its references are deleted.
 *
 * If the user did not press Ctrl and the object has any references, the user
 * is informed and nothing is deleted.
 *
 * If the user enters the mapmode and any object is selected, all selected
 * objects are deleted, if possible.
 *
 * @author imi
 */
public class DeleteAction extends MapMode implements ModifierExListener {
    // Cache previous mouse event (needed when only the modifier keys are pressed but the mouse isn't moved)
    private MouseEvent oldEvent;

    /**
     * elements that have been highlighted in the previous iteration. Used
     * to remove the highlight from them again as otherwise the whole data
     * set would have to be checked.
     */
    private transient WaySegment oldHighlightedWaySegment;

    private static final HighlightHelper HIGHLIGHT_HELPER = new HighlightHelper();
    private boolean drawTargetHighlight;

    enum DeleteMode {
        none(/* ICON(cursor/modifier/) */ "delete"),
        segment(/* ICON(cursor/modifier/) */ "delete_segment"),
        node(/* ICON(cursor/modifier/) */ "delete_node"),
        node_with_references(/* ICON(cursor/modifier/) */ "delete_node"),
        way(/* ICON(cursor/modifier/) */ "delete_way_only"),
        way_with_references(/* ICON(cursor/modifier/) */ "delete_way_normal"),
        way_with_nodes(/* ICON(cursor/modifier/) */ "delete_way_node_only");

        private final Cursor c;

        DeleteMode(String cursorName) {
            c = ImageProvider.getCursor("normal", cursorName);
        }

        /**
         * Returns the mode cursor.
         * @return the mode cursor
         */
        public Cursor cursor() {
            return c;
        }
    }

    private static class DeleteParameters {
        private DeleteMode mode;
        private Node nearestNode;
        private WaySegment nearestSegment;
    }

    /**
     * Construct a new DeleteAction. Mnemonic is the delete - key.
     * @since 11713
     */
    public DeleteAction() {
        super(tr("Delete Mode"),
                "delete",
                tr("Delete nodes or ways."),
                Shortcut.registerShortcut("mapmode:delete", tr("Mode: {0}", tr("Delete")),
                KeyEvent.VK_DELETE, Shortcut.CTRL),
                ImageProvider.getCursor("normal", "delete"));
    }

    @Override
    public void enterMode() {
        super.enterMode();
        if (!isEnabled())
            return;

        drawTargetHighlight = Config.getPref().getBoolean("draw.target-highlight", true);

        MapFrame map = MainApplication.getMap();
        map.mapView.addMouseListener(this);
        map.mapView.addMouseMotionListener(this);
        // This is required to update the cursors when ctrl/shift/alt is pressed
        map.keyDetector.addModifierExListener(this);
    }

    @Override
    public void exitMode() {
        super.exitMode();
        MapFrame map = MainApplication.getMap();
        map.mapView.removeMouseListener(this);
        map.mapView.removeMouseMotionListener(this);
        map.keyDetector.removeModifierExListener(this);
        removeHighlighting();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        doActionPerformed(e);
    }

    /**
     * Invoked when the action occurs.
     * @param e Action event
     */
    public void doActionPerformed(ActionEvent e) {
        MainLayerManager lm = MainApplication.getLayerManager();
        OsmDataLayer editLayer = lm.getEditLayer();
        if (editLayer == null) {
            return;
        }

        updateKeyModifiers(e);

        Command c;
        if (ctrl) {
            c = DeleteCommand.deleteWithReferences(lm.getEditDataSet().getSelected());
        } else {
            c = DeleteCommand.delete(lm.getEditDataSet().getSelected(), !alt /* also delete nodes in way */);
        }
        // if c is null, an error occurred or the user aborted. Don't do anything in that case.
        if (c != null) {
            UndoRedoHandler.getInstance().add(c);
            //FIXME: This should not be required, DeleteCommand should update the selection, otherwise undo/redo won't work.
            lm.getEditDataSet().setSelected();
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        mouseMoved(e);
    }

    /**
     * Listen to mouse move to be able to update the cursor (and highlights)
     * @param e The mouse event that has been captured
     */
    @Override
    public void mouseMoved(MouseEvent e) {
        oldEvent = e;
        giveUserFeedback(e);
    }

    /**
     * removes any highlighting that may have been set beforehand.
     */
    private void removeHighlighting() {
        HIGHLIGHT_HELPER.clear();
        DataSet ds = getLayerManager().getEditDataSet();
        if (ds != null) {
            ds.clearHighlightedWaySegments();
        }
    }

    /**
     * handles everything related to highlighting primitives and way
     * segments for the given pointer position (via MouseEvent) and modifiers.
     * @param e current mouse event
     * @param modifiers extended mouse modifiers, not necessarly taken from the given mouse event
     */
    private void addHighlighting(MouseEvent e, int modifiers) {
        if (!drawTargetHighlight)
            return;

        Set<OsmPrimitive> newHighlights = new HashSet<>();
        DeleteParameters parameters = getDeleteParameters(e, modifiers);

        if (parameters.mode == DeleteMode.segment) {
            // deleting segments is the only action not working on OsmPrimitives
            // so we have to handle them separately.
            repaintIfRequired(newHighlights, parameters.nearestSegment);
        } else {
            // don't call buildDeleteCommands for DeleteMode.segment because it doesn't support
            // silent operation and SplitWayAction will show dialogs. A lot.
            Command delCmd = buildDeleteCommands(e, modifiers, true);
            if (delCmd != null) {
                // all other cases delete OsmPrimitives directly, so we can safely do the following
                for (OsmPrimitive osm : delCmd.getParticipatingPrimitives()) {
                    newHighlights.add(osm);
                }
            }
            repaintIfRequired(newHighlights, null);
        }
    }

    private void repaintIfRequired(Set<OsmPrimitive> newHighlights, WaySegment newHighlightedWaySegment) {
        boolean needsRepaint = false;
        OsmDataLayer editLayer = getLayerManager().getEditLayer();

        if (newHighlightedWaySegment == null && oldHighlightedWaySegment != null) {
            if (editLayer != null) {
                editLayer.data.clearHighlightedWaySegments();
                needsRepaint = true;
            }
            oldHighlightedWaySegment = null;
        } else if (newHighlightedWaySegment != null && !newHighlightedWaySegment.equals(oldHighlightedWaySegment)) {
            if (editLayer != null) {
                editLayer.data.setHighlightedWaySegments(Collections.singleton(newHighlightedWaySegment));
                needsRepaint = true;
            }
            oldHighlightedWaySegment = newHighlightedWaySegment;
        }
        needsRepaint |= HIGHLIGHT_HELPER.highlightOnly(newHighlights);
        if (needsRepaint && editLayer != null) {
            editLayer.invalidate();
        }
    }

    /**
     * This function handles all work related to updating the cursor and highlights
     *
     * @param e current mouse event
     * @param modifiers extended mouse modifiers, not necessarly taken from the given mouse event
     */
    private void updateCursor(MouseEvent e, int modifiers) {
        if (!MainApplication.isDisplayingMapView())
            return;
        MapFrame map = MainApplication.getMap();
        if (!map.mapView.isActiveLayerVisible() || e == null)
            return;

        DeleteParameters parameters = getDeleteParameters(e, modifiers);
        map.mapView.setNewCursor(parameters.mode.cursor(), this);
    }

    /**
     * Gives the user feedback for the action he/she is about to do. Currently
     * calls the cursor and target highlighting routines. Allows for modifiers
     * not taken from the given mouse event.
     *
     * Normally the mouse event also contains the modifiers. However, when the
     * mouse is not moved and only modifier keys are pressed, no mouse event
     * occurs. We can use AWTEvent to catch those but still lack a proper
     * mouseevent. Instead we copy the previous event and only update the modifiers.
     * @param e mouse event
     * @param modifiers mouse modifiers
     */
    private void giveUserFeedback(MouseEvent e, int modifiers) {
        updateCursor(e, modifiers);
        addHighlighting(e, modifiers);
    }

    /**
     * Gives the user feedback for the action he/she is about to do. Currently
     * calls the cursor and target highlighting routines. Extracts modifiers
     * from mouse event.
     * @param e mouse event
     */
    private void giveUserFeedback(MouseEvent e) {
        giveUserFeedback(e, e.getModifiersEx());
    }

    /**
     * If user clicked with the left button, delete the nearest object.
     */
    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.getButton() != MouseEvent.BUTTON1)
            return;
        MapFrame map = MainApplication.getMap();
        if (!map.mapView.isActiveLayerVisible())
            return;

        // request focus in order to enable the expected keyboard shortcuts
        //
        map.mapView.requestFocus();

        Command c = buildDeleteCommands(e, e.getModifiersEx(), false);
        if (c != null) {
            UndoRedoHandler.getInstance().add(c);
        }

        getLayerManager().getEditDataSet().setSelected();
        giveUserFeedback(e);
    }

    @Override
    public String getModeHelpText() {
        // CHECKSTYLE.OFF: LineLength
        return tr("Click to delete. Shift: delete way segment. Alt: do not delete unused nodes when deleting a way. Ctrl: delete referring objects.");
        // CHECKSTYLE.ON: LineLength
    }

    @Override
    public boolean layerIsSupported(Layer l) {
        return isEditableDataLayer(l);
    }

    @Override
    protected void updateEnabledState() {
        setEnabled(MainApplication.isDisplayingMapView() && MainApplication.getMap().mapView.isActiveLayerDrawable());
    }

    /**
     * Deletes the relation in the context of the given layer.
     *
     * @param layer the layer in whose context the relation is deleted. Must not be null.
     * @param toDelete  the relation to be deleted. Must not be null.
     * @throws IllegalArgumentException if layer is null
     * @throws IllegalArgumentException if toDelete is null
     */
    public static void deleteRelation(OsmDataLayer layer, Relation toDelete) {
        deleteRelations(layer, Collections.singleton(toDelete));
    }

    /**
     * Deletes the relations in the context of the given layer.
     *
     * @param layer the layer in whose context the relations are deleted. Must not be null.
     * @param toDelete the relations to be deleted. Must not be null.
     * @throws IllegalArgumentException if layer is null
     * @throws IllegalArgumentException if toDelete is null
     */
    public static void deleteRelations(OsmDataLayer layer, Collection<Relation> toDelete) {
        CheckParameterUtil.ensureParameterNotNull(layer, "layer");
        CheckParameterUtil.ensureParameterNotNull(toDelete, "toDelete");

        final Command cmd = DeleteCommand.delete(toDelete);
        if (cmd != null) {
            // cmd can be null if the user cancels dialogs DialogCommand displays
            UndoRedoHandler.getInstance().add(cmd);
            for (Relation relation : toDelete) {
                if (layer.data.getSelectedRelations().contains(relation)) {
                    layer.data.toggleSelected(relation);
                }
                RelationDialogManager.getRelationDialogManager().close(layer, relation);
            }
        }
    }

    private DeleteParameters getDeleteParameters(MouseEvent e, int modifiers) {
        updateKeyModifiersEx(modifiers);

        DeleteParameters result = new DeleteParameters();

        MapView mapView = MainApplication.getMap().mapView;
        result.nearestNode = mapView.getNearestNode(e.getPoint(), OsmPrimitive::isSelectable);
        if (result.nearestNode == null) {
            result.nearestSegment = mapView.getNearestWaySegment(e.getPoint(), OsmPrimitive::isSelectable);
            if (result.nearestSegment != null) {
                if (shift) {
                    result.mode = DeleteMode.segment;
                } else if (ctrl) {
                    result.mode = DeleteMode.way_with_references;
                } else {
                    result.mode = alt ? DeleteMode.way : DeleteMode.way_with_nodes;
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
     * @param silent Set to true if the user should not be bugged with additional dialogs
     * @return delete command
     */
    private Command buildDeleteCommands(MouseEvent e, int modifiers, boolean silent) {
        DeleteParameters parameters = getDeleteParameters(e, modifiers);
        switch (parameters.mode) {
        case node:
            return DeleteCommand.delete(Collections.singleton(parameters.nearestNode), false, silent);
        case node_with_references:
            return DeleteCommand.deleteWithReferences(Collections.singleton(parameters.nearestNode), silent);
        case segment:
            return DeleteCommand.deleteWaySegment(parameters.nearestSegment);
        case way:
            return DeleteCommand.delete(Collections.singleton(parameters.nearestSegment.way), false, silent);
        case way_with_nodes:
            return DeleteCommand.delete(Collections.singleton(parameters.nearestSegment.way), true, silent);
        case way_with_references:
            return DeleteCommand.deleteWithReferences(Collections.singleton(parameters.nearestSegment.way), true);
        default:
            return null;
        }
    }

    /**
     * This is required to update the cursors when ctrl/shift/alt is pressed
     */
    @Override
    public void modifiersExChanged(int modifiers) {
        if (oldEvent == null)
            return;
        // We don't have a mouse event, so we pass the old mouse event but the new modifiers.
        giveUserFeedback(oldEvent, modifiers);
    }
}
