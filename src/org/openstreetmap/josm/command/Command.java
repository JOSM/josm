// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.command;

import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.PrimitiveData;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.AbstractVisitor;
import org.openstreetmap.josm.gui.ConditionalOptionPaneUtil;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.widgets.JMultilineLabel;
import org.openstreetmap.josm.tools.CheckParameterUtil;

/**
 * Classes implementing Command modify a dataset in a specific way. A command is
 * one atomic action on a specific dataset, such as move or delete.
 *
 * The command remembers the {@link OsmDataLayer} it is operating on.
 *
 * @author imi
 */
public abstract class Command extends PseudoCommand {

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

        private final LatLon latlon;
        private final EastNorth eastNorth; // cached EastNorth to be used for applying exact displacement
        private final boolean modified;

        /**
         * Constructs a new {@code OldNodeState} for the given node.
         * @param node The node whose state has to be remembered
         */
        public OldNodeState(Node node) {
            latlon = node.getCoor();
            eastNorth = node.getEastNorth();
            modified = node.isModified();
        }

        /**
         * Returns old lat/lon.
         * @return old lat/lon
         * @see Node#getCoor()
         */
        public final LatLon getLatlon() {
            return latlon;
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
            final int prime = 31;
            int result = 1;
            result = prime * result + ((eastNorth == null) ? 0 : eastNorth.hashCode());
            result = prime * result + ((latlon == null) ? 0 : latlon.hashCode());
            result = prime * result + (modified ? 1231 : 1237);
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            OldNodeState other = (OldNodeState) obj;
            if (eastNorth == null) {
                if (other.eastNorth != null)
                    return false;
            } else if (!eastNorth.equals(other.eastNorth))
                return false;
            if (latlon == null) {
                if (other.latlon != null)
                    return false;
            } else if (!latlon.equals(other.latlon))
                return false;
            if (modified != other.modified)
                return false;
            return true;
        }
    }

    /** the map of OsmPrimitives in the original state to OsmPrimitives in cloned state */
    private Map<OsmPrimitive, PrimitiveData> cloneMap = new HashMap<>();

    /** the layer which this command is applied to */
    private final OsmDataLayer layer;

    /**
     * Creates a new command in the context of the current edit layer, if any
     */
    public Command() {
        this.layer = Main.main == null ? null : Main.main.getEditLayer();
    }

    /**
     * Creates a new command in the context of a specific data layer
     *
     * @param layer the data layer. Must not be null.
     * @throws IllegalArgumentException if layer is null
     */
    public Command(OsmDataLayer layer) {
        CheckParameterUtil.ensureParameterNotNull(layer, "layer");
        this.layer = layer;
    }

    /**
     * Executes the command on the dataset. This implementation will remember all
     * primitives returned by fillModifiedData for restoring them on undo.
     * @return true
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
     * @param osm The requested OSM object
     * @return The original version of the requested object, if any
     */
    public PrimitiveData getOrig(OsmPrimitive osm) {
        return cloneMap.get(osm);
    }

    /**
     * Replies the layer this command is (or was) applied to.
     *
     */
    protected OsmDataLayer getLayer() {
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
    public abstract void fillModifiedData(Collection<OsmPrimitive> modified,
            Collection<OsmPrimitive> deleted,
            Collection<OsmPrimitive> added);

    /**
     * Return the primitives that take part in this command.
     */
    @Override public Collection<? extends OsmPrimitive> getParticipatingPrimitives() {
        return cloneMap.keySet();
    }

    /**
     * Check whether user is about to operate on data outside of the download area.
     * Request confirmation if he is.
     *
     * @param operation the operation name which is used for setting some preferences
     * @param dialogTitle the title of the dialog being displayed
     * @param outsideDialogMessage the message text to be displayed when data is outside of the download area
     * @param incompleteDialogMessage the message text to be displayed when data is incomplete
     * @param primitives the primitives to operate on
     * @param ignore {@code null} or a primitive to be ignored
     * @return true, if operating on outlying primitives is OK; false, otherwise
     */
    public static boolean checkAndConfirmOutlyingOperation(String operation,
            String dialogTitle, String outsideDialogMessage, String incompleteDialogMessage,
            Collection<? extends OsmPrimitive> primitives,
            Collection<? extends OsmPrimitive> ignore) {
        boolean outside = false;
        boolean incomplete = false;
        for (OsmPrimitive osm : primitives) {
            if (osm.isIncomplete()) {
                incomplete = true;
            } else if (osm.isOutsideDownloadArea()
                    && (ignore == null || !ignore.contains(osm))) {
                outside = true;
            }
        }
        if (outside) {
            JPanel msg = new JPanel(new GridBagLayout());
            msg.add(new JMultilineLabel("<html>" + outsideDialogMessage + "</html>"));
            boolean answer = ConditionalOptionPaneUtil.showConfirmationDialog(
                    operation + "_outside_nodes",
                    Main.parent,
                    msg,
                    dialogTitle,
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    JOptionPane.YES_OPTION);
            if (!answer)
                return false;
        }
        if (incomplete) {
            JPanel msg = new JPanel(new GridBagLayout());
            msg.add(new JMultilineLabel("<html>" + incompleteDialogMessage + "</html>"));
            boolean answer = ConditionalOptionPaneUtil.showConfirmationDialog(
                    operation + "_incomplete",
                    Main.parent,
                    msg,
                    dialogTitle,
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    JOptionPane.YES_OPTION);
            if (!answer)
                return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((cloneMap == null) ? 0 : cloneMap.hashCode());
        result = prime * result + ((layer == null) ? 0 : layer.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Command other = (Command) obj;
        if (cloneMap == null) {
            if (other.cloneMap != null)
                return false;
        } else if (!cloneMap.equals(other.cloneMap))
            return false;
        if (layer == null) {
            if (other.layer != null)
                return false;
        } else if (!layer.equals(other.layer))
            return false;
        return true;
    }
}
