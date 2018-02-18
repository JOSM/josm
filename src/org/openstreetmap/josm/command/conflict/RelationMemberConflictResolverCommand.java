// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.command.conflict;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import javax.swing.Icon;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.conflict.Conflict;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Logging;

/**
 * Represents the resolution of conflicts in the member list of two {@link Relation}s.
 * @since 1631
 */
public class RelationMemberConflictResolverCommand extends ConflictResolveCommand {
    /** the conflict to resolve */
    private final Conflict<Relation> conflict;
    /** the list of merged nodes. This becomes the list of news of my way after the command is executed */
    private final List<RelationMember> mergedMembers;

    /**
     * Constructs a new {@code RelationMemberConflictResolverCommand}.
     * @param conflict the conflict to resolve
     * @param mergedMembers the list of merged relation members
     */
    @SuppressWarnings("unchecked")
    public RelationMemberConflictResolverCommand(Conflict<? extends OsmPrimitive> conflict, List<RelationMember> mergedMembers) {
        super(conflict.getMy().getDataSet());
        this.conflict = (Conflict<Relation>) conflict;
        this.mergedMembers = mergedMembers;
    }

    @Override
    public String getDescriptionText() {
        return tr("Resolve conflicts in member list of relation {0}", conflict.getMy().getId());
    }

    @Override
    public Icon getDescriptionIcon() {
        return ImageProvider.get("data", "object");
    }

    @Override
    public boolean executeCommand() {
        // remember the current state of 'my' way
        //
        super.executeCommand();

        // replace the list of members of 'my' relation by the list of merged members
        //
        conflict.getMy().setMembers(mergedMembers);

        return true;
    }

    @Override
    public void fillModifiedData(Collection<OsmPrimitive> modified, Collection<OsmPrimitive> deleted,
            Collection<OsmPrimitive> added) {
        modified.add(conflict.getMy());
    }

    @Override
    public void undoCommand() {
        DataSet ds = getAffectedDataSet();
        if (!Main.main.containsDataSet(ds)) {
            Logging.warn(tr("Cannot undo command ''{0}'' because layer ''{1}'' is not present any more",
                    this.toString(),
                    ds.getName()
            ));
            return;
        }

        Main.main.setActiveDataSet(ds);

        // restore the former state
        //
        super.undoCommand();

        // restore a conflict if necessary
        //
        if (!ds.getConflicts().hasConflictForMy(conflict.getMy())) {
            ds.getConflicts().add(conflict);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), conflict, mergedMembers);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        if (!super.equals(obj)) return false;
        RelationMemberConflictResolverCommand that = (RelationMemberConflictResolverCommand) obj;
        return Objects.equals(conflict, that.conflict) &&
               Objects.equals(mergedMembers, that.mergedMembers);
    }
}
