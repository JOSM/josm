// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.pair.properties;

import static org.openstreetmap.josm.gui.conflict.pair.MergeDecisionType.UNDECIDED;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Observable;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.CoordinateConflictResolveCommand;
import org.openstreetmap.josm.command.DeletedStateConflictResolveCommand;
import org.openstreetmap.josm.command.PurgePrimitivesCommand;
import org.openstreetmap.josm.command.UndeletePrimitivesCommand;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.conflict.pair.MergeDecisionType;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.io.MultiFetchServerObjectReader;
import org.openstreetmap.josm.io.OsmTransferException;

/**
 * This is the model for resolving conflicts in the properties of the
 * {@see OsmPrimitive}s. In particular, it represents conflicts in the coordiates of {@see Node}s and
 * the deleted or visible state of {@see OsmPrimitive}s.
 *
 * This model is an {@see Observable}. It notifies registered {@see Observer}s whenever the
 * internal state changes.
 *
 * This model also emits property changes for {@see #RESOLVED_COMPLETELY_PROP}. Property change
 * listeners may register themselves using {@see #addPropertyChangeListener(PropertyChangeListener)}.
 *
 * @see Node#getCoor()
 * @see OsmPrimitive#deleted
 * @see OsmPrimitive#visible
 *
 */
public class PropertiesMergeModel extends Observable {

    static public final String RESOLVED_COMPLETELY_PROP = PropertiesMergeModel.class.getName() + ".resolvedCompletely";

    private OsmPrimitive my;

    private LatLon myCoords;
    private LatLon theirCoords;
    private MergeDecisionType coordMergeDecision;

    private boolean myDeletedState;
    private boolean theirDeletedState;
    private boolean myVisibleState;
    private boolean theirVisibleState;
    private MergeDecisionType deletedMergeDecision;
    private MergeDecisionType visibleMergeDecision;
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
     * replies true if there is a coordinate conflict and if this conflict is
     * resolved
     *
     * @return true if there is a coordinate conflict and if this conflict is
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
     * replies true if there is a  conflict in the visible state and if this conflict is
     * resolved
     *
     * @return true if there is a conflict in the visible state and if this conflict is
     * resolved; false, otherwise
     */
    public boolean isDecidedVisibleState() {
        return ! visibleMergeDecision.equals(UNDECIDED);
    }

    /**
     * replies true if the current decision for the coordinate conflict is <code>decision</code>
     *
     * @return true if the current decision for the coordinate conflict is <code>decision</code>;
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
     * replies true if the current decision for the visible state conflict is <code>decision</code>
     *
     * @return true if the current decision for the visible state conflict is <code>decision</code>;
     *  false, otherwise
     */
    public boolean isVisibleStateDecision(MergeDecisionType decision) {
        return visibleMergeDecision.equals(decision);
    }
    /**
     * populates the model with the differences between my and their version
     *
     * @param my my version of the primitive
     * @param their their version of the primitive
     */
    public void populate(OsmPrimitive my, OsmPrimitive their) {
        this.my = my;
        if (my instanceof Node) {
            myCoords = ((Node)my).getCoor();
            theirCoords = ((Node)their).getCoor();
        } else {
            myCoords = null;
            theirCoords = null;
        }

        myDeletedState = my.isDeleted();
        theirDeletedState = their.isDeleted();

        myVisibleState = my.isVisible();
        theirVisibleState = their.isVisible();

        coordMergeDecision = UNDECIDED;
        deletedMergeDecision = UNDECIDED;
        visibleMergeDecision = UNDECIDED;
        setChanged();
        notifyObservers();
        fireCompletelyResolved();
    }

    /**
     * replies the coordinates of my {@see OsmPrimitive}. null, if my primitive hasn't
     * coordinates (i.e. because it is a {@see Way}).
     *
     * @return the coordinates of my {@see OsmPrimitive}. null, if my primitive hasn't
     *  coordinates (i.e. because it is a {@see Way}).
     */
    public LatLon getMyCoords() {
        return myCoords;
    }

