// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.command.conflict;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Collection;
import java.util.Objects;

import javax.swing.Icon;

import org.openstreetmap.josm.data.conflict.Conflict;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.conflict.pair.MergeDecisionType;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Represents the resolution of a conflict between the coordinates of two {@link Node}s.
 *
 */
public class CoordinateConflictResolveCommand extends ConflictResolveCommand {

    /** the conflict to resolve */
    private final Conflict<? extends OsmPrimitive> conflict;

    /** the merge decision */
    private final MergeDecisionType decision;

    /**
     * constructor for coordinate conflict
     *
     * @param conflict the conflict data set
     * @param decision the merge decision
     */
    public CoordinateConflictResolveCommand(Conflict<? extends OsmPrimitive> conflict, MergeDecisionType decision) {
        super(conflict.getMy().getDataSet());
        this.conflict = conflict;
        this.decision = decision;
    }

    @Override
    public String getDescriptionText() {
        return tr("Resolve conflicts in coordinates in {0}", conflict.getMy().getId());
    }

    @Override
    public Icon getDescriptionIcon() {
        return ImageProvider.get("data", "object");
    }

    @Override
    public boolean executeCommand() {
        // remember the current state of modified primitives, i.e. of OSM primitive 'my'
        super.executeCommand();

        if (decision == MergeDecisionType.KEEP_MINE) {
            // do nothing
        } else if (decision == MergeDecisionType.KEEP_THEIR) {
            Node my = (Node) conflict.getMy();
            Node their = (Node) conflict.getTheir();
            my.setCoor(their.getCoor());
        } else
            // should not happen
            throw new IllegalStateException(tr("Cannot resolve undecided conflict."));

        // remember the layer this command was applied to
        rememberConflict(conflict);

        return true;
    }

    @Override
    public void fillModifiedData(Collection<OsmPrimitive> modified, Collection<OsmPrimitive> deleted,
            Collection<OsmPrimitive> added) {
        modified.add(conflict.getMy());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), conflict, decision);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        if (!super.equals(obj)) return false;
        CoordinateConflictResolveCommand that = (CoordinateConflictResolveCommand) obj;
        return decision == that.decision && Objects.equals(conflict, that.conflict);
    }
}
