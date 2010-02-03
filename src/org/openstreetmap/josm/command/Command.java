//License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.command;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.tree.MutableTreeNode;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.PrimitiveData;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.AbstractVisitor;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.tools.CheckParameterUtil;

/**
 * Classes implementing Command modify a dataset in a specific way. A command is
 * one atomic action on a specific dataset, such as move or delete.
 *
 * The command remembers the {@see OsmDataLayer} it is operating on.
 *
 * @author imi
 */
abstract public class Command {

    private static final class CloneVisitor extends AbstractVisitor {
        public Map<OsmPrimitive, PrimitiveData> orig = new HashMap<OsmPrimitive, PrimitiveData>();

        public void visit(Node n) {
            orig.put(n, n.save());
        }
        public void visit(Way w) {
            orig.put(w, w.save());
        }
        public void visit(Relation e) {
            orig.put(e, e.save());
        }
    }

    /** the map of OsmPrimitives in the original state to OsmPrimitives in cloned state */
    private Map<OsmPrimitive, PrimitiveData> cloneMap = new HashMap<OsmPrimitive, PrimitiveData>();

    /** the layer which this command is applied to */
    private OsmDataLayer layer;

    public Command() {
        this.layer = Main.map.mapView.getEditLayer();
    }

    /**
     * Creates a new command in the context of a specific data layer
     *
     * @param layer the data layer. Must not be null.
     * @throws IllegalArgumentException thrown if layer is null
     */
    public Command(OsmDataLayer layer) throws IllegalArgumentException {
        CheckParameterUtil.ensureParameterNotNull(layer, "layer");
        this.layer = layer;
    }

    /**
     * Executes the command on the dataset. This implementation will remember all
     * primitives returned by fillModifiedData for restoring them on undo.
     */
    public boolean executeCommand() {
        CloneVisitor visitor = new CloneVisitor();
        Collection<OsmPrimitive> all = new HashSet<OsmPrimitive>();
        fillModifiedData(all, all, all);
        for (OsmPrimitive osm : all) {
            osm.visit(visitor);
        }
        cloneMap = visitor.orig;
        return true;
    }

    /**
     * Undoes the command.
     * It can be assumed that all objects are in the same state they were before.
     * It can also be assumed that executeCommand was called exactly once before.
     *
     * This implementation undoes all objects stored by a former call to executeCommand.
     */
    public void undoCommand() {
        for (Entry<OsmPrimitive, PrimitiveData> e : cloneMap.entrySet()) {
            OsmPrimitive primitive = e.getKey();
            if (primitive.getDataSet() != null) {
                e.getKey().load(e.getValue());
            }
        }
    }

    /**
     * Called when a layer has been removed to have the command remove itself from
     * any buffer if it is not longer applicable to the dataset (e.g. it was part of
     * the removed layer)
     *
     * @param oldLayer the old layer
     * @return true if this command
     */
    public boolean invalidBecauselayerRemoved(Layer oldLayer) {
        if (!(oldLayer instanceof OsmDataLayer))
            return false;
        return layer == oldLayer;
    }

    /**
     * Lets other commands access the original version
     * of the object. Usually for undoing.
     */
    public PrimitiveData getOrig(OsmPrimitive osm) {
        PrimitiveData o = cloneMap.get(osm);
        if (o != null)
            return o;
        Main.debug("unable to find osm with id: " + osm.getId() + " hashCode: " + osm.hashCode());
        for (OsmPrimitive t : cloneMap.keySet()) {
            PrimitiveData to = cloneMap.get(t);
            Main.debug("now: " + t.getId() + " hashCode: " + t.hashCode());
            Main.debug("orig: " + to.getUniqueId() + " hashCode: " + to.hashCode());
        }
        return o;
    }

    /**
     * Replies the layer this command is (or was) applied to.
     *
     * @return
     */
    protected  OsmDataLayer getLayer() {
        return layer;
    }

    /**
     * Fill in the changed data this command operates on.
     * Add to the lists, don't clear them.
     *
     * @param modified The modified primitives
     * @param deleted The deleted primitives
     * @param added The added primitives
     */
    abstract public void fillModifiedData(Collection<OsmPrimitive> modified,
            Collection<OsmPrimitive> deleted,
            Collection<OsmPrimitive> added);

    abstract public MutableTreeNode description();

}
