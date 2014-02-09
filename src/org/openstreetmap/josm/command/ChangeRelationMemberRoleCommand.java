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

    public ChangeRelationMemberRoleCommand(Relation relation, int position, String newRole) {
        super();
        this.relation = relation;
        this.position = position;
        this.newRole = newRole;
    }

    @Override public boolean executeCommand() {
        if (position < 0 || position >= relation.getMembersCount())
            return false;

        oldRole = relation.getMember(position).getRole();
        if (newRole.equals(oldRole)) return true;
        relation.setMember(position, new RelationMember(newRole, relation.getMember(position).getMember()));

        oldModified = relation.isModified();
        relation.setModified(true);
        return true;
    }

    @Override public void undoCommand() {
        relation.setMember(position, new RelationMember(oldRole, relation.getMember(position).getMember()));
        relation.setModified(oldModified);
    }

    @Override public void fillModifiedData(Collection<OsmPrimitive> modified, Collection<OsmPrimitive> deleted, Collection<OsmPrimitive> added) {
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
}
