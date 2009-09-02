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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.DeleteCommand;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.WaySegment;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.dialogs.relation.RelationDialogManager;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
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

    private enum Cursors {
        none,
        node,
        segment,
        way_node_only,
        way_normal,
        way_only;

        private Cursor c = null;
        // This loads and caches the cursor for each
        public Cursor cursor() {
            if(c == null) {
                String nm = "delete_" + this.name().toLowerCase();
                // "None" has no special icon
                nm = nm.equals("delete_none") ? "delete" : nm;
                this.c = ImageProvider.getCursor("normal", nm);
            }
            return c;
        }
    }
    private Cursors currCursor = Cursors.none;

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
        } catch (SecurityException ex) {}

        currCursor = Cursors.none;
    }

    @Override public void exitMode() {
        super.exitMode();
        Main.map.mapView.removeMouseListener(this);
        Main.map.mapView.removeMouseMotionListener(this);
        try {
            Toolkit.getDefaultToolkit().removeAWTEventListener(this);
        } catch (SecurityException ex) {}
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
            c = DeleteCommand.delete(getEditLayer(),getCurrentDataSet().getSelected(), !alt);
        }
        if (c != null) {
            Main.main.undoRedo.add(c);
        }

        getCurrentDataSet().setSelected();
        Main.map.repaint();
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
     * @parm int modifiers
     */
    private void updateCursor(MouseEvent e, int modifiers) {
        if(!Main.map.mapView.isActiveLayerVisible() || e == null)
            return;

        // Clean old highlights
        //cleanOldHighlights();

        Command c = buildDeleteCommands(e, modifiers, true);
        if(c == null) {
            setCursor(Cursors.none);
            return;
        }

        Collection<OsmPrimitive> prims = new HashSet<OsmPrimitive>();
        Collection<OsmPrimitive> mods = new HashSet<OsmPrimitive>();
        c.fillModifiedData(mods, prims, prims);

        if(prims.size() == 0 && mods.size() == 0) {
            // Default-Cursor
            setCursor(Cursors.none);
            return;
        }

        // There are no deleted parts if solely a way segment is deleted
        // This is no the case when actually deleting only a segment but that
        // segment happens to be the whole way. This is an acceptable error
        // though
        if(prims.size() == 0) {
            setCursor(Cursors.segment);
        } else if(prims.size() == 1 && prims.toArray()[0] instanceof Node) {
            setCursor(Cursors.node);
        } else if(prims.size() == 1 && prims.toArray()[0] instanceof Way) {
            setCursor(Cursors.way_only);
        } else {
            // Decide between non-accel click where "useless" nodes are deleted
            // and ctrl-click where nodes and ways are deleted
            boolean ctrl = (modifiers & ActionEvent.CTRL_MASK) != 0;
            if(ctrl) {
                setCursor(Cursors.way_node_only);
            } else {
                setCursor(Cursors.way_normal);
            }

        }

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
    @Override public void mouseClicked(MouseEvent e) {
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
        return tr("Click to delete. Shift: delete way segment. Alt: don't delete unused nodes when deleting a way. Ctrl: delete referring objects.");
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
        if (layer == null)
            throw new IllegalArgumentException(tr("parameter ''{0}'' must not be null", "layer"));
        if (toDelete == null)
            throw new IllegalArgumentException(tr("parameter ''{0}'' must not be null", "toDelete"));

        Command cmd = DeleteCommand.delete(layer, Collections.singleton(toDelete));
        if (cmd != null) {
            // cmd can be null if the user cancels dialogs DialogCommand displays
            Main.main.undoRedo.add(cmd);
            RelationDialogManager.getRelationDialogManager().close(layer, toDelete);
            layer.fireDataChange();
        }
    }

    /**
     * This function takes any mouse event argument and builds the list of elements
     * that should be deleted but does not actually delete them.
     * @param e MouseEvent from which modifiers and position are taken
     * @param int modifiers For explanation: @see updateCursor
     * @param Simulate Set to true if the user should be bugged with additional
     *        dialogs
     * @return
     */
    private Command buildDeleteCommands(MouseEvent e, int modifiers, boolean simulate) {
        // Note: CTRL is the only modifier that is checked in MouseMove, don't
        // forget updating it there
        boolean ctrl = (modifiers & ActionEvent.CTRL_MASK) != 0;
        boolean shift = (modifiers & ActionEvent.SHIFT_MASK) != 0;
        boolean alt = (modifiers & (ActionEvent.ALT_MASK|InputEvent.ALT_GRAPH_MASK)) != 0;

        OsmPrimitive sel = Main.map.mapView.getNearestNode(e.getPoint());
        Command c = null;
        if (sel == null) {
            WaySegment ws = Main.map.mapView.getNearestWaySegment(e.getPoint());
            if (ws != null) {
                if (shift) {
                    c = DeleteCommand.deleteWaySegment(getEditLayer(),ws);
                } else if (ctrl) {
                    c = DeleteCommand.deleteWithReferences(getEditLayer(),Collections.singleton((OsmPrimitive)ws.way),true);
                } else {
                    c = DeleteCommand.delete(getEditLayer(),Collections.singleton((OsmPrimitive)ws.way), !alt, simulate);
                }
            }
        } else if (ctrl) {
            c = DeleteCommand.deleteWithReferences(getEditLayer(),Collections.singleton(sel));
        } else {
            c = DeleteCommand.delete(getEditLayer(),Collections.singleton(sel), !alt, simulate);
        }

        return c;
    }

    /**
     * This function sets the given cursor in a safe way. This implementation
     * differs from the on in DrawAction (it is favorable, too).
     * FIXME: Update DrawAction to use this "setCursor-style" and move function
     * to MapMode.
     * @param c
     */
    private void setCursor(final Cursors c) {
        if(currCursor.equals(c) || (!drawTargetCursor && currCursor.equals(Cursors.none)))
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
            currCursor = c;
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