    /**
     * replies the coordinates of their {@see OsmPrimitive}. null, if their primitive hasn't
     * coordinates (i.e. because it is a {@see Way}).
     *
     * @return the coordinates of my {@see OsmPrimitive}. null, if my primitive hasn't
     * coordinates (i.e. because it is a {@see Way}).
     */
    public LatLon getTheirCoords() {
        return theirCoords;
    }

    /**
     * replies the coordinates of the merged {@see OsmPrimitive}. null, if the current primitives
     * have no coordinates or if the conflict is yet {@see MergeDecisionType#UNDECIDED}
     *
     * @return the coordinates of the merged {@see OsmPrimitive}. null, if the current primitives
     * have no coordinates or if the conflict is yet {@see MergeDecisionType#UNDECIDED}
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
     * decides a conflict between my and their coordinates
     *
     * @param decision the decision
     */
    public void decideCoordsConflict(MergeDecisionType decision) {
        coordMergeDecision = decision;
        setChanged();
        notifyObservers();
        fireCompletelyResolved();
    }

    /**
     * replies my deleted state,
     * @return
     */
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

    /**
     * replies my visible state,
     * @return my visible state
     */
    public Boolean getMyVisibleState() {
        return myVisibleState;
    }

    /**
     * replies their visible state,
     * @return their visible state
     */
    public  Boolean getTheirVisibleState() {
        return theirVisibleState;
    }

    /**
     * replies the merged visible state; null, if the merge decision is
     * {@see MergeDecisionType#UNDECIDED}.
     *
     * @return the merged visible state
     */
    public Boolean getMergedVisibleState() {
        switch(visibleMergeDecision) {
        case KEEP_MINE: return myVisibleState;
        case KEEP_THEIR: return theirVisibleState;
        case UNDECIDED: return null;
        }
        // should not happen
        return null;
    }

    /**
     * decides the conflict between two deleted states
     * @param decision the decision (must not be null)
     *
     * @throws IllegalArgumentException thrown, if decision is null
     */
    public void decideDeletedStateConflict(MergeDecisionType decision) throws IllegalArgumentException{
        if (decision == null)
            throw new IllegalArgumentException(tr("Parameter ''{0}'' must not be null.", "decision"));
        this.deletedMergeDecision = decision;
        setChanged();
        notifyObservers();
        fireCompletelyResolved();
    }

    /**
     * decides the conflict between two visible states
     * @param decision the decision (must not be null)
     *
     * @throws IllegalArgumentException thrown, if decision is null
     */
    public void decideVisibleStateConflict(MergeDecisionType decision) throws IllegalArgumentException {
        if (decision == null)
            throw new IllegalArgumentException(tr("Parameter ''{0}'' must not be null.", "decision"));
        this.visibleMergeDecision = decision;
        setChanged();
        notifyObservers();
        fireCompletelyResolved();
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
        return !myCoords.equals(theirCoords);
    }

    /**
     * replies true if my and their primitive have a conflict between
     * their deleted states
     *
     * @return true if my and their primitive have a conflict between
     * their deleted states
     */
    public boolean hasDeletedStateConflict() {
        return myDeletedState != theirDeletedState;
    }

    /**
     * replies true if my and their primitive have a conflict between
     * their visible states
     *
     * @return true if my and their primitive have a conflict between
     * their visible states
     */
    public boolean hasVisibleStateConflict() {
        return myVisibleState != theirVisibleState;
    }

    /**
     * replies true if all conflict in this model are resolved
     *
     * @return true if all conflict in this model are resolved; false otherwise
     */
    public boolean isResolvedCompletely() {
        boolean ret = true;
        if (hasCoordConflict()) {
            ret = ret && ! coordMergeDecision.equals(UNDECIDED);
        }
        if (hasDeletedStateConflict()) {
            ret = ret && ! deletedMergeDecision.equals(UNDECIDED);
        }
        if (hasVisibleStateConflict()) {
            ret = ret && ! visibleMergeDecision.equals(UNDECIDED);
        }
        return ret;
    }

