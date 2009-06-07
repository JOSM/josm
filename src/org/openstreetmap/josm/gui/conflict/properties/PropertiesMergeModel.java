// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.properties;

import static org.openstreetmap.josm.gui.conflict.MergeDecisionType.UNDECIDED;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;

import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.CoordinateConflictResolveCommand;
import org.openstreetmap.josm.command.DeletedStateConflictResolveCommand;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.conflict.MergeDecisionType;

/**
 * This is the model for resolving conflicts in the values of same properties of
 * {@see OsmPrimitive}. In particular, it represents conflicts in the coordiates of {@see Node}s and
 * the deleted state of {@see OsmPrimitive}s.
 * 
 * This model is an {@see Observable}. It notifies registered {@see Observer}s whenever the
 * internal state changes.
 * 
 * This model also emits property changes for {@see #RESOLVED_COMPLETELY_PROP}. Property change
 * listeners may register themselves using {@see #addPropertyChangeListener(PropertyChangeListener)}.
 * 
 * @see Node#getCoor()
 * @see OsmPrimitive#deleted
 *
 */
public class PropertiesMergeModel extends Observable {

    static public final String RESOLVED_COMPLETELY_PROP = PropertiesMergeModel.class.getName() + ".resolvedCompletely";

    private LatLon myCoords;
    private LatLon theirCoords;
    private MergeDecisionType coordMergeDecision;

    private boolean myDeletedState;
    private boolean theirDeletedState;
    private MergeDecisionType deletedMergeDecision;
    private final PropertyChangeSupport support;
    private boolean resolvedCompletely;

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        support.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        support.removePropertyChangeListener(listener);
    }

    public void fireCompletelyResolved() {
        boolean oldValue = resolvedCompletely;
        resolvedCompletely = isResolvedCompletely();
        support.firePropertyChange(RESOLVED_COMPLETELY_PROP, oldValue, resolvedCompletely);
    }

    public PropertiesMergeModel() {
        coordMergeDecision = UNDECIDED;
        deletedMergeDecision = UNDECIDED;
        support = new PropertyChangeSupport(this);
        resolvedCompletely = false;
    }

    /**
     * replies true if there is a coordiate conflict and if this conflict is
     * resolved
     * 
     * @return true if there is a coordiate conflict and if this conflict is
     * resolved; false, otherwise
     */
    public boolean isDecidedCoord() {
        return ! coordMergeDecision.equals(UNDECIDED);
    }

    /**
     * replies true if there is a  conflict in the deleted state and if this conflict is
     * resolved
     * 
     * @return true if there is a conflict in the deleted state and if this conflict is
     * resolved; false, otherwise
     */
    public boolean isDecidedDeletedState() {
        return ! deletedMergeDecision.equals(UNDECIDED);
    }

    /**
     * replies true if the current decision for the coordiate conflict is <code>decision</code>
     * 
     * @return true if the current decision for the coordiate conflict is <code>decision</code>;
     *  false, otherwise
     */
    public boolean isCoordMergeDecision(MergeDecisionType decision) {
        return coordMergeDecision.equals(decision);
    }

    /**
     * replies true if the current decision for the deleted state conflict is <code>decision</code>
     * 
     * @return true if the current decision for the deleted state conflict is <code>decision</code>;
     *  false, otherwise
     */
    public boolean isDeletedStateDecision(MergeDecisionType decision) {
        return deletedMergeDecision.equals(decision);
    }

    /**
     * populates the model with the differences between my and their version
     * 
     * @param my my version of the primitive
     * @param their their version of the primitive
     */
    public void populate(OsmPrimitive my, OsmPrimitive their) {
        if (my instanceof Node) {
            myCoords = ((Node)my).getCoor();
            theirCoords = ((Node)their).getCoor();
        } else {
            myCoords = null;
            theirCoords = null;
        }

        myDeletedState = my.deleted;
        theirDeletedState = their.deleted;

        coordMergeDecision = UNDECIDED;
        deletedMergeDecision = UNDECIDED;
        setChanged();
        notifyObservers();
        fireCompletelyResolved();
    }


    public LatLon getMyCoords() {
        return myCoords;
    }

    public LatLon getTheirCoords() {
        return theirCoords;
    }

    public LatLon getMergedCoords() {
        switch(coordMergeDecision) {
        case KEEP_MINE: return myCoords;
        case KEEP_THEIR: return theirCoords;
        case UNDECIDED: return null;
        }
        // should not happen
        return null;
    }

    public void decideCoordsConflict(MergeDecisionType decision) {
        coordMergeDecision = decision;
        setChanged();
        notifyObservers();
        fireCompletelyResolved();
    }

    public Boolean getMyDeletedState() {
        return myDeletedState;
    }

    public  Boolean getTheirDeletedState() {
        return theirDeletedState;
    }

    public Boolean getMergedDeletedState() {
        switch(deletedMergeDecision) {
        case KEEP_MINE: return myDeletedState;
        case KEEP_THEIR: return theirDeletedState;
        case UNDECIDED: return null;
        }
        // should not happen
        return null;
    }

    public void decideDeletedStateConflict(MergeDecisionType decision) {
        this.deletedMergeDecision = decision;
        setChanged();
        notifyObservers();
        fireCompletelyResolved();
    }

    public boolean hasCoordConflict() {
        if (myCoords == null && theirCoords != null) return true;
        if (myCoords != null && theirCoords == null) return true;
        if (myCoords == null && theirCoords == null) return false;
        return !myCoords.equalsEpsilon(theirCoords);
    }

    public boolean hasDeletedStateConflict() {
        return myDeletedState != theirDeletedState;
    }

    public boolean isResolvedCompletely() {
        boolean ret = true;
        if (hasCoordConflict()) {
            ret = ret && ! coordMergeDecision.equals(UNDECIDED);
        }
        if (hasDeletedStateConflict()) {
            ret = ret && ! deletedMergeDecision.equals(UNDECIDED);
        }
        return ret;
    }

    public List<Command> buildResolveCommand(OsmPrimitive my, OsmPrimitive their) {
        ArrayList<Command> cmds = new ArrayList<Command>();
        if (hasCoordConflict() && isDecidedCoord()) {
            cmds.add(new CoordinateConflictResolveCommand((Node)my, (Node)their, coordMergeDecision));
        }
        if (hasDeletedStateConflict() && isDecidedDeletedState()) {
            cmds.add(new DeletedStateConflictResolveCommand(my, their, coordMergeDecision));
        }
        return cmds;
    }
}
