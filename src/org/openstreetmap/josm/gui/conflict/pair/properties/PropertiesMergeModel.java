// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.pair.properties;

import static org.openstreetmap.josm.gui.conflict.pair.MergeDecisionType.UNDECIDED;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.conflict.CoordinateConflictResolveCommand;
import org.openstreetmap.josm.command.conflict.DeletedStateConflictResolveCommand;
import org.openstreetmap.josm.data.conflict.Conflict;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.conflict.pair.MergeDecisionType;
import org.openstreetmap.josm.gui.util.ChangeNotifier;
import org.openstreetmap.josm.tools.CheckParameterUtil;

/**
 * This is the model for resolving conflicts in the properties of the
 * {@link OsmPrimitive}s. In particular, it represents conflicts in the coordinates of {@link Node}s and
 * the deleted or visible state of {@link OsmPrimitive}s.
 *
 * This model is a {@link ChangeNotifier}. It notifies registered {@link javax.swing.event.ChangeListener}s whenever the
 * internal state changes.
 *
 * This model also emits property changes for {@link #RESOLVED_COMPLETELY_PROP}. Property change
 * listeners may register themselves using {@link #addPropertyChangeListener(PropertyChangeListener)}.
 *
 * @see Node#getCoor()
 * @see OsmPrimitive#isDeleted
 * @see OsmPrimitive#isVisible
 *
 */
public class PropertiesMergeModel extends ChangeNotifier {

    public static final String RESOLVED_COMPLETELY_PROP = PropertiesMergeModel.class.getName() + ".resolvedCompletely";
    public static final String DELETE_PRIMITIVE_PROP = PropertiesMergeModel.class.getName() + ".deletePrimitive";

    private OsmPrimitive my;

    private LatLon myCoords;
    private LatLon theirCoords;
    private MergeDecisionType coordMergeDecision;

