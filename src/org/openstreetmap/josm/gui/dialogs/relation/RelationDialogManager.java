// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation;

import java.awt.Point;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;

import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerAddEvent;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerChangeListener;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerOrderChangeEvent;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerRemoveEvent;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

/**
 * RelationDialogManager keeps track of the open relation editors.
 *
 */
public class RelationDialogManager extends WindowAdapter implements LayerChangeListener {

    /** keeps track of open relation editors */
    private static RelationDialogManager relationDialogManager;

    /**
     * Replies the singleton {@link RelationDialogManager}
     *
     * @return the singleton {@link RelationDialogManager}
     */
    public static RelationDialogManager getRelationDialogManager() {
        if (RelationDialogManager.relationDialogManager == null) {
            RelationDialogManager.relationDialogManager = new RelationDialogManager();
            MainApplication.getLayerManager().addLayerChangeListener(RelationDialogManager.relationDialogManager);
        }
        return RelationDialogManager.relationDialogManager;
    }

    /**
     * Helper class for keeping the context of a relation editor. A relation editor
     * is open for a specific relation managed by a specific {@link OsmDataLayer}
     *
     */
    private static class DialogContext {
        public final Relation relation;
        public final OsmDataLayer layer;

        DialogContext(OsmDataLayer layer, Relation relation) {
            this.layer = layer;
            this.relation = relation;
        }

        @Override
        public int hashCode() {
            return Objects.hash(relation, layer);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            DialogContext that = (DialogContext) obj;
            return Objects.equals(relation, that.relation) &&
                    Objects.equals(layer, that.layer);
        }

        public boolean matchesLayer(OsmDataLayer layer) {
            if (layer == null) return false;
            return this.layer.equals(layer);
        }

        @Override
        public String toString() {
            return "[Context: layer=" + layer.getName() + ",relation=" + relation.getId() + ']';
        }
    }

    /** the map of open dialogs */
    private final Map<DialogContext, RelationEditor> openDialogs;

    /**
     * constructor
     */
    public RelationDialogManager() {
        openDialogs = new HashMap<>();
    }

    /**
     * Register the relation editor for a relation managed by a {@link OsmDataLayer}.
     *
     * @param layer the layer
     * @param relation the relation
     * @param editor the editor
     */
    public void register(OsmDataLayer layer, Relation relation, RelationEditor editor) {
        openDialogs.put(new DialogContext(layer, Optional.ofNullable(relation).orElseGet(Relation::new)), editor);
        editor.addWindowListener(this);
    }

    public void updateContext(OsmDataLayer layer, Relation relation, RelationEditor editor) {
        // lookup the entry for editor and remove it
        for (Iterator<Entry<DialogContext, RelationEditor>> it = openDialogs.entrySet().iterator(); it.hasNext();) {
            Entry<DialogContext, RelationEditor> entry = it.next();
            if (Objects.equals(entry.getValue(), editor)) {
                it.remove();
                break;
            }
        }
        // don't add a window listener. Editor is already known to the relation dialog manager
        openDialogs.put(new DialogContext(layer, relation), editor);
    }

    /**
     * Closes the editor open for a specific layer and a specific relation.
     *
     * @param layer  the layer
     * @param relation the relation
     */
    public void close(OsmDataLayer layer, Relation relation) {
        DialogContext context = new DialogContext(layer, relation);
        RelationEditor editor = openDialogs.get(context);
        if (editor != null) {
            editor.setVisible(false);
        }
    }

    /**
     * Replies true if there is an open relation editor for the relation managed
     * by the given layer. Replies false if relation is null.
     *
     * @param layer  the layer
     * @param relation  the relation. May be null.
     * @return true if there is an open relation editor for the relation managed
     * by the given layer; false otherwise
     */
    public boolean isOpenInEditor(OsmDataLayer layer, Relation relation) {
        if (relation == null) return false;
        DialogContext context = new DialogContext(layer, relation);
        return openDialogs.containsKey(context);
    }

    /**
     * Replies the editor for the relation managed by layer. Null, if no such editor
     * is currently open. Returns null, if relation is null.
     *
     * @param layer the layer
     * @param relation the relation
     * @return the editor for the relation managed by layer. Null, if no such editor
     * is currently open.
     *
     * @see #isOpenInEditor(OsmDataLayer, Relation)
     */
    public RelationEditor getEditorForRelation(OsmDataLayer layer, Relation relation) {
        if (relation == null) return null;
        DialogContext context = new DialogContext(layer, relation);
        return openDialogs.get(context);
    }

    @Override
    public void layerRemoving(LayerRemoveEvent e) {
        Layer oldLayer = e.getRemovedLayer();
        if (!(oldLayer instanceof OsmDataLayer))
            return;
        OsmDataLayer dataLayer = (OsmDataLayer) oldLayer;

        Iterator<Entry<DialogContext, RelationEditor>> it = openDialogs.entrySet().iterator();
        while (it.hasNext()) {
            Entry<DialogContext, RelationEditor> entry = it.next();
            if (entry.getKey().matchesLayer(dataLayer)) {
                RelationEditor editor = entry.getValue();
                it.remove();
                editor.setVisible(false);
                editor.dispose();
            }
        }
    }

    @Override
    public void layerAdded(LayerAddEvent e) {
        // ignore
    }

    @Override
    public void layerOrderChanged(LayerOrderChangeEvent e) {
        // ignore
    }

    @Override
    public void windowClosed(WindowEvent e) {
        Window w = e.getWindow();
        if (w instanceof RelationEditor) {
            RelationEditor editor = (RelationEditor) w;
            for (Iterator<Entry<DialogContext, RelationEditor>> it = openDialogs.entrySet().iterator(); it.hasNext();) {
                if (editor.equals(it.next().getValue())) {
                    it.remove();
                    break;
                }
            }
        }
    }

    /**
     * Replies true, if there is another open {@link RelationEditor} whose
     * upper left corner is close to <code>p</code>.
     *
     * @param p the reference point to check
     * @param thisEditor the current editor
     * @return true, if there is another open {@link RelationEditor} whose
     * upper left corner is close to <code>p</code>.
     */
    protected boolean hasEditorWithCloseUpperLeftCorner(Point p, RelationEditor thisEditor) {
        for (RelationEditor editor: openDialogs.values()) {
            if (editor == thisEditor) {
                continue;
            }
            Point corner = editor.getLocation();
            if (p.x >= corner.x -5 && corner.x + 5 >= p.x
                    && p.y >= corner.y -5 && corner.y + 5 >= p.y)
                return true;
        }
        return false;
    }

    /**
     * Positions a {@link RelationEditor} on the screen. Tries to center it on the
     * screen. If it hide another instance of an editor at the same position this
     * method tries to reposition <code>editor</code> by moving it slightly down and
     * slightly to the right.
     *
     * @param editor the editor
     */
    public void positionOnScreen(RelationEditor editor) {
        if (editor == null) return;
        if (!openDialogs.isEmpty()) {
            Point corner = editor.getLocation();
            while (hasEditorWithCloseUpperLeftCorner(corner, editor)) {
                // shift a little, so that the dialogs are not exactly on top of each other
                corner.x += 20;
                corner.y += 20;
            }
            editor.setLocation(corner);
        }
    }

}