    /**
     * builds the command(s) to apply the conflict resolutions to my primitive
     *
     * @param my  my primitive
     * @param their their primitive
     * @return the list of commands
     */
    public List<Command> buildResolveCommand(OsmPrimitive my, OsmPrimitive their) throws OperationCancelledException{
        ArrayList<Command> cmds = new ArrayList<Command>();
        if (hasVisibleStateConflict() && isDecidedVisibleState()) {
            if (isVisibleStateDecision(MergeDecisionType.KEEP_MINE)) {
                try {
                    UndeletePrimitivesCommand cmd = createUndeletePrimitiveCommand(my);
                    if (cmd == null)
                        throw new OperationCancelledException();
                    cmds.add(cmd);
                } catch(OsmTransferException e) {
                    handleExceptionWhileBuildingCommand(e);
                    throw new OperationCancelledException(e);
                }
            } else if (isVisibleStateDecision(MergeDecisionType.KEEP_THEIR)) {
                cmds.add(new PurgePrimitivesCommand(my));
            }
        }
        if (hasCoordConflict() && isDecidedCoord()) {
            cmds.add(new CoordinateConflictResolveCommand((Node)my, (Node)their, coordMergeDecision));
        }
        if (hasDeletedStateConflict() && isDecidedDeletedState()) {
            cmds.add(new DeletedStateConflictResolveCommand(my, their, deletedMergeDecision));
        }
        return cmds;
    }

    public OsmPrimitive getMyPrimitive() {
        return my;
    }

    /**
     *
     * @param id
     */
    protected void handleExceptionWhileBuildingCommand(Exception e) {
        e.printStackTrace();
        String msg = e.getMessage() != null ? e.getMessage() : e.toString();
        msg = msg.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;");
        JOptionPane.showMessageDialog(
                Main.parent,
                tr("<html>An error occurred while communicating with the server<br>"
                        + "Details: {0}</html>",
                        msg
                ),
                tr("Communication with server failed"),
                JOptionPane.ERROR_MESSAGE
        );
    }

    /**
     * User has decided to keep his local version of a primitive which had been deleted
     * on the server
     *
     * @param id the primitive id
     */
    protected UndeletePrimitivesCommand createUndeletePrimitiveCommand(OsmPrimitive my) throws OsmTransferException {
        if (my instanceof Node)
            return createUndeleteNodeCommand((Node)my);
        else if (my instanceof Way)
            return createUndeleteWayCommand((Way)my);
        else if (my instanceof Relation)
            return createUndeleteRelationCommand((Relation)my);
        return null;
    }
    /**
     * Undelete a node which is already deleted on the server. The API
     * doesn't offer a call for "undeleting" a node. We therefore create
     * a clone of the node which we flag as new. On the next upload the
     * server will assign the node a new id.
     *
     * @param node the node to undelete
     */
    protected UndeletePrimitivesCommand  createUndeleteNodeCommand(Node node) {
        return new UndeletePrimitivesCommand(node);
    }

    /**
     * displays a confirmation message. The user has to confirm that additional dependent
     * nodes should be undeleted too.
     *
     * @param way  the way
     * @param dependent a list of dependent nodes which have to be undelete too
     * @return true, if the user confirms; false, otherwise
     */
    protected boolean confirmUndeleteDependentPrimitives(Way way, ArrayList<OsmPrimitive> dependent) {
        String [] options = {
                tr("Yes, undelete them too"),
                tr("No, cancel operation")
        };
        int ret = JOptionPane.showOptionDialog(
                Main.parent,
                tr("<html>There are {0} additional nodes used by way {1}<br>"
                        + "which are deleted on the server.<br>"
                        + "<br>"
                        + "Do you want to undelete these nodes too?</html>",
                        Long.toString(dependent.size()), Long.toString(way.getId())),
                        tr("Undelete additional nodes?"),
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        options,
                        options[0]
        );

        switch(ret) {
        case JOptionPane.CLOSED_OPTION: return false;
        case JOptionPane.YES_OPTION: return true;
        case JOptionPane.NO_OPTION: return false;
        }
        return false;

    }