    private boolean myDeletedState;
    private boolean theirDeletedState;
    private List<OsmPrimitive> myReferrers;
    private List<OsmPrimitive> theirReferrers;
    private MergeDecisionType deletedMergeDecision;
    private final PropertyChangeSupport support;
    private Boolean resolvedCompletely;

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        support.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        support.removePropertyChangeListener(listener);
    }

    public void fireCompletelyResolved() {
        Boolean oldValue = resolvedCompletely;
        resolvedCompletely = isResolvedCompletely();
        support.firePropertyChange(RESOLVED_COMPLETELY_PROP, oldValue, resolvedCompletely);
    }

    /**
     * Constructs a new {@code PropertiesMergeModel}.
     */
    public PropertiesMergeModel() {
        coordMergeDecision = UNDECIDED;
        deletedMergeDecision = UNDECIDED;
        support = new PropertyChangeSupport(this);
        resolvedCompletely = null;
    }

    /**
     * replies true if there is a coordinate conflict and if this conflict is resolved
     *
     * @return true if there is a coordinate conflict and if this conflict is resolved; false, otherwise
     */
    public boolean isDecidedCoord() {
        return coordMergeDecision != UNDECIDED;
    }

    /**
     * replies true if there is a  conflict in the deleted state and if this conflict is resolved
     *
     * @return true if there is a conflict in the deleted state and if this conflict is
     * resolved; false, otherwise
     */
    public boolean isDecidedDeletedState() {
        return deletedMergeDecision != UNDECIDED;
    }

    /**
     * replies true if the current decision for the coordinate conflict is <code>decision</code>
     * @param decision conflict resolution decision
     *
     * @return true if the current decision for the coordinate conflict is <code>decision</code>;
     *  false, otherwise
     */
    public boolean isCoordMergeDecision(MergeDecisionType decision) {
        return coordMergeDecision == decision;
    }

    /**
     * replies true if the current decision for the deleted state conflict is <code>decision</code>
     * @param decision conflict resolution decision
     *
     * @return true if the current decision for the deleted state conflict is <code>decision</code>;
     *  false, otherwise
     */
    public boolean isDeletedStateDecision(MergeDecisionType decision) {
        return deletedMergeDecision == decision;
    }

    /**
     * Populates the model with the differences between local and server version
     *
     * @param conflict The conflict information
     */
    public void populate(Conflict<? extends OsmPrimitive> conflict) {
        this.my = conflict.getMy();
        OsmPrimitive their = conflict.getTheir();
        if (my instanceof Node) {
            myCoords = ((Node) my).getCoor();
            theirCoords = ((Node) their).getCoor();
        } else {
            myCoords = null;
            theirCoords = null;
        }

        myDeletedState = conflict.isMyDeleted() || my.isDeleted();
        theirDeletedState = their.isDeleted();

        myReferrers = my.getDataSet() == null ? Collections.<OsmPrimitive>emptyList() : my.getReferrers();
        theirReferrers = their.getDataSet() == null ? Collections.<OsmPrimitive>emptyList() : their.getReferrers();

        coordMergeDecision = UNDECIDED;
        deletedMergeDecision = UNDECIDED;
        fireStateChanged();
        fireCompletelyResolved();
    }

    /**
     * replies the coordinates of my {@link OsmPrimitive}. null, if my primitive hasn't
     * coordinates (i.e. because it is a {@link org.openstreetmap.josm.data.osm.Way}).
     *
     * @return the coordinates of my {@link OsmPrimitive}. null, if my primitive hasn't
     *  coordinates (i.e. because it is a {@link org.openstreetmap.josm.data.osm.Way}).
     */
    public LatLon getMyCoords() {
        return myCoords;
    }

    /**
     * replies the coordinates of their {@link OsmPrimitive}. null, if their primitive hasn't
     * coordinates (i.e. because it is a {@link org.openstreetmap.josm.data.osm.Way}).
     *
     * @return the coordinates of my {@link OsmPrimitive}. null, if my primitive hasn't
     * coordinates (i.e. because it is a {@link org.openstreetmap.josm.data.osm.Way}).
     */
    public LatLon getTheirCoords() {
        return theirCoords;
    }

    /**
     * replies the coordinates of the merged {@link OsmPrimitive}. null, if the current primitives
     * have no coordinates or if the conflict is yet {@link MergeDecisionType#UNDECIDED}
     *
     * @return the coordinates of the merged {@link OsmPrimitive}. null, if the current primitives
     * have no coordinates or if the conflict is yet {@link MergeDecisionType#UNDECIDED}
     */
    public LatLon getMergedCoords() {
        switch(coordMergeDecision) {
        case KEEP_MINE: return myCoords;
        case KEEP_THEIR: return theirCoords;
        case UNDECIDED: return null;
        }
        // should not happen
        return null;
    }

    /**
     * Decides a conflict between local and server coordinates
     *
     * @param decision the decision
     */
    public void decideCoordsConflict(MergeDecisionType decision) {
        coordMergeDecision = decision;
        fireStateChanged();
        fireCompletelyResolved();
    }

    /**
     * Replies deleted state of local dataset
     * @return The state of deleted flag
     */
    public Boolean getMyDeletedState() {
        return myDeletedState;
    }

    /**
     * Replies deleted state of Server dataset
     * @return The state of deleted flag
     */
    public Boolean getTheirDeletedState() {
        return theirDeletedState;
    }

    /**
     * Replies deleted state of combined dataset
     * @return The state of deleted flag
     */
    public Boolean getMergedDeletedState() {
        switch(deletedMergeDecision) {
        case KEEP_MINE: return myDeletedState;
        case KEEP_THEIR: return theirDeletedState;
        case UNDECIDED: return null;
        }
        // should not happen
        return null;
    }

    /**
     * Returns local referrers
     * @return The referrers
     */
    public List<OsmPrimitive> getMyReferrers() {
        return myReferrers;
    }

    /**
     * Returns server referrers
     * @return The referrers
     */
    public List<OsmPrimitive> getTheirReferrers() {
        return theirReferrers;
    }

    private boolean getMergedDeletedState(MergeDecisionType decision) {
        switch (decision) {
        case KEEP_MINE:
            return myDeletedState;
        case KEEP_THEIR:
            return theirDeletedState;
        default:
            return false;
        }
    }

    /**
     * decides the conflict between two deleted states
     * @param decision the decision (must not be null)
     *
     * @throws IllegalArgumentException if decision is null
     */
    public void decideDeletedStateConflict(MergeDecisionType decision) {
        CheckParameterUtil.ensureParameterNotNull(decision, "decision");

        boolean oldMergedDeletedState = getMergedDeletedState(this.deletedMergeDecision);
        boolean newMergedDeletedState = getMergedDeletedState(decision);

        this.deletedMergeDecision = decision;
        fireStateChanged();
        fireCompletelyResolved();

        if (oldMergedDeletedState != newMergedDeletedState) {
            support.firePropertyChange(DELETE_PRIMITIVE_PROP, oldMergedDeletedState, newMergedDeletedState);
        }
    }

    /**
     * replies true if my and their primitive have a conflict between
     * their coordinate values
     *
     * @return true if my and their primitive have a conflict between
     * their coordinate values; false otherwise
     */
    public boolean hasCoordConflict() {
        if (myCoords == null && theirCoords != null) return true;
        if (myCoords != null && theirCoords == null) return true;
        if (myCoords == null && theirCoords == null) return false;
        return myCoords != null && !myCoords.equalsEpsilon(theirCoords);
    }

    /**
     * replies true if my and their primitive have a conflict between
     * their deleted states
     *
     * @return <code>true</code> if my and their primitive have a conflict between
     * their deleted states
     */
    public boolean hasDeletedStateConflict() {
        return myDeletedState != theirDeletedState;
    }

    /**
     * replies true if all conflict in this model are resolved
     *
     * @return <code>true</code> if all conflict in this model are resolved; <code>false</code> otherwise
     */
    public boolean isResolvedCompletely() {
        boolean ret = true;
        if (hasCoordConflict()) {
            ret = ret && coordMergeDecision != UNDECIDED;
        }
        if (hasDeletedStateConflict()) {
            ret = ret && deletedMergeDecision != UNDECIDED;
        }
        return ret;
    }

    /**
     * Builds the command(s) to apply the conflict resolutions to my primitive
     *
     * @param conflict The conflict information
     * @return The list of commands
     */
    public List<Command> buildResolveCommand(Conflict<? extends OsmPrimitive> conflict) {
        List<Command> cmds = new ArrayList<>();
        if (hasCoordConflict() && isDecidedCoord()) {
            cmds.add(new CoordinateConflictResolveCommand(conflict, coordMergeDecision));
        }
        if (hasDeletedStateConflict() && isDecidedDeletedState()) {
            cmds.add(new DeletedStateConflictResolveCommand(conflict, deletedMergeDecision));
        }
        return cmds;
    }

    public OsmPrimitive getMyPrimitive() {
        return my;
    }

}
