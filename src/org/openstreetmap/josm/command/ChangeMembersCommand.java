// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.command;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.DefaultNameFormatter;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;

/**
 * Command that changes the members of a relation.
 * The same can be done with ChangeCommand, but this is more efficient.
 * @author Gerd Petermann
 * @since 17199
 *
 */
public class ChangeMembersCommand extends Command {

    private final Relation relation;
    private final List<RelationMember> cmdMembers;

    /**
     * Constructs a new {@code ChangeMembersCommand} in the context of a given data set.
     * @param data the data set. Must not be null.
     * @param relation the relation
     * @param newMembers the new member list, must not be empty
     */
    public ChangeMembersCommand(DataSet data, Relation relation, List<RelationMember> newMembers) {
        super(data);
        this.relation = Objects.requireNonNull(relation, "relation");
        this.cmdMembers = Objects.requireNonNull(newMembers, "newMembers");
        if (cmdMembers.isEmpty()) {
            throw new IllegalArgumentException("Members collection is empty");
        }
    }

    /**
     * Constructs a new {@code ChangeMembersCommand}  in the context of {@code r} data set.
     * @param relation the relation. It must belong to a data set
     * @param newMembers the new member list, must not be empty
     */
    public ChangeMembersCommand(Relation relation, List<RelationMember> newMembers) {
        this(relation.getDataSet(), relation, newMembers);
    }

    @Override
    public boolean executeCommand() {
        super.executeCommand();
        relation.setMembers(cmdMembers);
        relation.setModified(true);
        return true;

    }

    @Override
    public String getDescriptionText() {
        return tr("Change members of {0}", relation.getDisplayName(DefaultNameFormatter.getInstance()));
    }

    @Override
    public void fillModifiedData(Collection<OsmPrimitive> modified, Collection<OsmPrimitive> deleted,
            Collection<OsmPrimitive> added) {
        modified.add(relation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), relation, cmdMembers);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        if (!super.equals(obj)) return false;
        ChangeMembersCommand that = (ChangeMembersCommand) obj;
        return Objects.equals(relation, that.relation) &&
               Objects.equals(cmdMembers, that.cmdMembers);
    }

}