    protected boolean confirmUndeleteDependentPrimitives(Relation r, ArrayList<OsmPrimitive> dependent) {
        String [] options = {
                tr("Yes, undelete them too"),
                tr("No, cancel operation")
        };
        int ret = JOptionPane.showOptionDialog(
                Main.parent,
                tr("<html>There are {0} additional primitives referred to by relation {1}<br>"
                        + "which are deleted on the server.<br>"
                        + "<br>"
                        + "Do you want to undelete them too?</html>",
                        Long.toString(dependent.size()), Long.toString(r.getId())),
                        tr("Undelete dependent primitives?"),
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        options,
                        options[0]
        );

        switch(ret) {
        case JOptionPane.CLOSED_OPTION: return false;
        case JOptionPane.YES_OPTION: return true;
        case JOptionPane.NO_OPTION: return false;
        }
        return false;

    }

    /**
     * Creates the undelete command for a way which is already deleted on the server.
     *
     * This method also checks whether there are additional nodes referred to by
     * this way which are deleted on the server too.
     *
     * @param way the way to undelete
     * @return the undelete command
     * @see #createUndeleteNodeCommand(Node)
     */
    protected UndeletePrimitivesCommand createUndeleteWayCommand(final Way way) throws OsmTransferException {

        HashMap<Long,OsmPrimitive> candidates = new HashMap<Long,OsmPrimitive>();
        for (Node n : way.getNodes()) {
            if (!n.isNew() && !candidates.values().contains(n)) {
                candidates.put(n.getId(), n);
            }
        }
        MultiFetchServerObjectReader reader = new MultiFetchServerObjectReader();
        reader.append(candidates.values());
        DataSet ds = reader.parseOsm(NullProgressMonitor.INSTANCE);

        ArrayList<OsmPrimitive> toDelete = new ArrayList<OsmPrimitive>();
        for (OsmPrimitive their : ds.allPrimitives()) {
            if (candidates.keySet().contains(their.getId()) && ! their.isVisible()) {
                toDelete.add(candidates.get(their.getId()));
            }
        }
        if (!toDelete.isEmpty()) {
            if (! confirmUndeleteDependentPrimitives(way, toDelete))
                // FIXME: throw exception ?
                return null;
        }
        toDelete.add(way);
        return new UndeletePrimitivesCommand(toDelete);
    }

    /**
     * Creates an undelete command for a relation which is already deleted on the server.
     *
     * This method  checks whether there are additional primitives referred to by
     * this relation which are already deleted on the server.
     *
     * @param r the relation
     * @return the undelete command
     * @see #createUndeleteNodeCommand(Node)
     */
    protected UndeletePrimitivesCommand createUndeleteRelationCommand(final Relation r) throws OsmTransferException {

        HashMap<Long,OsmPrimitive> candidates = new HashMap<Long, OsmPrimitive>();
        for (RelationMember m : r.getMembers()) {
            if (!m.getMember().isNew() && !candidates.values().contains(m.getMember())) {
                candidates.put(m.getMember().getId(), m.getMember());
            }
        }

        MultiFetchServerObjectReader reader = new MultiFetchServerObjectReader();
        reader.append(candidates.values());
        DataSet ds = reader.parseOsm(NullProgressMonitor.INSTANCE);

        ArrayList<OsmPrimitive> toDelete = new ArrayList<OsmPrimitive>();
        for (OsmPrimitive their : ds.allPrimitives()) {
            if (candidates.keySet().contains(their.getId()) && ! their.isVisible()) {
                toDelete.add(candidates.get(their.getId()));
            }
        }
        if (!toDelete.isEmpty()) {
            if (! confirmUndeleteDependentPrimitives(r, toDelete))
                // FIXME: throw exception ?
                return null;
        }
        toDelete.add(r);
        return new UndeletePrimitivesCommand(toDelete);
    }

}
