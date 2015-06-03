// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.command;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Collection;

import javax.swing.Icon;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.gui.DefaultNameFormatter;
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
    private int position = -1;
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

    @Override
    public boolean executeCommand() {
        if (position < 0 || position >= relation.getMembersCount())
            return false;

        oldRole = relation.getMember(position).getRole();
        if (newRole.equals(oldRole)) return true;
        relation.setMember(position, new RelationMember(newRole, relation.getMember(position).getMember()));

        oldModified = relation.isModified();
        relation.setModified(true);
        return true;
    }

    @Override
    public void undoCommand() {
        relation.setMember(position, new RelationMember(oldRole, relation.getMember(position).getMember()));
        relation.setModified(oldModified);
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
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((newRole == null) ? 0 : newRole.hashCode());
        result = prime * result + ((oldModified == null) ? 0 : oldModified.hashCode());
        result = prime * result + ((oldRole == null) ? 0 : oldRole.hashCode());
        result = prime * result + position;
        result = prime * result + ((relation == null) ? 0 : relation.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        ChangeRelationMemberRoleCommand other = (ChangeRelationMemberRoleCommand) obj;
        if (newRole == null) {
            if (other.newRole != null)
                return false;
        } else if (!newRole.equals(other.newRole))
            return false;
        if (oldModified == null) {
            if (other.oldModified != null)
                return false;
        } else if (!oldModified.equals(other.oldModified))
            return false;
        if (oldRole == null) {
            if (other.oldRole != null)
                return false;
        } else if (!oldRole.equals(other.oldRole))
            return false;
        if (position != other.position)
            return false;
        if (relation == null) {
            if (other.relation != null)
                return false;
        } else if (!relation.equals(other.relation))
            return false;
        return true;
    }
}
