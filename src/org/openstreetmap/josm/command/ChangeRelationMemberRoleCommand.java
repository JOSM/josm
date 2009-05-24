// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.command;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Collection;

import javax.swing.JLabel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;

import org.openstreetmap.josm.data.osm.visitor.NameVisitor;

/**
 * Command that changes the role of a relation member
 *
 * @author Teemu Koskinen <teemu.koskinen@mbnet.fi>
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
    // Old value of modified;
    private Boolean oldModified;

    public ChangeRelationMemberRoleCommand(Relation relation, int position, String newRole) {
        this.relation = relation;
        this.position = position;
        this.newRole = newRole;
    }

    @Override public boolean executeCommand() {
        if (position < 0 || position >= relation.members.size()) {
            Main.debug("error changing the role");
            return false;
        }

        oldRole = relation.members.get(position).role;
        relation.members.get(position).role = newRole;

        oldModified = relation.modified;
        relation.modified = true;
        return true;
    }

    @Override public void undoCommand() {
        relation.members.get(position).role = oldRole;
        relation.modified = oldModified;
    }

    @Override public void fillModifiedData(Collection<OsmPrimitive> modified, Collection<OsmPrimitive> deleted, Collection<OsmPrimitive> added) {
        modified.add(relation);
    }

    @Override public MutableTreeNode description() {
        NameVisitor v = new NameVisitor();
        relation.visit(v);
        return new DefaultMutableTreeNode(new JLabel(tr("ChangeRelationMemberRole")+" "+tr(v.className)+" "+v.name, v.icon, JLabel.HORIZONTAL));
    }
}
