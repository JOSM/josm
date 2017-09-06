// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.command;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.PrimitiveData;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.AbstractVisitor;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.tools.CheckParameterUtil;

/**
 * Classes implementing Command modify a dataset in a specific way. A command is
 * one atomic action on a specific dataset, such as move or delete.
 *
 * The command remembers the {@link DataSet} it is operating on.
 *
 * @author imi
 * @since 21 (creation)
 * @since 10599 (signature)
 */
public abstract class Command implements PseudoCommand {

    /** IS_OK : operation is okay */
    public static final int IS_OK = 0;
    /** IS_OUTSIDE : operation on element outside of download area */
    public static final int IS_OUTSIDE = 1;
    /** IS_INCOMPLETE: operation on incomplete target */
    public static final int IS_INCOMPLETE = 2;

    private static final class CloneVisitor extends AbstractVisitor {
        public final Map<OsmPrimitive, PrimitiveData> orig = new LinkedHashMap<>();

        @Override
        public void visit(Node n) {
            orig.put(n, n.save());
        }

        @Override
        public void visit(Way w) {
            orig.put(w, w.save());
        }

        @Override
        public void visit(Relation e) {
            orig.put(e, e.save());
        }
    }

    /**
     * Small helper for holding the interesting part of the old data state of the objects.
     */
    public static class OldNodeState {

        private final LatLon latLon;
        private final EastNorth eastNorth; // cached EastNorth to be used for applying exact displacement
        private final boolean modified;

        /**
         * Constructs a new {@code OldNodeState} for the given node.
         * @param node The node whose state has to be remembered
         */
        public OldNodeState(Node node) {
            latLon = node.getCoor();
            eastNorth = node.getEastNorth();
            modified = node.isModified();
        }

        /**
         * Returns old lat/lon.
         * @return old lat/lon
         * @see Node#getCoor()
         * @since 10248
         */
        public final LatLon getLatLon() {
            return latLon;
        }

        /**
         * Returns old east/north.
         * @return old east/north
         * @see Node#getEastNorth()
         */
        public final EastNorth getEastNorth() {
            return eastNorth;
        }

        /**
         * Returns old modified state.
         * @return old modified state
         * @see Node #isModified()
         */
        public final boolean isModified() {
            return modified;
        }

        @Override
        public int hashCode() {
            return Objects.hash(latLon, eastNorth, modified);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            OldNodeState that = (OldNodeState) obj;
            return modified == that.modified &&
                    Objects.equals(latLon, that.latLon) &&
                    Objects.equals(eastNorth, that.eastNorth);
        }
    }

    /** the map of OsmPrimitives in the original state to OsmPrimitives in cloned state */
    private Map<OsmPrimitive, PrimitiveData> cloneMap = new HashMap<>();

    /**
     * the layer which this command is applied to
     * @deprecated to be removed end of 2017. Use {@link #data} instead
     */
    @Deprecated
    private final OsmDataLayer layer;

    /** the dataset which this command is applied to */
    private final DataSet data;

    /**
     * Creates a new command in the context of the current edit layer, if any
     * @deprecated to be removed end of 2017. Use {@link #Command(DataSet)} instead
     */
    @Deprecated
    public Command() {
        this.layer = MainApplication.getLayerManager().getEditLayer();
        this.data = layer != null ? layer.data : Main.main.getEditDataSet();
    }

    /**
     * Creates a new command in the context of a specific data layer
     *
     * @param layer the data layer. Must not be null.
     * @throws IllegalArgumentException if layer is null
     * @deprecated to be removed end of 2017. Use {@link #Command(DataSet)} instead
     */
    @Deprecated
    public Command(OsmDataLayer layer) {
        CheckParameterUtil.ensureParameterNotNull(layer, "layer");
        this.layer = layer;
        this.data = layer.data;
    }

    /**
     * Creates a new command in the context of a specific data set, without data layer
     *
     * @param data the data set. Must not be null.
     * @throws IllegalArgumentException if data is null
     * @since 11240
     */
    public Command(DataSet data) {
        CheckParameterUtil.ensureParameterNotNull(data, "data");
        this.layer = null;
        this.data = data;
    }

