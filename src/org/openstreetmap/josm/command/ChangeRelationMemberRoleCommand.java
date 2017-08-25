// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.command;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Collection;
import java.util.Objects;

import javax.swing.Icon;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.DefaultNameFormatter;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Command that changes the role of a relation member
 *
 * @author Teemu Koskinen &lt;teemu.koskinen@mbnet.fi&gt;
 */
public class ChangeRelationMemberRoleCommand extends Command {

    // The relation to be changed
    private final Relation relation;
    // Position of the member
    private final int position;
    // The new role
    private final String newRole;
    // The old role
    private String oldRole;
    // Old value of modified
    private Boolean oldModified;

    /**
     * Constructs a new {@code ChangeRelationMemberRoleCommand}.
     * @param relation The relation to be changed
     * @param position Member position
     * @param newRole New role
     */
    public ChangeRelationMemberRoleCommand(Relation relation, int position, String newRole) {
        this.relation = relation;
        this.position = position;
        this.newRole = newRole;
    }

    /**
     * Constructs a new {@code ChangeRelationMemberRoleCommand}.
     * @param dataSet The data set the role is in
     * @param relation The relation to be changed
     * @param position Member position
     * @param newRole New role
     * @since 12355
     */
    public ChangeRelationMemberRoleCommand(DataSet dataSet, Relation relation, int position, String newRole) {
        super(dataSet);
        this.relation = relation;
        this.position = position;
        this.newRole = newRole;
    }

    @Override
    public boolean executeCommand() {
        if (position >= 0 && position < relation.getMembersCount()) {
            oldRole = relation.getMember(position).getRole();
            if (!newRole.equals(oldRole)) {
                relation.setMember(position, new RelationMember(newRole, relation.getMember(position).getMember()));
                oldModified = relation.isModified();
                relation.setModified(true);
            }
        }
        return true;
    }

    @Override
    public void undoCommand() {
        if (position >= 0 && position < relation.getMembersCount()) {
            relation.setMember(position, new RelationMember(oldRole, relation.getMember(position).getMember()));
            if (oldModified != null) {
                relation.setModified(oldModified);
            }
        }
    }

    @Override
    public void fillModifiedData(Collection<OsmPrimitive> modified, Collection<OsmPrimitive> deleted, Collection<OsmPrimitive> added) {
        modified.add(relation);
    }

    @Override
    public String getDescriptionText() {
        return tr("Change relation member role for {0} {1}",
                OsmPrimitiveType.from(relation),
                relation.getDisplayName(DefaultNameFormatter.getInstance()));
    }

    @Override
    public Icon getDescriptionIcon() {
        return ImageProvider.get(relation.getDisplayType());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), relation, position, newRole, oldRole, oldModified);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        if (!super.equals(obj)) return false;
        ChangeRelationMemberRoleCommand that = (ChangeRelationMemberRoleCommand) obj;
        return position == that.position &&
                Objects.equals(relation, that.relation) &&
                Objects.equals(newRole, that.newRole) &&
                Objects.equals(oldRole, that.oldRole) &&
                Objects.equals(oldModified, that.oldModified);
    }
}
