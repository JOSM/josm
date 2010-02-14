// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions.mapmode;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.AWTEvent;
import java.awt.Cursor;
import java.awt.EventQueue;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.Collections;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.DeleteCommand;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.WaySegment;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.dialogs.relation.RelationDialogManager;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * An action that enables the user to delete nodes and other objects.
 *
 * The user can click on an object, which gets deleted if possible. When Ctrl is
 * pressed when releasing the button, the objects and all its references are
 * deleted.
 *
 * If the user did not press Ctrl and the object has any references, the user
 * is informed and nothing is deleted.
 *
 * If the user enters the mapmode and any object is selected, all selected
 * objects that can be deleted will.
 *
 * @author imi
 */

/**
 * This class contains stubs for highlighting affected primitives when affected.
 * However, way segments can be deleted as well, but cannot be highlighted
 * alone. If the highlight feature for this delete action is to be implemented
 * properly, highlighting way segments must be possible first. --xeen, 2009-09-02
 */
public class DeleteAction extends MapMode implements AWTEventListener {
    //private boolean drawTargetHighlight;
    private boolean drawTargetCursor;
    //private Collection<? extends OsmPrimitive> oldPrims = null;

    // Cache previous mouse event (needed when only the modifier keys are
    // pressed but the mouse isn't moved)
    private MouseEvent oldEvent = null;

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
    private DeleteMode currentMode = DeleteMode.none;

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
                Shortcut.registerShortcut("mapmode:delete", tr("Mode: {0}",tr("Delete")), KeyEvent.VK_D, Shortcut.GROUP_EDIT),
                mapFrame,
                ImageProvider.getCursor("normal", "delete"));
    }

    @Override public void enterMode() {
        super.enterMode();
        if (!isEnabled())
            return;
        //drawTargetHighlight = Main.pref.getBoolean("draw.target-highlight", true);
        drawTargetCursor = Main.pref.getBoolean("draw.target-cursor", true);

        Main.map.mapView.addMouseListener(this);
        Main.map.mapView.addMouseMotionListener(this);
        // This is required to update the cursors when ctrl/shift/alt is pressed
        try {
            Toolkit.getDefaultToolkit().addAWTEventListener(this, AWTEvent.KEY_EVENT_MASK);
        } catch (SecurityException ex) {
            System.out.println(ex);
        }

        currentMode = DeleteMode.none;
    }

    @Override public void exitMode() {
        super.exitMode();
        Main.map.mapView.removeMouseListener(this);
        Main.map.mapView.removeMouseMotionListener(this);
        try {
            Toolkit.getDefaultToolkit().removeAWTEventListener(this);
        } catch (SecurityException ex) {
            System.out.println(ex);
        }
    }

    @Override public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        if(!Main.map.mapView.isActiveLayerDrawable())
            return;
        doActionPerformed(e);
    }

    public void doActionPerformed(ActionEvent e) {
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
        if (c != null) {
            Main.main.undoRedo.add(c);
        }

        getCurrentDataSet().setSelected();
        Main.map.repaint();
    }

    @Override public void mouseDragged(MouseEvent e) {
        mouseMoved(e);
    }

    /**
     * Listen to mouse move to be able to update the cursor (and highlights)
     * @param MouseEvent The mouse event that has been captured
     */
    @Override public void mouseMoved(MouseEvent e) {
        oldEvent = e;
        updateCursor(e, e.getModifiers());
    }

    /**
     * This function handles all work related to updating the cursor and
     * highlights. For now, only the cursor is enabled because highlighting
     * requires WaySegment to be highlightable.
     *
     * Normally the mouse event also contains the modifiers. However, when the
     * mouse is not moved and only modifier keys are pressed, no mouse event
     * occurs. We can use AWTEvent to catch those but still lack a proper
     * mouseevent. Instead we copy the previous event and only update the
     * modifiers.
     *
     * @param MouseEvent
     * @param int modifiers
     */
    private void updateCursor(MouseEvent e, int modifiers) {
        if (!Main.isDisplayingMapView())
            return;
        if(!Main.map.mapView.isActiveLayerVisible() || e == null)
            return;

        // Clean old highlights
        //cleanOldHighlights();

        DeleteParameters parameters = getDeleteParameters(e, modifiers);
        setCursor(parameters.mode);

        // Needs to implement WaySegment highlight first
        /*if(drawTargetHighlight) {
            // Add new highlights
            for(OsmPrimitive p : prims) {
                p.highlighted = true;
            }
            oldPrims = prims;
        }*/

        // We only need to repaint if the highlights changed
        //Main.map.mapView.repaint();
    }

    /**
     * Small helper function that cleans old highlights
     */
    /*private void cleanOldHighlights() {
        if(oldPrims == null)
            return;
        for(OsmPrimitive p: oldPrims) {
            p.highlighted = false;
        }
    }*/

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
        Main.map.mapView.repaint();
    }

    @Override public String getModeHelpText() {
        return tr("Click to delete. Shift: delete way segment. Alt: do not delete unused nodes when deleting a way. Ctrl: delete referring objects.");
    }

    @Override public boolean layerIsSupported(Layer l) {
        return l instanceof OsmDataLayer;
    }

    @Override
    protected void updateEnabledState() {
        setEnabled(Main.map != null && Main.map.mapView != null && Main.map.mapView.isActiveLayerDrawable());
    }

    /**
     * Deletes the relation in the context of the given layer. Also notifies
     * {@see RelationDialogManager} and {@see OsmDataLayer#fireDataChange()} events.
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
            RelationDialogManager.getRelationDialogManager().close(layer, toDelete);
        }
    }

    private DeleteParameters getDeleteParameters(MouseEvent e, int modifiers) {
        // Note: CTRL is the only modifier that is checked in MouseMove, don't
        // forget updating it there
        boolean ctrl = (modifiers & ActionEvent.CTRL_MASK) != 0;
        boolean shift = (modifiers & ActionEvent.SHIFT_MASK) != 0;
        boolean alt = (modifiers & (ActionEvent.ALT_MASK|InputEvent.ALT_GRAPH_MASK)) != 0;

        DeleteParameters result = new DeleteParameters();

        result.nearestNode = Main.map.mapView.getNearestNode(e.getPoint());
        if (result.nearestNode == null) {
            result.nearestSegment = Main.map.mapView.getNearestWaySegment(e.getPoint());
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
     * @param int modifiers For explanation: @see updateCursor
     * @param silet Set to true if the user should not be bugged with additional
     *        dialogs
     * @return
     */
    private Command buildDeleteCommands(MouseEvent e, int modifiers, boolean silent) {
        DeleteParameters parameters = getDeleteParameters(e, modifiers);
        switch (parameters.mode) {
        case node:
            return DeleteCommand.delete(getEditLayer(),Collections.singleton(parameters.nearestNode), false, silent);
        case node_with_references:
            return DeleteCommand.deleteWithReferences(getEditLayer(),Collections.singleton(parameters.nearestNode));
        case segment:
            return DeleteCommand.deleteWaySegment(getEditLayer(), parameters.nearestSegment);
        case way:
            return DeleteCommand.delete(getEditLayer(), Collections.singleton(parameters.nearestSegment.way), false, silent);
        case way_with_nodes:
            return DeleteCommand.delete(getEditLayer(), Collections.singleton(parameters.nearestSegment.way), true, silent);
        case way_with_references:
            return DeleteCommand.deleteWithReferences(getEditLayer(),Collections.singleton(parameters.nearestSegment.way),true);
        default:
            return null;
        }
    }

    /**
     * This function sets the given cursor in a safe way. This implementation
     * differs from the on in DrawAction (it is favorable, too).
     * FIXME: Update DrawAction to use this "setCursor-style" and move function
     * to MapMode.
     * @param c
     */
    private void setCursor(final DeleteMode c) {
        if(currentMode.equals(c) || (!drawTargetCursor && currentMode.equals(DeleteMode.none)))
            return;
        try {
            // We invoke this to prevent strange things from happening
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                    // Don't change cursor when mode has changed already
                    if(!(Main.map.mapMode instanceof DeleteAction))
                        return;

                    Main.map.mapView.setCursor(c.cursor());
                    //System.out.println("Set cursor to: " + c.name());
                }
            });
            currentMode = c;
        } catch(Exception e) {}
    }

    /**
     * This is required to update the cursors when ctrl/shift/alt is pressed
     */
    public void eventDispatched(AWTEvent e) {
        // We don't have a mouse event, so we pass the old mouse event but the
        // new modifiers.
        updateCursor(oldEvent, ((InputEvent)e).getModifiers());
    }
}