    /**
     * Executes the command on the dataset. This implementation will remember all
     * primitives returned by fillModifiedData for restoring them on undo.
     * <p>
     * The layer should be invalidated after execution so that it can be re-painted.
     * @return true
     * @see #invalidateAffectedLayers()
     */
    public boolean executeCommand() {
        CloneVisitor visitor = new CloneVisitor();
        Collection<OsmPrimitive> all = new ArrayList<>();
        fillModifiedData(all, all, all);
        for (OsmPrimitive osm : all) {
            osm.accept(visitor);
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
     * @param oldLayer the old layer that was removed
     * @return true if this command is invalid after that layer is removed.
     * @deprecated to be removed end of 2017.
     */
    @Deprecated
    public boolean invalidBecauselayerRemoved(Layer oldLayer) {
        return layer == oldLayer;
    }

    /**
     * Lets other commands access the original version
     * of the object. Usually for undoing.
     * @param osm The requested OSM object
     * @return The original version of the requested object, if any
     */
    public PrimitiveData getOrig(OsmPrimitive osm) {
        return cloneMap.get(osm);
    }

    /**
     * Replies the layer this command is (or was) applied to.
     * @return the layer this command is (or was) applied to
     * @deprecated to be removed end of 2017. Use {@link #getAffectedDataSet} instead
     */
    @Deprecated
    protected OsmDataLayer getLayer() {
        return layer;
    }

    /**
     * Gets the data set this command affects.
     * @return The data set. May be <code>null</code> if no layer was set and no edit layer was found.
     * @since 10467
     */
    public DataSet getAffectedDataSet() {
        return data;
    }

    /**
     * Fill in the changed data this command operates on.
     * Add to the lists, don't clear them.
     *
     * @param modified The modified primitives
     * @param deleted The deleted primitives
     * @param added The added primitives
     */
    public abstract void fillModifiedData(Collection<OsmPrimitive> modified,
            Collection<OsmPrimitive> deleted,
            Collection<OsmPrimitive> added);

    /**
     * Return the primitives that take part in this command.
     * The collection is computed during execution.
     */
    @Override
    public Collection<? extends OsmPrimitive> getParticipatingPrimitives() {
        return cloneMap.keySet();
    }

    /**
     * Check whether user is about to operate on data outside of the download area.
     *
     * @param primitives the primitives to operate on
     * @param ignore {@code null} or a primitive to be ignored
     * @return true, if operating on outlying primitives is OK; false, otherwise
     */
    public static int checkOutlyingOrIncompleteOperation(
            Collection<? extends OsmPrimitive> primitives,
            Collection<? extends OsmPrimitive> ignore) {
        int res = 0;
        for (OsmPrimitive osm : primitives) {
            if (osm.isIncomplete()) {
                res |= IS_INCOMPLETE;
            } else if (osm.isOutsideDownloadArea()
                    && (ignore == null || !ignore.contains(osm))) {
                res |= IS_OUTSIDE;
            }
        }
        return res;
    }

    /**
     * Ensures that all primitives that are participating in this command belong to the affected data set.
     *
     * Commands may use this in their update methods to check the consitency of the primitives they operate on.
     * @throws AssertionError if no {@link DataSet} is set or if any primitive does not belong to that dataset.
     */
    protected void ensurePrimitivesAreInDataset() {
        for (OsmPrimitive primitive : this.getParticipatingPrimitives()) {
            if (primitive.getDataSet() != this.getAffectedDataSet()) {
                throw new AssertionError("Primitive is of wrong data set for this command: " + primitive);
            }
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(cloneMap, layer, data);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Command command = (Command) obj;
        return Objects.equals(cloneMap, command.cloneMap) &&
               Objects.equals(layer, command.layer) &&
               Objects.equals(data, command.data);
    }

    /**
     * Invalidate all layers that were affected by this command.
     * @see Layer#invalidate()
     * @deprecated to be removed end of 2017.
     */
    @Deprecated
    public void invalidateAffectedLayers() {
        OsmDataLayer layer = getLayer();
        if (layer != null) {
            layer.invalidate();
        }
    }
}
