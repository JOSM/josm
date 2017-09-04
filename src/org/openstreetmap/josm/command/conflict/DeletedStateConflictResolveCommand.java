// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.command.conflict;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;

import javax.swing.Icon;

import org.openstreetmap.josm.data.conflict.Conflict;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.conflict.pair.MergeDecisionType;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Represents the resolution of a conflict between the deleted flag of two {@link OsmPrimitive}s.
 * @since 1654
 */
public class DeletedStateConflictResolveCommand extends ConflictResolveCommand {

    /** the conflict to resolve */
    private final Conflict<? extends OsmPrimitive> conflict;

    /** the merge decision */
    private final MergeDecisionType decision;

    /**
     * Constructs a new {@code DeletedStateConflictResolveCommand}.
     *
     * @param conflict the conflict data set
     * @param decision the merge decision
     */
    public DeletedStateConflictResolveCommand(Conflict<? extends OsmPrimitive> conflict, MergeDecisionType decision) {
        super(conflict.getMy().getDataSet());
        this.conflict = conflict;
        this.decision = decision;
    }

    @Override
    public String getDescriptionText() {
        return tr("Resolve conflicts in deleted state in {0}", conflict.getMy().getId());
    }

    @Override
    public Icon getDescriptionIcon() {
        return ImageProvider.get("data", "object");
    }

    @Override
    public boolean executeCommand() {
        // remember the current state of modified primitives, i.e. of OSM primitive 'my'
        super.executeCommand();

        if (decision.equals(MergeDecisionType.KEEP_MINE)) {
            if (conflict.getMy().isDeleted() || conflict.isMyDeleted()) {
                // because my was involved in a conflict it my still be referred
                // to from a way or a relation. Fix this now.
                deleteMy();
            }
        } else if (decision.equals(MergeDecisionType.KEEP_THEIR)) {
            if (conflict.getTheir().isDeleted()) {
                deleteMy();
            } else {
                conflict.getMy().setDeleted(false);
            }
        } else
            // should not happen
            throw new IllegalStateException(tr("Cannot resolve undecided conflict."));

        rememberConflict(conflict);
        return true;
    }

    private void deleteMy() {
        Set<OsmPrimitive> referrers = getAffectedDataSet().unlinkReferencesToPrimitive(conflict.getMy());
        for (OsmPrimitive p : referrers) {
            if (!p.isNew() && !p.isDeleted()) {
                p.setModified(true);
            }
        }
        conflict.getMy().setDeleted(true);
    }

    @Override
    public void fillModifiedData(Collection<OsmPrimitive> modified, Collection<OsmPrimitive> deleted,
            Collection<OsmPrimitive> added) {
        modified.add(conflict.getMy());
        modified.addAll(conflict.getMy().getReferrers());
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
        DeletedStateConflictResolveCommand that = (DeletedStateConflictResolveCommand) obj;
        return decision == that.decision && Objects.equals(conflict, that.conflict);
    }
}
