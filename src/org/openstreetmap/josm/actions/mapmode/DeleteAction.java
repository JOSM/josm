// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions.mapmode;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.Collections;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.DeleteCommand;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
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
public class DeleteAction extends MapMode {

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
        Main.map.mapView.addMouseListener(this);
    }

    @Override public void exitMode() {
        super.exitMode();
        Main.map.mapView.removeMouseListener(this);
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
     * If user clicked with the left button, delete the nearest object.
     * position.
     */
    @Override public void mouseClicked(MouseEvent e) {
        if (e.getButton() != MouseEvent.BUTTON1)
            return;
        if(!Main.map.mapView.isActiveLayerVisible())
            return;
        boolean ctrl = (e.getModifiers() & ActionEvent.CTRL_MASK) != 0;
        boolean shift = (e.getModifiers() & ActionEvent.SHIFT_MASK) != 0;
        boolean alt = (e.getModifiers() & (ActionEvent.ALT_MASK|InputEvent.ALT_GRAPH_MASK)) != 0;

        OsmPrimitive sel = Main.map.mapView.getNearestNode(e.getPoint());
        Command c = null;
        if (sel == null) {
            WaySegment ws = Main.map.mapView.getNearestWaySegment(e.getPoint());
            if (ws != null) {
                if (shift) {
                    c = DeleteCommand.deleteWaySegment(getEditLayer(),ws);
                } else if (ctrl) {
                    c = DeleteCommand.deleteWithReferences(getEditLayer(),Collections.singleton((OsmPrimitive)ws.way));
                } else {
                    c = DeleteCommand.delete(getEditLayer(),Collections.singleton((OsmPrimitive)ws.way), !alt);
                }
            }
        } else if (ctrl) {
            c = DeleteCommand.deleteWithReferences(getEditLayer(),Collections.singleton(sel));
        } else {
            c = DeleteCommand.delete(getEditLayer(),Collections.singleton(sel), !alt);
        }
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
        if (toDelete == null)
            return;
        Command cmd = DeleteCommand.delete(layer, Collections.singleton(toDelete));
        if (cmd != null) {
            // cmd can be null if the user cancels dialogs DialogCommand displays
            //
            Main.main.undoRedo.add(cmd);
            RelationDialogManager.getRelationDialogManager().close(layer, toDelete);
            layer.fireDataChange();
        }
    }
}
